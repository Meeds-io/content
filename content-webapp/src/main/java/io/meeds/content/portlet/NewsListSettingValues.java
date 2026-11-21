/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.portlet;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.meeds.content.news.rest.model.NewsTargetingEntity;
import io.meeds.content.news.service.NewsTargetingService;

/**
 * The shapes the News List settings accept, when they are saved and when they
 * are read back for the page: a stored value of another shape stands for the
 * setting's default.
 */
public final class NewsListSettingValues {

  private static final int          MAX_LIMIT       = 100;

  private static final Pattern      NUMBER_PATTERN  = Pattern.compile("\\d{1,3}");

  private static final Pattern      ABSOLUTE_LINK   = Pattern.compile("(?i)https?://[^\\s<>\"']+");

  private static final Pattern      RELATIVE_LINK   = Pattern.compile("/(?!/)[^\\s<>\"'\\\\]*");

  private NewsListSettingValues() {
    // Utility class
  }

  public static boolean isValidLimit(String limit) {
    if (StringUtils.isBlank(limit)) {
      return true;
    }
    if (!NUMBER_PATTERN.matcher(limit).matches()) {
      return false;
    }
    int value = Integer.parseInt(limit);
    return value > 0 && value <= MAX_LIMIT;
  }

  public static boolean isValidSeeAllUrl(String url) {
    return StringUtils.isBlank(url) || ABSOLUTE_LINK.matcher(url).matches() || RELATIVE_LINK.matcher(url).matches();
  }

  public static boolean isKnownTarget(String target, NewsTargetingService newsTargetingService) {
    return StringUtils.isBlank(target)
        || newsTargetingService.getAllTargets().stream().map(NewsTargetingEntity::getName).anyMatch(target::equals);
  }

  public static String limitOrDefault(String limit) {
    return isValidLimit(limit) ? limit : "";
  }

  public static String seeAllUrlOrDefault(String url) {
    return isValidSeeAllUrl(url) ? url : "";
  }

  public static String targetOrDefault(String target, NewsTargetingService newsTargetingService) {
    return isKnownTarget(target, newsTargetingService) ? target : "";
  }

}
