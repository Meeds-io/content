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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.component.RequestLifeCycle;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.news.model.News;
import io.meeds.news.model.NewsTargetObject;
import io.meeds.news.rest.NewsTargetingEntity;
import io.meeds.news.service.NewsTargetingService;
import io.meeds.news.utils.NewsUtils;

@RunWith(MockitoJUnitRunner.class)
public class NewsTargetingImplTest {

  private static final MockedStatic<ExoContainerContext> EXO_CONTAINER_CONTEXT = mockStatic(ExoContainerContext.class);

  private static final MockedStatic<CommonsUtils>        COMMONS_UTILS         = mockStatic(CommonsUtils.class);

  private static final MockedStatic<RestUtils>           REST_UTILS            = mockStatic(RestUtils.class);

  @Mock
  MetadataService                                        metadataService;

  @Mock
  IdentityManager                                        identityManager;

  @Mock
  IdentityRegistry                                       identityRegistry;

  @Mock
  SpaceService                                           spaceService;

  @Mock
  Space                                                  space;

  @Mock
  ExoContainer                                           container;

  @Mock
  private OrganizationService                            organizationService;

  @Mock
  private GroupHandler                                   groupHandler;

  @InjectMocks
  NewsTargetingServiceImpl                               newsTargetingService;

  @Before
  public void setUp() {
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getCurrentContainer()).thenReturn(container);
  }

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    EXO_CONTAINER_CONTEXT.close();
    COMMONS_UTILS.close();
    REST_UTILS.close();
  }

  @Test
  public void testGetAllTargets() throws Exception {
    // Given
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(IdentityRegistry.class)).thenReturn(identityRegistry);
    org.exoplatform.services.security.Identity identity = mock(org.exoplatform.services.security.Identity.class);
    REST_UTILS.when(() -> RestUtils.getCurrentUser()).thenReturn("root");
    when(identityRegistry.getIdentity("root")).thenReturn(identity);
    MetadataType metadataType = new MetadataType(4, "newsTarget");
    List<Metadata> newsTargets = new LinkedList<>();
    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(1);
    newsTargets.add(sliderNews);

    Metadata latestNews = new Metadata();
    latestNews.setName("latestNews");
    latestNews.setCreatedDate(200);
    HashMap<String, String> latestNewsProperties = new HashMap<>();
    latestNewsProperties.put("label", "latest news");
    latestNews.setProperties(latestNewsProperties);
    latestNews.setId(2);
    newsTargets.add(latestNews);

    Metadata testNews = new Metadata();
    testNews.setName("testNews");
    testNews.setCreatedDate(300);
    HashMap<String, String> testNewsProperties = new HashMap<>();
    testNewsProperties.put("label", "test news");
    testNewsProperties.put(NewsUtils.TARGET_PERMISSIONS, "space:1");
    testNews.setProperties(testNewsProperties);
    testNews.setId(3);
    newsTargets.add(testNews);

    Space space = new Space();
    space.setId("1");
    space.setDisplayName("Space1");
    space.setPrettyName("space1");
    space.setAvatarUrl("");

    when(metadataService.getMetadatas(metadataType.getName(), 0)).thenReturn(newsTargets);
    when(spaceService.getSpaceById("1")).thenReturn(space);

    // When
    List<NewsTargetingEntity> allTargets = newsTargetingService.getAllTargets();

    // Then
    assertNotNull(allTargets);
    assertEquals(3, allTargets.size());
    assertEquals("sliderNews", allTargets.get(0).getName());
    assertEquals("latestNews", allTargets.get(1).getName());
    assertEquals("testNews", allTargets.get(2).getName());
  }

  @Test
  public void testGetAllowedTargets() throws Exception {
    // Given
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(IdentityRegistry.class)).thenReturn(identityRegistry);
    org.exoplatform.services.security.Identity identity = mock(org.exoplatform.services.security.Identity.class);
    REST_UTILS.when(RestUtils::getCurrentUser).thenReturn("user");
    MetadataType metadataType = new MetadataType(4, "newsTarget");
    List<Metadata> newsTargets = new LinkedList<>();
    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(1);
    newsTargets.add(sliderNews);

    when(metadataService.getMetadatas(metadataType.getName(), 0)).thenReturn(newsTargets);

    // When
    List<NewsTargetingEntity> allowedTargets = newsTargetingService.getAllowedTargets(identity);

    // Then
    assertNotNull(allowedTargets);
    assertEquals(0, allowedTargets.size());

    // Given
    Metadata latestNews = new Metadata();
    latestNews.setName("latestNews");
    latestNews.setCreatedDate(200);
    HashMap<String, String> latestNewsProperties = new HashMap<>();
    latestNewsProperties.put("label", "latest news");
    latestNewsProperties.put(NewsUtils.TARGET_PERMISSIONS, "space:1");
    latestNews.setProperties(latestNewsProperties);
    latestNews.setId(2);
    newsTargets.add(latestNews);

    Space space = new Space();
    space.setId("1");
    space.setDisplayName("Space1");
    space.setPrettyName("space1");
    space.setAvatarUrl("");

    when(metadataService.getMetadatas(metadataType.getName(), 0)).thenReturn(newsTargets);
    when(spaceService.getSpaceById("1")).thenReturn(space);

    // When
    COMMONS_UTILS.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);
    allowedTargets = newsTargetingService.getAllowedTargets(identity);

    // Then
    assertNotNull(allowedTargets);
    assertEquals(0, allowedTargets.size());

    try (MockedStatic<NewsUtils> newsUtils = mockStatic(NewsUtils.class)) {
      // Given
      newsUtils.when(() -> NewsUtils.canPublishNews(space.getId(), identity)).thenReturn(true);

      // When
      allowedTargets = newsTargetingService.getAllowedTargets(identity);

      // Then
      assertNotNull(allowedTargets);
      assertEquals(1, allowedTargets.size());
      assertEquals("latestNews", allowedTargets.get(0).getName());
      assertFalse(allowedTargets.get(0).isRestrictedAudience());

      // Given
      Metadata testNews = new Metadata();
      testNews.setName("testNews");
      testNews.setCreatedDate(200);
      HashMap<String, String> testNewsProperties = new HashMap<>();
      testNewsProperties.put("label", "test news");
      testNewsProperties.put(NewsUtils.TARGET_PERMISSIONS, "/platform/administrators");
      testNews.setProperties(testNewsProperties);
      testNews.setId(3);
      newsTargets.add(testNews);

      when(organizationService.getGroupHandler()).thenReturn(groupHandler);
      Group group = new GroupImpl();
      group.setId("/platform/administrators");
      group.setGroupName("Administrators");

      when(groupHandler.findGroupById("/platform/administrators")).thenReturn(group);
      when(identity.isMemberOf("/platform/administrators", "publisher")).thenReturn(false);

      // when
      allowedTargets = newsTargetingService.getAllowedTargets(identity);

      // Then
      assertNotNull(allowedTargets);
      assertEquals(1, allowedTargets.size());
      assertEquals("latestNews", allowedTargets.get(0).getName());

      // Given
      when(identity.isMemberOf("/platform/administrators", "publisher")).thenReturn(true);

      // when
      allowedTargets = newsTargetingService.getAllowedTargets(identity);

      // Then
      assertNotNull(allowedTargets);
      assertEquals(2, allowedTargets.size());
      assertEquals("latestNews", allowedTargets.get(0).getName());
      assertEquals("testNews", allowedTargets.get(1).getName());
      assertFalse(allowedTargets.get(0).isRestrictedAudience());

      // Given
      when(spaceService.getSpaceById("1")).thenReturn(null);

      // when
      allowedTargets = newsTargetingService.getAllowedTargets(identity);

      // Then
      assertNotNull(allowedTargets);
      assertEquals(1, allowedTargets.size());
      assertEquals("testNews", allowedTargets.get(0).getName());

      // Given
      when(groupHandler.findGroupById("/platform/administrators")).thenReturn(null);

      // when
      allowedTargets = newsTargetingService.getAllowedTargets(identity);

      // Then
      assertNotNull(allowedTargets);
      assertEquals(0, allowedTargets.size());
    }
  }

  @Test
  public void testGetTargetsByNews() throws Exception {
    // Given
    News news = new News();
    news.setId("123456");
    news.setSpaceId("1");

    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    sliderNews.setId(1);

    List<MetadataItem> metadataItems = new LinkedList<>();
    MetadataItem metadataItem = new MetadataItem();
    metadataItem.setCreatedDate(100);
    metadataItem.setCreatorId(1);
    metadataItem.setId(1);
    metadataItem.setObjectId("123456");
    metadataItem.setMetadata(sliderNews);
    metadataItems.add(metadataItem);

    NewsTargetObject newsTargetObject = new NewsTargetObject("news", "123456", null, 1L);
    when(metadataService.getMetadataItemsByMetadataTypeAndObject(NewsTargetingService.METADATA_TYPE.getName(),
                                                                 newsTargetObject)).thenReturn(metadataItems);

    // When
    List<String> newsTargets = newsTargetingService.getTargetsByNews(news);

    // Then
    assertNotNull(newsTargets);
    assertEquals(1, newsTargets.size());
    assertEquals("sliderNews", newsTargets.get(0));
  }

  @Test
  public void testSaveNewsTargets() throws Exception {
    // Given
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity("root");
    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    sliderNews.setId(1);

    MetadataItem metadataItem = new MetadataItem();
    metadataItem.setCreatedDate(100);
    metadataItem.setCreatorId(1);
    metadataItem.setId(1);
    metadataItem.setObjectId("123456");
    metadataItem.setMetadata(sliderNews);

    List<String> targets = new LinkedList<>();
    targets.add("sliderNews");

    News news = new News();
    news.setSpaceId("1");
    news.setTitle("Test news");
    news.setAuthor("user1");
    news.setTargets(targets);
    news.setId("123456");

    NewsTargetObject newsTargetObject = new NewsTargetObject("news", "123456", null, 1L);
    MetadataKey metadataKey = new MetadataKey(NewsTargetingService.METADATA_TYPE.getName(), "sliderNews", 0);
    Identity userIdentity = new Identity("1");
    when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root")).thenReturn(userIdentity);
    Authenticator authenticator = mock(Authenticator.class);
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(IdentityRegistry.class)).thenReturn(identityRegistry);
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getCurrentContainer()).thenReturn(container);
    when(spaceService.getSpaceById("1")).thenReturn(space);
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(Authenticator.class)).thenReturn(authenticator);
    when(authenticator.createIdentity("root")).thenReturn(identity);
    List<MembershipEntry> memberships = new LinkedList<>();
    MembershipEntry membershipEntry = new MembershipEntry("/platform/web-contributors", "publisher");
    memberships.add(membershipEntry);
    identity.setMemberships(memberships);
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("displayed", String.valueOf(true));

    COMMONS_UTILS.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);

    // When
    newsTargetingService.saveNewsTarget(news, true, news.getTargets(), "root");

    // Then
    verify(identityManager, times(1)).getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root");
    verify(metadataService, times(1)).createMetadataItem(newsTargetObject, metadataKey, properties, 1, false);

  }

  @Test
  public void testGetNewsTargetItemsByTargetName() throws Exception {

    Metadata sliderNews = new Metadata();
    sliderNews.setName("newsTargets");
    sliderNews.setCreatedDate(100);
    sliderNews.setId(1);

    MetadataItem metadataItem = new MetadataItem();
    metadataItem.setCreatedDate(100);
    metadataItem.setCreatorId(1);
    metadataItem.setId(1);
    metadataItem.setObjectId("123456");
    metadataItem.setMetadata(sliderNews);
    List<MetadataItem> metadataItems = new LinkedList<>();
    metadataItems.add(metadataItem);
    when(metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndMetadataItemProperty("newsTargets",
                                                                                               NewsTargetingService.METADATA_TYPE.getName(),
                                                                                               "news",
                                                                                               "displayed",
                                                                                               String.valueOf(true),
                                                                                               0,
                                                                                               10)).thenReturn(metadataItems);

    // When
    List<MetadataItem> newsTargetsItems = newsTargetingService.getNewsTargetItemsByTargetName("newsTargets", 0, 10);

    // Then
    assertNotNull(newsTargetsItems);
    assertEquals(1, newsTargetsItems.size());
  }

  @Test
  public void testDeleteTargetByName() throws IllegalAccessException {
    // Given
    String username = "user";
    Identity userIdentity = new Identity();
    userIdentity.setRemoteId(username);

    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(IdentityRegistry.class)).thenReturn(identityRegistry);
    org.exoplatform.services.security.Identity identity = mock(org.exoplatform.services.security.Identity.class);
    when(identity.isMemberOf("/platform/web-contributors", "manager")).thenReturn(true);

    List<Metadata> newsTargets = new LinkedList<>();
    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(1);
    newsTargets.add(sliderNews);

    List<NewsTargetingEntity> targets = new LinkedList<>();
    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName("test1");
    targets.add(newsTargetingEntity);

    when(metadataService.getMetadataByKey(any())).thenReturn(sliderNews);
    newsTargetingService.deleteTargetByName(targets.get(0).getName(), identity);

    // When
    verify(metadataService, atLeastOnce()).deleteMetadataById(newsTargets.get(0).getId());
  }

  @Test
  public void testCreateTarget() throws IllegalAccessException {
    // Given
    org.exoplatform.services.security.Identity currentIdentity = new org.exoplatform.services.security.Identity("root");
    MembershipEntry membershipentry = new MembershipEntry("/platform/web-contributors", "manager");
    List<MembershipEntry> memberships = new ArrayList<MembershipEntry>();
    memberships.add(membershipentry);
    currentIdentity.setMemberships(memberships);
    Identity userIdentity = new Identity("organization", "root");
    userIdentity.setId("1");
    when(identityManager.getOrCreateIdentity(anyString(), anyString())).thenReturn(userIdentity);

    List<Metadata> newsTargets = new LinkedList<>();
    Metadata sliderNews = new Metadata();
    MetadataType metadataType = new MetadataType(4, "newsTarget");
    sliderNews.setType(metadataType);
    sliderNews.setName("sliderNews");
    sliderNews.setCreatorId(1);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(0);
    newsTargets.add(sliderNews);

    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName(sliderNews.getName());
    newsTargetingEntity.setProperties(sliderNews.getProperties());
    when(metadataService.createMetadata(sliderNews, 1)).thenReturn(sliderNews);

    Metadata createdMetadata = newsTargetingService.createNewsTarget(newsTargetingEntity, currentIdentity);

    // Then
    assertNotNull(createdMetadata);
    assertEquals(sliderNews.getId(), createdMetadata.getId());
    assertEquals(sliderNews.getName(), createdMetadata.getName());

    // use case when adding a target with the same name
    when(metadataService.getMetadataByKey(any())).thenReturn(sliderNews);
    try {
      newsTargetingService.createNewsTarget(newsTargetingEntity, currentIdentity);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    // use case when adding a target with user that is not a manager
    String username1 = "John";
    String id1 = "2";
    Identity userIdentity1 = new Identity();
    userIdentity1.setId(id1);
    userIdentity1.setRemoteId(username1);

    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(IdentityRegistry.class)).thenReturn(identityRegistry);
    org.exoplatform.services.security.Identity identity1 = mock(org.exoplatform.services.security.Identity.class);
    try {
      newsTargetingService.createNewsTarget(newsTargetingEntity, identity1);
      fail();
    } catch (IllegalAccessException e) {
      // Expected
    }
  }

  @Test
  public void testUpdateTarget() throws IllegalAccessException {
    // Given
    org.exoplatform.services.security.Identity currentIdentity = new org.exoplatform.services.security.Identity("root");
    MembershipEntry membershipentry = new MembershipEntry("/platform/web-contributors", "manager");
    List<MembershipEntry> memberships = new ArrayList<MembershipEntry>();
    memberships.add(membershipentry);
    currentIdentity.setMemberships(memberships);
    Identity userIdentity = new Identity("organization", "root");
    userIdentity.setId("1");
    when(identityManager.getOrCreateIdentity(any(), any())).thenReturn(userIdentity);

    List<Metadata> newsTargets = new LinkedList<>();
    Metadata sliderNews = new Metadata();
    MetadataType metadataType = new MetadataType(4, "newsTarget");
    sliderNews.setType(metadataType);
    sliderNews.setName("sliderNews");
    sliderNews.setCreatorId(1);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNewsProperties.put("description", "description slider news");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(0);
    newsTargets.add(sliderNews);

    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName(sliderNews.getName());
    newsTargetingEntity.setProperties(sliderNews.getProperties());
    when(metadataService.createMetadata(sliderNews, 1)).thenReturn(sliderNews);

    Metadata createdMetadata = newsTargetingService.createNewsTarget(newsTargetingEntity, currentIdentity);

    String originalTargetName = "sliderNews";
    NewsTargetingEntity newsTargetingEntityUpdated = new NewsTargetingEntity();
    newsTargetingEntityUpdated.setName("sliderNews update");
    newsTargetingEntityUpdated.setProperties(sliderNews.getProperties());
    MetadataKey targetMetadataKey = new MetadataKey(NewsTargetingService.METADATA_TYPE.getName(), originalTargetName, 0);
    when(metadataService.updateMetadata(createdMetadata, 1)).thenReturn(sliderNews);
    when(metadataService.getMetadataByKey(targetMetadataKey)).thenReturn(createdMetadata);

    Metadata updatedMetadata = newsTargetingService.updateNewsTargets(originalTargetName,
                                                                      newsTargetingEntityUpdated,
                                                                      currentIdentity);

    // Then
    assertNotNull(updatedMetadata);
    assertEquals(sliderNews.getId(), updatedMetadata.getId());
    assertEquals(sliderNews.getName(), updatedMetadata.getName());

    // use case when updating a target with the same name and same description
    when(metadataService.getMetadataByKey(any())).thenReturn(sliderNews);
    try {
      newsTargetingService.updateNewsTargets(originalTargetName, newsTargetingEntity, currentIdentity);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    when(metadataService.getMetadataByKey(any())).thenReturn(null);
    try {
      newsTargetingService.updateNewsTargets(originalTargetName, newsTargetingEntity, currentIdentity);
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

  }

}
