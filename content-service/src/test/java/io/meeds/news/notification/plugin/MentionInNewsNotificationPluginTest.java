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
package io.meeds.news.notification.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.notification.Utils;

import io.meeds.news.notification.utils.NotificationConstants;

@RunWith(MockitoJUnitRunner.class)
public class MentionInNewsNotificationPluginTest {

  private static MockedStatic<CommonsUtils>        COMMONS_UTILS;

  private static MockedStatic<Utils>               SOCIAL_UTILS;

  private static MockedStatic<PluginKey>           PLUGIN_KEY;

  private static MockedStatic<ExoContainerContext> EXO_CONTAINER_CONTEXT;

  @Mock
  private UserHandler                              userhandler;

  @Mock
  private SpaceService                             spaceService;

  @Mock
  private InitParams                               initParams;

  @Mock
  private OrganizationService                      orgService;

  @BeforeClass
  public static void beforeRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS = mockStatic(CommonsUtils.class);
    PLUGIN_KEY = mockStatic(PluginKey.class);
    SOCIAL_UTILS = mockStatic(Utils.class);
    EXO_CONTAINER_CONTEXT = mockStatic(ExoContainerContext.class);
  }

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
    SOCIAL_UTILS.close();
    PLUGIN_KEY.close();
    EXO_CONTAINER_CONTEXT.close();
  }

  @Test
  public void shouldMakeNotificationForMentionInNewsContext() throws Exception {
    // Given
    when(orgService.getUserHandler()).thenReturn(userhandler);
    MentionInNewsNotificationPlugin newsPlugin = new MentionInNewsNotificationPlugin(initParams);
    Set<String> mentionedIds = new HashSet<>(Collections.singleton("0"));

    COMMONS_UTILS.when(() -> CommonsUtils.getService(NotificationService.class)).thenReturn(null);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(NotificationCompletionService.class)).thenReturn(null);
    NotificationContext ctx =
                            NotificationContextImpl.cloneInstance()
                                                   .append(PostNewsNotificationPlugin.CONTENT_TITLE, "title")
                                                   .append(PostNewsNotificationPlugin.CONTENT_AUTHOR, "root")
                                                   .append(PostNewsNotificationPlugin.CURRENT_USER, "root")
                                                   .append(PostNewsNotificationPlugin.CONTENT_SPACE_ID, "1")
                                                   .append(PostNewsNotificationPlugin.CONTENT_SPACE, "space1")
                                                   .append(MentionInNewsNotificationPlugin.MENTIONED_IDS, mentionedIds)
                                                   .append(PostNewsNotificationPlugin.ILLUSTRATION_URL,
                                                           "http://localhost:8080/rest/v1/news/id123/illustration")
                                                   .append(PostNewsNotificationPlugin.AUTHOR_AVATAR_URL,
                                                           "http://localhost:8080/portal/rest/v1/social/users/default-image/avatar")
                                                   .append(PostNewsNotificationPlugin.ACTIVITY_LINK,
                                                           "http://localhost:8080/portal/intranet/activity?id=38")
                                                   .append(MentionInNewsNotificationPlugin.CONTEXT,
                                                           NotificationConstants.NOTIFICATION_CONTEXT.MENTION_IN_NEWS);

    User contentAuthorUser = mock(User.class);
    when(userhandler.findUserByName("root")).thenReturn(contentAuthorUser);
    User currentUser = mock(User.class);
    when(userhandler.findUserByName("root")).thenReturn(currentUser);
    when(currentUser.getFullName()).thenReturn("root root");
    User user1 = mock(User.class);
    User user2 = mock(User.class);

    User[] receivers = new User[2];
    receivers[0] = user1;
    receivers[1] = user2;

    mockIdGeneratorService();
    COMMONS_UTILS.when(() -> CommonsUtils.getService(OrganizationService.class)).thenReturn(orgService);
    Space space = new Space();
    space.setId("1");
    space.setGroupId("space1");

    // When
    NotificationInfo notificationInfo = newsPlugin.makeNotification(ctx);

    // Then
    assertEquals("root", notificationInfo.getFrom());
    assertEquals("", notificationInfo.getTitle());
    assertEquals("title", notificationInfo.getValueOwnerParameter("CONTENT_TITLE"));
    assertEquals("root", notificationInfo.getValueOwnerParameter("CONTENT_AUTHOR"));
    assertEquals("http://localhost:8080/rest/v1/news/id123/illustration",
                 notificationInfo.getValueOwnerParameter("ILLUSTRATION_URL"));
    assertEquals("http://localhost:8080/portal/rest/v1/social/users/default-image/avatar",
                 notificationInfo.getValueOwnerParameter("AUTHOR_AVATAR_URL"));
    assertEquals("space1", notificationInfo.getValueOwnerParameter("CONTENT_SPACE"));
    assertEquals("http://localhost:8080/portal/intranet/activity?id=38",
                 notificationInfo.getValueOwnerParameter("ACTIVITY_LINK"));
  }

  public static void mockIdGeneratorService() {
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getService(IDGeneratorService.class))
                         .thenReturn(new IDGeneratorService() {
                           @Override
                           public String generateStringID(Object o) {
                             return "123456";
                           }

                           @Override
                           public long generateLongID(Object o) {
                             return 123456;
                           }

                           @Override
                           public Serializable generateID(Object o) {
                             return 123456;
                           }

                           @Override
                           public int generatIntegerID(Object o) {
                             return 123456;
                           }
                         });
  }

}
