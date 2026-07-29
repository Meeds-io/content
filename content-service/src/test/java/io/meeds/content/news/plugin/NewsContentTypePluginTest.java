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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
import io.meeds.content.utils.ContentUtils;

@RunWith(MockitoJUnitRunner.class)
public class NewsContentTypePluginTest {

  private static final String JOHN = "john";

  @Mock
  private NewsService         newsService;

  @Mock
  private SpaceService        spaceService;

  @Mock
  private IdentityManager     identityManager;

  @Mock
  private AttachmentService   attachmentService;

  @InjectMocks
  private NewsContentTypePlugin plugin;

  private Identity currentIdentity;

  @Before
  public void setUp() {
    currentIdentity = new Identity(JOHN);
    when(attachmentService.getAttachmentFileIds(anyString(), anyString())).thenReturn(Collections.emptyList());
  }

  @Test
  public void testGetTypeLabelKeyOrder() {
    assertEquals(ContentUtils.CONTENT_TYPE_NEWS, plugin.getType());
    assertEquals("content.list.filter.contentType.news", plugin.getLabelKey());
    assertEquals(10, plugin.getOrder());
  }

  @Test
  public void testSearchReturnsEmptyWhenCategoryLinkedIdsEmpty() throws Exception {
    ContentFilter filter = new ContentFilter();
    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, Collections.emptySet(), Collections.emptySet());
    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchReturnsEmptyWhenNewsServiceReturnsNull() throws Exception {
    ContentFilter filter = new ContentFilter();
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenReturn(null);
    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null, null);
    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchMapsNewsToContentEntry() throws Exception {
    ContentFilter filter = new ContentFilter();
    News news = new News();
    news.setId("1");
    news.setTitle("Article Title");
    news.setUrl("/article/1");
    news.setAuthor(JOHN);
    news.setAuthorDisplayName("John Doe");
    news.setSpaceId("space1");
    news.setUpdateDate(new java.util.Date());
    news.setViewsCount(5L);
    news.setCategories(Arrays.asList(1L, 2L));
    news.setCanEdit(true);
    news.setCanDelete(true);
    news.setActivityId("activity1");
    news.setSpaceUrl("/portal/g/:spaces:space1/space-1");
    news.setLang("fr");
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenReturn(Arrays.asList(news));
    when(attachmentService.getAttachmentFileIds(eq(NewsPageAttachmentPlugin.OBJECT_TYPE), eq("1"))).thenReturn(Arrays.asList("f1", "f2"));

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null, null);

