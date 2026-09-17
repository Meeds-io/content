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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.news.mcp.model.NewsModel;
import io.meeds.content.news.mcp.model.NewsTargetModel;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.rest.model.NewsTargetingEntity;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.service.NewsTargetingService;
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
  private NewsTargetingService    newsTargetingService;

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
    when(container.getComponentInstanceOfType(NewsTargetingService.class)).thenReturn(newsTargetingService);

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
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(null);

    tool.updateNews(NEWS_ID, TITLE, SUMMARY, CONTENT, "en");
  }

  @Test(expected = IllegalAccessException.class)
  public void updateNewsWhenUserCannotEditShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(false);

    tool.updateNews(NEWS_ID, TITLE, SUMMARY, CONTENT, "en");
  }

  @Test
  public void updateNewsShouldUpdateTitleSummaryAndContent() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
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

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(true);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);

    NewsModel result = runWithStaticMocks(() -> tool.getNewsById(NEWS_ID, null));

    assertEquals(NEWS_ID, result.id());
    assertEquals(TITLE, result.title());
  }

  @Test(expected = IllegalAccessException.class)
  public void getNewsByIdWhenUserCannotViewShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(false);

    tool.getNewsById(NEWS_ID, null);
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
  public void listNewsTargetsShouldReturnTargetsWithCanPublishFlag() throws Exception { // NOSONAR
    NewsTargetingEntity slider = mockTarget("slider", "Homepage slider");
    NewsTargetingEntity latest = mockTarget("latestNews", "Latest news");

    when(newsTargetingService.getAllTargets()).thenReturn(List.of(slider, latest));
    when(newsTargetingService.getAllowedTargets(currentIdentity)).thenReturn(List.of(slider));

    List<NewsTargetModel> result = tool.listNewsTargets();

    assertEquals(2, result.size());
    NewsTargetModel sliderModel = result.stream().filter(t -> "slider".equals(t.name())).findFirst().orElseThrow();
    NewsTargetModel latestModel = result.stream().filter(t -> "latestNews".equals(t.name())).findFirst().orElseThrow();
    assertEquals("Homepage slider", sliderModel.label());
    assertTrue(sliderModel.canPublish());
    assertFalse(latestModel.canPublish());
  }

  @Test(expected = IllegalAccessException.class)
  public void publishNewsWhenUserCannotPublishShouldThrowAccessException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(false);

    tool.publishNews(NEWS_ID, List.of("slider"));
  }

  @Test
  public void publishNewsWithTargetsShouldCallSaveNewsTarget() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(true);

    List<String> targets = List.of("slider", "latestNews");
    NewsModel result = runWithStaticMocks(() -> tool.publishNews(NEWS_ID, targets));

    assertEquals(NEWS_ID, result.id());
    verify(newsTargetingService).saveNewsTarget(news, true, targets, USER);
    verify(newsService, never()).postNews(any(News.class), anyString());
  }

  @Test
  public void publishNewsWithEmptyTargetsShouldFallBackToStreamPublish() throws Exception { // NOSONAR
    News article = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(article);
    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.LATEST_DRAFT.name().toLowerCase()))).thenReturn(null);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(true);
    when(newsService.updateNews(eq(article), eq(USER), eq(true), anyBoolean(), anyString(), anyString()))
                                                                                                         .thenReturn(article);

    runWithStaticMocks(() -> tool.publishNews(NEWS_ID, Collections.emptyList()));

    verify(newsService).postNews(article, USER);
    verify(newsTargetingService, never()).saveNewsTarget(any(News.class), anyBoolean(), anyList(), anyString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void scheduleNewsWhenPublishDateBlankShouldThrowException() throws Exception { // NOSONAR
    tool.scheduleNews(NEWS_ID, " ", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void scheduleNewsWhenPublishDateInvalidShouldThrowException() throws Exception { // NOSONAR
    tool.scheduleNews(NEWS_ID, "not-a-date", null);
  }

  @Test
  public void scheduleNewsShouldSetScheduleDateAndCallService() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canScheduleNews(String.valueOf(SPACE_ID), currentIdentity, news)).thenReturn(true);
    when(newsService.scheduleNews(eq(news), eq(currentIdentity), eq(NewsObjectType.ARTICLE.name()))).thenReturn(news);

    List<String> targets = List.of("slider");
    NewsModel result = runWithStaticMocks(() -> tool.scheduleNews(NEWS_ID, "2026-07-10T09:00:00Z", targets));

    assertEquals(NEWS_ID, result.id());
    verify(news).setSchedulePostDate("2026-07-10T09:00:00Z");
    verify(news).setPublicationState(NewsService.STAGED);
    verify(newsService).scheduleNews(news, currentIdentity, NewsObjectType.ARTICLE.name());
    verify(newsTargetingService).saveNewsTarget(news, false, targets, USER);
  }

  @Test(expected = IllegalAccessException.class)
  public void scheduleNewsWhenUserCannotScheduleShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canScheduleNews(String.valueOf(SPACE_ID), currentIdentity, news)).thenReturn(false);

    tool.scheduleNews(NEWS_ID, "2026-07-10T09:00:00Z", null);
  }

  @Test
  public void unpublishNewsShouldCallService() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(true);

    NewsModel result = runWithStaticMocks(() -> tool.unpublishNews(NEWS_ID));

    assertEquals(NEWS_ID, result.id());
    verify(newsService).unpublishNews(String.valueOf(NEWS_ID), USER, false);
  }

  @Test(expected = IllegalAccessException.class)
  public void unpublishNewsWhenUserCannotPublishShouldThrowException() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)),
                                 eq(currentIdentity),
                                 eq(false),
                                 eq(NewsObjectType.ARTICLE.name().toLowerCase()))).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(spaceService.canPublishOnSpace(space, USER)).thenReturn(false);

    tool.unpublishNews(NEWS_ID);
  }

  private NewsTargetingEntity mockTarget(String name, String label) {
    NewsTargetingEntity target = new NewsTargetingEntity();
    target.setName(name);
    target.setProperties(Map.of("label", label));
    return target;
  }

  @Test
  public void updateNewsShouldSetSummaryOnProperties() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties properties = news.getProperties();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
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

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New summary", null, "en"));

    // the cover image is not wiped when only the summary is updated
    assertEquals(cover, properties.getFeaturedImage());
    verify(noteService).saveNoteMetadata(eq(properties), eq("en"), eq(1L));
  }

  // Regression for EXO-88373: editing a translation's summary must not clobber
  // that translation's OWN cover with the default article's. The "fr" backing
  // note already has its own distinct featured image (600); updating the "fr"
  // summary must keep 600, never copy the default's (500).
  @Test
  public void updateNewsInLanguageShouldPreserveExistingTranslationCover() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    news.getProperties().setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));

    Page frNote = mock(Page.class);
    lenient().when(frNote.getLang()).thenReturn("fr");
    NotePageProperties frProperties = new NotePageProperties();
    frProperties.setSummary("fr summary");
    frProperties.setFeaturedImage(new NoteFeaturedImage(600L, null, null, 0L, 0L, null, null));
    lenient().when(frNote.getProperties()).thenReturn(frProperties);
    Page defaultPage = mock(Page.class);
    NotePageProperties defaultPageProperties = new NotePageProperties();
    defaultPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(defaultPage.getProperties()).thenReturn(defaultPageProperties);

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);
    when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultPage);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New fr summary", null, "fr"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(1L));
    // the write carries the translation's OWN cover (600), not the default's (500)
    assertEquals(Long.valueOf(600L), captor.getValue().getFeaturedImage().getId());
    assertEquals("New fr summary", captor.getValue().getSummary());
  }

  // A brand-new translation (no per-language backing-note metadata yet) still
  // inherits the default article's cover (500).
  @Test
  public void updateNewsNewTranslationShouldInheritDefaultCover() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    news.getProperties().setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));

    Page frNote = mock(Page.class);
    lenient().when(frNote.getProperties()).thenReturn(null);
    Page defaultPage = mock(Page.class);
    NotePageProperties defaultPageProperties = new NotePageProperties();
    defaultPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(defaultPage.getProperties()).thenReturn(defaultPageProperties);

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultPage);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New summary", null, "fr"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(1L));
    // a brand-new translation inherits the default article's cover (500)
    assertEquals(Long.valueOf(500L), captor.getValue().getFeaturedImage().getId());
  }

  // EXO-90294: a blank language is the article's OWN default version. It used to
  // fall back to whatever lang the loaded article carried, so an update meant for
  // the main article could land on a translation.
  // Unreachable today: buildArticle sets the lang from the version it read, and
  // the lang-null query matches `p.lang IS NULL`, so getLang() is always null
  // after a lang-less load. Kept so the tool does not depend on that property of
  // another class.
  @Test
  public void updateNewsWithoutLanguageShouldWriteTheDefaultArticle() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    lenient().when(news.getLang()).thenReturn("fr");

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "New summary", null, null));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq(null), eq(1L));
    verify(news, atLeastOnce()).setLang(null);
  }

  // EXO-90294: NewsService#updateNews routes to addNewArticleVersionWithLang only
  // when news.getLang() is set. The tool never set it, so update_news with a
  // language wrote the translated title and body into the MAIN article.
  @Test
  public void updateNewsInLanguageShouldCarryThatLanguageIntoTheTitleAndBodyWrite() throws Exception { // NOSONAR
    News news = mockNews();
    // a distinct object, so the lang set on the refresh cannot stand in
    News refreshed = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news, refreshed);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);
    // stubbed but not expected, so the mutant fails on times(1) not on an NPE
    lenient().when(newsService.updateNews(eq(refreshed), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                                     .thenReturn(refreshed);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, "Titre FR", null, "Contenu FR", "fr"));

    verify(news).setTitle("Titre FR");
    verify(news).setBody("<p>Contenu FR</p>");
    // the object the title/body write is made from carries the language
    verify(news).setLang("fr");
    verify(newsService).updateNews(eq(news),
                                   eq(USER),
                                   eq(false),
                                   anyBoolean(),
                                   eq(NewsObjectType.ARTICLE.name().toLowerCase()),
                                   eq("CONTENT_AND_TITLE"));
    // EXO-90294: exactly ONE write -- every updateNews with a lang creates a page
    // version unconditionally, so a second would duplicate the translation's.
    verify(newsService, times(1)).updateNews(any(News.class),
                                             anyString(),
                                             anyBoolean(),
                                             anyBoolean(),
                                             anyString(),
                                             anyString());
    verify(newsService, never()).updateNews(eq(refreshed), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString());
  }

  @Test
  public void getNewsByIdShouldExposeIllustrationUrl() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                    eq(currentIdentity),
                                    eq(false),
                                    anyString(),
                                    any())).thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(true);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);

    NewsModel result = runWithStaticMocks(() -> tool.getNewsById(NEWS_ID, null));

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

  // EXO-90294: the refresh re-reads the article, and for a language with no page
  // version yet buildArticle falls back to the DEFAULT version -- handing those
  // properties to the lang write made it overwrite the summary just saved.
  @Test
  public void updateNewsSummaryOnlyInLanguageShouldNotBeRevertedByTheRefresh() throws Exception { // NOSONAR
    News news = mockNews();
    // a language with no version yet re-reads with the DEFAULT's properties
    News refreshed = mockNews();
    NotePageProperties defaultProperties = new NotePageProperties();
    defaultProperties.setSummary("default summary");
    lenient().when(refreshed.getProperties()).thenReturn(defaultProperties);
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news, refreshed);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(refreshed), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                            .thenReturn(refreshed);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "resume fr", null, "fr"));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("fr"), eq(1L));
    // the version write carries the summary just saved, not the default's
    verify(refreshed).setProperties(captor.capture());
    assertEquals("resume fr", captor.getValue().getSummary());
    // ACCEPTED by the PO: a metadata-only write for a language with no version
    // yet creates one, and its title/body are the default's until translated --
    // the version is what makes the metadata readable at all.
    verify(refreshed, never()).setTitle(anyString());
    verify(refreshed, never()).setBody(anyString());
  }

  // EXO-90294: a language with no version of its own inherits the default's cover
  // id; reusing it replaces the default article's binary in place.
  @Test
  public void setNewsIllustrationOnAnInheritedCoverShouldNotReuseTheDefaultsFileId() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties defaultProperties = news.getProperties();
    defaultProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    // no "de" version: the returned page has no lang
    Page defaultPage = mock(Page.class);
    lenient().when(defaultPage.getLang()).thenReturn(null);
    lenient().when(defaultPage.getProperties()).thenReturn(defaultProperties);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("de")))
             .thenReturn(defaultPage);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null)))
             .thenReturn(defaultPage);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithUploadMocks(() -> tool.setNewsIllustration(NEWS_ID, "https://meeds.test/cover.png", null, null, null, "alt", "de"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("de"), eq(1L));
    assertNull(captor.getValue().getFeaturedImage().getId());
  }

  // EXO-90294: removing a translation's INHERITED illustration would call
  // removeNoteFeaturedImage(isDraft=false), whose file deletion is unguarded.
  @Test
  public void removeNewsIllustrationOnAnInheritedCoverShouldRefuse() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties defaultProperties = news.getProperties();
    defaultProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    Page defaultPage = mock(Page.class);
    lenient().when(defaultPage.getLang()).thenReturn(null);
    lenient().when(defaultPage.getProperties()).thenReturn(defaultProperties);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    lenient().when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                                  eq(currentIdentity),
                                                  eq(false),
                                                  anyString(),
                                                  any())).thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("de")))
             .thenReturn(defaultPage);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null)))
             .thenReturn(defaultPage);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    // the unguarded path is stubbed so the mutant fails on the deletion
    lenient().when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                                .thenReturn(news);

    try {
      runWithStaticMocks(() -> tool.removeNewsIllustration(NEWS_ID, "de"));
      fail("removing an inherited illustration must not be accepted");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("de"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // EXO-90294: the code is normalized once at the tool boundary, so the metadata,
  // the ownership verdict and the write cannot resolve to different languages.
  @Test
  public void updateNewsWithAPaddedUpperCaseLanguageShouldTargetTheSameTranslation() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    // decoys answering the raw and trimmed codes, so dropping the normalization
    // fails on the assertions below rather than on an unresolved language
    Page rawCodePage = mock(Page.class);
    lenient().when(rawCodePage.getLang()).thenReturn("FR");
    NotePageProperties rawCodeProperties = new NotePageProperties();
    rawCodeProperties.setFeaturedImage(new NoteFeaturedImage(700L, null, null, 0L, 0L, null, null));
    lenient().when(rawCodePage.getProperties()).thenReturn(rawCodeProperties);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("  FR  ")))
             .thenReturn(rawCodePage);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("FR")))
             .thenReturn(rawCodePage);
    Page frPage = mock(Page.class);
    lenient().when(frPage.getLang()).thenReturn("fr");
    NotePageProperties frPageProperties = new NotePageProperties();
    frPageProperties.setFeaturedImage(new NoteFeaturedImage(600L, null, null, 0L, 0L, null, null));
    lenient().when(frPage.getProperties()).thenReturn(frPageProperties);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frPage);
    // the default read, so a resolution/write disagreement shows which language
    // won instead of dying on an NPE
    Page defaultPage = mock(Page.class);
    NotePageProperties defaultPageProperties = new NotePageProperties();
    defaultPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(defaultPage.getProperties()).thenReturn(defaultPageProperties);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultPage);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.updateNews(NEWS_ID, null, "resume fr", null, "  FR  "));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(1L));
    verify(news, atLeastOnce()).setLang("fr");
    // and the metadata was resolved from THAT translation, not from the raw code
    assertEquals(Long.valueOf(600L), captor.getValue().getFeaturedImage().getId());
  }

  // EXO-90294: a translation may have a version of its OWN and still name the
  // default's file, so "does a version exist" is the wrong question; the id is.
  @Test
  public void setNewsIllustrationOnATranslationSharingTheDefaultsFileShouldNotReuseTheFileId() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    Page defaultPage = mock(Page.class);
    NotePageProperties defaultPageProperties = new NotePageProperties();
    defaultPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(defaultPage.getProperties()).thenReturn(defaultPageProperties);
    // the "fr" version exists but still points at the default's file
    Page frPage = mock(Page.class);
    lenient().when(frPage.getLang()).thenReturn("fr");
    NotePageProperties frPageProperties = new NotePageProperties();
    frPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(frPage.getProperties()).thenReturn(frPageProperties);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frPage);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultPage);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                       .thenReturn(news);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithUploadMocks(() -> tool.setNewsIllustration(NEWS_ID, "https://meeds.test/cover.png", null, null, null, "alt", "fr"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(1L));
    assertNull(captor.getValue().getFeaturedImage().getId());
  }

  // Same sequence on the removal side: the fr version exists but shares the
  // default's file, so deleting it would destroy the default article's image.
  @Test
  public void removeNewsIllustrationOnATranslationSharingTheDefaultsFileShouldRefuse() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties loaded = news.getProperties();
    loaded.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    Page defaultPage = mock(Page.class);
    NotePageProperties defaultPageProperties = new NotePageProperties();
    defaultPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(defaultPage.getProperties()).thenReturn(defaultPageProperties);
    Page frPage = mock(Page.class);
    lenient().when(frPage.getLang()).thenReturn("fr");
    NotePageProperties frPageProperties = new NotePageProperties();
    frPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(frPage.getProperties()).thenReturn(frPageProperties);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    lenient().when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                                  eq(currentIdentity),
                                                  eq(false),
                                                  anyString(),
                                                  any())).thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frPage);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultPage);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    lenient().when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                                .thenReturn(news);

    try {
      runWithStaticMocks(() -> tool.removeNewsIllustration(NEWS_ID, "fr"));
      fail("removing an illustration shared with the default article must not be accepted");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("fr"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // EXO-90294: without the default's metadata, ownership is unknowable and this
  // guard's callers delete or replace the file, so a failed read must refuse.
  @Test
  public void removeNewsIllustrationShouldRefuseWhenTheDefaultVersionCannotBeRead() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties loaded = news.getProperties();
    loaded.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    Page frPage = mock(Page.class);
    lenient().when(frPage.getLang()).thenReturn("fr");
    NotePageProperties frPageProperties = new NotePageProperties();
    frPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(frPage.getProperties()).thenReturn(frPageProperties);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    lenient().when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                                  eq(currentIdentity),
                                                  eq(false),
                                                  anyString(),
                                                  any())).thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frPage);
    when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null)))
                                                                                              .thenThrow(new WikiException("storage down"));
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    lenient().when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                                .thenReturn(news);

    try {
      runWithStaticMocks(() -> tool.removeNewsIllustration(NEWS_ID, "fr"));
      fail("a cover whose ownership could not be established must not be deleted");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("fr"));
      // says what happened rather than blaming the default
      assertTrue(e.getMessage().contains("could not be read"));
      assertFalse(e.getMessage().contains("set_news_illustration"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // EXO-90294: getNoteByIdAndLang returns null for a missing page rather than
  // throwing, so a null read is as blind as a throw and must refuse too.
  @Test
  public void removeNewsIllustrationShouldRefuseWhenTheDefaultVersionReadsNull() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();
    NotePageProperties loaded = news.getProperties();
    loaded.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    Page frPage = mock(Page.class);
    lenient().when(frPage.getLang()).thenReturn("fr");
    NotePageProperties frPageProperties = new NotePageProperties();
    frPageProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    lenient().when(frPage.getProperties()).thenReturn(frPageProperties);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    lenient().when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                                  eq(currentIdentity),
                                                  eq(false),
                                                  anyString(),
                                                  any())).thenReturn(news);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frPage);
    when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(null);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    // the unguarded path is stubbed so the mutant fails on the deletion
    lenient().when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString()))
                                                                                                                .thenReturn(news);

    try {
      runWithStaticMocks(() -> tool.removeNewsIllustration(NEWS_ID, "fr"));
      fail("a cover whose ownership could not be established must not be deleted");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("could not be read"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // EXO-90294: both reads in refreshAndModel are contractually nullable --
  // getNewsByIdAndLang is documented so, and addNewArticleVersionWithLang (the
  // branch taken when lang is set) ends with an explicit `return null`. An NPE
  // here reaches the model as "check your Tool input types" AFTER the write
  // succeeded, which invites a retry that writes a second version.
  @Test
  public void setNewsIllustrationShouldReportAFailedReadBackRatherThanThrowNpe() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    // the post-write re-read comes back null
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(null);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);

    try {
      runWithUploadMocks(() -> tool.setNewsIllustration(NEWS_ID, "https://meeds.test/cover.png", null, null, null, "alt", null));
      fail("a failed read back must be reported, not dereferenced");
    } catch (ObjectNotFoundException e) {
      assertTrue(e.getMessage().contains("could not be read back"));
    }
  }

  // Same method, the other nullable: the re-read succeeds but the version write
  // returns null. A LANGUAGE is required for the fixture to be real -- only then
  // does updateNews route to addNewArticleVersionWithLang, the branch with the
  // `return null`; with no language it falls through to `return news`, which
  // cannot be null on a path that null-checked the argument one frame above.
  @Test
  public void setNewsIllustrationShouldReportAFailedVersionWriteRatherThanThrowNpe() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
    when(spaceService.getSpaceById(String.valueOf(SPACE_ID))).thenReturn(space);
    when(newsService.canEditNews(news, USER)).thenReturn(true);
    when(newsService.updateNews(eq(news), eq(USER), eq(false), anyBoolean(), anyString(), anyString())).thenReturn(null);

    try {
      runWithUploadMocks(() -> tool.setNewsIllustration(NEWS_ID, "https://meeds.test/cover.png", null, null, null, "alt", "fr"));
      fail("a failed version write must be reported, not dereferenced");
    } catch (ObjectNotFoundException e) {
      assertTrue(e.getMessage().contains("could not be read back"));
    }
  }

  @Test
  public void setNewsIllustrationShouldSaveMetadataAndRefresh() throws Exception { // NOSONAR
    News news = mockNews();
    Space space = mockSpace();
    mockUserIdentity();

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    // the post-write refresh re-reads the article in the language it wrote
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
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
    NotePageProperties ownProperties = news.getProperties();
    ownProperties.setFeaturedImage(new NoteFeaturedImage(500L, null, null, 0L, 0L, null, null));
    Space space = mockSpace();
    mockUserIdentity();
    // "en" has a version of its own, so removing its cover is legitimate
    Page ownPage = mock(Page.class);
    lenient().when(ownPage.getLang()).thenReturn("en");
    lenient().when(ownPage.getProperties()).thenReturn(ownProperties);
    when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(ownPage);
    Page defaultPage = mock(Page.class);
    NotePageProperties defaultPageProperties = new NotePageProperties();
    defaultPageProperties.setFeaturedImage(new NoteFeaturedImage(700L, null, null, 0L, 0L, null, null));
    lenient().when(defaultPage.getProperties()).thenReturn(defaultPageProperties);
    lenient().when(noteService.getNoteByIdAndLang(eq(NEWS_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultPage);

    when(newsService.getNewsById(eq(String.valueOf(NEWS_ID)), eq(currentIdentity), eq(false), anyString()))
                                                                                                       .thenReturn(news);
    // the post-write refresh re-reads the article in the language it wrote
    when(newsService.getNewsByIdAndLang(eq(String.valueOf(NEWS_ID)),
                                        eq(currentIdentity),
                                        eq(false),
                                        anyString(),
                                        any())).thenReturn(news);
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
