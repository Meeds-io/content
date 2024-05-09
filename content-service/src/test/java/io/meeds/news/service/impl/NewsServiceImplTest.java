/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.news.service.impl;

import static io.meeds.news.service.impl.NewsServiceImpl.*;
import static io.meeds.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static io.meeds.news.utils.NewsUtils.NewsObjectType.LATEST_DRAFT;
import static io.meeds.news.utils.NewsUtils.NewsUpdateType.CONTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.wiki.WikiException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

import io.meeds.news.filter.NewsFilter;
import io.meeds.news.model.News;
import io.meeds.news.model.NewsDraftObject;
import io.meeds.news.model.NewsLatestDraftObject;
import io.meeds.news.service.NewsService;
import io.meeds.news.service.NewsTargetingService;
import io.meeds.news.utils.NewsUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NewsServiceImplTest {

  @Mock
  private SpaceService                                 spaceService;

  @Mock
  private NoteService                                  noteService;

  @Mock
  private MetadataService                              metadataService;

  @Mock
  private FileService                                  fileService;

  @Mock
  private UploadService                                uploadService;

  @Mock
  private IndexingService                              indexingService;

  @Mock
  NewsTargetingService                                 newsTargetingService;

  @Mock
  IdentityManager                                      identityManager;

  @Mock
  ActivityManager                                      activityManager;

  @Mock
  WikiService                                          wikiService;

  @Mock
  private Identity                                     johnIdentity;

  private NewsService                                  newsService;

  private static final MockedStatic<CommonsUtils>      COMMONS_UTILS      = mockStatic(CommonsUtils.class);

  private static final MockedStatic<PortalContainer>   PORTAL_CONTAINER   = mockStatic(PortalContainer.class);

  private static final MockedStatic<NewsUtils>         NEWS_UTILS         = mockStatic(NewsUtils.class);

  private static final MockedStatic<ConversationState> CONVERSATION_STATE = mockStatic(ConversationState.class);

  private static final MockedStatic<MentionUtils>      MENTION_UTILS      = mockStatic(MentionUtils.class);

  @Before
  public void setUp() {
    this.newsService = new NewsServiceImpl(spaceService,
                                           noteService,
                                           metadataService,
                                           fileService,
                                           newsTargetingService,
                                           indexingService,
                                           identityManager,
                                           activityManager,
                                           wikiService,
                                           uploadService);

    when(johnIdentity.getUserId()).thenReturn("john");
    ConversationState conversationState = mock(ConversationState.class);
    CONVERSATION_STATE.when(ConversationState::getCurrent).thenReturn(conversationState);
    CONVERSATION_STATE.when(() -> ConversationState.getCurrent().getIdentity()).thenReturn(johnIdentity);
  }

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
    PORTAL_CONTAINER.close();
    NEWS_UTILS.close();
    MENTION_UTILS.close();
  }

  @Test
  public void testCreateDraftArticle() throws Exception {

    // Given
    News draftArticle = new News();
    draftArticle.setAuthor("john");
    draftArticle.setTitle("draft article for new page");
    draftArticle.setSummary("draft article summary for new page");
    draftArticle.setBody("draft body");
    draftArticle.setPublicationState("draft");

    Space space = mock(Space.class);
    when(spaceService.getSpaceById(draftArticle.getSpaceId())).thenReturn(space);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space);
    when(space.getGroupId()).thenReturn("/space/groupId");
    when(space.getId()).thenReturn("1");

    DraftPage draftPage = new DraftPage();
    draftPage.setContent(draftArticle.getBody());
    draftPage.setTitle(draftArticle.getTitle());
    draftPage.setId("1");
    draftPage.setAuthor("john");

    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    when(spaceService.getSpaceById(any())).thenReturn(space);
    when(spaceService.isSuperManager(anyString())).thenReturn(true);
    Wiki wiki = mock(Wiki.class);
    when(wikiService.getWikiByTypeAndOwner(anyString(), anyString())).thenReturn(wiki);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);

    // When
    News savedDraftArticle = newsService.createNews(draftArticle, identity);

    // Then
    assertNull(savedDraftArticle);

    // Given
    when(noteService.getNoteOfNoteBookByName("group",
                                             space.getGroupId(),
                                             NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME)).thenReturn(rootPage);
    when(noteService.createDraftForNewPage(any(DraftPage.class), anyLong())).thenReturn(draftPage);
    when(rootPage.getId()).thenReturn("1");
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(true);
    when(spaceService.canRedactOnSpace(any(Space.class), any(Identity.class))).thenReturn(true);

    // When
    savedDraftArticle = newsService.createNews(draftArticle, identity);

    // Then
    assertNotNull(savedDraftArticle);
    verify(metadataService, times(1)).createMetadataItem(any(NewsDraftObject.class),
                                                         any(MetadataKey.class),
                                                         any(Map.class),
                                                         anyLong());
    assertNotNull(savedDraftArticle.getId());
    assertEquals(draftPage.getId(), savedDraftArticle.getId());
    assertEquals(draftPage.getTitle(), savedDraftArticle.getTitle());
    assertEquals(draftPage.getContent(), savedDraftArticle.getBody());
    assertEquals(draftPage.getAuthor(), savedDraftArticle.getAuthor());
  }

  @Test
  public void testGetDraftArticleById() throws Exception {

    // Given
    DraftPage draftPage = new DraftPage();
    draftPage.setContent("draft body");
    draftPage.setTitle("draft article for new page");
    draftPage.setId("1");
    draftPage.setAuthor("john");
    draftPage.setWikiOwner("/space/groupId");

    Space space = mockSpace();

    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_SUMMARY, draftPage.getContent());
    when(metadataItem.getProperties()).thenReturn(properties);
    PORTAL_CONTAINER.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    COMMONS_UTILS.when(CommonsUtils::getCurrentPortalOwner).thenReturn("dw");
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    NEWS_UTILS.when(() -> NewsUtils.buildDraftUrl(any())).thenReturn("url");
    MENTION_UTILS.when(() -> MentionUtils.substituteUsernames(anyString(), anyString())).thenReturn(draftPage.getContent());

    // When
    News news = newsService.getNewsById("1", identity, false, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase());

    // Then
    assertNotNull(news);
    assertEquals(draftPage.getId(), news.getId());
    assertEquals(draftPage.getAuthor(), news.getAuthor());
    assertEquals(draftPage.getContent(), news.getBody());
    assertEquals("draft", news.getPublicationState());
    assertEquals(space.getDisplayName(), news.getSpaceDisplayName());
    assertEquals(space.getAvatarUrl(), news.getSpaceAvatarUrl());
    assertEquals("url", news.getUrl());
  }

  @Test
  public void testUpdateDraftArticle() throws Exception {

    // Given
    DraftPage draftPage = new DraftPage();
    draftPage.setContent("draft body");
    draftPage.setTitle("draft article for new page");
    draftPage.setId("1");
    draftPage.setAuthor("john");
    draftPage.setWikiOwner("/space/groupId");

    Space space = mockSpace();

    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);
    PORTAL_CONTAINER.when(() -> PortalContainer.getCurrentPortalContainerName()).thenReturn("portal");
    COMMONS_UTILS.when(() -> CommonsUtils.getCurrentPortalOwner()).thenReturn("dw");
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(identity);
    News news = new News();
    news.setAuthor("john");
    news.setTitle("new draft title");
    news.setBody("draft body");
    news.setId("1");
    news.setPublicationState("draft");
    news.setSpaceId(space.getId());
    news.setSummary("news summary");

    DraftPage expecteddraftPage = new DraftPage();
    expecteddraftPage.setTitle(news.getTitle());
    expecteddraftPage.setAuthor(news.getAuthor());
    expecteddraftPage.setContent(news.getBody());
    expecteddraftPage.setId(news.getId());
    expecteddraftPage.setWikiOwner("/space/groupId");

    // When, Then
    assertThrows(IllegalAccessException.class, () -> newsService.updateNews(news, "john", false, false, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase()));

    // Given
    when(spaceService.canRedactOnSpace(space, identity)).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");
    when(noteService.updateDraftForNewPage(any(DraftPage.class), anyLong())).thenReturn(expecteddraftPage);

    // When
    newsService.updateNews(news, "john", false, false, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase());

    // Then
    verify(noteService, times(1)).updateDraftForNewPage(eq(expecteddraftPage), anyLong());
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong());
  }

  @Test
  public void testDeleteDraftArticle() throws Exception {

    // Given
    DraftPage draftPage = new DraftPage();
    draftPage.setContent("draft body");
    draftPage.setTitle("draft article for new page");
    draftPage.setId("1");
    draftPage.setAuthor("john");
    draftPage.setWikiOwner("/space/groupId");

    Space space = mockSpace();

    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);
    PORTAL_CONTAINER.when(() -> PortalContainer.getCurrentPortalContainerName()).thenReturn("portal");
    COMMONS_UTILS.when(() -> CommonsUtils.getCurrentPortalOwner()).thenReturn("dw");
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(identity);
    when(spaceService.canRedactOnSpace(space, identity)).thenReturn(true);

    // When
    newsService.deleteNews(draftPage.getId(), identity, true);

    // Then
    verify(noteService, times(1)).removeDraftById(draftPage.getId());
    verify(metadataService, times(1)).deleteMetadataItem(any(Long.class), anyBoolean());
  }


  @Test
  public void testGetPublishedArticles() throws Exception {
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setPublishedNews(true);
    Map<String, String> properties = new HashMap<>();
    properties.put(PUBLISHED, "true");
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = List.of(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);

    mockBuildArticle(metadataItems);

    List<News> newsList = newsService.getNews(newsFilter, johnIdentity);
    assertNotNull(newsList);
    assertEquals(newsList.size(), 1);
  }

  @Test
  public void testGetPostedArticles() throws Exception {
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setSpaces(List.of("1"));
    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_PUBLICATION_STATE, POSTED);
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = List.of(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);

    mockBuildArticle(metadataItems);

    List<News> newsList = newsService.getNews(newsFilter, johnIdentity);
    assertNotNull(newsList);
    assertEquals(newsList.size(), 1);
  }

  @Test
  public void testGetMyPostedArticles() throws Exception {
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setSpaces(List.of("1"));
    newsFilter.setAuthor("john");
    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_PUBLICATION_STATE, POSTED);
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = List.of(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);

    org.exoplatform.social.core.identity.model.Identity identity = mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identity.getId()).thenReturn("1");
    when(identityManager.getOrCreateUserIdentity(newsFilter.getAuthor())).thenReturn(identity);
    mockBuildArticle(metadataItems);

    List<News> newsList = newsService.getNews(newsFilter, johnIdentity);
    assertNotNull(newsList);
    assertEquals(newsList.size(), 1);
  }

  @Test
  public void testGetDraftArticles() throws Exception {

    // Given
    DraftPage draftPage = new DraftPage();
    draftPage.setContent("draft body");
    draftPage.setTitle("draft article for new page");
    draftPage.setId("1");
    draftPage.setAuthor("john");
    draftPage.setWikiOwner("/space/groupId");

    Space space1 = mockSpace();
    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);

    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_SUMMARY, draftPage.getContent());
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = Arrays.asList(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    PORTAL_CONTAINER.when(() -> PortalContainer.getCurrentPortalContainerName()).thenReturn("portal");
    COMMONS_UTILS.when(() -> CommonsUtils.getCurrentPortalOwner()).thenReturn("dw");
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    List<Space> allowedDraftNewsSpaces = Arrays.asList(space1);
    NEWS_UTILS.when(() -> NewsUtils.getAllowedDraftArticleSpaceIds(identity, new ArrayList<>())).thenReturn(allowedDraftNewsSpaces);
    when(metadataService.getMetadataItemsByFilter(any(), anyLong(), anyLong())).thenReturn(metadataItems);

    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    // When
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setDraftNews(true);
    newsFilter.setOffset(0);
    newsFilter.setLimit(10);
    List<News> newsList = newsService.getNews(newsFilter, identity);

    // Then
    assertNotNull(newsList);
    assertEquals(newsList.size(), 1);
  }

  @Test
  public void testPostNews() throws Exception {

    // Given
    News newsArticle = new News();
    newsArticle.setAuthor("john");
    newsArticle.setTitle("news article for new page");
    newsArticle.setSummary("news article summary for new page");
    newsArticle.setBody("news body");
    newsArticle.setPublicationState(POSTED);
    newsArticle.setId("1");
    newsArticle.setActivities("1:2;3:4");

    Identity identity = mockIdentity();

    Space space = mockSpace();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(true);
    when(spaceService.canRedactOnSpace(any(Space.class), any(Identity.class))).thenReturn(true);

    Wiki wiki = mock(Wiki.class);
    when(wikiService.getWikiByTypeAndOwner(anyString(), anyString())).thenReturn(wiki);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
    when(rootPage.getId()).thenReturn("1");
    when(noteService.getNoteOfNoteBookByName("group",
                                             space.getGroupId(),
                                             NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME)).thenReturn(rootPage);

    Page newsArticlePage = new Page();
    newsArticlePage.setTitle(newsArticle.getTitle());
    newsArticlePage.setContent(newsArticle.getBody());
    newsArticlePage.setParentPageId(rootPage.getId());
    newsArticlePage.setAuthor(newsArticle.getAuthor());
    newsArticlePage.setLang(null);

    Page createdPage = mock(Page.class);
    when(createdPage.getId()).thenReturn("1");
    when(noteService.createNote(wiki, rootPage.getName(), newsArticlePage, identity)).thenReturn(createdPage);
    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(new org.exoplatform.social.core.identity.model.Identity("1"));

    // When
    newsService.createNews(newsArticle, identity);

    // Then
    verify(noteService, times(1)).createNote(wiki, rootPage.getName(), newsArticlePage, identity);
    verify(noteService, times(1)).createVersionOfNote(createdPage, identity.getUserId());
    verify(noteService, times(1)).getPublishedVersionByPageIdAndLang(1L, null);
    verify(metadataService, atLeast(1)).createMetadataItem(any(MetadataObject.class),
                                                         any(MetadataKey.class),
                                                         any(Map.class),
                                                         anyLong());
  }

  @Test
  public void testCreateDraftArticleForExistingPage() throws Exception {
    // Given
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");

    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);

    when(existingPage.getAuthor()).thenReturn("john");
    when(pageVersion.getTitle()).thenReturn("title");
    when(pageVersion.getContent()).thenReturn("content");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");

    Space space = mockSpace();

    Identity identity = mockIdentity();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    NEWS_UTILS.when(() -> NewsUtils.processMentions(anyString(), any())).thenReturn(new HashSet<>());

    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    when(noteService.getLatestDraftPageByUserAndTargetPageAndLang(anyLong(), anyString(), anyString())).thenReturn(null);

    News news = new News();
    news.setAuthor("john");
    news.setTitle("new draft title");
    news.setBody("draft body");
    news.setId("1");
    news.setPublicationState("draft");
    news.setSpaceId("1");
    news.setSummary("news summary");
    news.setOriginalBody("body");

    // When, Then
      assertThrows(IllegalAccessException.class, () -> newsService.updateNews(news, "john", false, false, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase()));

    // Given
    when(spaceService.canRedactOnSpace(space, identity)).thenReturn(true);
    when(spaceService.isSuperManager(anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getUpdatedDate()).thenReturn(new Date());
    when(draftPage.getCreatedDate()).thenReturn(new Date());
    when(draftPage.getAuthor()).thenReturn("john");
    when(draftPage.getId()).thenReturn("1");
    when(noteService.createDraftForExistPage(any(DraftPage.class),
                                             any(Page.class),
                                             nullable(String.class),
                                             anyLong(),
                                             anyString())).thenReturn(draftPage);

    // When
    newsService.updateNews(news, "john", false, false, LATEST_DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase());

    // Then
    verify(noteService, times(1)).createDraftForExistPage(any(DraftPage.class),
                                                          eq(existingPage),
                                                          nullable(String.class),
                                                          anyLong(),
                                                          anyString());
    verify(metadataService, times(1)).createMetadataItem(any(NewsLatestDraftObject.class),
                                                         any(MetadataKey.class),
                                                         any(Map.class),
                                                         anyLong());

  }

  @Test
  public void testUpdateDraftArticleForExistingPage() throws Exception {
    // Given
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");

    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);

    Space space = mockSpace();

    Identity identity = mockIdentity();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    NEWS_UTILS.when(() -> NewsUtils.processMentions(anyString(), any())).thenReturn(new HashSet<>());
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getUpdatedDate()).thenReturn(new Date());
    when(draftPage.getCreatedDate()).thenReturn(new Date());
    when(draftPage.getAuthor()).thenReturn("john");
    when(draftPage.getId()).thenReturn("1");
    when(draftPage.getContent()).thenReturn("body");
    when(draftPage.getWikiOwner()).thenReturn("/space/groupId");
    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    when(noteService.getLatestDraftPageByUserAndTargetPageAndLang(anyLong(),
                                                                  anyString(),
                                                                  nullable(String.class))).thenReturn(draftPage);

    News news = new News();
    news.setAuthor("john");
    news.setTitle("new draft title");
    news.setBody("draft body");
    news.setId("1");
    news.setPublicationState("draft");
    news.setSpaceId("1");
    news.setSummary("news summary");
    news.setOriginalBody("body");

    // When, Then
    assertThrows(IllegalAccessException.class, () -> newsService.updateNews(news, "john", false, false, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase()));

    // Given
    when(spaceService.canRedactOnSpace(space, identity)).thenReturn(true);
    when(spaceService.isSuperManager(anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    when(noteService.updateDraftForExistPage(any(DraftPage.class),
                                             any(Page.class),
                                             nullable(String.class),
                                             anyLong(),
                                             anyString())).thenReturn(draftPage);

    // When
    newsService.updateNews(news, "john", false, false, LATEST_DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase());

    // Then
    verify(noteService, times(1)).updateDraftForExistPage(any(DraftPage.class),
                                                          eq(existingPage),
                                                          nullable(String.class),
                                                          anyLong(),
                                                          anyString());
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong());
  }

  @Test
  public void testUpdateNewsArticle() throws Exception {
    // Given
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");

    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);

    Space space = mockSpace();

    Identity identity = mockIdentity();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    NEWS_UTILS.when(() -> NewsUtils.processMentions(anyString(), any())).thenReturn(new HashSet<>());
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    DraftPage draftPage = mock(DraftPage.class);
    ;
    when(draftPage.getId()).thenReturn("1");

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    when(noteService.getLatestDraftPageByUserAndTargetPageAndLang(anyLong(),
                                                                  anyString(),
                                                                  nullable(String.class))).thenReturn(draftPage);

    when(existingPage.getAuthor()).thenReturn("john");
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");

    News news = new News();
    news.setAuthor("john");
    news.setTitle("new draft title");
    news.setBody("draft body");
    news.setId("1");
    news.setPublicationState(POSTED);
    news.setSpaceId("1");
    news.setSummary("news summary");
    news.setOriginalBody("body");

    // When, Then
    assertThrows(IllegalAccessException.class, () -> newsService.updateNews(news, "john", false, false, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(), CONTENT.name().toLowerCase()));

    // Given
    when(spaceService.canRedactOnSpace(space, identity)).thenReturn(true);
    when(spaceService.isSuperManager(anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    when(noteService.updateNote(any(Page.class))).thenReturn(existingPage);

    // When
    newsService.updateNews(news, "john", false, false, ARTICLE.name().toLowerCase(), CONTENT.name().toLowerCase());

    // Then
    verify(noteService, times(1)).updateNote(any(Page.class));
    verify(noteService, times(1)).createVersionOfNote(existingPage, identity.getUserId());
    verify(noteService, times(2)).getPublishedVersionByPageIdAndLang(1L, null);
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong());
  }

  @Test
  public void testDeleteNewsArticle() throws Exception {
    // Given
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");
    when(existingPage.getWikiType()).thenReturn("group");
    when(existingPage.getName()).thenReturn("news");

    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
            any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_ACTIVITIES, "1:1;");
    when(metadataItem.getProperties()).thenReturn(properties);
    Space space = mockSpace();
    Identity identity = mockIdentity();
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(new org.exoplatform.social.core.identity.model.Identity("1"));
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);

    when(existingPage.getAuthor()).thenReturn("john");
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");
    //
    assertThrows(IllegalAccessException.class, () -> newsService.deleteNews(existingPage.getId(), identity, false));

    // when
    when(spaceService.canRedactOnSpace(space, identity)).thenReturn(true);
    when(noteService.deleteNote(existingPage.getWikiType(), existingPage.getWikiOwner(), existingPage.getName())).thenReturn(true);
    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getId()).thenReturn("1");
    when(noteService.getLatestDraftOfPage(existingPage, identity.getUserId())).thenReturn(draftPage);
    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);

    newsService.deleteNews(existingPage.getId(), identity, false);

    //Then
    verify(noteService, times(1)).deleteNote(existingPage.getWikiType(), existingPage.getWikiOwner(), existingPage.getName());
    verify(noteService, times(1)).removeDraftById("1");
    verify(activityManager, times(1)).deleteActivity("1");
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong());
  }

  @Test
  public void testScheduleNews() throws Exception {
    Space space = mockSpace();
    Identity identity = mockIdentity();
    when(spaceService.isManager(space, identity.getUserId())).thenReturn(true);
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
            any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);

    Wiki wiki = mock(Wiki.class);
    when(wikiService.getWikiByTypeAndOwner(anyString(), anyString())).thenReturn(wiki);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
    when(rootPage.getId()).thenReturn("1");
    when(noteService.getNoteOfNoteBookByName("group",
            space.getGroupId(),
            NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME)).thenReturn(rootPage);

    News newsArticle = new News();
    newsArticle.setAuthor("john");
    newsArticle.setTitle("news article");
    newsArticle.setSummary("news article summary");
    newsArticle.setBody("news body");
    newsArticle.setPublicationState("staged");
    newsArticle.setId("1");
    newsArticle.setSpaceId("1");
    newsArticle.setPublished(false);
    newsArticle.setSchedulePostDate("30/05/2024 08:00:00");

    org.exoplatform.social.core.identity.model.Identity identity1 =
            mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    newsService.scheduleNews(newsArticle, identity, DRAFT);
    verify(noteService, times(1)).createNote(any(Wiki.class), anyString(), any(Page.class), any(Identity.class));
  }

  @Test
  public void testGetScheduledArticles() throws Exception {
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setScheduledNews(true);
    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_PUBLICATION_STATE, "staged");
    properties.put(NEWS_DELETED, String.valueOf(false));
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = List.of(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);

    mockBuildArticle(metadataItems);

    List<News> newsList = newsService.getNews(newsFilter, johnIdentity);
    assertNotNull(newsList);
    assertEquals(newsList.size(), 1);
  }

  private void mockBuildArticle(List<MetadataItem> metadataItems) throws WikiException {
    when(metadataService.getMetadataItemsByFilter(any(), anyLong(), anyLong())).thenReturn(metadataItems);
    Page page = new Page();
    page.setContent("article body");
    page.setTitle("article");
    page.setId("1");
    page.setAuthor("john");
    page.setWikiOwner("/space/groupId");
    Space space = mock(Space.class);
    when(space.getId()).thenReturn("1");
    when(space.getGroupId()).thenReturn("/space/groupId");
    when(space.getAvatarUrl()).thenReturn("space/avatar/url");
    when(space.getDisplayName()).thenReturn("spaceDisplayName");
    when(space.getVisibility()).thenReturn("public");
    when(spaceService.isSuperManager(anyString())).thenReturn(true);
    when(spaceService.getSpaceById(any())).thenReturn(space);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space);

    when(noteService.getNoteById(anyString())).thenReturn(page);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
            any(MetadataObject.class))).thenReturn(metadataItems);
    PORTAL_CONTAINER.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    COMMONS_UTILS.when(CommonsUtils::getCurrentPortalOwner).thenReturn("dw");
    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
    when(noteService.getNoteById(anyString())).thenReturn(page);
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(johnIdentity);

    PageVersion pageVersion = mock(PageVersion.class);

    when(pageVersion.getTitle()).thenReturn("title");
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getContent()).thenReturn("content");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");

    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    MENTION_UTILS.when(() -> MentionUtils.substituteUsernames(anyString(), anyString())).thenReturn("content");
  }

  private Space mockSpace() {
    Space space = mock(Space.class);
    when(space.getId()).thenReturn("1");
    when(space.getGroupId()).thenReturn("/space/groupId");
    when(space.getAvatarUrl()).thenReturn("space/avatar/url");
    when(space.getDisplayName()).thenReturn("spaceDisplayName");
    when(space.getVisibility()).thenReturn("public");
    when(spaceService.isSuperManager(anyString())).thenReturn(true);
    when(spaceService.getSpaceById(any())).thenReturn(space);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space);
    return space;
  }

  private Identity mockIdentity() {
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(identity);
    return identity;
  }
}