    assertEquals(1, entries.size());
    ContentEntry entry = entries.get(0);
    assertEquals("1", entry.getId());
    assertEquals(ContentUtils.CONTENT_TYPE_NEWS, entry.getContentType());
    assertEquals("Article Title", entry.getTitle());
    assertEquals(JOHN, entry.getAuthorUsername());
    assertEquals(2, entry.getAttachmentsCount());
    assertTrue(entry.isPublished());
    assertTrue(entry.isCanEdit());
    assertEquals("activity1", entry.getActivityId());
    assertEquals("/portal/g/:spaces:space1/space-1", entry.getSpaceUrl());
    assertEquals("fr", entry.getLang());
  }

  @Test
  public void testSearchFallsBackToBodyWhenSummaryBlank() throws Exception {
    // No properties/summary set on this article - the summary must fall
    // back to the raw body content, the same way NoteContentTypePlugin
    // already does for notes, instead of showing nothing.
    ContentFilter filter = new ContentFilter();
    News news = new News();
    news.setId("1");
    news.setBody("<p>Some <b>rich</b> article content.</p>");
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenReturn(Arrays.asList(news));

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null, null);

    assertEquals(1, entries.size());
    assertEquals("Some rich article content.", entries.get(0).getSummary());
  }

  @Test
  public void testSearchFiltersOutNonAllowedCategoryLinkedIds() throws Exception {
    ContentFilter filter = new ContentFilter();
    News allowed = new News();
    allowed.setId("1");
    News notAllowed = new News();
    notAllowed.setId("2");
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenReturn(Arrays.asList(allowed, notAllowed));

    Set<String> categoryLinkedIds = Collections.singleton("1");
    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, categoryLinkedIds, Collections.emptySet());

    assertEquals(1, entries.size());
    assertEquals("1", entries.get(0).getId());
  }

  @Test
  public void testSearchMatchesByActivityLinkedIdWhenOwnIdNotLinked() throws Exception {
    // Once published, a News article's own category link is redirected onto
    // its underlying Activity (NewsCategoryPlugin#toCategoryObject) - so a
    // category filter match must also be recognized via the article's own
    // activityId, not only via its own id.
    ContentFilter filter = new ContentFilter();
    News published = new News();
    published.setId("1");
    published.setActivityId("activity1");
    News other = new News();
    other.setId("2");
    other.setActivityId("activity2");
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenReturn(Arrays.asList(published, other));

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, Collections.emptySet(), Collections.singleton("activity1"));

    assertEquals(1, entries.size());
    assertEquals("1", entries.get(0).getId());
  }

  @Test
  public void testSearchUsesSearchNewsWhenTextProvided() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setSearchText("keyword");
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                        new org.exoplatform.social.core.identity.model.Identity("1",
                                                                                                                                JOHN,
                                                                                                                                OrganizationIdentityProvider.NAME,
                                                                                                                                false,
                                                                                                                                true,
                                                                                                                                null,
                                                                                                                                null,
                                                                                                                                null);
    when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, JOHN)).thenReturn(socialIdentity);
    when(newsService.searchNews(any(NewsFilter.class), eq(socialIdentity))).thenReturn(Collections.emptyList());

    plugin.search(filter, 20, currentIdentity, null, null);

    verify(newsService).searchNews(any(NewsFilter.class), eq(socialIdentity));
    verify(newsService, never()).getNews(any(NewsFilter.class), any());
  }

  @Test
  public void testSearchWithTextResolvesSpaceIdentityIds() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setSearchText("keyword");
    filter.setSpaces(Arrays.asList("space1"));
    Space space = new Space();
    space.setPrettyName("space1PrettyName");
    when(spaceService.getSpaceById("space1")).thenReturn(space);
    org.exoplatform.social.core.identity.model.Identity spaceIdentity =
                                                                       mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(spaceIdentity.getIdentityId()).thenReturn(42L);
    when(identityManager.getOrCreateSpaceIdentity(anyString())).thenReturn(spaceIdentity);
    when(newsService.searchNews(any(NewsFilter.class), any())).thenAnswer(invocation -> {
      NewsFilter usedFilter = invocation.getArgument(0);
      assertEquals(Arrays.asList("42"), usedFilter.getSpaces());
      return Collections.emptyList();
    });

    plugin.search(filter, 20, currentIdentity, null, null);

    verify(newsService).searchNews(any(NewsFilter.class), any());
  }

  @Test
  public void testSearchWithMyContentStatusSetsAuthor() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_MY_CONTENT);
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenAnswer(invocation -> {
      NewsFilter usedFilter = invocation.getArgument(0);
      assertEquals(JOHN, usedFilter.getAuthor());
      return Collections.emptyList();
    });

    plugin.search(filter, 20, currentIdentity, null, null);
  }

  @Test
  public void testSearchWithScheduledStatus() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_SCHEDULED);
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenAnswer(invocation -> {
      NewsFilter usedFilter = invocation.getArgument(0);
      assertTrue(usedFilter.isScheduledNews());
      assertEquals(JOHN, usedFilter.getAuthor());
      return Collections.emptyList();
    });

    List<ContentEntry> entries = plugin.search(filter, 20, currentIdentity, null, null);
    assertTrue(entries.isEmpty());
  }

  @Test
  public void testSearchWithDraftStatus() throws Exception {
    ContentFilter filter = new ContentFilter();
    filter.setStatus(ContentUtils.STATUS_DRAFT);
    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity))).thenAnswer(invocation -> {
      NewsFilter usedFilter = invocation.getArgument(0);
      assertTrue(usedFilter.isDraftNews());
      return Collections.emptyList();
    });

    plugin.search(filter, 20, currentIdentity, null, null);
  }

  @Test
  public void testDeleteArticle() throws Exception {
    plugin.delete("1", ContentUtils.STATUS_PUBLISHED, currentIdentity);
    verify(newsService).deleteNews("1", currentIdentity, NewsUtils.NewsObjectType.ARTICLE.name());
  }

  @Test
  public void testDeleteDraft() throws Exception {
    plugin.delete("1", ContentUtils.STATUS_DRAFT, currentIdentity);
    verify(newsService).deleteNews("1", currentIdentity, NewsUtils.NewsObjectType.LATEST_DRAFT.name());
  }

}
