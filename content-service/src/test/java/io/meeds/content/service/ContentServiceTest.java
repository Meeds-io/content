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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.ContentType;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.plugin.ContentTypePlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

@RunWith(MockitoJUnitRunner.class)
public class ContentServiceTest {

  private static final String JOHN = "john";

  @Mock
  private ApplicationContext  applicationContext;

  @Mock
  private CategoryLinkService categoryLinkService;

  @InjectMocks
  private ContentService      contentService;

  private ContentTypePlugin   newsPlugin;

  private ContentTypePlugin   notesPlugin;

  private Identity            currentIdentity;

  @Before
  public void setUp() throws Exception {
    newsPlugin = mock(ContentTypePlugin.class);
    when(newsPlugin.getType()).thenReturn("news");
    when(newsPlugin.getLabelKey()).thenReturn("content.list.filter.contentType.news");
    when(newsPlugin.getOrder()).thenReturn(10);

    notesPlugin = mock(ContentTypePlugin.class);
    when(notesPlugin.getType()).thenReturn("notes");
    when(notesPlugin.getLabelKey()).thenReturn("content.list.filter.contentType.notes");
    when(notesPlugin.getOrder()).thenReturn(30);

    Map<String, ContentTypePlugin> plugins = new LinkedHashMap<>();
    plugins.put("notesPlugin", notesPlugin);
    plugins.put("newsPlugin", newsPlugin);
    when(applicationContext.getBeansOfType(ContentTypePlugin.class)).thenReturn(plugins);

    contentService.init();

    currentIdentity = new Identity(JOHN);
  }

  private ContentEntry entry(String id, String type, Date date) {
    ContentEntry entry = new ContentEntry();
    entry.setId(id);
    entry.setContentType(type);
    entry.setDate(date);
    return entry;
  }

  @Test
  public void testGetContentTypesSortedByOrder() {
    List<ContentType> types = contentService.getContentTypes();
    assertEquals(2, types.size());
    assertEquals("news", types.get(0).getType());
    assertEquals("notes", types.get(1).getType());
  }

  @Test
  public void testGetContentListMergesAndSortsByDateDesc() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setOffset(0);
    filter.setLimit(20);

    ContentEntry olderNews = entry("1", "news", new Date(1000));
    ContentEntry newerNote = entry("2", "notes", new Date(2000));
    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(Arrays.asList(olderNews));
    when(notesPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(Arrays.asList(newerNote));

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(2, items.size());
    assertEquals("2", items.get(0).getId());
    assertEquals("1", items.get(1).getId());
  }

