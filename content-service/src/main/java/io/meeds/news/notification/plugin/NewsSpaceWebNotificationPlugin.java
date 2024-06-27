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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.social.notification.model.SpaceWebNotificationItem;
import org.exoplatform.social.notification.plugin.SpaceWebNotificationPlugin;

import io.meeds.news.model.News;
import io.meeds.news.notification.utils.NotificationConstants;
import io.meeds.news.service.NewsService;

public class NewsSpaceWebNotificationPlugin extends SpaceWebNotificationPlugin {

  private static final Log   LOG = ExoLogger.getLogger(NewsSpaceWebNotificationPlugin.class);

  public static final String ID  = "NewsSpaceWebNotificationPlugin";

  private ActivityManager    activityManager;

  private NewsService        newsService;

  public NewsSpaceWebNotificationPlugin(ActivityManager activityManager,
                                        NewsService newsService,
                                        IdentityManager identityManager,
                                        InitParams params) {
    super(identityManager, params);
    this.activityManager = activityManager;
    this.newsService = newsService;
  }

  @Override
  public SpaceWebNotificationItem getSpaceApplicationItem(NotificationInfo notification) {
    String newsId = notification.getValueOwnerParameter(NotificationConstants.NEWS_ID);
    News news = null;
    try {
      news = newsService.getNewsArticleById(newsId);
    } catch (Exception e) {
      LOG.warn("Error retrieving news by id {}", newsId, e);
      return null;
    }
    if (news == null) {
      LOG.debug("News by id {} wasn't found. The space web notification will not be sent.", newsId);
      return null;
    }
    String activityId = news.getActivityId();
    if (StringUtils.isBlank(activityId)) {
      return null;
    }
    ExoSocialActivity activity = activityManager.getActivity(activityId);
    MetadataObject metadataObject;
    if (activity.isHidden()) {
      return null;
    }
    if (activity.isComment()) {
      ExoSocialActivity parentActivity = activityManager.getActivity(activity.getParentId());
      metadataObject = parentActivity.getMetadataObject();
    } else {
      metadataObject = activity.getMetadataObject();
    }
    SpaceWebNotificationItem spaceWebNotificationItem = new SpaceWebNotificationItem(metadataObject.getType(),
                                                                                     metadataObject.getId(),
                                                                                     0,
                                                                                     metadataObject.getSpaceId());
    spaceWebNotificationItem.setActivityId(activityId);
    if (activity.isComment()) {
      spaceWebNotificationItem.addApplicationSubItem(activity.getId());
    }
    return spaceWebNotificationItem;
  }
}
