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

import java.util.List;
import java.util.regex.Pattern;

/**
 * The shapes the settings of the News List and Content List drawers accept
 * when they are saved. A setting that is not posted is not checked: it keeps
 * its stored value.
 */
public final class ListViewSettingValues {

  public static final int          MAX_CATEGORY_DEPTH    = 50;

  private static final List<String> SOURCE_OPTIONS        = List.of("posted", "target", "selectedList");

  // a view template is registered by name on the client, so its name is
  // checked for its shape
  private static final Pattern     VIEW_TEMPLATE_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

  private static final Pattern     ID_LIST_PATTERN       = Pattern.compile("\\d{1,18}(,\\d{1,18})*");

  private static final Pattern     DEPTH_PATTERN         = Pattern.compile("\\d{1,2}");

  private ListViewSettingValues() {
    // Utility class
  }

  public static boolean isValidFlag(String flag) {
    return flag == null || "true".equals(flag) || "false".equals(flag);
  }

  public static boolean isValidViewTemplate(String viewTemplate) {
    return viewTemplate == null || viewTemplate.isEmpty() || VIEW_TEMPLATE_PATTERN.matcher(viewTemplate).matches();
  }

  public static boolean isValidSourceOption(String sourceOption) {
    return sourceOption == null || sourceOption.isEmpty() || SOURCE_OPTIONS.contains(sourceOption);
  }

  /**
   * @param  ids a posted list of ids
   * @return     true when it is not posted, blank, or comma separated whole
   *             numbers
   */
  public static boolean isValidIdList(String ids) {
    return ids == null || ids.isEmpty() || ID_LIST_PATTERN.matcher(ids).matches();
  }

  /**
   * @param  depth a posted category depth
   * @return       true when it is not posted, blank, or a whole number the
   *               drawer offers (0 to {@value #MAX_CATEGORY_DEPTH})
   */
  public static boolean isValidCategoryDepth(String depth) {
    return depth == null
        || depth.isEmpty()
        || DEPTH_PATTERN.matcher(depth).matches() && Integer.parseInt(depth) <= MAX_CATEGORY_DEPTH;
  }

}
