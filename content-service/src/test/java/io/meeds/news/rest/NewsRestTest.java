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
package io.meeds.news.rest;

import static io.meeds.news.service.impl.NewsServiceImpl.POSTED;
import static io.meeds.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static io.meeds.news.utils.NewsUtils.NewsUpdateType.CONTENT_AND_TITLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

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
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.news.filter.NewsFilter;
import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class NewsRestTest {

  public static final String                      JOHN          = "john";

  private static final MockedStatic<CommonsUtils> COMMONS_UTILS = mockStatic(CommonsUtils.class);

  private static final MockedStatic<RestUtils>    REST_UTILS    = mockStatic(RestUtils.class);

  @Mock
  NewsService                                     newsService;

  @Mock
  SpaceService                                    spaceService;

  @Mock
  IdentityManager                                 identityManager;

  @Mock
  PortalContainer                                 container;

  @Mock
  FavoriteService                                 favoriteService;

  @Mock
  TagService                                      tagService;

  @InjectMocks
  private NewsRest newsRestController;

  private HttpServletRequest request;

  @Before
  public void setup() {
    newsRestController.init();
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     new org.exoplatform.social.core.identity.model.Identity("1",
                                                                                                                             JOHN,
                                                                                                                             OrganizationIdentityProvider.NAME,
                                                                                                                             false,
                                                                                                                             true,
                                                                                                                             null,
                                                                                                                             null,
                                                                                                                             null);
    when(identityManager.getOrCreateUserIdentity(JOHN)).thenReturn(userIdentity);
    request = mock(HttpServletRequest.class);
    when(request.getLocale()).thenReturn(Locale.ENGLISH);
  }

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
    REST_UTILS.close();
  }

  @Test
  public void shouldGetNewsWhenNewsExistsAndUserIsMemberOfTheSpace() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    lenient().when(newsService.getNewsByIdAndLang(nullable(String.class), any(), nullable(Boolean.class), nullable(String.class), nullable(String.class)))
             .thenReturn(news);
    lenient().when(spaceService.getSpaceById(nullable(String.class))).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(false);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", null, null, false, null);

    // Then
    assertEquals(200, response.getStatusCodeValue());
    News fetchedNews = (News) response.getBody();
    assertNotNull(fetchedNews);
  }

  @Test
  public void shouldGetNewsByGivenTargetName() throws Exception {
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    List<News> newsList = new LinkedList<>();
    News news = new News();
    news.setId("1");
    List<String> targets = new LinkedList<>();
    targets.add("sliderNews");
    news.setTargets(targets);
    newsList.add(news);
    NewsFilter newsFilter = new NewsFilter();
    newsFilter.setLimit(10);
    lenient().when(newsService.getNewsByTargetName(any(NewsFilter.class), anyString(), any(Identity.class))).thenReturn(newsList);

    // When
    ResponseEntity response = newsRestController.getNewsByTarget( "sliderNews", 0, 10, false, request);

    // Then
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsEntityNews = newsEntity.getNews();
    assertEquals(1, newsEntityNews.size());
  }

  @Test
  public void shouldReturnBadRequestWhenNoActivityId() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    // When
    ResponseEntity response = newsRestController.getNewsByActivityId(null);
    // Then
    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  public void shouldReturnNotFoundWhenNewsNotFound() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    String activityId = "activityId";

    // When
    ResponseEntity response = newsRestController.getNewsByActivityId(activityId);

    // Then
    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  public void shouldReturnNotFoundWhenNotAccessible() throws Exception {
    // Given
    String activityId = "activityId";

    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    when(newsService.getNewsByActivityId(activityId, currentIdentity)).thenThrow(IllegalAccessException.class);

    // When
    ResponseEntity response = newsRestController.getNewsByActivityId(activityId);

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldReturnNotFoundWhenNewsWithActivityNotFoundException() throws Exception {
    // Given
    String activityId = "activityId";

    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    when(newsService.getNewsByActivityId(activityId, currentIdentity)).thenThrow(ObjectNotFoundException.class);

    // When
    ResponseEntity response = newsRestController.getNewsByActivityId(activityId);

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldReturnServerErrorWhenNewsWithActivitythrowsException() throws Exception {
    // Given;
    String activityId = "activityId";

    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    when(newsService.getNewsByActivityId(activityId, currentIdentity)).thenThrow(RuntimeException.class);

    // When
    ResponseEntity response = newsRestController.getNewsByActivityId(activityId);

    // Then
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldReturnNewsWhenNewsIsFound() throws Exception {
    // Given
    String activityId = "activityId";

    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    News news = mock(News.class);
    when(newsService.getNewsByActivityId(activityId, currentIdentity)).thenReturn(news);

    // When
    ResponseEntity response = newsRestController.getNewsByActivityId(activityId);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    assertEquals(news, response.getBody());
  }

  @Test
  public void shouldGetNewsWhenNewsExistsAndUserIsNotMemberOfTheSpaceButSuperManager() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    lenient().when(newsService.getNewsByIdAndLang(anyString(), any(), anyBoolean(), nullable(String.class), nullable(String.class))).thenReturn(news);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(false);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", null, null, false, null);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNotFoundWhenNewsNotExists() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), nullable(String.class))).thenReturn(null);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", null, null, false, null);

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNewsSpacesWhenNewsExistsAndUserIsMemberOfTheSpace() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setActivities("1:1;2:2");
    news.setSpaceId("1");
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    Space space2 = new Space();
    space1.setId("2");
    space1.setPrettyName("space2");
    lenient().when(newsService.getNewsByIdAndLang(anyString(), any(), anyBoolean(), nullable(String.class), nullable(String.class))).thenReturn(news);
    lenient().when(spaceService.getSpaceById("1")).thenReturn(space1);
    lenient().when(spaceService.getSpaceById("2")).thenReturn(space2);
    lenient().when(spaceService.isMember(space1, JOHN)).thenReturn(true);
    lenient().when(spaceService.isMember(space2, JOHN)).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(false);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", "spaces", null, false, null);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetOKWhenUpdatingNewsAndNewsExistsAndUserIsAuthorized() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News existingNews = new News();
    existingNews.setTitle("Title");
    existingNews.setBody("Body");
    existingNews.setPublicationState("draft");
    existingNews.setCanEdit(true);
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), eq(null))).thenReturn(existingNews);
    News updatedNews = new News();
    updatedNews.setTitle("Updated Title");
    updatedNews.setBody("Updated Body");
    updatedNews.setPublicationState(POSTED);
    lenient().when(newsService.updateNews(existingNews, JOHN, false, updatedNews.isPublished(), null, CONTENT_AND_TITLE.name())).then(returnsFirstArg());

    // When
    ResponseEntity response = newsRestController.updateNews("1", false, null, CONTENT_AND_TITLE.name(), updatedNews);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    News returnedNews = (News) response.getBody();
    assertNotNull(returnedNews);
    assertEquals("Updated Title", returnedNews.getTitle());
    assertEquals("Updated Body", returnedNews.getBody());
    assertEquals(POSTED, returnedNews.getPublicationState());

    when(newsRestController.updateNews("1", false, null, CONTENT_AND_TITLE.name(), updatedNews)).thenThrow(IllegalAccessException.class);

    // When
    response = newsRestController.updateNews("1", false, null, CONTENT_AND_TITLE.name(), updatedNews);

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNotFoundWhenUpdatingNewsAndNewsNotExists() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), nullable(String.class))).thenReturn(null);

    // When
    ResponseEntity response = newsRestController.updateNews("1", false, null, CONTENT_AND_TITLE.name(), new News());

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatusCode().value());
  }
  
  @Test
  public void shouldGetOKWhenUpdatingAndPinNewsAndNewsExistsAndAndUserIsAuthorized() throws Exception {
    // Given
    News existingNews = new News();
    existingNews.setTitle("unpinned title");
    existingNews.setBody("unpinned body");
    existingNews.setUploadId(null);
    String sDate1 = "22/08/2019";
    Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(sDate1);
    existingNews.setCreationDate(date1);
    existingNews.setPublished(false);
    existingNews.setId("id123");
    existingNews.setSpaceId("space");
    existingNews.setCanEdit(true);

    News updatedNews = new News();
    updatedNews.setPublished(true);
    updatedNews.setTitle("pinned title");
    updatedNews.setBody("pinned body");

    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    List<MembershipEntry> memberships = new LinkedList<MembershipEntry>();
    memberships.add(new MembershipEntry("/platform/web-contributors", "publisher"));
    currentIdentity.setMemberships(memberships);

    lenient().when(newsService.getNewsById("id123", currentIdentity, false, null)).thenReturn(existingNews);
    lenient().when(newsService.updateNews(existingNews, JOHN, false, updatedNews.isPublished(), null, CONTENT_AND_TITLE.name())).then(returnsFirstArg());

    // When
    ResponseEntity response = newsRestController.updateNews("id123", false, null, CONTENT_AND_TITLE.name(), updatedNews);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    News returnedNews = (News) response.getBody();
    assertNotNull(returnedNews);
    assertEquals("pinned title", returnedNews.getTitle());
    assertEquals("pinned body", returnedNews.getBody());
  }

  @Test
  public void shouldGetBadRequestWhenUpdatingNewsAndUpdatedNewsIsNull() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    // When
    ResponseEntity response = newsRestController.updateNews("1", false, null, CONTENT_AND_TITLE.name(), null);

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
  }
  
  @Test
  public void shouldGetOKWhenSavingDraftsAndUserIsMemberOfTheSpaceAndSuperManager() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setSpaceId("1");

    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), anyString())).thenReturn(news);
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.createNews(news);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());

    when(newsRestController.createNews(news)).thenThrow(IllegalAccessException.class);

    // When
    response = newsRestController.createNews(news);

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetOkWhenCreateNewsWithPublishedState() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setPublicationState(POSTED);
    news.setSpaceId("1");
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), anyString())).thenReturn(news);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.createNews(news);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetOkWhenScheduleNews() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setPublicationState("staged");
    news.setCanEdit(true);
    news.setSpaceId("1");
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(),  anyString())).thenReturn(news);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.scheduleNews(ARTICLE.name().toLowerCase(), news);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetBadRequestWhenCreatingNewsDraftAndNewsIsNull() throws Exception {
    // Given
    News news = new News();
    news.setId("1");
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), anyString())).thenReturn(null);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(false);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.createNews(new News());

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNewsDraftWhenNewsDraftExistsAndUserIsMemberOfTheSpaceAndSuperManager() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setSpaceId("1");

    when(newsService.getNewsByIdAndLang(anyString(), any(), anyBoolean(), nullable(String.class), nullable(String.class))).thenReturn(news);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", null, null, false, null);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNotFoundWhenNewsDraftNotExists() throws Exception {
    // Given
    
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(newsService.getNewsByIdAndLang(anyString(), any(), anyBoolean(), anyString(), nullable(String.class))).thenReturn(null);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", null, null, false, null);

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetBadRequestWhenNewsDraftIsNull() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(newsService.getNewsByIdAndLang(anyString(), any(), anyBoolean(), anyString(), nullable(String.class))).thenReturn(null);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.getNewsById(null, null, null, false, null);

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNewsDraftListWhenNewsDraftsExistsAndUserIsMemberOfTheSpaceAndSuperManager() throws Exception {
    // Given
    lenient().when(request.getRemoteUser()).thenReturn("john");
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setSpaceId("1");
    News news2 = new News();
    news2.setId("2");
    news2.setSpaceId("1");
    List<News> newsDrafts = new ArrayList<>();
    newsDrafts.add(news);
    newsDrafts.add(news2);

    lenient().when(newsService.getNews(any(NewsFilter.class), any())).thenReturn(newsDrafts);
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, "1", "drafts", "", 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNotAuthorizedWhenNewsDraftsExistsAndUserIsNotMemberOfTheSpaceNorSuperManager() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setSpaceId("1");
    News news2 = new News();
    news2.setId("2");
    news2.setSpaceId("1");
    List<News> newsDrafts = new ArrayList<>();
    newsDrafts.add(news);
    newsDrafts.add(news2);
    lenient().when(newsService.getNews(any(NewsFilter.class), any())).thenReturn(newsDrafts);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(false);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(false);

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, "1", "draft", null, 0, 10, false, request);
    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNotAuthorizedWhenNewsDraftsExistsAndUserNotExists() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    lenient().when(request.getRemoteUser()).thenReturn("");
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setSpaceId("1");
    News news2 = new News();
    news2.setId("2");
    news2.setSpaceId("1");
    List<News> newsDrafts = new ArrayList<>();
    newsDrafts.add(news);
    newsDrafts.add(news2);
    lenient().when(newsService.getNews(any(NewsFilter.class), any())).thenReturn(newsDrafts);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);

    // When
    ResponseEntity response = newsRestController.getNews("mike", "1", "draft", null, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldDeleteNewsWhenNewsExists() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setAuthor(JOHN);
    news.setSpaceId("1");
    news.setCanDelete(true);

    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), anyString())).thenReturn(news);
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    Response response = newsRestController.deleteNews("1", ARTICLE.name().toLowerCase(), 0L);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(newsService).deleteNews("1", currentIdentity, ARTICLE.name().toLowerCase());
  }

  @Test
  public void shouldNotDeleteNewsWhenUserIsNotDraftAuthor() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setAuthor("mary");
    news.setSpaceId("1");
    news.setCanDelete(true);

    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), anyString())).thenReturn(news);
    Space space1 = new Space();
    space1.setId("1");
    space1.setPrettyName("space1");
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    Response response = newsRestController.deleteNews("1", ARTICLE.name().toLowerCase(), 0L);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(newsService).deleteNews("1", currentIdentity, ARTICLE.name().toLowerCase());
  }

  @Test
  public void shouldGetNotFoundWhenDeletingNewsDraftThatNotExists() throws Exception {
    // Given
    
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), nullable(String.class))).thenReturn(null);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    Response response = newsRestController.deleteNews("1", ARTICLE.name().toLowerCase(), 0L);

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    verify(newsService, never()).deleteNews("1", currentIdentity, ARTICLE.name().toLowerCase());
  }

  @Test
  public void shouldGetBadRequestWhenDeletingNewsDraftWithIdNull() throws Exception {
    // Given

    
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), nullable(String.class))).thenReturn(null);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(true);

    // When
    Response response = newsRestController.deleteNews(null, ARTICLE.name().toLowerCase(), 0L);

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    verify(newsService, never()).deleteNews("1", currentIdentity, ARTICLE.name().toLowerCase());
  }

  @Test
  public void shouldGetAllPublishedNewsWhenExist() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news1 = new News();
    News news2 = new News();
    News news3 = new News();
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.getNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, null, "", null, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertEquals(3, newsList.size());
  }

  @Test
  public void shouldGetEmptyListWhenNoPublishedExists() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    NewsFilter newsFilter = new NewsFilter();
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(newsService.getNews(newsFilter, currentIdentity)).thenReturn(null);

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, null, null, null, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    assertNotNull(newsEntity);
    assertEquals(0, newsEntity.getNews().size());
  }

  @Test
  public void shouldGetOKWhenViewNewsAndNewsExists() throws Exception {
    // Given
    
    
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setSpaceId("space1");
    news.setViewsCount((long) 6);

    lenient().when(newsService.getNewsByIdAndLang("1", currentIdentity, false, null, null)).thenReturn(news);
    Space space1 = new Space();
    space1.setPrettyName("space1");
    lenient().when(spaceService.getSpaceById("space1")).thenReturn(space1);
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);
    lenient().when(spaceService.isSuperManager(eq(JOHN))).thenReturn(false);

    // When
    ResponseEntity response = newsRestController.getNewsById("1", null, null, false, null);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetNotFoundWhenViewNewsAndNewsIsNull() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setViewsCount((long) 6);

    lenient().when(newsService.getNewsById("1", currentIdentity, false, null)).thenReturn(news);
    // When
    ResponseEntity response = newsRestController.getNewsById("2", null, null, false, null);
    ;

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetAllPinnedNewsWhenExist() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news1 = new News();
    news1.setPublished(true);
    news1.setAuthor(JOHN);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setPublished(true);
    news2.setAuthor(JOHN);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setPublished(true);
    news3.setAuthor(JOHN);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(newsService.getNews(any(), any())).thenReturn(allNews);
    lenient().when(newsService.getNewsCount(any())).thenReturn(allNews.size());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, null, "pinned", null, 0, 10, true, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(true, newsList.get(i).isPublished());
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
    }
    assertEquals(0, newsEntity.getOffset().intValue());
    assertEquals(10, newsEntity.getLimit().intValue());
    assertEquals(3, newsEntity.getSize().intValue());
  }

  @Test
  public void shouldGetAllNewsWhenSearchingWithTextInTheGivenSpaces() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String text = "search";
    String spacesIds = "4,1";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(TagService.class)).thenReturn(tagService);
    REST_UTILS.when(() -> RestUtils.getCurrentUserIdentityId()).thenReturn(1L);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spacesIds, "", text, 0, 5, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
      assertEquals(text, newsList.get(i).getTitle());
      assertEquals(true, spacesIds.contains(newsList.get(i).getSpaceId()));
    }
    assertEquals(0, newsEntity.getOffset().intValue());
    assertEquals(5, newsEntity.getLimit().intValue());
    assertNull(newsEntity.getSize());
  }

  @Test
  public void shouldGetAllNewsWhenSearchingWithTextInTheGivenSpace() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String text = "search";
    String spaceId = "4";
    News news1 = new News();
    news1.setSpaceId(spaceId);
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId(spaceId);
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId(spaceId);
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(TagService.class)).thenReturn(tagService);
    REST_UTILS.when(() -> RestUtils.getCurrentUserIdentityId()).thenReturn(1L);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spaceId, "", text, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
      assertEquals(text, newsList.get(i).getTitle());
      assertEquals("4", newsList.get(i).getSpaceId());
    }
  }

  @Test
  public void shouldGetAllNewsWhenSearchingWithTagTextInTheGivenSpace() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String tagText = "tagText";
    String newsBody = "body including tag text #tagText";
    String spaceId = "4";
    News news1 = new News();
    news1.setSpaceId(spaceId);
    news1.setAuthor(JOHN);
    news1.setTitle("newsTitle");
    news1.setBody(newsBody);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId(spaceId);
    news2.setAuthor(JOHN);
    news2.setTitle("newsTitle");
    news2.setBody(newsBody);
    news2.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(TagService.class)).thenReturn(tagService);
    REST_UTILS.when(() -> RestUtils.getCurrentUserIdentityId()).thenReturn(1L);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spaceId, "", tagText, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(2, newsList.size());
    for (News newsItem : newsList) {
      assertTrue(newsItem.getBody().contains(tagText));
    }
  }

  @Test
  public void shouldGetPinnedNewsWhenSearchingWithTextInTheGivenSpaces() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String text = "search";
    String spacesIds = "4,1";
    News news1 = new News();
    news1.setPublished(true);
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setPublished(true);
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setPublished(true);
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(TagService.class)).thenReturn(tagService);
    REST_UTILS.when(() -> RestUtils.getCurrentUserIdentityId()).thenReturn(1L);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spacesIds, "pinned", text, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(true, newsList.get(i).isPublished());
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
      assertEquals(text, newsList.get(i).getTitle());
      assertEquals(true, spacesIds.contains(newsList.get(i).getSpaceId()));
    }
  }

  @Test
  public void shouldGetUnauthorizedWhenSearchingWithTextInNonMemberSpaces() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String text = "search";
    String spacesIds = "4,1";
    News news1 = new News();
    news1.setPublished(true);
    News news2 = new News();
    news2.setPublished(true);
    News news3 = new News();
    news3.setPublished(true);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.getNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(false);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spacesIds, "pinned", text, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetMyPostedNewsWhenExists() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String filter = "myPosted";
    String text = "text";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.getNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, null, filter, null, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
    }
  }

  @Test
  public void shouldGetMyPostedNewsWhenFilteringWithTheGivenSpaces() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String filter = "myPosted";
    String text = "text";
    String spacesIds = "4,1";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.getNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spacesIds, filter, null, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
      assertEquals(true, spacesIds.contains(newsList.get(i).getSpaceId()));
    }
  }

  @Test
  public void shouldGetMyPostedNewsWhenSearchingWithTheGivenSpaces() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    String filter = "myPosted";
    String text = "text";
    String spacesIds = "4,1";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(TagService.class)).thenReturn(tagService);
    REST_UTILS.when(() -> RestUtils.getCurrentUserIdentityId()).thenReturn(1L);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, spacesIds, filter, text, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(3, newsList.size());
    for (int i = 0; i < newsList.size(); i++) {
      assertEquals(POSTED, newsList.get(i).getPublicationState());
      assertEquals(JOHN, newsList.get(i).getAuthor());
      assertEquals(text, newsList.get(i).getTitle());
      assertEquals(true, spacesIds.contains(newsList.get(i).getSpaceId()));
    }
  }

  @Test
  public void shouldGetStagedNewsWhenCurrentUserIsAuthor() throws Exception {
    // Given
    HttpServletRequest request = mock(HttpServletRequest.class);
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    lenient().when(request.getLocale()).thenReturn(new Locale("en"));
    News news = new News();
    news.setSpaceId("1");
    news.setAuthor(JOHN);
    news.setPublicationState("staged");
    List<News> allNews = new ArrayList<>();
    allNews.add(news);

    lenient().when(newsService.getNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.getNews(JOHN, null, null, null, 0, 10, false, request);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    NewsEntity newsEntity = (NewsEntity) response.getBody();
    List<News> newsList = newsEntity.getNews();
    assertNotNull(newsList);
    assertEquals(1, newsList.size());
    assertEquals("staged", newsList.get(0).getPublicationState());
    assertEquals(JOHN, newsList.get(0).getAuthor());
  }

  @Test
  public void shouldDeleteNews() throws Exception {
    // Given
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setCanDelete(true);
    lenient().when(newsService.getNewsById(anyString(), any(), anyBoolean(), anyString())).thenReturn(news);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    lenient().when(spaceService.isMember(any(Space.class), eq(JOHN))).thenReturn(true);

    // When
    Response response = newsRestController.deleteNews(news.getId(), ARTICLE.name().toLowerCase(), 0);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldGetBadRequestWhenSearchingWithoutQueryAndFavorites() throws Exception {
    // Given
    String text = "text";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());

    // When
    ResponseEntity response = newsRestController.search("", "", 0, null, 10, false);

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetBadRequestWhenSearchingWithNegativeOffset() throws Exception {
    // Given
    String text = "text";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    setCurrentUser(JOHN);

    // When
    ResponseEntity response = newsRestController.search("query", "", -1, null, 10, false);

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetBadRequestWhenSearchingWithNegativeLimit() throws Exception {
    // Given
    String text = "text";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    setCurrentUser(JOHN);

    // When
    ResponseEntity response = newsRestController.search("query", "", 0, null, -1, false);

    // Then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode().value());
  }

  @Test
  public void shouldGetnewsListWhenSearchingWithQuery() throws Exception {
    // Given
    String text = "text";
    String spacesIds = "4,1";
    News news1 = new News();
    news1.setSpaceId("4");
    news1.setAuthor(JOHN);
    news1.setTitle(text);
    news1.setPublicationState(POSTED);
    News news2 = new News();
    news2.setSpaceId("1");
    news2.setAuthor(JOHN);
    news2.setTitle(text);
    news2.setPublicationState(POSTED);
    News news3 = new News();
    news3.setSpaceId("4");
    news3.setAuthor(JOHN);
    news3.setTitle(text);
    news3.setPublicationState(POSTED);
    List<News> allNews = new ArrayList<>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);
    lenient().when(newsService.searchNews(any(), any())).thenReturn(allNews);
    lenient().when(spaceService.isMember(any(Space.class), any())).thenReturn(true);
    lenient().when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    setCurrentUser(JOHN);

    // When
    ResponseEntity response = newsRestController.search(text, "", 0, null, 10, false);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    List<NewsSearchResultEntity> newsList = (List<NewsSearchResultEntity>) response.getBody();
    assertNotNull(newsList);
    assertEquals(0, newsList.size());
  }

  private void setCurrentUser(final String name) {
    ConversationState.setCurrent(new ConversationState(new org.exoplatform.services.security.Identity(name)));
  }

  @Test
  public void testMarkAsRead() throws Exception {
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    when(newsService.getNewsById("1", currentIdentity, false, ARTICLE.name().toLowerCase())).thenReturn(news);
    doNothing().when(newsService).markAsRead(news, JOHN);
    Response response = newsRestController.markNewsAsRead( "1");
    verify(newsService, times(1)).markAsRead(news, JOHN);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteArticleTranslation() throws Exception {
    Identity currentIdentity = new Identity(JOHN);
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    News news = new News();
    news.setId("1");
    news.setCanDelete(false);
    news.setLang("fr");
    when(newsService.getNewsById("1", currentIdentity, false, ARTICLE.name())).thenReturn(news);
    //
    Response response = newsRestController.deleteArticleTranslation(news.getId(), news.getLang());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    news.setCanDelete(true);
    response = newsRestController.deleteArticleTranslation(news.getId(), news.getLang());
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

  }
}
