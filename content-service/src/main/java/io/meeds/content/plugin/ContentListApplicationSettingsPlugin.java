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
package io.meeds.content.plugin;

import static io.meeds.content.utils.ApplicationSettingsUtils.booleanSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.idsSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.integerSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.objectSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.toBoolean;
import static io.meeds.content.utils.ApplicationSettingsUtils.toIds;
import static io.meeds.content.utils.ApplicationSettingsUtils.toInt;
import static io.meeds.content.utils.ApplicationSettingsUtils.translationsSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.content.utils.ContentUtils;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.social.category.service.CategoryService;

/**
 * Configures a Content list window with the options of its settings
 * drawer, stored as the preferences its action URL stores
 */
@Component
@Profile("layout")
public class ContentListApplicationSettingsPlugin extends ListViewApplicationSettingsPlugin {

  public static final String  PORTLET_NAME                     = "ContentListView";

  public static final String  SHOW_HEADER                      = "show_header";

  public static final String  ALLOW_FILTERING_PER_CATEGORY     = "allow_filtering_per_category";

  public static final String  CATEGORY_DEPTH                   = "category_depth";

  public static final String  CATEGORY_IDS                     = "category_ids";

  public static final String  EXCLUDE_CATEGORY_IDS             = "exclude_category_ids";

  public static final String  CATEGORY_NOT_FOUND               = "does not exist: category";

  public static final int     HEADER_MAX_LENGTH                = 150;

  public static final int     MAX_CATEGORY_DEPTH               = 50;

  public static final int     DEFAULT_CATEGORY_DEPTH           = 4;

  private static final String SHOW_HEADER_PREFERENCE           = "showHeader";

  private static final String HEADER_TITLE_PREFERENCE          = "headerTitle";

  private static final String ALLOW_FILTERING_PREFERENCE       = "allowFilteringPerCategory";

  private static final String CATEGORY_DEPTH_PREFERENCE        = "categoryDepth";

  private static final String CATEGORY_IDS_PREFERENCE          = "categoryIds";

  private static final String EXCLUDE_CATEGORY_IDS_PREFERENCE  = "excludeCategoryIds";

