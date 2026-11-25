/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class NewsAttachmentPluginTest {

  @Mock
  private NewsService                        newsService;

  @Mock
  private AttachmentService                  attachmentService;

  @Mock
  private NoteService                        noteService;

  @InjectMocks
  private NewsPageAttachmentPlugin plugin;

  @Before
  public void setUp() {
    plugin.init();
  }

  @Test
  public void testGetObjectType() {
    Assert.assertEquals("articlePage", plugin.getObjectType());
  }

  @Test
  public void testHasAccessPermission() throws Exception {
    org.exoplatform.services.security.Identity userIdentity = mock(org.exoplatform.services.security.Identity.class);
    News news = mock(News.class);

    when(newsService.isArticlePage("1")).thenReturn(true);
    when(newsService.getNewsArticleById(anyString())).thenReturn(news);
    when(newsService.canViewNews(news, userIdentity.getUserId())).thenReturn(true);

    assertTrue(plugin.hasAccessPermission(userIdentity, "1"));
  }

  @Test
  public void testHasEditPermission() throws Exception {
    org.exoplatform.services.security.Identity userIdentity = mock(org.exoplatform.services.security.Identity.class);
    News news = mock(News.class);

    when(newsService.isArticlePage("1")).thenReturn(true);
    when(newsService.getNewsById("1", userIdentity, false, ARTICLE.name())).thenReturn(news);
    when(news.isCanEdit()).thenReturn(true);

    assertTrue(plugin.hasEditPermission(userIdentity, "1"));
  }

  @Test
  public void testArticleAccessDoesNotFallBackToTheNote() throws Exception {
    Identity userIdentity = mock(Identity.class);
    News news = mock(News.class);
    when(newsService.isArticlePage("1")).thenReturn(true);
    when(newsService.getNewsArticleById("1")).thenReturn(news);

    assertFalse(plugin.hasAccessPermission(userIdentity, "1"));
    verify(noteService, never()).getNoteById("1", userIdentity);
  }

  /**
   * A live article whose build fails (the getters log it and answer null) is
   * refused, never granted the note's rights
   */
  @Test
  public void testArticleWhoseBuildFailsIsRefused() throws Exception {
    Identity userIdentity = mock(Identity.class);
    when(newsService.isArticlePage("1")).thenReturn(true);
    Page note = viewableManageableNote();
    org.mockito.Mockito.lenient().when(noteService.getNoteById("1", userIdentity)).thenReturn(note);

    assertFalse(plugin.hasAccessPermission(userIdentity, "1"));
    assertFalse(plugin.hasEditPermission(userIdentity, "1"));
    verify(noteService, never()).getNoteById("1", userIdentity);
  }

  @Test
  public void testUnreadableArticleItemIsRefused() throws Exception {
    Identity userIdentity = mock(Identity.class);
    when(newsService.isArticlePage("1")).thenThrow(new IllegalStateException("storage error"));
    Page note = viewableManageableNote();
    org.mockito.Mockito.lenient().when(noteService.getNoteById("1", userIdentity)).thenReturn(note);

    assertFalse(plugin.hasAccessPermission(userIdentity, "1"));
    assertFalse(plugin.hasEditPermission(userIdentity, "1"));
    verify(noteService, never()).getNoteById("1", userIdentity);
  }

  private Page viewableManageableNote() {
    Page note = mock(Page.class);
    org.mockito.Mockito.lenient().when(note.isCanView()).thenReturn(true);
    org.mockito.Mockito.lenient().when(note.isCanManage()).thenReturn(true);
    return note;
  }

  @Test
  public void testKeptNoteAttachmentsFollowTheNoteViewRight() throws Exception {
    Identity userIdentity = mock(Identity.class);
    Page note = mock(Page.class);
    when(noteService.getNoteById("1", userIdentity)).thenReturn(note);

    when(note.isCanView()).thenReturn(true);
    assertTrue(plugin.hasAccessPermission(userIdentity, "1"));

    when(note.isCanView()).thenReturn(false);
    assertFalse(plugin.hasAccessPermission(userIdentity, "1"));
  }

  @Test
  public void testKeptNoteAttachmentsFollowTheNoteManageRight() throws Exception {
    Identity userIdentity = mock(Identity.class);
    Page note = mock(Page.class);
    when(noteService.getNoteById("1", userIdentity)).thenReturn(note);

    when(note.isCanManage()).thenReturn(true);
    assertTrue(plugin.hasEditPermission(userIdentity, "1"));

    when(note.isCanManage()).thenReturn(false);
    assertFalse(plugin.hasEditPermission(userIdentity, "1"));
  }

  @Test
  public void testDeletedNoteAttachmentsAreRefused() throws Exception {
    Identity userIdentity = mock(Identity.class);
    Page note = mock(Page.class);
    when(noteService.getNoteById("1", userIdentity)).thenReturn(note);
    org.mockito.Mockito.lenient().when(note.isCanView()).thenReturn(true);
    org.mockito.Mockito.lenient().when(note.isCanManage()).thenReturn(true);
    when(note.isDeleted()).thenReturn(true);

    assertFalse(plugin.hasAccessPermission(userIdentity, "1"));
    assertFalse(plugin.hasEditPermission(userIdentity, "1"));
  }

  @Test
  public void testUnreadableNoteAttachmentsAreRefused() throws Exception {
    Identity userIdentity = mock(Identity.class);
    when(noteService.getNoteById("1", userIdentity)).thenThrow(new IllegalAccessException("refused"));

    assertFalse(plugin.hasAccessPermission(userIdentity, "1"));
    assertFalse(plugin.hasEditPermission(userIdentity, "1"));
  }

}
