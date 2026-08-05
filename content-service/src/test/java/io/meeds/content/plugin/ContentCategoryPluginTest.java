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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.utils.ContentUtils;
import io.meeds.social.activity.plugin.ActivityCategoryPlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryPluginService;

@RunWith(MockitoJUnitRunner.class)
public class ContentCategoryPluginTest {

  @Mock
  private CategoryLinkService   categoryLinkService;

  @Mock
  private NewsService           newsService;

  @Mock
  private NoteService           noteService;

  @Mock
  private CategoryPluginService categoryPluginService;

  @InjectMocks
  private ContentCategoryPlugin plugin;

  @Before
  public void setUp() {
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NEWS)).thenReturn(Collections.emptyList());
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NOTES)).thenReturn(Collections.emptyList());
    when(categoryLinkService.getLinkedIds(ActivityCategoryPlugin.OBJECT_TYPE)).thenReturn(Collections.emptyList());
  }

  @Test
  public void testGetType() {
    assertEquals("content", plugin.getType());
  }

  @Test
  public void testCanAccessAndCanEditAlwaysDenied() {
    assertFalse(plugin.canAccess("1", "john"));
    assertFalse(plugin.canEdit("1", "john"));
  }

  @Test
  public void testGetCategoryIdsUnionsNewsNotesAndActivityWhenLinkedContentExists() {
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NEWS)).thenReturn(Arrays.asList(1L, 2L));
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NOTES)).thenReturn(Arrays.asList(2L, 3L));
    when(categoryLinkService.getLinkedIds(ActivityCategoryPlugin.OBJECT_TYPE)).thenReturn(Arrays.asList(3L, 4L));
    for (long categoryId : new long[] { 1L, 2L, 3L, 4L }) {
      when(categoryLinkService.getLinkedObjects(eq(categoryId), anyList(), eq(0), eq(20)))
                                                                                          .thenReturn(Collections.singletonList(new CategoryObject(ContentUtils.CONTENT_TYPE_NEWS,
                                                                                                                                                    "news" + categoryId,
                                                                                                                                                    0)));
    }
    when(newsService.getNewsArticleById(anyString())).thenReturn(mock(News.class));

    List<Long> categoryIds = plugin.getCategoryIds();

    assertEquals(4, categoryIds.size());
    assertTrue(categoryIds.containsAll(Arrays.asList(1L, 2L, 3L, 4L)));
  }

  @Test
  public void testGetCategoryIdsEmptyWhenNothingLinked() {
    assertTrue(plugin.getCategoryIds().isEmpty());
  }

  @Test
  public void testGetCategoryIdsExcludesCategoryWhenLinkedNewsWasDeleted() {
    // Simulates an orphaned category link: the News article was deleted but
    // its category-link metadata was never cleaned up.
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NEWS)).thenReturn(Arrays.asList(1L));
    when(categoryLinkService.getLinkedObjects(eq(1L), anyList(), eq(0), eq(20)))
                                                                                .thenReturn(Collections.singletonList(new CategoryObject(ContentUtils.CONTENT_TYPE_NEWS,
                                                                                                                                          "deletedNews",
                                                                                                                                          0)));
    when(newsService.getNewsArticleById("deletedNews")).thenReturn(null);

    assertTrue(plugin.getCategoryIds().isEmpty());
  }

  @Test
  public void testGetCategoryIdsKeepsCategoryWhenLinkedNoteExists() {
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NOTES)).thenReturn(Arrays.asList(5L));
    when(categoryLinkService.getLinkedObjects(eq(5L), anyList(), eq(0), eq(20)))
                                                                                .thenReturn(Collections.singletonList(new CategoryObject(ContentUtils.CONTENT_TYPE_NOTES,
                                                                                                                                          "note1",
                                                                                                                                          0)));
    when(noteService.getNoteById("note1")).thenReturn(new Page());

    assertEquals(Arrays.asList(5L), plugin.getCategoryIds());
  }

  @Test
  public void testGetCategoryIdsKeepsCategoryWhenActivityResolvesToExistingNews() {
    // A posted article's category link is recorded under "activity" (using
    // the activity's own id) - it only counts once resolved back to the News
    // it actually represents.
    CategoryObject activityLink = new CategoryObject(ActivityCategoryPlugin.OBJECT_TYPE, "activity1", 0);
    CategoryObject resolvedNews = new CategoryObject(ContentUtils.CONTENT_TYPE_NEWS, "news1", 0);
    when(categoryLinkService.getLinkedIds(ActivityCategoryPlugin.OBJECT_TYPE)).thenReturn(Arrays.asList(6L));
    when(categoryLinkService.getLinkedObjects(eq(6L), anyList(), eq(0), eq(20)))
                                                                                .thenReturn(Collections.singletonList(activityLink));
    when(categoryPluginService.getObject(activityLink)).thenReturn(resolvedNews);
    when(newsService.getNewsArticleById("news1")).thenReturn(mock(News.class));

    assertEquals(Arrays.asList(6L), plugin.getCategoryIds());
  }

  @Test
  public void testGetCategoryIdsOnlyKeepsTheOneAssociatedCategoryAmongFourTopLevelCategories() {
    // Reproduces the reported scenario: 4 top-level categories exist
    // (ids 1-4), only category 2 ("Knowledge Sharing") is actually linked to
    // an existing Note - categories 1, 3 and 4 must NOT be returned, even
    // though a raw (unvalidated) getLinkedIds lookup might otherwise surface
    // them via an unrelated/orphaned link.
    when(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NOTES)).thenReturn(Arrays.asList(2L));
    when(categoryLinkService.getLinkedObjects(eq(2L), anyList(), eq(0), eq(20)))
                                                                                .thenReturn(Collections.singletonList(new CategoryObject(ContentUtils.CONTENT_TYPE_NOTES,
                                                                                                                                          "note1",
                                                                                                                                          0)));
    when(noteService.getNoteById("note1")).thenReturn(new Page());

    List<Long> categoryIds = plugin.getCategoryIds();

    assertEquals(Arrays.asList(2L), categoryIds);
  }

  @Test
  public void testGetCategoryIdsExcludesCategoryWhenActivityIsUnrelatedToContent() {
    // A plain status update (or any other app's activity) tagged with a
    // category via the generic cross-app feature must not count as "content".
    CategoryObject activityLink = new CategoryObject(ActivityCategoryPlugin.OBJECT_TYPE, "activity1", 0);
    when(categoryLinkService.getLinkedIds(ActivityCategoryPlugin.OBJECT_TYPE)).thenReturn(Arrays.asList(6L));
    when(categoryLinkService.getLinkedObjects(eq(6L), anyList(), eq(0), eq(20)))
                                                                                .thenReturn(Collections.singletonList(activityLink));
    when(categoryPluginService.getObject(activityLink)).thenReturn(activityLink);

    assertTrue(plugin.getCategoryIds().isEmpty());
  }

}
