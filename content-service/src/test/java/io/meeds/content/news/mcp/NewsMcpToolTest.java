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
package io.meeds.content.news.mcp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.news.mcp.model.NewsModel;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils.NewsObjectType;
import io.meeds.mcp.server.tool.model.SpaceModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.SpaceToolUtils;
import io.meeds.mcp.server.tool.util.UploadToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;

@RunWith(MockitoJUnitRunner.class)
public class NewsMcpToolTest {

  private static final String     CONTENT  = "Content";

  private static final String     SUMMARY  = "Summary";

  private static final String     TITLE    = "Title";

  private static final String     USER     = "root";

  private static final long       NEWS_ID  = 12L;

  private static final long       SPACE_ID = 33L;

  @Mock
  private PortalContainer         container;

  @Mock
  private SpaceService            spaceService;

  @Mock
  private ActivityManager         activityManager;

  @Mock
  private PermanentLinkService    permanentLinkService;

  @Mock
  private I18NActivityProcessor   i18NActivityProcessor;

  @Mock
  private IdentityManager         identityManager;

  @Mock
  private NewsService             newsService;

  @Mock
  private UserACL                 userAcl;

  @Mock
  private ProfilePropertyService  profilePropertyService;

  @Mock
  private TranslationService      translationService;

  @Mock
  private UserPortalConfigService portalConfigService;

  @Mock
  private NoteService             noteService;

  @Mock
  private UploadService           uploadService;

  @Mock
  private AttachmentService       attachmentService;

  @Mock
  private FileService             fileService;

  @Mock
  private Identity                currentIdentity;

  private NewsMcpTool             tool;

  @Before
  public void setUp() {
    when(container.getComponentInstanceOfType(SpaceService.class)).thenReturn(spaceService);
    when(container.getComponentInstanceOfType(ActivityManager.class)).thenReturn(activityManager);
    when(container.getComponentInstanceOfType(PermanentLinkService.class)).thenReturn(permanentLinkService);
    when(container.getComponentInstanceOfType(I18NActivityProcessor.class)).thenReturn(i18NActivityProcessor);
    when(container.getComponentInstanceOfType(IdentityManager.class)).thenReturn(identityManager);
    when(container.getComponentInstanceOfType(NewsService.class)).thenReturn(newsService);
    when(container.getComponentInstanceOfType(UserACL.class)).thenReturn(userAcl);
    when(container.getComponentInstanceOfType(ProfilePropertyService.class)).thenReturn(profilePropertyService);
    when(container.getComponentInstanceOfType(TranslationService.class)).thenReturn(translationService);
    when(container.getComponentInstanceOfType(UserPortalConfigService.class)).thenReturn(portalConfigService);
    when(container.getComponentInstanceOfType(NoteService.class)).thenReturn(noteService);
    when(container.getComponentInstanceOfType(UploadService.class)).thenReturn(uploadService);
    when(container.getComponentInstanceOfType(AttachmentService.class)).thenReturn(attachmentService);
    when(container.getComponentInstanceOfType(FileService.class)).thenReturn(fileService);

    lenient().when(currentIdentity.getUserId()).thenReturn(USER);
    lenient().when(userAcl.getUserIdentity(USER)).thenReturn(currentIdentity);

    PortalConfig portalConfig = mock(PortalConfig.class);
    lenient().when(portalConfig.getName()).thenReturn("intranet");
    lenient().when(portalConfigService.getDefaultSite(USER)).thenReturn(portalConfig);

    tool = new TestableNewsMcpTool(container);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createNewsWhenSpaceIdMissingShouldThrowException() throws Exception { // NOSONAR
    tool.createNews(TITLE, SUMMARY, CONTENT, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createNewsWhenTitleBlankShouldThrowException() throws Exception { // NOSONAR
    tool.createNews(" ", SUMMARY, CONTENT, SPACE_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createNewsWhenContentBlankShouldThrowException() throws Exception { // NOSONAR
    tool.createNews(TITLE, SUMMARY, " ", SPACE_ID);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void createNewsWhenSpaceDoesNotExistShouldThrowException() throws Exception { // NOSONAR
    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(null);

    tool.createNews(TITLE, SUMMARY, CONTENT, SPACE_ID);
  }

  @Test(expected = IllegalAccessException.class)
  public void createNewsWhenUserCannotCreateNewsShouldThrowException() throws Exception { // NOSONAR
    Space space = mockSpace();

    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);
    when(newsService.canCreateNews(space, currentIdentity)).thenReturn(false);

    tool.createNews(TITLE, SUMMARY, CONTENT, SPACE_ID);
  }

  @Test
  public void createNewsShouldCreateAndReturnNewsModel() throws Exception { // NOSONAR
    Space space = mockSpace();
    News createdNews = mockNews();

    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);
    when(newsService.canCreateNews(space, currentIdentity)).thenReturn(true);
    when(newsService.createNewsArticlePage(any(News.class), eq(USER))).thenReturn(createdNews);
    when(newsService.getNewsArticleById(String.valueOf(NEWS_ID))).thenReturn(createdNews);

    NewsModel result = runWithStaticMocks(() -> tool.createNews(TITLE, SUMMARY, "**Content**", SPACE_ID));

    assertEquals(NEWS_ID, result.id());
    assertEquals(TITLE, result.title());
    verify(newsService).createNewsArticlePage(any(News.class), eq(USER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void updateNewsWhenNewsIdInvalidShouldThrowException() throws Exception { // NOSONAR
    tool.updateNews(0, TITLE, SUMMARY, CONTENT, "en");
  }

  @Test(expected = ObjectNotFoundException.class)
  public void updateNewsWhenNewsDoesNotExistShouldThrowException() throws Exception { // NOSONAR
    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase())))
                                                                                  .thenReturn(null);

    tool.updateNews(NEWS_ID, TITLE, SUMMARY, CONTENT, "en");
  }

  @Test(expected = IllegalAccessException.class)
  public void updateNewsWhenUserCannotEditShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(false);

    tool.updateNews(NEWS_ID, TITLE, SUMMARY, CONTENT, "en");
  }

  @Test
  public void updateNewsShouldUpdateTitleSummaryAndContent() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    NewsModel result = runWithStaticMocks(() -> tool.updateNews(NEWS_ID, "Updated", "Updated summary", "Updated content", "en"));

    assertEquals(NEWS_ID, result.id());
    verify(news).setTitle("Updated");
    verify(news).setBody("<p>Updated content</p>");
    // title/body persisted via CONTENT_AND_TITLE, then the article is refreshed
    verify(newsService, atLeastOnce()).updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString());
    // summary persisted as note metadata (not via CONTENT_AND_TITLE)
    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("en"), eq(1L));
    assertEquals("Updated summary", news.getProperties().getSummary());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void deleteNewsWhenNewsDoesNotExistShouldThrowException() throws Exception { // NOSONAR
    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(null);

    tool.deleteNews(NEWS_ID);
  }

  @Test
  public void deleteNewsShouldDeleteArticle() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);

