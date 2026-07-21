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
package io.meeds.content.news.service;

import static io.meeds.content.news.service.NewsService.DRAFT;
import static io.meeds.content.news.service.NewsService.EXTERNAL_PAGE;
import static io.meeds.content.news.service.NewsService.NEWS_ACTIVITIES;
import static io.meeds.content.news.service.NewsService.NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME;
import static io.meeds.content.news.service.NewsService.NEWS_DELETED;
import static io.meeds.content.news.service.NewsService.NEWS_PUBLICATION_STATE;
import static io.meeds.content.news.service.NewsService.POSTED;
import static io.meeds.content.news.service.NewsService.PUBLISHED;
import static io.meeds.content.news.service.NewsService.SCHEDULE_POST_DATE;
import static io.meeds.content.news.service.NewsService.UNPUBLISH_SCHEDULED;
import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.LATEST_DRAFT;
import static io.meeds.content.news.utils.NewsUtils.NewsUpdateType.CATEGORIES;
import static io.meeds.content.news.utils.NewsUtils.NewsUpdateType.CONTENT_AND_TITLE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.social.core.space.SpaceUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.model.NewsDraftObject;
import io.meeds.content.news.model.NewsLatestDraftObject;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.plugin.NewsCategoryPlugin;
import io.meeds.content.news.plugin.NewsPageAttachmentPlugin;
import io.meeds.content.news.search.NewsESSearchResult;
import io.meeds.content.news.search.NewsSearchConnector;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class NewsServiceTest {

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

  @Mock
  private NewsSearchConnector                          newsSearchConnector;

  @Mock
  private UserACL                                      userAcl;

  @Mock
  private CategoryLinkService                          categoryLinkService;

  @InjectMocks
  private NewsService                                  newsService;

  private static final MockedStatic<CommonsUtils>      COMMONS_UTILS      = mockStatic(CommonsUtils.class);

  private static final MockedStatic<PortalContainer>   PORTAL_CONTAINER   = mockStatic(PortalContainer.class);

  private static final MockedStatic<NewsUtils>         NEWS_UTILS         = mockStatic(NewsUtils.class);

  private static final MockedStatic<ConversationState> CONVERSATION_STATE = mockStatic(ConversationState.class);

  private static final MockedStatic<MentionUtils>      MENTION_UTILS      = mockStatic(MentionUtils.class);

  private static final MockedStatic<SpaceUtils>        SPACE_UTILS        = mockStatic(SpaceUtils.class);

  @Before
  public void setUp() {
    when(johnIdentity.getUserId()).thenReturn("john");
    ConversationState conversationState = mock(ConversationState.class);
    CONVERSATION_STATE.when(ConversationState::getCurrent).thenReturn(conversationState);
    CONVERSATION_STATE.when(() -> ConversationState.getCurrent().getIdentity()).thenReturn(johnIdentity);
    // updateNewsActivity() links categories on every update where a post/pin decision is
    // supplied (see updateNews()), regardless of newsUpdateType - so any such call reaches
    // CommonsUtils.getService(CategoryLinkService.class) and must find a stub here.
    COMMONS_UTILS.when(() -> CommonsUtils.getService(CategoryLinkService.class)).thenReturn(categoryLinkService);
    when(categoryLinkService.getLinkedIds(any(CategoryObject.class))).thenReturn(Collections.emptyList());
  }

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
    PORTAL_CONTAINER.close();
    NEWS_UTILS.close();
    MENTION_UTILS.close();
    SPACE_UTILS.close();
  }

  @Test
  public void testCreateDraftArticle() throws Exception {

    // Given
    News draftArticle = new News();
    draftArticle.setAuthor("john");
    draftArticle.setTitle("draft article for new page");
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
    when(noteService.createDraftForNewPage(any(DraftPage.class), anyLong(), anyLong())).thenReturn(draftPage);
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
                                                         anyLong(),
                                                         anyBoolean());
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
    when(metadataItem.getProperties()).thenReturn(properties);
    PORTAL_CONTAINER.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    COMMONS_UTILS.when(CommonsUtils::getCurrentPortalOwner).thenReturn("dw");
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    NEWS_UTILS.when(() -> NewsUtils.buildDraftUrl(any())).thenReturn("url");
    MENTION_UTILS.when(() -> MentionUtils.substituteUsernames(anyString(), anyString())).thenReturn(draftPage.getContent());
    MENTION_UTILS.when(() -> MentionUtils.substituteRoleWithLocale(anyString(), any())).thenReturn(draftPage.getContent());

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

    DraftPage expecteddraftPage = new DraftPage();
    expecteddraftPage.setTitle(news.getTitle());
    expecteddraftPage.setAuthor(news.getAuthor());
    expecteddraftPage.setContent(news.getBody());
    expecteddraftPage.setId(news.getId());
    expecteddraftPage.setWikiOwner("/space/groupId");

    // When, Then
    assertThrows(IllegalAccessException.class,
                 () -> newsService.updateNews(news,
                                              "john",
                                              false,
                                              false,
                                              NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(),
                                              CONTENT_AND_TITLE.name()));

    // Given
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");
    when(noteService.updateDraftForNewPage(any(DraftPage.class), anyLong(), anyLong())).thenReturn(expecteddraftPage);

    // When
    newsService.updateNews(news,
                           "john",
                           false,
                           false,
                           NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(),
                           CONTENT_AND_TITLE.name());

    // Then
    verify(noteService, times(1)).updateDraftForNewPage(eq(expecteddraftPage), anyLong(), anyLong());
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

    mockSpace();

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

    //
    assertThrows(IllegalAccessException.class,
                 () -> newsService.deleteNews(draftPage.getId(), identity, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase()));

    // When
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);

    newsService.deleteNews(draftPage.getId(), identity, NewsUtils.NewsObjectType.DRAFT.name().toLowerCase());

    // Then
    verify(noteService, times(1)).removeDraftById(draftPage.getId());
    verify(metadataService, times(1)).deleteMetadataItem(any(Long.class), anyBoolean());

    // delete draft for existing page case
    Page existingPage = mock(Page.class);
    when(existingPage.getId()).thenReturn("1");
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(noteService.getLatestDraftPageByUserAndTargetPageAndLang(anyLong(), anyString(), any())).thenReturn(draftPage);
    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    // When
    newsService.deleteNews(draftPage.getId(), identity, LATEST_DRAFT.name().toLowerCase());

    // Then
    verify(noteService, atLeast(1)).removeDraftById(draftPage.getId());
    verify(metadataService, atLeast(1)).deleteMetadataItem(any(Long.class), anyBoolean());
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

    org.exoplatform.social.core.identity.model.Identity identity =
                                                                 mock(org.exoplatform.social.core.identity.model.Identity.class);
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
    draftPage.setOwner("john");
    draftPage.setWikiOwner("/space/groupId");

    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);

    Map<String, String> properties = new HashMap<>();
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = Arrays.asList(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    Space space1 = mockSpace();
    List<Space> myFilteredSpaces = Arrays.asList(space1);
    NEWS_UTILS.when(() -> NewsUtils.getMyFilteredSpacesIds(identity, new ArrayList<>())).thenReturn(myFilteredSpaces);
    when(metadataService.getMetadataItemsByFilter(any(), anyLong(), anyLong())).thenReturn(metadataItems);
    when(spaceService.canRedactOnSpace(any(Space.class), any(Identity.class))).thenReturn(true);
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(identity);

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
  public void testGetDraftArticlesWithOrphanedMetadata() throws Exception {

    // Given
    DraftPage draftPage1 = new DraftPage();
    draftPage1.setContent("draft body 1");
    draftPage1.setTitle("draft article 1");
    draftPage1.setId("1");
    draftPage1.setOwner("john");
    draftPage1.setWikiOwner("/space/groupId");

    DraftPage draftPage2 = new DraftPage();
    draftPage2.setContent("draft body 2");
    draftPage2.setTitle("draft article 2");
    draftPage2.setId("2");
    draftPage2.setOwner("john");
    draftPage2.setWikiOwner("/space/groupId");

    when(noteService.getDraftNoteById(eq("1"), anyString())).thenReturn(draftPage1);
    when(noteService.getDraftNoteById(eq("2"), anyString())).thenReturn(draftPage2);
    // "orphan" simulates a metadata row whose underlying draft note was deleted
    // without cleaning up its metadata item
    when(noteService.getDraftNoteById(eq("orphan"), anyString())).thenReturn(null);

    Map<String, String> properties = new HashMap<>();
    MetadataItem orphanMetadataItem = mock(MetadataItem.class);
    when(orphanMetadataItem.getObjectId()).thenReturn("orphan");
    when(orphanMetadataItem.getProperties()).thenReturn(properties);
    MetadataItem metadataItem1 = mock(MetadataItem.class);
    when(metadataItem1.getObjectId()).thenReturn("1");
    when(metadataItem1.getProperties()).thenReturn(properties);
    MetadataItem metadataItem2 = mock(MetadataItem.class);
    when(metadataItem2.getObjectId()).thenReturn("2");
    when(metadataItem2.getProperties()).thenReturn(properties);

    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(List.of(metadataItem1));
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    Space space1 = mockSpace();
    List<Space> myFilteredSpaces = Arrays.asList(space1);
    NEWS_UTILS.when(() -> NewsUtils.getMyFilteredSpacesIds(identity, new ArrayList<>())).thenReturn(myFilteredSpaces);
    // First page (offset=0, limit=2) returns the orphan alongside one real draft,
    // so only 1 real draft is resolved out of a full page of 2 metadata rows.
    when(metadataService.getMetadataItemsByFilter(any(), eq(0L), eq(2L)))
                                                                          .thenReturn(List.of(orphanMetadataItem, metadataItem1));
    // Second page (offset=2, limit=2) returns the remaining real draft.
    when(metadataService.getMetadataItemsByFilter(any(), eq(2L), eq(2L))).thenReturn(List.of(metadataItem2));
    when(spaceService.canRedactOnSpace(any(Space.class), any(Identity.class))).thenReturn(true);
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(identity);

    // When
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setDraftNews(true);
    newsFilter.setOffset(0);
    newsFilter.setLimit(2);
    List<News> newsList = newsService.getNews(newsFilter, identity);

    // Then
    assertNotNull(newsList);
    assertEquals(newsList.size(), 2);
  }

  @Test
  public void testPostNews() throws Exception {

    // Given
    News newsArticle = new News();
    newsArticle.setAuthor("john");
    newsArticle.setTitle("news article for new page");
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
    newsArticlePage.setProperties(new NotePageProperties(Long.parseLong(newsArticle.getId()), null, null, false, false, true));
    newsArticlePage.setLang(null);
    newsArticlePage.setAttachmentObjectType(NewsPageAttachmentPlugin.OBJECT_TYPE);

    Page createdPage = mock(Page.class);
    when(createdPage.getId()).thenReturn("1");
    when(noteService.createNote(wiki, rootPage.getName(), newsArticlePage, identity, false)).thenReturn(createdPage);
    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    when(pageVersion.getAuthor()).thenReturn("john");
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(new org.exoplatform.social.core.identity.model.Identity("1"));

    // When
    newsService.createNews(newsArticle, identity);

    // Then
    verify(noteService, times(1)).createNote(wiki, rootPage.getName(), newsArticlePage, identity, false);
    verify(noteService, times(1)).getPublishedVersionByPageIdAndLang(1L, null);
    verify(metadataService, atLeast(1)).createMetadataItem(any(MetadataObject.class),
                                                           any(MetadataKey.class),
                                                           anyMap(),
                                                           anyLong(),
                                                           anyBoolean());
    Page note = new Page();
    note.setId("1");
    note.setTitle(newsArticle.getTitle());
    note.setContent(newsArticle.getBody());
    note.setParentPageId(rootPage.getId());
    note.setAuthor(newsArticle.getAuthor());
    when(noteService.getNoteById(anyString())).thenReturn(note);
    clearInvocations(noteService, metadataService);
    newsService.createNews(newsArticle, identity);
    verify(noteService, times(0)).createNote(wiki, rootPage.getName(), newsArticlePage, identity, false);
    verify(noteService, times(1)).getPublishedVersionByPageIdAndLang(1L, null);
    verify(metadataService, atLeast(1)).createMetadataItem(any(MetadataObject.class),
                                                           any(MetadataKey.class),
                                                           anyMap(),
                                                           anyLong(),
                                                           anyBoolean());

    clearInvocations(activityManager);
    newsArticlePage.setAuthor(null);
    newsService.createNews(newsArticle, identity);
    verify(activityManager, times(1)).saveActivityNoReturn(any(), any());
  }

  @Test
  public void testCreateDraftArticleForExistingPage() throws Exception {
    // Given
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");
    when(existingPage.getWikiType()).thenReturn(PortalConfig.GROUP_TYPE);

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

    mockSpace();
    mockIdentity();

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
    news.setOriginalBody("body");

    // When, Then
    assertThrows(IllegalAccessException.class,
                 () -> newsService.updateNews(news,
                                              "john",
                                              false,
                                              false,
                                              NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(),
                                              CONTENT_AND_TITLE.name()));

    // Given
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getUpdatedDate()).thenReturn(new Date());
    when(draftPage.getCreatedDate()).thenReturn(new Date());
    when(draftPage.getAuthor()).thenReturn("john");
    when(draftPage.getId()).thenReturn("1");
    when(draftPage.getContent()).thenReturn("content");
    when(noteService.createDraftForExistPage(any(DraftPage.class),
                                             any(Page.class),
                                             nullable(String.class),
                                             anyLong(),
                                             anyString())).thenReturn(draftPage);

    // When
    newsService.updateNews(news, "john", false, false, LATEST_DRAFT.name().toLowerCase(), CONTENT_AND_TITLE.name());

    // Then
    verify(noteService, times(1)).createDraftForExistPage(any(DraftPage.class),
                                                          eq(existingPage),
                                                          nullable(String.class),
                                                          anyLong(),
                                                          anyString());
    verify(metadataService, times(1)).createMetadataItem(any(NewsLatestDraftObject.class),
                                                         any(MetadataKey.class),
                                                         any(Map.class),
                                                         anyLong(),
                                                         anyBoolean());

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

    mockSpace();
    mockIdentity();

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
    news.setOriginalBody("body");

    // When, Then
    assertThrows(IllegalAccessException.class,
                 () -> newsService.updateNews(news,
                                              "john",
                                              false,
                                              false,
                                              NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(),
                                              CONTENT_AND_TITLE.name()));

    // Given
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    when(noteService.updateDraftForExistPage(any(DraftPage.class),
                                             any(Page.class),
                                             nullable(String.class),
                                             anyLong(),
                                             anyString())).thenReturn(draftPage);

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    // When
    newsService.updateNews(news, "john", false, false, LATEST_DRAFT.name().toLowerCase(), CONTENT_AND_TITLE.name());

    // Then
    verify(noteService, times(1)).updateDraftForExistPage(any(DraftPage.class),
                                                          eq(existingPage),
                                                          nullable(String.class),
                                                          anyLong(),
                                                          anyString());
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong(), anyBoolean());
  }

  @Test
  public void testUpdateNewsArticle() throws Exception {
    // Given
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");
    when(existingPage.getWikiType()).thenReturn(PortalConfig.GROUP_TYPE);

    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);

    mockSpace();

    Identity identity = mockIdentity();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    NEWS_UTILS.when(() -> NewsUtils.processMentions(anyString(), any())).thenReturn(new HashSet<>());
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    DraftPage draftPage = mock(DraftPage.class);

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
    news.setOriginalBody("body");

    // When, Then
    assertThrows(IllegalAccessException.class,
                 () -> newsService.updateNews(news,
                                              "john",
                                              false,
                                              false,
                                              NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(),
                                              CONTENT_AND_TITLE.name()));

    // Given
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    when(noteService.updateNote(any(Page.class), any(), any(), anyBoolean())).thenReturn(existingPage);

    // When
    newsService.updateNews(news, "john", false, false, ARTICLE.name().toLowerCase(), CONTENT_AND_TITLE.name());

    // Then
    verify(noteService, times(1)).updateNote(any(Page.class), any(), any(), anyBoolean());
    verify(noteService, times(1)).createVersionOfNote(existingPage, identity.getUserId());
    verify(noteService, times(2)).getPublishedVersionByPageIdAndLang(1L, null);
  }

  @Test
  public void testUpdateNewsCategoriesWithoutActivity() throws Exception {
    // Given: an article that was never posted to the activity stream (no activityId) - the
    // exact scenario where updateNewsActivity() must fall back to linking categories directly
    // on the article instead of silently no-op'ing because there's no Activity to link on.
    Page existingPage = mock(Page.class);
    when(noteService.getNoteById(anyString())).thenReturn(existingPage);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");
    when(existingPage.getWikiType()).thenReturn(PortalConfig.GROUP_TYPE);
    when(existingPage.getAuthor()).thenReturn("john");

    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class))).thenReturn(metadataItems);
    Map<String, String> properties = new HashMap<>();
    when(metadataItem.getProperties()).thenReturn(properties);

    mockSpace();
    mockIdentity();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    NEWS_UTILS.when(() -> NewsUtils.processMentions(anyString(), any())).thenReturn(new HashSet<>());
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    when(activityManager.getActivity(nullable(String.class))).thenReturn(null);

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");
    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getId()).thenReturn("1");
    when(noteService.getLatestDraftPageByUserAndTargetPageAndLang(anyLong(),
                                                                  anyString(),
                                                                  nullable(String.class))).thenReturn(draftPage);

    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    when(noteService.updateNote(any(Page.class), any(), any(), anyBoolean())).thenReturn(existingPage);
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                        mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(socialIdentity);
    when(socialIdentity.getId()).thenReturn("1");

    News news = new News();
    news.setAuthor("john");
    news.setTitle("new draft title");
    news.setBody("draft body");
    news.setId("1");
    news.setPublicationState(POSTED);
    news.setSpaceId("1");
    news.setOriginalBody("body");
    news.setCategories(Arrays.asList(3L, 5L));

    // When
    newsService.updateNews(news, "john", false, false, ARTICLE.name().toLowerCase(), CATEGORIES.name());

    // Then: categories are linked directly on the article (CategoryObject("news", "1", ...)),
    // not silently dropped for lack of an activityId.
    CategoryObject expectedObject = NewsCategoryPlugin.toCategoryObject(news);
    verify(categoryLinkService, times(1)).link(3L, expectedObject);
    verify(categoryLinkService, times(1)).link(5L, expectedObject);
    verify(categoryLinkService, never()).unlink(anyLong(), any());
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
    mockSpace();
    Identity identity = mockIdentity();
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(new org.exoplatform.social.core.identity.model.Identity("1"));
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);

    when(existingPage.getOwner()).thenReturn("john");
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");
    //
    assertThrows(IllegalAccessException.class,
                 () -> newsService.deleteNews(existingPage.getId(), identity, ARTICLE.name().toLowerCase()));

    // when
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    when(spaceService.isRedactor(any(Space.class), anyString())).thenReturn(false);
    when(spaceService.isManager(any(Space.class), anyString())).thenReturn(false);
    when(spaceService.isMember(anyString(), anyString())).thenReturn(true);

    when(noteService.deleteNote(existingPage.getWikiType(),
                                existingPage.getWikiOwner(),
                                existingPage.getName())).thenReturn(true);
    DraftPage draftPage = mock(DraftPage.class);
    NotePageProperties draftProperties = new NotePageProperties();
    NoteFeaturedImage noteFeaturedImage = new NoteFeaturedImage();
    noteFeaturedImage.setId(123L);
    draftProperties.setFeaturedImage(noteFeaturedImage);
    when(draftPage.getId()).thenReturn("1");
    when(draftPage.getProperties()).thenReturn(draftProperties);
    when(noteService.getDraftNoteById(anyString(), anyString())).thenReturn(draftPage);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(new org.exoplatform.social.core.identity.model.Identity("1"));
    doNothing().when(noteService).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());

    when(existingPage.isDeleted()).thenReturn(false);
    newsService.deleteNews(existingPage.getId(), identity, ARTICLE.name().toLowerCase());

    // Then
    verify(noteService, times(1)).deleteNote(existingPage.getWikiType(), existingPage.getWikiOwner(), existingPage.getName());
    verify(activityManager, times(1)).deleteActivity("1");
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong(), anyBoolean());

    clearInvocations(noteService, activityManager, metadataService);
    when(existingPage.isDeleted()).thenReturn(true);

    newsService.deleteNews(existingPage.getId(), identity, ARTICLE.name().toLowerCase());

    verify(noteService, times(0)).deleteNote(existingPage.getWikiType(), existingPage.getWikiOwner(), existingPage.getName());
    verify(noteService, times(0)).removeDraftById("1");
    verify(activityManager, times(1)).deleteActivity("1");
    verify(metadataService, times(1)).updateMetadataItem(any(MetadataItem.class), anyLong(), anyBoolean());
  }

  @Test
  public void testScheduleNews() throws Exception {
    Space space = mockSpace();
    Identity identity = mockIdentity();
    when(spaceService.isMember(space, identity.getUserId())).thenReturn(true);
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
    newsArticle.setOwner("john");
    newsArticle.setTitle("news article");
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
    verify(noteService, times(1)).createNote(any(Wiki.class), anyString(), any(Page.class), any(Identity.class), anyBoolean());
  }

  @Test
  public void testGetScheduledArticles() throws Exception {
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setScheduledNews(true);
    Map<String, String> properties = new HashMap<>();
    Map<String, String> properties2 = new HashMap<>();
    properties.put(NEWS_PUBLICATION_STATE, "staged");
    properties.put(NEWS_DELETED, String.valueOf(false));
    MetadataItem metadataItem = mock(MetadataItem.class);
    MetadataItem metadataItem2 = mock(MetadataItem.class);
    properties2.put(UNPUBLISH_SCHEDULED, "true");
    properties2.put(NEWS_DELETED, String.valueOf(false));
    List<MetadataItem> metadataItems = List.of(metadataItem, metadataItem2);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);
    when(metadataItem2.getObjectId()).thenReturn("2");
    when(metadataItem2.getProperties()).thenReturn(properties2);

    mockBuildArticle(metadataItems);
    when(spaceService.isMember(any(Space.class), anyString())).thenReturn(true);
    when(spaceService.isManager(any(Space.class), anyString())).thenReturn(true);

    List<News> newsList = newsService.getNews(newsFilter, johnIdentity);
    assertNotNull(newsList);
    assertEquals(newsList.size(), 2);
  }

  @Test
  public void searchNews() throws Exception {
    List<NewsESSearchResult> results = new ArrayList<>();
    NewsESSearchResult newsESSearchResult = new NewsESSearchResult();
    newsESSearchResult.setId("1");
    results.add(newsESSearchResult);
    when(newsSearchConnector.search(any(), any())).thenReturn(results);
    Map<String, String> properties = new HashMap<>();
    properties.put(NEWS_PUBLICATION_STATE, "staged");
    properties.put(NEWS_DELETED, String.valueOf(false));
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = List.of(metadataItem);
    when(metadataItem.getObjectId()).thenReturn("1");
    when(metadataItem.getProperties()).thenReturn(properties);
    mockBuildArticle(metadataItems);
    org.exoplatform.social.core.identity.model.Identity currentIdentity =
                                                                        mock(org.exoplatform.social.core.identity.model.Identity.class);
    List<News> news = newsService.searchNews(new NewsFilter(), currentIdentity);
    assertNotNull(news);
    assertEquals(news.size(), 1);
  }

  @Test
  public void testUnScheduleNews() throws Exception {
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setScheduledNews(true);
    Map<String, String> properties = new HashMap<>();
    properties.put(SCHEDULE_POST_DATE, "05/05/2024 08:00:00 +0100");
    MetadataItem metadataItem = mock(MetadataItem.class);
    List<MetadataItem> metadataItems = List.of(metadataItem);
    when(metadataItem.getProperties()).thenReturn(properties);

    mockBuildArticle(metadataItems);
    Space space = mockSpace();
    when(space.getGroupId()).thenReturn("/spaces/test");
    Wiki wiki = mock(Wiki.class);
    when(wikiService.getWikiByTypeAndOwner(anyString(), anyString())).thenReturn(wiki);
    org.exoplatform.wiki.model.Page rootPage = mock(org.exoplatform.wiki.model.Page.class);
    when(rootPage.getName()).thenReturn(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
    when(rootPage.getId()).thenReturn("1");
    when(noteService.getNoteOfNoteBookByName("group",
                                             space.getGroupId(),
                                             NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME)).thenReturn(rootPage);

    News newsArticle = mock(News.class);
    when(newsArticle.getId()).thenReturn("1");
    when(newsArticle.getBody()).thenReturn("body");
    when(newsArticle.getPublicationState()).thenReturn("staged");
    when(newsArticle.getProperties()).thenReturn(new NotePageProperties());

    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getUpdatedDate()).thenReturn(new Date());
    when(draftPage.getCreatedDate()).thenReturn(new Date());
    when(draftPage.getAuthor()).thenReturn("john");
    when(draftPage.getId()).thenReturn("1");
    when(noteService.createDraftForNewPage(any(DraftPage.class), anyLong(), anyLong())).thenReturn(draftPage);

    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    newsService.unScheduleNews(newsArticle, space, "john");

    verify(noteService, times(1)).createDraftForNewPage(any(DraftPage.class), anyLong(), anyLong());
    verify(noteService, times(1)).deleteNote(anyString(), anyString(), anyString());

    clearInvocations(noteService);
    properties.put(EXTERNAL_PAGE, "true");
    newsService.unScheduleNews(newsArticle, space, "john");
    verify(noteService, times(0)).createDraftForNewPage(any(DraftPage.class), anyLong(), anyLong());
    verify(noteService, times(0)).deleteNote(anyString(), anyString(), anyString());
    verify(metadataService, times(2)).deleteMetadataItemsByObject(any());
  }

  @Test
  public void testAddNewsArticleTranslation() throws Exception {
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
    mockBuildArticle(metadataItems);

    mockSpace();

    Identity identity = mockIdentity();
    NEWS_UTILS.when(() -> NewsUtils.canPublishNews(anyString(), any(Identity.class))).thenReturn(false);
    NEWS_UTILS.when(() -> NewsUtils.processMentions(anyString(), any())).thenReturn(new HashSet<>());
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);

    DraftPage draftPage = mock(DraftPage.class);

    when(draftPage.getId()).thenReturn("1");

    PageVersion pageVersion = mock(PageVersion.class);
    when(noteService.getPublishedVersionByPageIdAndLang(1L, "fr")).thenReturn(pageVersion);
    when(noteService.getLatestDraftPageByUserAndTargetPageAndLang(anyLong(),
                                                                  anyString(),
                                                                  anyString())).thenReturn(draftPage);

    when(existingPage.getAuthor()).thenReturn("john");
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("full name");
    when(pageVersion.getContent()).thenReturn("content");

    News news = new News();
    news.setAuthor("john");
    news.setTitle("new draft title");
    news.setBody("draft body");
    news.setId("1");
    news.setPublicationState(POSTED);
    news.setSpaceId("1");
    news.setOriginalBody("body");
    news.setLang("fr");

    // When, Then
    assertThrows(IllegalAccessException.class,
                 () -> newsService.updateNews(news,
                                              "john",
                                              false,
                                              false,
                                              NewsUtils.NewsObjectType.DRAFT.name().toLowerCase(),
                                              CONTENT_AND_TITLE.name()));

    // Given
    when(spaceService.isSuperManager(any(Space.class), anyString())).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity identity1 =
                                                                  mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(anyString())).thenReturn(identity1);
    when(identity1.getId()).thenReturn("1");

    when(noteService.updateNote(any(Page.class), any(), any(), anyBoolean())).thenReturn(existingPage);
    // When
    newsService.updateNews(news, "john", false, false, ARTICLE.name().toLowerCase(), CONTENT_AND_TITLE.name());

    // Then
    verify(noteService, times(1)).updateNote(any(Page.class), any(), any(), anyBoolean());
    verify(noteService, times(1)).createVersionOfNote(existingPage, identity.getUserId());
    verify(noteService, times(2)).getPublishedVersionByPageIdAndLang(1L, null);
    NEWS_UTILS.verify(() -> NewsUtils.broadcastEvent(eq(NewsUtils.ADD_ARTICLE_TRANSLATION), any(), any()), times(1));
  }

  @Test
  public void testSearchNewsOfOneSpace() {
    NewsFilter filter = new NewsFilter();
    String spaceId = "1";
    long spaceIdentityId = 10L;
    filter.setSpaces(Arrays.asList(spaceId));
    filter.setAuthor("john");
    filter.setSearchText("test");
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(userIdentity.getRemoteId()).thenReturn("root");
    SPACE_UTILS.when(() -> SpaceUtils.getSpaceIdentityIds(anyString(), anyList())).thenReturn(Arrays.asList(String.valueOf(spaceIdentityId)));



    newsService.search(userIdentity, filter);
    assertEquals(1, filter.getSpaces().size());
    assertEquals(Long.toString(spaceIdentityId), filter.getSpaces().getFirst());
    verify(newsSearchConnector, times(1)).search(userIdentity, filter);

  }

  @Test
  public void testUpdateMetadataProperties() throws Exception {

    // Given - article not found (noteService returns null)
    when(noteService.getNoteById(eq("99"))).thenReturn(null);

    Map<String, String> inputProperties = new HashMap<>();
    inputProperties.put("key1", "value1");

    // When, Then
    assertThrows(ObjectNotFoundException.class,
                 () -> newsService.updateMetadataProperties("99", inputProperties, 1L));

    Page existingPage = mock(Page.class);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");
    when(existingPage.getWikiType()).thenReturn(PortalConfig.GROUP_TYPE);
    when(existingPage.getAuthor()).thenReturn("john");
    when(noteService.getNoteById(eq("1"))).thenReturn(existingPage);

    Space space = mock(Space.class);
    when(space.getId()).thenReturn("1");
    when(space.getGroupId()).thenReturn("/space/groupId");
    when(space.getAvatarUrl()).thenReturn("space/avatar/url");
    when(space.getDisplayName()).thenReturn("spaceDisplayName");
    when(space.getVisibility()).thenReturn("public");
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space);

    PageVersion pageVersion = mock(PageVersion.class);
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getTitle()).thenReturn("title");
    when(pageVersion.getContent()).thenReturn("content");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("John Doe");
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);

    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    PORTAL_CONTAINER.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    COMMONS_UTILS.when(CommonsUtils::getCurrentPortalOwner).thenReturn("dw");
    NEWS_UTILS.when(() -> NewsUtils.buildNewsArticleUrl(any(), any())).thenReturn("url");

    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class)))
        .thenReturn(new ArrayList<>());

    assertThrows(ObjectNotFoundException.class,
                 () -> newsService.updateMetadataProperties("1", inputProperties, 1L));

    MetadataItem existingMetadataItem = mock(MetadataItem.class);
    Map<String, String> existingProperties = new HashMap<>();
    existingProperties.put("existingKey", "existingValue");
    existingProperties.put("key1", "oldValue");

    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class),
                                                             any(MetadataObject.class)))
        .thenReturn(List.of(existingMetadataItem));
    when(existingMetadataItem.getProperties()).thenReturn(existingProperties);
    when(existingMetadataItem.getId()).thenReturn(1L);

    Map<String, String> newProperties = new HashMap<>();
    newProperties.put("key1", "newValue");
    newProperties.put("key2", "value2");

    // When
    Map<String, String> mergedResult = newsService.updateMetadataProperties("1", newProperties, 1L);

    // Then
    verify(metadataService, times(1)).updateMetadataItem(eq(existingMetadataItem), eq(1L), eq(false));
    assertNotNull(mergedResult);
    assertEquals("newValue", mergedResult.get("key1"));
    assertEquals("value2", mergedResult.get("key2"));
    assertEquals("existingValue", mergedResult.get("existingKey"));
  }

  @Test
  public void testRemoveArticleMetadataProperty() throws Exception {
    Page existingPage = mock(Page.class);
    when(existingPage.getId()).thenReturn("1");
    when(existingPage.getWikiOwner()).thenReturn("/space/groupId");
    when(existingPage.getWikiType()).thenReturn(PortalConfig.GROUP_TYPE);
    when(existingPage.getAuthor()).thenReturn("john");
    when(noteService.getNoteById(eq("1"))).thenReturn(existingPage);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(mock(Space.class));

    Space space = mock(Space.class);
    when(space.getId()).thenReturn("1");
    when(space.getGroupId()).thenReturn("/space/groupId");
    when(space.getVisibility()).thenReturn("public");
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space);

    PageVersion pageVersion = mock(PageVersion.class);
    when(pageVersion.getAuthor()).thenReturn("john");
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    when(pageVersion.getAuthorFullName()).thenReturn("John Doe");
    when(noteService.getPublishedVersionByPageIdAndLang(1L, null)).thenReturn(pageVersion);
    when(newsTargetingService.getTargetsByNews(any(News.class))).thenReturn(null);
    PORTAL_CONTAINER.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    COMMONS_UTILS.when(CommonsUtils::getCurrentPortalOwner).thenReturn("dw");
    NEWS_UTILS.when(() -> NewsUtils.buildNewsArticleUrl(any(), any())).thenReturn("url");

    MetadataItem articleMetadataItem = mock(MetadataItem.class);
    when(articleMetadataItem.getProperties()).thenReturn(new HashMap<>());

    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class), any(MetadataObject.class)))
        .thenReturn(new ArrayList<>());

    newsService.removeArticleMetadataProperty("1", "key1", 1L);
    verify(metadataService, times(0)).updateMetadataItem(any(MetadataItem.class), anyLong(), anyBoolean());

    MetadataItem pageMetadataItem = mock(MetadataItem.class);
    Map<String, String> props = new HashMap<>();
    props.put("key1", "value1");
    props.put("key2", "value2");
    when(pageMetadataItem.getProperties()).thenReturn(props);

    when(metadataService.getMetadataItemsByMetadataAndObject(any(MetadataKey.class), any(MetadataObject.class)))
        .thenReturn(List.of(articleMetadataItem))
        .thenReturn(List.of(pageMetadataItem));

    newsService.removeArticleMetadataProperty("1", "key1", 1L);

    verify(metadataService, times(1)).updateMetadataItem(eq(pageMetadataItem), eq(1L), eq(false));
    assertFalse(props.containsKey("key1"));
    assertEquals("value2", props.get("key2"));
  }

  private void mockBuildArticle(List<MetadataItem> metadataItems) throws WikiException {
    when(metadataService.getMetadataItemsByFilter(any(), anyLong(), anyLong())).thenReturn(metadataItems);
    Page page = new Page();
    page.setContent("article body");
    page.setTitle("article");
    page.setId("1");
    page.setOwner("john");
    page.setWikiOwner("/space/groupId");
    page.setName("article name");
    page.setWikiType("group");
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
    when(spaceService.getSpaceByGroupId(nullable(String.class))).thenReturn(space);
    return space;
  }

  private Identity mockIdentity() {
    Identity identity = mock(Identity.class);
    when(identity.getUserId()).thenReturn("john");
    NEWS_UTILS.when(() -> NewsUtils.getUserIdentity(anyString())).thenReturn(identity);
    return identity;
  }
}
