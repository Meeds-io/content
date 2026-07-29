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
package io.meeds.content.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
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
import io.meeds.content.utils.ContentUtils;
import io.meeds.notes.plugin.NoteCategoryPlugin;

@Component
public class NoteContentTypePlugin implements ContentTypePlugin {

  @Autowired
  private NoteService     noteService;

  @Autowired
  private SpaceService    spaceService;

  @Autowired
  private IdentityManager identityManager;

  @Autowired
  private AttachmentService attachmentService;

  @Override
  public String getType() {
    return ContentUtils.CONTENT_TYPE_NOTES;
  }

  @Override
  public String getLabelKey() {
    return "content.list.filter.contentType.notes";
  }

  @Override
  public int getOrder() {
    return 30;
  }

  @Override
  public List<ContentEntry> search(ContentFilter filter,
                                   int fetchLimit,
                                   Identity currentIdentity,
                                   Set<String> categoryLinkedIds,
                                   Set<String> activityLinkedIds) throws Exception {
    String status = effectiveStatus(filter.getStatus());
    if (StringUtils.equals(status, ContentUtils.STATUS_SCHEDULED) || StringUtils.equals(status, ContentUtils.STATUS_DRAFT)) {
      // Regular notes have no publish workflow (no scheduled/draft state).
      return Collections.emptyList();
    }
    if (categoryLinkedIds != null && CollectionUtils.isEmpty(categoryLinkedIds) && CollectionUtils.isEmpty(activityLinkedIds)) {
      return Collections.emptyList();
    }

    WikiSearchData searchData = new WikiSearchData(null, null, null, null, null);
    if (StringUtils.isNotBlank(filter.getSearchText())) {
      searchData.setTitle(filter.getSearchText());
      searchData.setContent(filter.getSearchText());
    }
    // Required on every call, regardless of status: the ES connector uses it
    // to resolve the searching user's own space memberships for ACL-based
    // result filtering (WikiElasticSearchServiceConnector.getUserSpaceIds) -
    // it is not an author filter, hence the separate author check below.
    searchData.setUserId(currentIdentity.getUserId());
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
      // Only a note's own id is known before fetching its Page - when there
      // is no Activity-linked bucket to also check, a plain id mismatch can
      // be ruled out without fetching. Once published though, a note's own
      // category link is redirected onto its Activity (see
      // NoteCategoryPlugin#toCategoryObject), so its activityId (known only
      // once fetched) must still be checked whenever that bucket is non-empty.
      if (categoryLinkedIds != null && CollectionUtils.isEmpty(activityLinkedIds) && !categoryLinkedIds.contains(noteId)) {
        continue;
      }
      try {
        Page note = noteService.getNoteById(noteId, currentIdentity);
        if (note != null
            && (categoryLinkedIds == null
                || categoryLinkedIds.contains(noteId)
                || (StringUtils.isNotBlank(note.getActivityId()) && activityLinkedIds.contains(note.getActivityId())))
            && (!StringUtils.equals(status, ContentUtils.STATUS_MY_CONTENT)
                || StringUtils.equals(note.getAuthor(), currentIdentity.getUserId()))
            // Published excludes notes only saved and never published
            // (announced to a space feed): a published note has an
            // activityId, exactly like a News article.
            && (!StringUtils.equals(status, ContentUtils.STATUS_PUBLISHED)
                || StringUtils.isNotBlank(note.getActivityId()))) {
          entries.add(toContentEntry(note, currentIdentity));
        }
      } catch (IllegalAccessException e) {
        // Current user cannot view this note, skip it.
      }
    }
    return entries;
  }

  @Override
  public void delete(String id, String status, Identity currentIdentity) throws Exception {
    Page note = noteService.getNoteById(id, currentIdentity);
    if (note == null) {
      throw new ObjectNotFoundException("Content with id " + id + " was not found");
    }
    if (!noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, currentIdentity)) {
      throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to delete note " + id);
    }
    noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), note.getName(), currentIdentity);
  }

  private String effectiveStatus(String status) {
    return StringUtils.isBlank(status) ? ContentUtils.STATUS_PUBLISHED : status;
  }

  private ContentEntry toContentEntry(Page note, Identity currentIdentity) {
    ContentEntry entry = new ContentEntry();
    entry.setId(note.getId());
    entry.setContentType(ContentUtils.CONTENT_TYPE_NOTES);
    entry.setIcon("fa-clipboard");
    entry.setTitle(note.getTitle());
    entry.setSummary(resolveNoteSummary(note));
    entry.setUrl(note.getUrl());
    entry.setAuthorUsername(note.getAuthor());
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
    entry.setSpaceGroupId(note.getWikiOwner());
    entry.setParentId(note.getParentPageId());
    entry.setDate(note.getUpdatedDate());
    entry.setAttachmentsCount(attachmentService.getAttachmentFileIds(note.getAttachmentObjectType(), note.getId()).size());
    // Page.getCategoryIds() is never populated by NoteService itself - only
    // the REST layer (NotesRestService) fills it in, via this same static
    // helper, after fetching a note. Call it directly here for the same
    // result the Notes application already shows.
    entry.setCategoryIds(NoteCategoryPlugin.getCategoryIds(note));
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
