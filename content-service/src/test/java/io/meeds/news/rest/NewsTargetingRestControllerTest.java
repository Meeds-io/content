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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.model.Metadata;

import io.meeds.news.service.NewsTargetingService;
import io.meeds.news.utils.NewsUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class NewsTargetingRestControllerTest {

  @Mock
  NewsTargetingService newsTargetingService;

  @Mock
  PortalContainer      container;

  @Mock
  IdentityManager      identityManager;

  @InjectMocks
  private NewsTargetingRestController newsTargetingRestController;



  @Before
  public void setup() {
    newsTargetingRestController.init();
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
  }

  @Test
  public void shouldReturnOkWhenGetTargets() {
    // Given
    when(newsTargetingService.getAllTargets()).thenReturn(new ArrayList<>());

    // When
    ResponseEntity response = newsTargetingRestController.getAllTargets();

    // Then
    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
  }

  @Test
  public void shouldReturnOkWhenGetAllowedTargets() {
    Identity currentIdentity = new Identity("john");
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    // Given
    lenient().when(newsTargetingService.getAllTargets()).thenReturn(new ArrayList<>());
    // When
    ResponseEntity response = newsTargetingRestController.getAllowedTargets();

    // Then
    assertEquals(org.springframework.http.HttpStatus.OK.value(), response.getStatusCodeValue());

    when(newsTargetingRestController.getAllowedTargets()).thenThrow(RuntimeException.class);

    // When
    response = newsTargetingRestController.getAllowedTargets();

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCodeValue());

  }

  @Test
  public void shouldReturnOkWhenDeleteNewsTarget() {
    // Given
    Identity currentIdentity = new Identity("john");
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    List<NewsTargetingEntity> targets = new LinkedList<>();
    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName("test1");
    targets.add(newsTargetingEntity);
    lenient().when(newsTargetingService.getAllTargets()).thenReturn(targets);

    // When
    Response response = newsTargetingRestController.deleteTarget(targets.get(0).getName(), 0);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    when(newsTargetingRestController.deleteTarget(targets.get(0).getName(), 0)).thenThrow(RuntimeException.class);

    // When
    response = newsTargetingRestController.deleteTarget(targets.get(0).getName(), 0);

    // Then
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldReturnOkWhenCreateTargets() throws IllegalAccessException {
    // Given

    Identity currentIdentity = new Identity("john");
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNewsProperties.put(NewsUtils.TARGET_PERMISSIONS, "space:1");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(1);
    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName(sliderNews.getName());
    newsTargetingEntity.setProperties(sliderNewsProperties);
    lenient().when(newsTargetingService.createNewsTarget(newsTargetingEntity, currentIdentity)).thenReturn(sliderNews);

    // When
    Response response = newsTargetingRestController.createNewsTarget(newsTargetingEntity);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    when(newsTargetingRestController.createNewsTarget(newsTargetingEntity)).thenThrow(RuntimeException.class);

    // When
    response = newsTargetingRestController.createNewsTarget(newsTargetingEntity);

    // Then
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

  }

  @Test
  public void shouldReturnOkWhenUpdateTargets() throws IllegalAccessException {
    // Given
    Identity currentIdentity = new Identity("john");
    ConversationState.setCurrent(new ConversationState(currentIdentity));
    Metadata sliderNews = new Metadata();
    sliderNews.setName("sliderNews");
    sliderNews.setCreatedDate(100);
    HashMap<String, String> sliderNewsProperties = new HashMap<>();
    sliderNewsProperties.put("label", "slider news");
    sliderNewsProperties.put(NewsUtils.TARGET_PERMISSIONS, "space:1");
    sliderNews.setProperties(sliderNewsProperties);
    sliderNews.setId(1);
    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName(sliderNews.getName());
    newsTargetingEntity.setProperties(sliderNewsProperties);
    String originalTargetName = "sliderNews";
    lenient().when(newsTargetingService.updateNewsTargets(originalTargetName, newsTargetingEntity, currentIdentity))
             .thenReturn(sliderNews);

    // When
    Response response = newsTargetingRestController.updateNewsTarget(newsTargetingEntity, originalTargetName);

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    when(newsTargetingRestController.updateNewsTarget(newsTargetingEntity,
                                                       originalTargetName)).thenThrow(RuntimeException.class);

    // When
    response = newsTargetingRestController.updateNewsTarget(newsTargetingEntity, originalTargetName);

    // Then
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

}
