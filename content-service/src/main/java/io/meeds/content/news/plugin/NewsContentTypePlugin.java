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
package io.meeds.content.news.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.plugin.ContentTypePlugin;
import io.meeds.content.utils.ContentUtils;

@Component
public class NewsContentTypePlugin implements ContentTypePlugin {

  @Autowired
  private NewsService     newsService;

  @Autowired
  private SpaceService    spaceService;

  @Autowired
  private IdentityManager identityManager;

  @Autowired
  private AttachmentService attachmentService;

  @Override
  public String getType() {
    return ContentUtils.CONTENT_TYPE_NEWS;
  }

  @Override
  public String getLabelKey() {
    return "content.list.filter.contentType.news";
  }

  @Override
  public int getOrder() {
    return 10;
  }

  @Override
  public List<ContentEntry> search(ContentFilter filter,
                                   int fetchLimit,
                                   Identity currentIdentity,
                                   Set<String> categoryLinkedIds) throws Exception {
    if (categoryLinkedIds != null && CollectionUtils.isEmpty(categoryLinkedIds)) {
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
    String status = effectiveStatus(filter.getStatus());
    return newsList.stream()
                   .filter(Objects::nonNull)
                   .filter(news -> categoryLinkedIds == null || categoryLinkedIds.contains(news.getId()))
                   .map(news -> toContentEntry(news, status))
                   .collect(Collectors.toList());
  }

  @Override
  public void delete(String id, String status, Identity currentIdentity) throws Exception {
    String newsObjectType = StringUtils.equals(status, ContentUtils.STATUS_DRAFT) ? NewsUtils.NewsObjectType.LATEST_DRAFT.name()
                                                                                   : NewsUtils.NewsObjectType.ARTICLE.name();
    newsService.deleteNews(id, currentIdentity, newsObjectType);
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
    }
    // Published status: leave every flag unset. NewsFilter.publishedNews is
    // NOT "generic published" - NewsRest.buildFilter maps it to the narrow
    // FilterType.PINNED feature. The genuine "posted/published articles"
    // query is NewsService.getNews's own default fall-through
    // (getPostedArticles), reached only when no flag is set.
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
    entry.setSummary(resolveNewsSummary(news));
    entry.setIllustrationUrl(news.getIllustrationURL());
    entry.setUrl(news.getUrl());
    entry.setAuthorUsername(news.getAuthor());
    entry.setAuthorDisplayName(news.getAuthorDisplayName());
    entry.setAuthorAvatarUrl(news.getAuthorAvatarUrl());
    entry.setSpaceId(news.getSpaceId());
    entry.setSpaceDisplayName(news.getSpaceDisplayName());
    entry.setSpaceAvatarUrl(news.getSpaceAvatarUrl());
    entry.setDate(news.getUpdateDate());
    entry.setLikesCount(news.getLikesCount());
    entry.setCommentsCount(news.getCommentsCount());
    entry.setViewsCount(news.getViewsCount() == null ? 0 : news.getViewsCount());
    entry.setAttachmentsCount(attachmentService.getAttachmentFileIds(NewsPageAttachmentPlugin.OBJECT_TYPE, news.getId()).size());
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

  private String resolveNewsSummary(News news) {
    String summary = news.getProperties() != null ? news.getProperties().getSummary() : null;
    if (StringUtils.isBlank(summary) && StringUtils.isNotBlank(news.getBody())) {
      String text = org.exoplatform.wiki.utils.Utils.html2text(news.getBody());
      summary = text.length() > 200 ? text.substring(0, 200) : text;
    }
    return summary;
  }

}
