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
package io.meeds.content.news.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.MetadataFilter;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class NewsIndexingServiceConnectorTest {

  NewsIndexingServiceConnector newsIndexingServiceConnector = null;

  @Mock
  NewsService                  newsService;

  @Mock
  IdentityManager              identityManager;

  @Mock
  ActivityManager              activityManager;

  @Mock
  MetadataService              metadataService;

  @Test
  public void testGetAllIdsListsArticlesThenTheirTranslationsByPage() {
    newsIndexingServiceConnector = new NewsIndexingServiceConnector(identityManager,
                                                                    getParams(),
                                                                    newsService,
                                                                    activityManager,
                                                                    metadataService);
    when(metadataService.getMetadataItemsByFilter(any(MetadataFilter.class), eq(0L), eq(0L))).thenReturn(List.of(item("1"),
                                                                                                                  item("2"),
                                                                                                                  item("3")));
    when(newsService.getArticleLanguages("1", false)).thenReturn(List.of("fr", "en"));
    when(newsService.getArticleLanguages("2", false)).thenReturn(List.of());
    when(newsService.getArticleLanguages("3", false)).thenReturn(List.of("fr"));

    assertEquals(List.of("1", "2"), newsIndexingServiceConnector.getAllIds(0, 2));
    assertEquals(List.of("3", "1-fr"), newsIndexingServiceConnector.getAllIds(2, 2));
    assertEquals(List.of("1-en", "3-fr"), newsIndexingServiceConnector.getAllIds(4, 2));
    assertTrue(newsIndexingServiceConnector.getAllIds(6, 2).isEmpty());

    ArgumentCaptor<MetadataFilter> filter = ArgumentCaptor.forClass(MetadataFilter.class);
    // The empty answer after a last page filled to the limit reads nothing again
    verify(metadataService, times(1)).getMetadataItemsByFilter(filter.capture(), eq(0L), eq(0L));
    assertEquals(NewsService.NEWS_METADATA_PAGE_OBJECT_TYPE, filter.getValue().getMetadataObjectTypes().get(0));
    assertEquals(NewsService.POSTED, filter.getValue().getMetadataProperties().get(NewsService.NEWS_PUBLICATION_STATE));
    assertEquals("false", filter.getValue().getMetadataProperties().get(NewsService.NEWS_DELETED));
    // Without space ids the DAO reads the combined properties only
    assertEquals(filter.getValue().getMetadataProperties(), filter.getValue().getCombinedMetadataProperties());
    assertTrue(CollectionUtils.isEmpty(filter.getValue().getMetadataSpaceIds()));

    // A new reindex lists the articles again
    assertEquals(List.of("1", "2"), newsIndexingServiceConnector.getAllIds(0, 2));
    verify(metadataService, times(2)).getMetadataItemsByFilter(any(MetadataFilter.class), eq(0L), eq(0L));
  }

  @Test
  public void testGetAllIdsListsAgainWhenAReindexStartsAfterAnInterruptedOne() {
    newsIndexingServiceConnector = new NewsIndexingServiceConnector(identityManager,
                                                                    getParams(),
                                                                    newsService,
                                                                    activityManager,
                                                                    metadataService);
    when(metadataService.getMetadataItemsByFilter(any(MetadataFilter.class), eq(0L), eq(0L))).thenReturn(List.of(item("1"),
                                                                                                                  item("2"),
                                                                                                                  item("3"),
                                                                                                                  item("4")))
                                                                                             .thenReturn(List.of(item("7"),
                                                                                                                 item("8"),
                                                                                                                 item("9")));

    // The first reindex stops after two full pages, the listing still cached
    assertEquals(List.of("1", "2"), newsIndexingServiceConnector.getAllIds(0, 2));
    assertEquals(List.of("3", "4"), newsIndexingServiceConnector.getAllIds(2, 2));

    assertEquals(List.of("7", "8"), newsIndexingServiceConnector.getAllIds(0, 2));
    verify(metadataService, times(2)).getMetadataItemsByFilter(any(MetadataFilter.class), eq(0L), eq(0L));
  }

  @Test
  public void testGetAllIdsReadsTheWholeListingInOneQueryWithoutDuplicates() {
    newsIndexingServiceConnector = new NewsIndexingServiceConnector(identityManager,
                                                                    getParams(),
                                                                    newsService,
                                                                    activityManager,
                                                                    metadataService);
    List<MetadataItem> items = new ArrayList<>(IntStream.range(0, 450).mapToObj(i -> item(String.valueOf(i))).toList());
    items.add(item("0"));
    when(metadataService.getMetadataItemsByFilter(any(MetadataFilter.class), eq(0L), eq(0L))).thenReturn(items);

    List<String> ids = newsIndexingServiceConnector.getAllIds(0, 1000);

    assertEquals(450, ids.size());
    verify(metadataService, times(1)).getMetadataItemsByFilter(any(MetadataFilter.class), anyLong(), anyLong());
  }

  private MetadataItem item(String articleId) {
    MetadataItem item = new MetadataItem();
    item.setObjectId(articleId);
    return item;
  }

  @Test
  public void testCreate() {
    newsIndexingServiceConnector = new NewsIndexingServiceConnector(identityManager,
                                                                    getParams(),
                                                                    newsService,
                                                                    activityManager,
                                                                    metadataService);

    try {
      newsIndexingServiceConnector.create(null);
      fail("IllegalArgumentException should be thrown");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      newsIndexingServiceConnector.create("1");
      fail("IllegalStateException should be thrown");
    } catch (IllegalStateException e) {
      // Expected
    }

    News news = new News();
    news.setId("1");
    news.setTitle("news1");
    news.setAuthor("root");
    news.setCreationDate(new Date());
    news.setUpdateDate(new Date());
    news.setActivities("1:1;1:2;1:3");

    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setId("1");
    activity.setParentId("2");
    activity.setParentCommentId("3");
    activity.setType("type");
    activity.setPosterId("posterId");
    activity.setPostedTime(1234L);
    activity.setUpdated(4321L);

    ActivityStreamImpl activityStream = new ActivityStreamImpl();
    activity.setActivityStream(activityStream);
    activityStream.setId("id");
    activityStream.setPrettyId("prettyId");
    activityStream.setType(ActivityStream.Type.SPACE);

    Identity posterIdentity = new Identity("posterId");
    Profile posterProfile = new Profile(posterIdentity);
    posterProfile.setProperty("fullName", "Root Root");
    posterIdentity.setProfile(posterProfile);
    try {
      when(newsService.getNewsArticleById("1")).thenReturn(news);
    } catch (Exception e) {
      e.printStackTrace();
    }
    when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root")).thenReturn(posterIdentity);
    when(activityManager.getActivity("1")).thenReturn(activity);

    Identity streamOwner = new Identity("streamOwner");
    when(identityManager.getOrCreateIdentity(ActivityStream.Type.SPACE.getProviderId(), "prettyId")).thenReturn(streamOwner);

    Document document = newsIndexingServiceConnector.create("1");
    assertNotNull(document);
    assertEquals("1", document.getId());
    assertEquals("root", document.getFields().get("posterId"));
    assertEquals("Root Root", document.getFields().get("posterName"));
    assertNotNull(document.getLastUpdatedDate());
    assertNotNull(document.getPermissions());
    assertEquals(1, document.getPermissions().size());
  }

  private InitParams getParams() {
    InitParams params = new InitParams();
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setName("constructor.params");
    params.addParameter(propertiesParam);
    propertiesParam.setProperty("index_current", "index_name");
    return params;
  }

}
