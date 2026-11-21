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
package io.meeds.content.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.meeds.content.news.service.NewsTargetingService;

/**
 * The shapes the News List settings accept, when they are saved and when they
 * are read back for the page: a stored value of another shape stands for the
 * setting's default.
 */
public final class NewsListSettingValues {

  // what the drawer posts for a cleared target (FormData stringifies null)
  public static final String        NULL_TARGET     = "null";

  private static final Pattern      NUMBER_PATTERN  = Pattern.compile("\\d{1,9}");

  private static final Pattern      LEADING_CONTROL = Pattern.compile("^[\\u0000-\\u0020]+");

  private static final Pattern      LINK_SCHEME     = Pattern.compile("^([A-Za-z][A-Za-z0-9+.-]*):");

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
    return Integer.parseInt(limit) > 0;
  }

  public static boolean isValidSeeAllUrl(String url) {
    if (StringUtils.isBlank(url)) {
      return true;
    }
    // a link is read as a browser reads it
    String link = LEADING_CONTROL.matcher(url.replaceAll("[\\t\\r\\n]", "")).replaceFirst("");
    Matcher scheme = LINK_SCHEME.matcher(link);
    return !scheme.find() || "http".equalsIgnoreCase(scheme.group(1)) || "https".equalsIgnoreCase(scheme.group(1));
  }

  public static boolean isKnownTarget(String target, NewsTargetingService newsTargetingService) {
    return isBlankTarget(target)
        || newsTargetingService.isTarget(target);
  }

  /**
   * @param  target a posted or stored target
   * @return        true when it stands for no target: blank or the text the
   *                drawer posts for a cleared one
   */
  public static boolean isBlankTarget(String target) {
    return StringUtils.isBlank(target) || NULL_TARGET.equals(target);
  }

  public static String limitOrDefault(String limit) {
    return isValidLimit(limit) ? limit : "";
  }

  public static String seeAllUrlOrDefault(String url) {
    return isValidSeeAllUrl(url) ? url : "";
  }

  /**
   * @param  target a stored target, as stored
   * @param  source a stored source of the articles, as stored
   * @return        the stored source; for a window saved before the source
   *                existed, the target source when a target remains stored,
   *                even one that no longer exists; else blank
   */
  public static String sourceOrDefault(String target, String source) {
    if (StringUtils.isNotBlank(source)) {
      return source;
    }
    return isBlankTarget(target) ? "" : "target";
  }

  public static String targetOrDefault(String target, NewsTargetingService newsTargetingService) {
    return !isBlankTarget(target) && isKnownTarget(target, newsTargetingService) ? target : "";
  }

}
