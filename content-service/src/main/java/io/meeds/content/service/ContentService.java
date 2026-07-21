/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.content.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.utils.ContentUtils;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

/**
 * Merges content coming from several content-producing addons (News
 * articles, Notes) into a single, uniform, paginated list. News and Notes
 * are each queried independently then merged/sorted/sliced in memory: this
 * is an accepted MVP tradeoff (see the eXIP7.3.0.12 technical spec's Volume
 * & Performance section) since neither side shares a common
 * filter/pagination type today.
 */
@Service
public class ContentService {

  // Upper bound on how many objects linked to a category are considered when
  // filtering the merged list by category. Combining a category filter with
  // News/Notes' own independent pagination has no exact solution without a
  // shared index (see the eXIP7.3.0.12 tech spec's Volume & Performance
  // section); this cap keeps it correct for any reasonably-sized category.
  private static final int CATEGORY_LINKS_FETCH_CAP = 500;

  @Autowired
  private NewsService         newsService;

  @Autowired
  private NoteService         noteService;

  @Autowired
  private SpaceService        spaceService;

  @Autowired
  private IdentityManager     identityManager;

  @Autowired
  private CategoryLinkService categoryLinkService;

  public List<ContentEntry> getContentList(ContentFilter filter, Identity currentIdentity) throws Exception {
    int offset = filter.getOffset();
    int limit = filter.getLimit();
    boolean byCategory = filter.getCategoryId() != null;
    // Each source is fetched from 0 up to a bound since neither source can be
    // paginated against the other's results ahead of the merge: when
    // filtering by category, the bound is the category-links cap (so any
    // in-category item can surface regardless of its date-sort position);
    // otherwise it is simply (offset + limit).
    int fetchLimit = byCategory ? CATEGORY_LINKS_FETCH_CAP : offset + limit;
    Map<String, Set<String>> categoryLinkedIds = byCategory ? resolveCategoryLinkedIds(filter) : null;

    List<ContentEntry> entries = new ArrayList<>();
    if (filter.hasContentType(ContentUtils.CONTENT_TYPE_NEWS)) {
      entries.addAll(getNewsEntries(filter, fetchLimit, currentIdentity, categoryLinkedIds));
    }
    if (filter.hasContentType(ContentUtils.CONTENT_TYPE_NOTES)) {
      entries.addAll(getNoteEntries(filter, fetchLimit, currentIdentity, categoryLinkedIds));
    }
    // "event" content type has no backing service yet (content has no
    // dependency on agenda) - see ContentUtils.CONTENT_TYPE_EVENT.

    entries.sort(Comparator.comparing(ContentEntry::getDate, Comparator.nullsLast(Comparator.<Date> reverseOrder())));
    return entries.stream().skip(offset).limit(limit).collect(Collectors.toList());
  }