  @Autowired
  private CategoryService     categoryService;

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public Map<String, Object> getSettingsSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(SHOW_HEADER, booleanSchema("Shows the header"));
    properties.put(HEADER_TITLE,
                   translationsSchema(HEADER_MAX_LENGTH,
                                      "Header title per language tag, replacing every language, required in the default language; not in a site container"));
    properties.put(ALLOW_FILTERING_PER_CATEGORY, booleanSchema("Lets readers filter the contents per category"));
    properties.put(CATEGORY_DEPTH, integerSchema(0, MAX_CATEGORY_DEPTH, "Depth of the category filter"));
    properties.put(CATEGORY_IDS, idsSchema("Ids of the categories whose contents are listed, in display order; [] lists every category"));
    properties.put(EXCLUDE_CATEGORY_IDS, idsSchema("Ids of the categories whose contents are not listed; [] excludes none"));
    return objectSchema(properties);
  }

  @Override
  protected String getCmsType() {
    return ContentUtils.CONTENT_LIST_VIEW_CMS_TYPE;
  }

  @Override
  protected String getTranslationObjectType() {
    return ContentUtils.CONTENT_LIST_VIEW_TYPE;
  }

  @Override
  protected String getHeaderField() {
    return ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD;
  }

  @Override
  protected int getHeaderMaxLength() {
    return HEADER_MAX_LENGTH;
  }

  @Override
  protected Context getMappingContext() {
    return ContentUtils.CONTENT_LIST_VIEW_CONTEXT;
  }

  @Override
  protected Scope getMappingScope() {
    return ContentUtils.CONTENT_LIST_VIEW_SCOPE;
  }

  @Override
  protected List<String> getOptions() {
    return Arrays.asList(SHOW_HEADER,
                         HEADER_TITLE,
                         ALLOW_FILTERING_PER_CATEGORY,
                         CATEGORY_DEPTH,
                         CATEGORY_IDS,
                         EXCLUDE_CATEGORY_IDS);
  }

  @Override
  protected Map<String, Object> readPreferences(Map<String, String> preferences) {
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put(SHOW_HEADER, !"false".equals(preferences.get(SHOW_HEADER_PREFERENCE)));
    settings.put(ALLOW_FILTERING_PER_CATEGORY, !"false".equals(preferences.get(ALLOW_FILTERING_PREFERENCE)));
    String depth = preferences.get(CATEGORY_DEPTH_PREFERENCE);
    settings.put(CATEGORY_DEPTH,
                 StringUtils.isNotBlank(depth) && StringUtils.isNumeric(depth) ? Integer.parseInt(depth) : DEFAULT_CATEGORY_DEPTH);
    settings.put(CATEGORY_IDS, toIdList(preferences.get(CATEGORY_IDS_PREFERENCE)));
    settings.put(EXCLUDE_CATEGORY_IDS, toIdList(preferences.get(EXCLUDE_CATEGORY_IDS_PREFERENCE)));
    return settings;
  }

  @Override
  protected Map<String, String> toPreferences(ApplicationSettingsContext context,
                                              Map<String, Object> settings,
                                              Identity identity) throws IllegalAccessException, ObjectNotFoundException {
    Map<String, String> preferences = new LinkedHashMap<>();
    if (settings.containsKey(SHOW_HEADER)) {
      preferences.put(SHOW_HEADER_PREFERENCE, String.valueOf(toBoolean(settings.get(SHOW_HEADER), SHOW_HEADER)));
    }
    if (settings.containsKey(ALLOW_FILTERING_PER_CATEGORY)) {
      preferences.put(ALLOW_FILTERING_PREFERENCE,
                      String.valueOf(toBoolean(settings.get(ALLOW_FILTERING_PER_CATEGORY), ALLOW_FILTERING_PER_CATEGORY)));
    }
    if (settings.containsKey(CATEGORY_DEPTH)) {
      preferences.put(CATEGORY_DEPTH_PREFERENCE,
                      String.valueOf(toInt(settings.get(CATEGORY_DEPTH), CATEGORY_DEPTH, 0, MAX_CATEGORY_DEPTH)));
    }
    if (settings.containsKey(CATEGORY_IDS)) {
      preferences.put(CATEGORY_IDS_PREFERENCE, toCategoryIds(settings.get(CATEGORY_IDS), CATEGORY_IDS, context.getUsername()));
    }
    if (settings.containsKey(EXCLUDE_CATEGORY_IDS)) {
      preferences.put(EXCLUDE_CATEGORY_IDS_PREFERENCE,
                      toCategoryIds(settings.get(EXCLUDE_CATEGORY_IDS), EXCLUDE_CATEGORY_IDS, context.getUsername()));
    }
    return preferences;
  }

  @Override
  protected Map<String, String> getHeaderPreferences(Map<String, String> header) {
    Map<String, String> preferences = new LinkedHashMap<>();
    preferences.put(HEADER_TITLE_PREFERENCE, header.get(getDefaultLanguage()));
    return preferences;
  }

  private String toCategoryIds(Object value, String option, String username) throws IllegalAccessException,
                                                                             ObjectNotFoundException {
    List<Long> ids = toIds(value, option);
    for (long id : ids) {
      try {
        categoryService.getCategory(id, username, Locale.ENGLISH);
      } catch (ObjectNotFoundException e) {
        throw new ObjectNotFoundException(CATEGORY_NOT_FOUND);
      }
    }
    return StringUtils.join(ids, ",");
  }

  private List<Long> toIdList(String value) {
    List<Long> ids = new ArrayList<>();
    for (String id : StringUtils.split(StringUtils.defaultString(value), ',')) {
      if (StringUtils.isNotBlank(id) && StringUtils.isNumeric(id.trim())) {
        ids.add(Long.parseLong(id.trim()));
      }
    }
    return ids;
  }

}
