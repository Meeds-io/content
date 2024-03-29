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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.notification.Utils;

import io.meeds.news.notification.utils.NotificationConstants;
import io.meeds.news.notification.utils.NotificationUtils;

public class MentionInNewsNotificationPlugin extends BaseNotificationPlugin {

  private static final Log                                                        LOG             =
                                                                                      ExoLogger.getLogger(MentionInNewsNotificationPlugin.class);

  public final static String                                                      ID              =
                                                                                     "MentionInNewsNotificationPlugin";

  public static final Pattern                                                     MENTION_PATTERN =
                                                                                                  Pattern.compile("@([^\\s]+)|@([^\\s]+)$");

  public static final ArgumentLiteral<Set>                                        MENTIONED_IDS   =
                                                                                                new ArgumentLiteral<Set>(Set.class,
                                                                                                                         "MENTIONED_IDS");

  public static final ArgumentLiteral<NotificationConstants.NOTIFICATION_CONTEXT> CONTEXT         =
                                                                                          new ArgumentLiteral<NotificationConstants.NOTIFICATION_CONTEXT>(NotificationConstants.NOTIFICATION_CONTEXT.class,
                                                                                                                                                          "CONTEXT");

  public MentionInNewsNotificationPlugin(InitParams initParams) {
    super(initParams);
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
    NotificationConstants.NOTIFICATION_CONTEXT context = ctx.value(CONTEXT);

    String currentUserName = ctx.value(PostNewsNotificationPlugin.CURRENT_USER);
    String currentUserFullName = currentUserName;
    try {
      currentUserFullName = NotificationUtils.getUserFullName(currentUserName);
    } catch (Exception e) {
      LOG.error("An error occured when trying to retreive a user with username " + currentUserName + " " + e.getMessage(), e);
    }
    String contentSpaceId = ctx.value(PostNewsNotificationPlugin.CONTENT_SPACE_ID);
    String contentAuthor = ctx.value(PostNewsNotificationPlugin.CONTENT_AUTHOR);
    String contentSpaceName = ctx.value(PostNewsNotificationPlugin.CONTENT_SPACE);
    List<String> mentionedIds = new ArrayList<>(ctx.value(MENTIONED_IDS));
    String newsTitle = ctx.value(PostNewsNotificationPlugin.CONTENT_TITLE);
    String illustrationUrl = ctx.value(PostNewsNotificationPlugin.ILLUSTRATION_URL);
    String authorAvatarUrl = ctx.value(PostNewsNotificationPlugin.AUTHOR_AVATAR_URL);
    String activityLink = ctx.value(PostNewsNotificationPlugin.ACTIVITY_LINK);
    String newsId = ctx.value(PostNewsNotificationPlugin.NEWS_ID);

    Set<String> receivers = new HashSet<>();
    String[] mentionnedIdArray = new String[mentionedIds.size()];
    Utils.sendToMentioners(receivers, mentionedIds.toArray(mentionnedIdArray), currentUserName, contentSpaceId);
    return NotificationInfo.instance()
                           .setFrom(currentUserName)
                           .setSpaceId(Long.parseLong(contentSpaceId))
                           .to(new ArrayList<>(receivers))
                           .key(getKey())
                           .with(NotificationConstants.CONTENT_TITLE, newsTitle)
                           .with(NotificationConstants.CONTENT_AUTHOR, contentAuthor)
                           .with(NotificationConstants.CURRENT_USER, currentUserFullName)
                           .with(NotificationConstants.CONTENT_SPACE, contentSpaceName)
                           .with(NotificationConstants.ILLUSTRATION_URL, illustrationUrl)
                           .with(NotificationConstants.AUTHOR_AVATAR_URL, authorAvatarUrl)
                           .with(NotificationConstants.ACTIVITY_LINK, activityLink)
                           .with(NotificationConstants.CONTEXT, context.getContext())
                           .with(NotificationConstants.MENTIONED_IDS, String.valueOf(mentionedIds))
                           .with(NotificationConstants.NEWS_ID, newsId)
                           .end();
  }
}
