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

public class NotificationConstants {

  public static final String NEWS_ID           = "NEWS_ID";

  public static final String CONTENT_TITLE     = "CONTENT_TITLE";

  public static final String CONTENT_AUTHOR    = "CONTENT_AUTHOR";

  public static final String CONTENT_URL       = "CONTENT_URL";

  public static final String ILLUSTRATION_URL  = "ILLUSTRATION_URL";

  public static final String AUTHOR_AVATAR_URL = "AUTHOR_AVATAR_URL";

  public static final String CONTENT_SPACE     = "CONTENT_SPACE";

  public static final String CONTENT_SPACE_ID  = "CONTENT_SPACE_ID";

  public static final String ACTIVITY_LINK     = "ACTIVITY_LINK";

  public static final String CONTEXT           = "CONTEXT";

  public static final String CURRENT_USER      = "CURRENT_USER";

  public static final String MENTIONED_IDS     = "MENTIONED_IDS";

  public static enum NOTIFICATION_CONTEXT {
    POST_NEWS("POST NEWS"), MENTION_IN_NEWS("MENTION IN NEWS"), PUBLISH_NEWS("PUBLISH NEWS");

    private String context;

    private NOTIFICATION_CONTEXT(String context) {
      this.context = context;
    }

    public String getContext() {
      return this.context;
    }
  }

}