  public void deleteContent(String id, String contentType, String status, Identity currentIdentity) throws Exception {
    if (StringUtils.equals(contentType, ContentUtils.CONTENT_TYPE_NOTES)) {
      Page note = noteService.getNoteById(id, currentIdentity);
      if (note == null) {
        throw new ObjectNotFoundException("Content with id " + id + " was not found");
      }
      if (!noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, currentIdentity)) {
        throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to delete note " + id);
      }
      noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), note.getName(), currentIdentity);
    } else {
      String newsObjectType = StringUtils.equals(status, ContentUtils.STATUS_DRAFT) ? NewsUtils.NewsObjectType.LATEST_DRAFT.name()
                                                                                     : NewsUtils.NewsObjectType.ARTICLE.name();
      newsService.deleteNews(id, currentIdentity, newsObjectType);
    }
  }

  private Map<String, Set<String>> resolveCategoryLinkedIds(ContentFilter filter) {
    List<String> types = new ArrayList<>();
    if (filter.hasContentType(ContentUtils.CONTENT_TYPE_NEWS)) {
      types.add(ContentUtils.CONTENT_TYPE_NEWS);
    }
    if (filter.hasContentType(ContentUtils.CONTENT_TYPE_NOTES)) {
      types.add(ContentUtils.CONTENT_TYPE_NOTES);
    }
    if (types.isEmpty()) {
      return Collections.emptyMap();
    }
    List<CategoryObject> linkedObjects = categoryLinkService.getLinkedObjects(filter.getCategoryId(),
                                                                              types,
                                                                              0,
                                                                              CATEGORY_LINKS_FETCH_CAP);
    Map<String, Set<String>> linkedIdsByType = new HashMap<>();
    for (CategoryObject linkedObject : linkedObjects) {
      linkedIdsByType.computeIfAbsent(linkedObject.getType(), key -> new HashSet<>()).add(linkedObject.getId());
    }
    return linkedIdsByType;
  }

  private List<ContentEntry> getNewsEntries(ContentFilter filter,
                                            int fetchLimit,
                                            Identity currentIdentity,
                                            Map<String, Set<String>> categoryLinkedIds) throws Exception {
    Set<String> allowedIds = categoryLinkedIds == null ? null : categoryLinkedIds.get(ContentUtils.CONTENT_TYPE_NEWS);
    if (categoryLinkedIds != null && CollectionUtils.isEmpty(allowedIds)) {
      return Collections.emptyList();
    }

    NewsFilter newsFilter = toNewsFilter(filter, fetchLimit, currentIdentity.getUserId());
    List<News> newsList;
    if (StringUtils.isNotBlank(filter.getSearchText())) {
      org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                          identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                              currentIdentity.getUserId());
      newsList = newsService.searchNews(newsFilter, socialIdentity);
    } else {
      newsList = newsService.getNews(newsFilter, currentIdentity);
    }
    if (newsList == null) {
      return Collections.emptyList();
    }
    return newsList.stream()
                   .filter(Objects::nonNull)
                   .filter(news -> allowedIds == null || allowedIds.contains(news.getId()))
                   .map(news -> toContentEntry(news, effectiveStatus(filter.getStatus())))
                   .collect(Collectors.toList());
  }

  private List<ContentEntry> getNoteEntries(ContentFilter filter,
                                            int fetchLimit,
                                            Identity currentIdentity,
                                            Map<String, Set<String>> categoryLinkedIds) throws Exception {
    String status = effectiveStatus(filter.getStatus());
    if (StringUtils.equals(status, ContentUtils.STATUS_SCHEDULED) || StringUtils.equals(status, ContentUtils.STATUS_DRAFT)) {
      // Regular notes have no publish workflow (no scheduled/draft state).
      return Collections.emptyList();
    }
    Set<String> allowedIds = categoryLinkedIds == null ? null : categoryLinkedIds.get(ContentUtils.CONTENT_TYPE_NOTES);
    if (categoryLinkedIds != null && CollectionUtils.isEmpty(allowedIds)) {
      return Collections.emptyList();
    }

    WikiSearchData searchData = new WikiSearchData(null, null, null, null, null);
    if (StringUtils.isNotBlank(filter.getSearchText())) {
      searchData.setTitle(filter.getSearchText());
      searchData.setContent(filter.getSearchText());
    }
    if (StringUtils.equals(status, ContentUtils.STATUS_MY_CONTENT)) {
      searchData.setUserId(currentIdentity.getUserId());
    }
    if (CollectionUtils.isNotEmpty(filter.getSpaces())) {
      searchData.setSpaceIds(filter.getSpaces());
    }
    searchData.setOffset(0);
    searchData.setLimit(fetchLimit);
    searchData.setSortField("updatedDate");
    searchData.setSortDirection("desc");

    List<SearchResult> results = noteService.search(searchData).getAll();
    if (CollectionUtils.isEmpty(results)) {
      return Collections.emptyList();
    }
    List<ContentEntry> entries = new ArrayList<>();
    for (SearchResult result : results) {
      String noteId = String.valueOf(result.getId());
      if (allowedIds != null && !allowedIds.contains(noteId)) {
        continue;
      }
      try {
        Page note = noteService.getNoteById(noteId, currentIdentity);
        if (note != null) {
          entries.add(toContentEntry(note, currentIdentity));
        }
      } catch (IllegalAccessException e) {
        // Current user cannot view this note, skip it.
      }
    }
    return entries;
  }

  private NewsFilter toNewsFilter(ContentFilter filter, int fetchLimit, String currentUser) {
    NewsFilter newsFilter = new NewsFilter();
    boolean textSearch = StringUtils.isNotBlank(filter.getSearchText());
    newsFilter.setSpaces(resolveNewsSpaces(filter.getSpaces(), textSearch));

    String status = effectiveStatus(filter.getStatus());
    if (StringUtils.equals(status, ContentUtils.STATUS_MY_CONTENT)) {
      newsFilter.setAuthor(currentUser);
    } else if (StringUtils.equals(status, ContentUtils.STATUS_SCHEDULED)) {
      newsFilter.setAuthor(currentUser);
      newsFilter.setScheduledNews(true);
    } else if (StringUtils.equals(status, ContentUtils.STATUS_DRAFT)) {
      newsFilter.setAuthor(currentUser);
      newsFilter.setDraftNews(true);
    } else {
      newsFilter.setPublishedNews(true);
    }
    if (textSearch) {
      newsFilter.setSearchText(filter.getSearchText());
    }
    newsFilter.setOffset(0);
    newsFilter.setLimit(fetchLimit);
    newsFilter.setSortField("UPDATED_DATE");
    return newsFilter;
  }

  private List<String> resolveNewsSpaces(List<String> spaceIds, boolean textSearch) {
    if (CollectionUtils.isEmpty(spaceIds)) {
      return Collections.emptyList();
    }
    if (!textSearch) {
      return spaceIds;
    }
    // The ES-backed news search expects space identity ids, not space ids.
    List<String> identityIds = new ArrayList<>();
    for (String spaceId : spaceIds) {
      Space space = spaceService.getSpaceById(spaceId);
      if (space != null) {
        org.exoplatform.social.core.identity.model.Identity spaceIdentity =
                                                                           identityManager.getOrCreateSpaceIdentity(space.getPrettyName());
        if (spaceIdentity != null) {
          identityIds.add(String.valueOf(spaceIdentity.getIdentityId()));
        }
      }
    }
    return identityIds;
  }

  private String effectiveStatus(String status) {
    return StringUtils.isBlank(status) ? ContentUtils.STATUS_PUBLISHED : status;
  }

  private ContentEntry toContentEntry(News news, String status) {
    ContentEntry entry = new ContentEntry();
    entry.setId(news.getId());
    entry.setContentType(ContentUtils.CONTENT_TYPE_NEWS);
    entry.setIcon("fa-newspaper");
    entry.setTitle(news.getTitle());
    entry.setSummary(news.getProperties() == null ? null : news.getProperties().getSummary());
    entry.setIllustrationUrl(news.getIllustrationURL());
    entry.setUrl(news.getUrl());
    entry.setAuthorDisplayName(news.getAuthorDisplayName());
    entry.setAuthorAvatarUrl(news.getAuthorAvatarUrl());
    entry.setSpaceId(news.getSpaceId());
    entry.setSpaceDisplayName(news.getSpaceDisplayName());
    entry.setSpaceAvatarUrl(news.getSpaceAvatarUrl());
    entry.setDate(news.getUpdateDate());
    entry.setLikesCount(news.getLikesCount());
    entry.setCommentsCount(news.getCommentsCount());
    entry.setViewsCount(news.getViewsCount() == null ? 0 : news.getViewsCount());
    entry.setCategoryIds(news.getCategories());
    entry.setPublished(StringUtils.equals(status, ContentUtils.STATUS_PUBLISHED)
        || StringUtils.equals(status, ContentUtils.STATUS_MY_CONTENT));
    entry.setDraft(StringUtils.equals(status, ContentUtils.STATUS_DRAFT));
    entry.setScheduled(StringUtils.equals(status, ContentUtils.STATUS_SCHEDULED));
    entry.setCanEdit(news.isCanEdit());
    entry.setCanDelete(news.isCanDelete());
    entry.setCanPublish(news.isCanPublish());
    entry.setCanSchedule(news.isCanSchedule());
    return entry;
  }

  private ContentEntry toContentEntry(Page note, Identity currentIdentity) {
    ContentEntry entry = new ContentEntry();
    entry.setId(note.getId());
    entry.setContentType(ContentUtils.CONTENT_TYPE_NOTES);
    entry.setIcon("fa-clipboard");
    entry.setTitle(note.getTitle());
    entry.setSummary(resolveNoteSummary(note));
    entry.setUrl(note.getUrl());
    entry.setAuthorDisplayName(note.getAuthorFullName());
    if (StringUtils.isNotBlank(note.getAuthor())) {
      org.exoplatform.social.core.identity.model.Identity authorIdentity = identityManager.getOrCreateUserIdentity(note.getAuthor());
      if (authorIdentity != null && authorIdentity.getProfile() != null) {
        entry.setAuthorAvatarUrl(authorIdentity.getProfile().getAvatarUrl());
      }
    }
    Space space = StringUtils.isBlank(note.getWikiOwner()) ? null : spaceService.getSpaceByGroupId(note.getWikiOwner());
    if (space != null) {
      entry.setSpaceId(space.getId());
      entry.setSpaceDisplayName(space.getDisplayName());
      entry.setSpaceAvatarUrl(space.getAvatarUrl());
    }
    entry.setDate(note.getUpdatedDate());
    entry.setCategoryIds(note.getCategoryIds());
    entry.setPublished(true);
    entry.setDraft(false);
    entry.setScheduled(false);
    entry.setCanEdit(noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, currentIdentity));
    entry.setCanDelete(entry.isCanEdit());
    entry.setCanPublish(false);
    entry.setCanSchedule(false);
    return entry;
  }

  private String resolveNoteSummary(Page note) {
    String summary = note.getProperties() != null ? note.getProperties().getSummary() : null;
    if (StringUtils.isBlank(summary) && StringUtils.isNotBlank(note.getContent())) {
      String text = org.exoplatform.wiki.utils.Utils.html2text(note.getContent());
      summary = text.length() > 200 ? text.substring(0, 200) : text;
    }
    return summary;
  }

}
