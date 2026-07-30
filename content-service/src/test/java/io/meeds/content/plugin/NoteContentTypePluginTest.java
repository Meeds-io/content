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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.search.SearchResult;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.utils.ContentUtils;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

@RunWith(MockitoJUnitRunner.class)
public class NoteContentTypePluginTest {

  private static final String JOHN = "john";

  private static final MockedStatic<CommonsUtils> COMMONS_UTILS = mockStatic(CommonsUtils.class);

  @Mock
  private NoteService         noteService;

  @Mock
  private IdentityManager     identityManager;

  @Mock
  private AttachmentService   attachmentService;

  @Mock
  private CategoryLinkService categoryLinkService;

  @Mock
  private SpaceService        spaceService;

  @InjectMocks
  private NoteContentTypePlugin plugin;

  private Identity currentIdentity;

  @Before
  public void setUp() {
    currentIdentity = new Identity(JOHN);
    when(attachmentService.getAttachmentFileIds(anyString(), anyString())).thenReturn(Collections.emptyList());
    // NoteCategoryPlugin.getCategoryIds(note) - called by toContentEntry -
    // reaches these services via the static container lookup, not injection.
    lenient().when(CommonsUtils.getService(CategoryLinkService.class)).thenReturn(categoryLinkService);
    lenient().when(CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);
    lenient().when(categoryLinkService.getLinkedIds(any(CategoryObject.class))).thenReturn(Collections.emptyList());
  }

  @AfterClass
  public static void afterRunBare() {
    COMMONS_UTILS.close();
  }

  @Test
  public void testGetTypeLabelKeyOrder() {
    assertEquals(ContentUtils.CONTENT_TYPE_NOTES, plugin.getType());
    assertEquals("content.list.filter.contentType.notes", plugin.getLabelKey());
    assertEquals(30, plugin.getOrder());
  }

