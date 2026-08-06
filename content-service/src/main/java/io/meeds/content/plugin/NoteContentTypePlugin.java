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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.NoteConstants;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.utils.ContentUtils;
import io.meeds.notes.plugin.NoteCategoryPlugin;

@Component
public class NoteContentTypePlugin implements ContentTypePlugin {

  private static final Log LOG = ExoLogger.getLogger(NoteContentTypePlugin.class);

  @Autowired
  private NoteService     noteService;

  @Autowired
  private SpaceService    spaceService;

  @Autowired
  private IdentityManager identityManager;

  @Autowired
  private AttachmentService attachmentService;

  @Autowired
  private FavoriteService favoriteService;

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
                                   Set<String> categoryLinkedIds) throws Exception {
    String status = effectiveStatus(filter.getStatus());
    if (StringUtils.equals(status, ContentUtils.STATUS_SCHEDULED)) {
      // Regular notes have no scheduled-publish workflow.
      return Collections.emptyList();
    }
    if (categoryLinkedIds != null && CollectionUtils.isEmpty(categoryLinkedIds)) {
      return Collections.emptyList();
    }
    if (StringUtils.equals(status, ContentUtils.STATUS_DRAFT)) {
      // Drafts are staged, unsaved edits (DraftPage), not linkable to a
      // category, so a category filter can never match one.
      return categoryLinkedIds != null ? Collections.emptyList() : searchDraftNotes(filter, fetchLimit, currentIdentity);
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
      if (categoryLinkedIds != null && !categoryLinkedIds.contains(noteId)) {
        continue;
      }
      try {
        Page note = noteService.getNoteById(noteId, currentIdentity);
        if (note != null
            && (!StringUtils.equals(status, ContentUtils.STATUS_MY_CONTENT)
                || StringUtils.equals(note.getAuthor(), currentIdentity.getUserId()))) {
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
    if (StringUtils.equals(status, ContentUtils.STATUS_DRAFT)) {
      DraftPage draft = noteService.getDraftNoteById(id, currentIdentity.getUserId());
      if (draft == null) {
        throw new ObjectNotFoundException("Content with id " + id + " was not found");
      }
      noteService.removeDraftById(id);
      return;
    }
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

  private List<ContentEntry> searchDraftNotes(ContentFilter filter, int fetchLimit, Identity currentIdentity) throws Exception {
    List<Long> spaceIds = NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces());
    List<ContentEntry> entries = new ArrayList<>();
    for (Long spaceId : spaceIds) {
      Space space = spaceService.getSpaceById(String.valueOf(spaceId));
      if (space == null) {
        continue;
      }
      List<DraftPage> drafts;
      try {
        drafts = noteService.getDraftsOfWiki(space.getGroupId(), PortalConfig.GROUP_TYPE, NoteConstants.NOTE_HOME_NAME);
      } catch (Exception e) {
        // A space whose wiki has no Home page yet (e.g. it never had a note
        // saved) makes NoteService fail instead of returning an empty list -
        // skip it rather than failing the whole content list.
        LOG.debug("Unable to retrieve draft notes of space {}", space.getId(), e);
        continue;
      }
      for (DraftPage draft : drafts) {
        // Drafts are personal staging data: only the current user's own
        // drafts are ever shown, regardless of space membership.
        if (StringUtils.equals(draft.getAuthor(), currentIdentity.getUserId())) {
          entries.add(toDraftContentEntry(draft, space));
        }
      }
    }
    entries.sort(Comparator.comparing(ContentEntry::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
    return entries.size() > fetchLimit ? entries.subList(0, fetchLimit) : entries;
  }

  private ContentEntry toDraftContentEntry(DraftPage draft, Space space) {
    ContentEntry entry = new ContentEntry();
    entry.setId(draft.getId());
    entry.setContentType(ContentUtils.CONTENT_TYPE_NOTES);
    entry.setIcon("fa-clipboard");
    entry.setTitle(draft.getTitle());
    entry.setSummary(resolveNoteSummary(draft));
    entry.setAuthorUsername(draft.getAuthor());
    entry.setAuthorDisplayName(draft.getAuthorFullName());
    if (StringUtils.isNotBlank(draft.getAuthor())) {
      org.exoplatform.social.core.identity.model.Identity authorIdentity = identityManager.getOrCreateUserIdentity(draft.getAuthor());
      if (authorIdentity != null && authorIdentity.getProfile() != null) {
        entry.setAuthorAvatarUrl(authorIdentity.getProfile().getAvatarUrl());
      }
    }
    entry.setSpaceId(space.getId());
    entry.setSpaceDisplayName(space.getDisplayName());
    entry.setSpaceAvatarUrl(space.getAvatarUrl());
    entry.setSpaceGroupId(space.getGroupId());
    entry.setParentId(draft.getParentPageId());
    entry.setDate(draft.getUpdatedDate());
    entry.setPublished(false);
    entry.setDraft(true);
    entry.setScheduled(false);
    entry.setCanEdit(true);
    entry.setCanDelete(true);
    entry.setCanPublish(false);
    entry.setCanSchedule(false);
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
    entry.setCanPublish(canPublishNote(note, entry.isCanEdit()));
    entry.setCanSchedule(false);
    entry.setFavorite(isFavorite(note.getId(), currentIdentity));
    return entry;
  }

  private boolean isFavorite(String noteId, Identity currentIdentity) {
    long userIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(currentIdentity.getUserId()).getId());
    return favoriteService.isFavorite(new Favorite(ContentUtils.CONTENT_TYPE_NOTES, noteId, null, userIdentityId));
  }

  private boolean canPublishNote(Page note, boolean canEdit) {
    return canEdit
        && StringUtils.equals(PortalConfig.GROUP_TYPE, note.getWikiType())
        && StringUtils.isNotBlank(note.getWikiOwner())
        && note.getWikiOwner().startsWith("/spaces/");
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