  @Test
  public void testGetContentListByIncludedCategoriesSortsByCategoryOrderThenDate() throws Exception {
    // The admin-configured "Per category" list order (ContentFilter's
    // includeCategoryIds, already reflecting the order set/reordered in the
    // settings drawer) must drive item display order, not just which
    // category pills show: an item matching an earlier-ranked category
    // comes first even if an item matching a later-ranked category is more
    // recent.
    ContentFilter filter = new ContentFilter();
    filter.setLimit(20);
    filter.setIncludeCategoryIds(Arrays.asList(10L, 20L));

    ContentEntry newerInSecondCategory = entry("1", "news", new Date(2000));
    newerInSecondCategory.setCategoryIds(Arrays.asList(20L));
    ContentEntry olderInFirstCategory = entry("2", "news", new Date(1000));
    olderInFirstCategory.setCategoryIds(Arrays.asList(10L));
    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), any()))
                                                                            .thenReturn(Arrays.asList(newerInSecondCategory,
                                                                                                       olderInFirstCategory));
    when(notesPlugin.search(eq(filter), anyInt(), eq(currentIdentity), any())).thenReturn(java.util.Collections.emptyList());

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(2, items.size());
    assertEquals("2", items.get(0).getId());
    assertEquals("1", items.get(1).getId());
  }

  @Test
  public void testGetContentListRespectsContentTypesFilter() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setContentTypes(Arrays.asList("news"));
    filter.setLimit(20);

    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(Arrays.asList(entry("1", "news", new Date())));

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(1, items.size());
    verify(notesPlugin, never()).search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void testGetContentListAppliesPagination() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setOffset(1);
    filter.setLimit(1);

    List<ContentEntry> newsEntries = new ArrayList<>();
    newsEntries.add(entry("1", "news", new Date(3000)));
    newsEntries.add(entry("2", "news", new Date(2000)));
    newsEntries.add(entry("3", "news", new Date(1000)));
    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(newsEntries);
    when(notesPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(java.util.Collections.emptyList());

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(1, items.size());
    assertEquals("2", items.get(0).getId());
  }

  @Test
  public void testGetContentListExcludesCategoryIds() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setLimit(20);
    filter.setExcludeCategoryIds(Arrays.asList(5L));

    ContentEntry excluded = entry("1", "news", new Date(2000));
    excluded.setCategoryIds(Arrays.asList(5L));
    ContentEntry kept = entry("2", "news", new Date(1000));
    kept.setCategoryIds(Arrays.asList(6L));
    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(Arrays.asList(excluded, kept));
    when(notesPlugin.search(eq(filter), anyInt(), eq(currentIdentity), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(java.util.Collections.emptyList());

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(1, items.size());
    assertEquals("2", items.get(0).getId());
  }

  @Test
  public void testGetContentListByCategoryResolvesLinkedIds() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setLimit(20);
    filter.setCategoryId(9L);

    CategoryObject newsObject = new CategoryObject();
    newsObject.setType("news");
    newsObject.setId("1");
    when(categoryLinkService.getLinkedObjects(eq(9L), anyList(), eq(0), anyInt())).thenReturn(Arrays.asList(newsObject));

    ContentEntry matchingNews = entry("1", "news", new Date());
    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), eq(Set.of("1")))).thenReturn(Arrays.asList(matchingNews));
    when(notesPlugin.search(eq(filter), anyInt(), eq(currentIdentity), eq(Set.of("1")))).thenReturn(java.util.Collections.emptyList());

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(1, items.size());
    assertEquals("1", items.get(0).getId());
  }

  @Test
  public void testGetContentListByCategoryMatchesAcrossTypesForSharedId() throws Exception {
    // A published Note's category link is recorded under whichever type its
    // shared Activity's specific metadata object currently resolves to - for
    // News/Notes that is always "news" once published
    // (NewsService#updateNewsActivity/postNewsActivity), never the note's
    // own "notes" type, even when the category was added from the Notes UI.
    // Since News and Notes are the same wiki Page row (same id) once linked
    // to the same Activity, resolveCategoryLinkedIds must hand every plugin
    // the same flat, non-type-partitioned id set so the Notes candidate
    // still matches by its own (shared) id.
    ContentFilter filter = new ContentFilter();
    filter.setLimit(20);
    filter.setCategoryId(9L);

    CategoryObject newsObject = new CategoryObject();
    newsObject.setType("news");
    newsObject.setId("2");
    when(categoryLinkService.getLinkedObjects(eq(9L), anyList(), eq(0), anyInt())).thenReturn(Arrays.asList(newsObject));

    ContentEntry matchingNote = entry("2", "notes", new Date());
    when(newsPlugin.search(eq(filter), anyInt(), eq(currentIdentity), eq(Set.of("2")))).thenReturn(java.util.Collections.emptyList());
    when(notesPlugin.search(eq(filter), anyInt(), eq(currentIdentity), eq(Set.of("2")))).thenReturn(Arrays.asList(matchingNote));

    List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

    assertEquals(1, items.size());
    assertEquals("2", items.get(0).getId());
  }

  @Test
  public void testDeleteContentDelegatesToMatchingPlugin() throws Exception {
    contentService.deleteContent("1", "news", "published", currentIdentity);
    verify(newsPlugin).delete("1", "published", currentIdentity);
  }

  @Test
  public void testDeleteContentThrowsWhenTypeUnknown() {
    try {
      contentService.deleteContent("1", "unknown-type", "published", currentIdentity);
      fail("Expected ObjectNotFoundException");
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }
  }

}
