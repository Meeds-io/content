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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.social.notification.model.SpaceWebNotificationItem;

import io.meeds.news.model.News;
import io.meeds.news.notification.utils.NotificationConstants;
import io.meeds.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class NewsSpaceWebNotificationPluginTest {

  private static final String                   ACTIVITY_NOTIFICATION_PLUGIN_ID = "ACTIVITY_NOTIFICATION";

  private static final String                   USERNAME                        = "username";

  @Mock
  private IdentityManager                       identityManager;

  @Mock
  private ActivityManager                       activityManager;

  @Mock
  private InitParams                            initParams;

  @Mock
  private NewsService                           newsService;

  private static NewsSpaceWebNotificationPlugin newsSpaceWebNotificationPlugin;

  @Before
  public void setUp() throws Exception {
    ValuesParam pluginIdsValues = new ValuesParam();
    pluginIdsValues.setValues(Arrays.asList(ACTIVITY_NOTIFICATION_PLUGIN_ID));
    when(initParams.getValuesParam("notification.plugin.ids")).thenReturn(pluginIdsValues);
    newsSpaceWebNotificationPlugin =
                                   new NewsSpaceWebNotificationPlugin(activityManager, newsService, identityManager, initParams);
  }

  @Test
  public void testGetSpaceApplicationItem() throws Exception {
    NotificationInfo notificationInfo = mock(NotificationInfo.class);
    Identity userIdentity = mock(Identity.class);
    String activityId = "activityId";
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    MetadataObject metadataObject = mock(MetadataObject.class);
    News news = mock(News.class);
    long spaceId = 12l;
    long userIdentityId = 15l;
    String newsId = "newsId";
    String metadataObjectType = ExoSocialActivityImpl.DEFAULT_ACTIVITY_METADATA_OBJECT_TYPE;
    String metadataObjectId = activityId;
    when(notificationInfo.getValueOwnerParameter(NotificationConstants.NEWS_ID)).thenReturn(newsId);
    when(newsService.getNewsArticleById(newsId)).thenReturn(news);
    when(news.getActivityId()).thenReturn(activityId);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);
    when(userIdentity.getId()).thenReturn(String.valueOf(userIdentityId));
    when(activityManager.getActivity(activityId)).thenReturn(activity);
    when(activity.getMetadataObject()).thenReturn(metadataObject);
    when(metadataObject.getType()).thenReturn(metadataObjectType);
    when(metadataObject.getId()).thenReturn(metadataObjectId);
    when(metadataObject.getSpaceId()).thenReturn(spaceId);

    SpaceWebNotificationItem spaceApplicationItem = newsSpaceWebNotificationPlugin.getSpaceApplicationItem(notificationInfo,
                                                                                                           USERNAME);
    assertNotNull(spaceApplicationItem);
    assertEquals(activityId, spaceApplicationItem.getApplicationItemId());
    assertEquals(metadataObjectType, spaceApplicationItem.getApplicationName());
    assertEquals(spaceId, spaceApplicationItem.getSpaceId());
    assertEquals(userIdentityId, spaceApplicationItem.getUserId());
  }
}
