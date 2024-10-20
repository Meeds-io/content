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
package io.meeds.news.notification.utils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;

public class NotificationUtils {

  public static String getUserFullName(String userName) throws Exception {
    OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);
    UserHandler userHandler = organizationService.getUserHandler();
    User user = userHandler.findUserByName(userName);
    if (user == null) {
      throw new Exception("An error occured when trying to retreive a user with username " + userName);
    }
    return user.getFullName();
  }

  public static String getNotificationActivityLink(Space space, String activityId, boolean isMember) {
    String activityLink = "";
    if (isMember) {
      activityLink = getActivityPermalink(activityId);
    } else {
      activityLink = getNotificationActivityLinkForNotSpaceMembers(space);
    }
    String baseUrl = PropertyManager.getProperty("gatein.email.domain.url");
    return baseUrl == null ? activityLink : baseUrl.concat(activityLink);
  }

  public static String getNotificationActivityLinkForNotSpaceMembers(Space space) {
    return "/".concat(PortalContainer.getCurrentPortalContainerName())
              .concat("/s/")
              .concat(space.getId());
  }

  private static String getActivityPermalink(String activityId) {
    return LinkProvider.getSingleActivityUrl(activityId);
  }

}