    tool.deleteNews(NEWS_ID);

    verify(newsService).deleteNews(String.valueOf(NEWS_ID), currentIdentity, NewsObjectType.ARTICLE.name());
  }

  @Test
  public void getNewsByIdShouldReturnNewsModel() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(true);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);

    NewsModel result = runWithStaticMocks(() -> tool.getNewsById(NEWS_ID));

    assertEquals(NEWS_ID, result.id());
    assertEquals(TITLE, result.title());
  }

  @Test(expected = IllegalAccessException.class)
  public void getNewsByIdWhenUserCannotViewShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(false);

    tool.getNewsById(NEWS_ID);
  }

  @Test
  public void getNewsListShouldUseDefaultPaginationAndMapNews() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNews(any(NewsFilter.class), eq(currentIdentity)))
                                                                         .thenReturn(Collections.singletonList(news));
    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);

    List<NewsModel> result = runWithStaticMocks(() -> tool.getNewsList(SPACE_ID, null, null));

    assertEquals(1, result.size());
    assertEquals(NEWS_ID, result.get(0).id());
    verify(newsService).getNews(any(NewsFilter.class), eq(currentIdentity));
  }

  @Test
  public void searchNewsShouldSearchAndMapNews() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                       mock(org.exoplatform.social.core.identity.model.Identity.class);

    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(socialIdentity);
    when(newsService.searchNews(any(NewsFilter.class), eq(socialIdentity)))
                                                                           .thenReturn(Collections.singletonList(news));
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);

    List<NewsModel> result = runWithStaticMocks(() -> tool.searchNews("title", null, 0, 5));

    assertEquals(1, result.size());
    assertEquals(NEWS_ID, result.get(0).id());
    verify(newsService).searchNews(any(NewsFilter.class), eq(socialIdentity));
  }

  @Test(expected = ObjectNotFoundException.class)
  public void publishNewsWhenNewsDoesNotExistShouldThrowException() throws Exception { // NOSONAR
    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(null);

    tool.publishNewsInActivityStream(NEWS_ID);
  }

  @Test(expected = IllegalAccessException.class)
  public void publishNewsWhenUserCannotPublishShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(false);

    tool.publishNewsInActivityStream(NEWS_ID);
  }

  @Test
  public void publishNewsShouldUpdatePostAndReturnActivity() throws Exception { // NOSONAR
    News article = mockNews();
    News draft = mockNews("13", SPACE_ID, "Draft title");
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase())))
                                                                                  .thenReturn(article);
    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.LATEST_DRAFT.name().toLowerCase())))
                                                                                       .thenReturn(draft);

    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(true);
    when(newsService.updateNews(eq(article), eq(USER), eq(true), anyBoolean(), anyString(), anyString()))
                                                                                                         .thenReturn(article);

    runWithStaticMocks(() -> tool.publishNewsInActivityStream(NEWS_ID));

    verify(article).setPublicationState(NewsService.POSTED);
    verify(article).setPublisher(USER);
    verify(article).setTitle("Draft title");
    verify(newsService).postNews(article, USER);
  }

  @Test
  public void updateNewsShouldSetSummaryOnProperties() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties properties = news.getProperties();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New summary", null, "en"));

    assertEquals("New summary", properties.getSummary());
    // summary persisted via the metadata-aware path, not CONTENT_AND_TITLE
    verify(noteService).saveNoteMetadata(eq(properties), eq("en"), eq(1L));
  }

  @Test
  public void updateNewsShouldPreserveFeaturedImageWhenUpdatingSummary() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties properties = news.getProperties();
    NoteFeaturedImage cover = new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null);
    properties.setFeaturedImage(cover);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New summary", null, "en"));

    // the cover image is not wiped when only the summary is updated
    assertEquals(cover, properties.getFeaturedImage());
    verify(noteService).saveNoteMetadata(eq(properties), eq("en"), eq(1L));
  }

  @Test
  public void updateNewsShouldUseArticleLanguageWhenLanguageBlank() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    lenient().when(news.getLang()).thenReturn("fr");

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New summary", null, null));

    // blank language falls back to the article's default language
    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("fr"), eq(1L));
  }

  @Test
  public void getNewsByIdShouldExposeIllustrationUrl() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(true);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);

    NewsModel result = runWithStaticMocks(() -> tool.getNewsById(NEWS_ID));

    assertEquals("/portal/rest/notes/illustration/" + NEWS_ID, result.illustrationUrl());
  }

  @Test(expected = IllegalAccessException.class)
  public void setNewsIllustrationWhenUserCannotEditShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(false);

    tool.setNewsIllustration(NEWS_ID, "https://meeds.test/cover.png", null, null, null, "alt", "en");
  }

  @Test
  public void setNewsIllustrationShouldSaveMetadataAndRefresh() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    NewsModel result = runWithUploadMocks(() -> tool.setNewsIllustration(NEWS_ID,
                                                                         "https://meeds.test/cover.png",
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         "alt",
                                                                         "en"));

    assertEquals(NEWS_ID, result.id());
    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("en"), eq(1L));
    verify(newsService).updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void removeNewsIllustrationWhenNoCoverShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);

    tool.removeNewsIllustration(NEWS_ID, "en");
  }

  @Test
  public void removeNewsIllustrationShouldRemoveFeaturedImageAndRefresh() throws Exception { // NOSONAR
    News news = mockNews();
    news.getProperties().setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                           .thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    NewsModel result = runWithStaticMocks(() -> tool.removeNewsIllustration(NEWS_ID, "en"));

    assertEquals(NEWS_ID, result.id());
    verify(noteService).removeNoteFeaturedImage(eq(NEWS_ID), eq(500L), eq("en"), eq(false), eq(1L));
    verify(newsService).updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString());
  }

  private void mockUserIdentity() {
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                       mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(socialIdentity.getId()).thenReturn("1");
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(socialIdentity);
  }

  private News mockNews() {
    return mockNews(String.valueOf(NEWS_ID), SPACE_ID, TITLE);
  }

  private News mockNews(String id, long spaceId, String title) {
    News news = mock(News.class);
    NotePageProperties properties = new NotePageProperties();
    properties.setSummary(SUMMARY);

    lenient().when(news.getId()).thenReturn(id);
    lenient().when(news.getTitle()).thenReturn(title);
    lenient().when(news.getBody()).thenReturn("<p>Content</p>");
    lenient().when(news.getUrl()).thenReturn("/news/" + id);
    lenient().when(news.getSpaceId()).thenReturn(String.valueOf(spaceId));
    lenient().when(news.getProperties()).thenReturn(properties);
    lenient().when(news.getPublisher()).thenReturn(USER);
    lenient().when(news.getPublicationDate()).thenReturn(new Date());
    lenient().when(news.isPublished()).thenReturn(true);
    lenient().when(news.isActivityPosted()).thenReturn(true);
    lenient().when(news.getActivityId()).thenReturn("99");
    lenient().when(news.getViewsCount()).thenReturn(3L);
    lenient().when(news.getCommentsCount()).thenReturn(1);
    lenient().when(news.getLikesCount()).thenReturn(2);
    lenient().when(news.getIllustrationURL()).thenReturn("/portal/rest/notes/illustration/" + id);
    return news;
  }

  private Space mockSpace() {
    Space space = mock(Space.class);
    lenient().when(space.getSpaceId()).thenReturn(SPACE_ID);
    lenient().when(space.getDisplayName()).thenReturn("Space");
    lenient().when(space.getDescription()).thenReturn("Space description");
    lenient().when(space.getVisibility()).thenReturn("private");
    lenient().when(space.getRegistration()).thenReturn("closed");
    lenient().when(space.getAvatarUrl()).thenReturn("/avatar");
    lenient().when(space.getBannerUrl()).thenReturn("/banner");
    lenient().when(space.getMembers()).thenReturn(new String[] { USER });
    lenient().when(space.getManagers()).thenReturn(new String[] { USER });
    lenient().when(space.getCategoryIds()).thenReturn(Collections.emptyList());
    return space;
  }

  private <T> T runWithStaticMocks(CheckedSupplier<T> supplier) throws Exception { // NOSONAR
    try (MockedStatic<McpToolUtils> mcp = mockStatic(McpToolUtils.class);
        MockedStatic<CommonsUtils> commons = mockStatic(CommonsUtils.class);
        MockedStatic<UserToolUtils> users = mockStatic(UserToolUtils.class);
        MockedStatic<SpaceToolUtils> spaces = mockStatic(SpaceToolUtils.class)) {

      mcp.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
      mcp.when(() -> McpToolUtils.formatDate(any(Date.class))).thenReturn("2024-01-01T00:00:00Z");
      mcp.when(() -> McpToolUtils.markdownToHtml(anyString()))
         .thenAnswer(invocation -> "<p>" + invocation.getArgument(0) + "</p>");

      commons.when(CommonsUtils::getCurrentDomain).thenReturn("https://meeds.test");

      UserModel user = new UserModel();
      user.setUsername(USER);
      user.setDisplayName("Root User");

      users.when(() -> UserToolUtils.toUserModel(any(),
                                                 any(),
                                                 any(),
                                                 any(),
                                                 any(),
                                                 anyString(),
                                                 anyString(),
                                                 any(),
                                                 anyBoolean()))
           .thenReturn(user);

      SpaceModel spaceModel = new SpaceModel();
      spaceModel.setSpaceId(SPACE_ID);
      spaceModel.setName("Space");

      spaces.when(() -> SpaceToolUtils.toSpaceModel(any(), any(), anyString()))
            .thenReturn(spaceModel);

      return supplier.get();
    }
  }

  private <T> T runWithUploadMocks(CheckedSupplier<T> supplier) throws Exception { // NOSONAR
    try (MockedStatic<UploadToolUtils> upload = mockStatic(UploadToolUtils.class)) {
      UploadToolUtils.FetchedContent image = new UploadToolUtils.FetchedContent(new byte[] { 1, 2, 3 }, "image/png", "image.png");
      upload.when(() -> UploadToolUtils.resolveImage(any(),
                                                     any(),
                                                     any(),
                                                     any(),
                                                     anyLong()))
            .thenReturn(image);
      upload.when(() -> UploadToolUtils.materialize(any(), any(), anyString(), anyString())).thenReturn("upload-1");
      return runWithStaticMocks(supplier);
    }
  }

  private class TestableNewsMcpTool extends NewsMcpTool {

    TestableNewsMcpTool(PortalContainer container) {
      super(container);
    }

    @Override
    public Identity getCurrentUserAclIdentity() {
      return currentIdentity;
    }

    @Override
    public String getCurrentUserName() {
      return USER;
    }

    @Override
    public Locale getCurrentUserLocale() {
      return Locale.ENGLISH;
    }

    @Override
    public Locale getCurrentUserLocale(String username) {
      return Locale.ENGLISH;
    }
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws Exception; // NOSONAR
  }

}
