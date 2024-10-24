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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.news.notification.utils.NotificationConstants;
import io.meeds.news.notification.utils.NotificationUtils;
import io.meeds.news.utils.NewsUtils;

public class PublishNewsNotificationPlugin extends BaseNotificationPlugin {

  private static final Log   LOG = ExoLogger.getLogger(PublishNewsNotificationPlugin.class);

  public static final String ID  = "PublishNewsNotificationPlugin";

  private SpaceService       spaceService;

  public PublishNewsNotificationPlugin(InitParams initParams, SpaceService spaceService) {
    super(initParams);
    this.spaceService = spaceService;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    String contentTitle = ctx.value(PostNewsNotificationPlugin.CONTENT_TITLE);
    NotificationConstants.NOTIFICATION_CONTEXT context = ctx.value(PostNewsNotificationPlugin.CONTEXT);
    String contentAuthorUserName = ctx.value(PostNewsNotificationPlugin.CONTENT_AUTHOR);
    String contentAuthor = contentAuthorUserName;
    try {
      contentAuthor = NotificationUtils.getUserFullName(contentAuthorUserName);
    } catch (Exception e) {
      LOG.error("An error occurred when trying to retrieve a user with username " + contentAuthorUserName + " " + e.getMessage(),
                e);
    }
    String currentUserName = ctx.value(PostNewsNotificationPlugin.CURRENT_USER);
    String currentUserFullName = currentUserName;
    try {
      currentUserFullName = NotificationUtils.getUserFullName(currentUserName);
    } catch (Exception e) {
      LOG.error("An error occurred when trying to retrieve a user with username " + currentUserName + " " + e.getMessage(), e);
    }
    String contentSpaceName = ctx.value(PostNewsNotificationPlugin.CONTENT_SPACE);
    String illustrationUrl = ctx.value(PostNewsNotificationPlugin.ILLUSTRATION_URL);
    String authorAvatarUrl = ctx.value(PostNewsNotificationPlugin.AUTHOR_AVATAR_URL);
    String activityLink = ctx.value(PostNewsNotificationPlugin.ACTIVITY_LINK);
    String newsId = ctx.value(PostNewsNotificationPlugin.NEWS_ID);
    String contentSpaceId = ctx.value(PostNewsNotificationPlugin.CONTENT_SPACE_ID);
    String audience = ctx.value(PostNewsNotificationPlugin.AUDIENCE);
    NotificationInfo notificationInfo = NotificationInfo.instance()
                                                        .setFrom(currentUserName)
                                                        .setSpaceId(Long.parseLong(contentSpaceId))
                                                        .with(NotificationConstants.CONTENT_TITLE, contentTitle)
                                                        .with(NotificationConstants.CONTENT_AUTHOR, contentAuthor)
                                                        .with(NotificationConstants.CURRENT_USER, currentUserFullName)
                                                        .with(NotificationConstants.CONTENT_SPACE, contentSpaceName)
                                                        .with(NotificationConstants.ILLUSTRATION_URL, illustrationUrl)
                                                        .with(NotificationConstants.AUTHOR_AVATAR_URL, authorAvatarUrl)
                                                        .with(NotificationConstants.ACTIVITY_LINK, activityLink)
                                                        .with(NotificationConstants.CONTEXT, context.getContext())
                                                        .with(NotificationConstants.NEWS_ID, newsId)
                                                        .key(getKey());

    if (audience.equals(NewsUtils.SPACE_NEWS_AUDIENCE)) {
      notificationInfo.to(getSpaceReceivers(contentSpaceId, currentUserName));
    } else {
      notificationInfo.setSendAllInternals(true);
      List<String> excludedUsers = new ArrayList<>();
      excludedUsers.add(currentUserName);
      if (audience.equals("excludeSpaceMembers")) { // Notification will not be
                                                    // sent to news space
                                                    // members when news
                                                    // audience is changed from
                                                    // "space" to "all"
        excludedUsers.addAll(getSpaceReceivers(contentSpaceId, currentUserName));
      }
      notificationInfo.exclude(excludedUsers);
    }
    return notificationInfo.end();
  }

  private List<String> getSpaceReceivers(String contentSpaceId, String currentUserName) {
    Space space = spaceService.getSpaceById(contentSpaceId);
    return space != null ? Arrays.stream(space.getMembers()).filter(member -> !member.equals(currentUserName)).toList()
                         : new ArrayList<>();
  }
}