  @Test
  public void testSearchReturnsEmptyForScheduledStatus() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_SCHEDULED);
    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);
    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchReturnsEmptyForDraftStatus() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_DRAFT);
    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);
    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchReturnsEmptyWhenCategoryLinkedIdsEmpty() throws Exception {
    ContentFilter filter = new ContentFilter();
    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, Collections.emptySet());
    assertTrue(entries.isEmpty());
  }

  @SuppressWarnings("unchecked")
  private void mockSearchResults(SearchResult... results) throws Exception {
    PageList<SearchResult> pageList = mock(PageList.class);
    when(pageList.getAll()).thenReturn(Arrays.asList(results));
    when(noteService.search(any())).thenReturn(pageList);
  }

  @Test
  public void testSearchMapsPublishedNoteToContentEntry() throws Exception {
    ContentFilter filter = new ContentFilter();
    SearchResult result = new SearchResult();
    result.setId(1L);
    mockSearchResults(result);

    Page note = new Page();
    note.setId("1");
    note.setTitle("Note Title");
    note.setAuthor(JOHN);
    note.setActivityId("activity1");
    note.setWikiOwner("space1GroupId");
    note.setParentPageId("parent1");
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(note);
    when(noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, currentIdentity)).thenReturn(true);
    when(attachmentService.getAttachmentFileIds(eq(note.getAttachmentObjectType()), eq("1"))).thenReturn(Arrays.asList("f1"));

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);

    assertEquals(1, entries.size());
    ContentEntry entry = entries.get(0);
    assertEquals("1", entry.getId());
    assertEquals(ContentUtils.CONTENT_TYPE_NOTES, entry.getContentType());
    assertEquals("Note Title", entry.getTitle());
    assertEquals(JOHN, entry.getAuthorUsername());
    assertEquals(1, entry.getAttachmentsCount());
    assertTrue(entry.isPublished());
    assertTrue(entry.isCanEdit());
    assertTrue(entry.isCanDelete());
    assertFalse(entry.isCanPublish());
    assertFalse(entry.isCanSchedule());
    assertEquals("space1GroupId", entry.getSpaceGroupId());
    assertEquals("parent1", entry.getParentId());
  }

  @Test
  public void testSearchResolvesCategoryIdsViaNoteCategoryPlugin() throws Exception {
    // Page.getCategoryIds() is never populated by NoteService itself; the
    // entry's categoryIds must come from NoteCategoryPlugin's own resolution
    // instead - the same one the Notes application relies on.
    ContentFilter filter = new ContentFilter();
    SearchResult result = new SearchResult();
    result.setId(1L);
    mockSearchResults(result);

    Page note = new Page();
    note.setId("1");
    note.setActivityId("activity1");
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(note);
    when(categoryLinkService.getLinkedIds(any(CategoryObject.class))).thenReturn(Arrays.asList(7L, 8L));

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);

    assertEquals(1, entries.size());
    assertEquals(Arrays.asList(7L, 8L), entries.get(0).getCategoryIds());
  }

  @Test
  public void testSearchExcludesUnpublishedNoteWhenStatusPublished() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_PUBLISHED);
    SearchResult result = new SearchResult();
    result.setId(1L);
    mockSearchResults(result);

    Page note = new Page();
    note.setId("1");
    note.setActivityId(null);
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(note);

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);

    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchMyContentFiltersByAuthor() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_MY_CONTENT);
    SearchResult mine = new SearchResult();
    mine.setId(1L);
    SearchResult notMine = new SearchResult();
    notMine.setId(2L);
    mockSearchResults(mine, notMine);

    Page myNote = new Page();
    myNote.setId("1");
    myNote.setAuthor(JOHN);
    Page otherNote = new Page();
    otherNote.setId("2");
    otherNote.setAuthor("someoneElse");
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(myNote);
    when(noteService.getNoteById("2", currentIdentity)).thenReturn(otherNote);

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);

    assertEquals(1, entries.size());
    assertEquals("1", entries.get(0).getId());
  }

  @Test
  public void testSearchFiltersOutNonAllowedCategoryLinkedIds() throws Exception {
    ContentFilter filter = new ContentFilter();
    SearchResult allowed = new SearchResult();
    allowed.setId(1L);
    SearchResult notAllowed = new SearchResult();
    notAllowed.setId(2L);
    mockSearchResults(allowed, notAllowed);

    Page note = new Page();
    note.setId("1");
    note.setActivityId("activity1");
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(note);

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, Collections.singleton("1"));

    assertEquals(1, entries.size());
    verify(noteService, never()).getNoteById(eq("2"), any());
  }

  @Test
  public void testSearchSkipsNoteWhenAccessDenied() throws Exception {
    ContentFilter filter = new ContentFilter();
    SearchResult result = new SearchResult();
    result.setId(1L);
    mockSearchResults(result);
    when(noteService.getNoteById("1", currentIdentity)).thenThrow(new IllegalAccessException("no access"));

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);

    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchReturnsEmptyWhenNoResults() throws Exception {
    ContentFilter filter = new ContentFilter();
    mockSearchResults();

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null);

    assertTrue(entries.isEmpty());
  }

  @Test
  public void testDeleteThrowsWhenNoteNotFound() throws Exception {
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(null);
    try {
      plugin.delete("1", ContentUtils.STATUS_PUBLISHED, currentIdentity);
      fail("Expected ObjectNotFoundException");
    } catch (ObjectNotFoundException e) {
      // expected
    }
  }

  @Test
  public void testDeleteThrowsWhenNoPermission() throws Exception {
    Page note = new Page();
    note.setId("1");
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(note);
    when(noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, currentIdentity)).thenReturn(false);
    try {
      plugin.delete("1", ContentUtils.STATUS_PUBLISHED, currentIdentity);
      fail("Expected IllegalAccessException");
    } catch (IllegalAccessException e) {
      // expected
    }
  }

  @Test
  public void testDeleteSucceeds() throws Exception {
    Page note = new Page();
    note.setId("1");
    note.setWikiType("group");
    note.setWikiOwner("/spaces/space1");
    note.setName("myNote");
    when(noteService.getNoteById("1", currentIdentity)).thenReturn(note);
    when(noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, currentIdentity)).thenReturn(true);

    plugin.delete("1", ContentUtils.STATUS_PUBLISHED, currentIdentity);

    verify(noteService).deleteNote("group", "/spaces/space1", "myNote", currentIdentity);
  }

}
