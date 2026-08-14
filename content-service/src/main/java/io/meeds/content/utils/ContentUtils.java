/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.utils;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.wiki.utils.Utils;

public class ContentUtils {

  private static final int   SUMMARY_MAX_LENGTH = 200;

  private static final String SUMMARY_ELLIPSIS   = "...";

  private ContentUtils() {
    // Utility class
  }

  /**
   * @param storedSummary the content's own, explicitly-set summary, if any
   * @param htmlContent the content's full body/content, used as a fallback
   *          (converted to plain text and truncated) when no summary was
   *          explicitly set
   * @return the summary to display for this content
   */
  public static String resolveSummary(String storedSummary, String htmlContent) {
    if (StringUtils.isNotBlank(storedSummary)) {
      return storedSummary;
    }
    if (StringUtils.isBlank(htmlContent)) {
      return storedSummary;
    }
    String text = Utils.html2text(htmlContent);
    return text.length() > SUMMARY_MAX_LENGTH ? text.substring(0, SUMMARY_MAX_LENGTH) + SUMMARY_ELLIPSIS : text;
  }

  public static final String CONTENT_TYPE_NEWS          = "news";

  public static final String CONTENT_TYPE_NOTES         = "notes";

  public static final String STATUS_PUBLISHED           = "published";

  public static final String STATUS_MY_CONTENT          = "myContent";

  public static final String STATUS_SCHEDULED           = "scheduled";

  public static final String STATUS_DRAFT               = "draft";

  public static final String  CONTENT_LIST_VIEW_SCOPE_NAME   = "CONTENT_LIST_VIEW_SCOPE";

  public static final String  CONTENT_LIST_VIEW_CONTEXT_NAME = "CONTENT_LIST_VIEW_CONTEXT";

  public static final Context CONTENT_LIST_VIEW_CONTEXT       = Context.GLOBAL.id(CONTENT_LIST_VIEW_CONTEXT_NAME);

  public static final Scope   CONTENT_LIST_VIEW_SCOPE         = Scope.APPLICATION.id(CONTENT_LIST_VIEW_SCOPE_NAME);

  /**
   * Translation field name under which the Content List portlet's header
   * title is saved - must stay in sync with the same literal used in
   * {@code ContentListSettingsDrawer.vue} (headerTitleFieldName) and
   * {@code content-list-view/main.js}'s {@code getTranslations(...)} call.
   */
  public static final String  CONTENT_LIST_VIEW_TYPE          = "contentListView";

  public static final String  CONTENT_LIST_HEADER_TITLE_FIELD = "headerTitleInput";

}
