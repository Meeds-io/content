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
package io.meeds.content.news.plugin;

import static io.meeds.content.utils.ApplicationSettingsUtils.booleanSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.enumSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.idsSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.integerSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.notAccepted;
import static io.meeds.content.utils.ApplicationSettingsUtils.objectSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.parseIds;
import static io.meeds.content.utils.ApplicationSettingsUtils.parseInt;
import static io.meeds.content.utils.ApplicationSettingsUtils.stringSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.toBoolean;
import static io.meeds.content.utils.ApplicationSettingsUtils.toIds;
import static io.meeds.content.utils.ApplicationSettingsUtils.toInt;
import static io.meeds.content.utils.ApplicationSettingsUtils.toUrl;
import static io.meeds.content.utils.ApplicationSettingsUtils.toValue;
import static io.meeds.content.utils.ApplicationSettingsUtils.translationsSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.content.news.model.News;
import io.meeds.content.news.rest.model.NewsTargetingEntity;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.service.NewsTargetingService;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.plugin.ListViewApplicationSettingsPlugin;
import io.meeds.layout.model.ApplicationSettingsContext;

/**
 * Configures a News list window with the options of its settings drawer,
 * stored as the preferences its action URL stores
 */
@Component
@Profile("layout")
public class NewsListApplicationSettingsPlugin extends ListViewApplicationSettingsPlugin {

  public static final String        PORTLET_NAME               = "NewsListView";

  public static final String        VIEW_TEMPLATE              = "view_template";

  public static final String        SOURCE                     = "source";

  public static final String        NEWS_TARGET                = "news_target";

  public static final String        SELECTED_ARTICLE_IDS       = "selected_article_ids";

  public static final String        LIMIT                      = "limit";

  public static final String        SHOW_HEADER                = "show_header";

  public static final String        SHOW_SEE_ALL               = "show_see_all";

  public static final String        SEE_ALL_URL                = "see_all_url";

  public static final String        SHOW_ARTICLE_TITLE         = "show_article_title";

  public static final String        SHOW_ARTICLE_SUMMARY       = "show_article_summary";

  public static final String        SHOW_ARTICLE_IMAGE         = "show_article_image";

  public static final String        SHOW_ARTICLE_AUTHOR        = "show_article_author";

  public static final String        SHOW_ARTICLE_SPACE         = "show_article_space";

  public static final String        SHOW_ARTICLE_REACTIONS     = "show_article_reactions";

  public static final String        SHOW_ARTICLE_DATE          = "show_article_date";

  public static final String        SOURCE_POSTED              = "posted";

  public static final String        SOURCE_TARGET              = "target";

  public static final String        SOURCE_SELECTED_LIST       = "selectedList";

  public static final String        DEFAULT_VIEW_TEMPLATE      = "NewsCards";

  public static final String        MOSAIC_VIEW_TEMPLATE       = "NewsMosaic";

  public static final String        LATEST_VIEW_TEMPLATE       = "NewsLatest";

  public static final String        ARTICLE_NOT_FOUND          = "does not exist: article";

  public static final String        TARGET_NOT_FOUND           = "does not exist: news_target";

  public static final int           HEADER_MAX_LENGTH          = 100;

  public static final int           DEFAULT_LIMIT              = 4;

  public static final int           MOSAIC_MAX_LIMIT           = 3;

  public static final int           LATEST_MAX_LIMIT           = 4;

  public static final int           MAX_LIMIT                  = 1000;

  public static final List<String>  VIEW_TEMPLATES             = Collections.unmodifiableList(Arrays.asList("NewsSlider",
                                                                               LATEST_VIEW_TEMPLATE,
                                                                               "NewsAlert",
                                                                               "NewsList",
                                                                               MOSAIC_VIEW_TEMPLATE,
                                                                               "NewsStories",
                                                                               DEFAULT_VIEW_TEMPLATE));

  public static final List<String>  SOURCES                    = Collections.unmodifiableList(Arrays.asList(SOURCE_POSTED,
                                                                               SOURCE_TARGET,
                                                                               SOURCE_SELECTED_LIST));

  private static final String       VIEW_TEMPLATE_PREFERENCE   = "viewTemplate";

  private static final String       SOURCE_PREFERENCE          = "articlesSourceOption";

  private static final String       NEWS_TARGET_PREFERENCE     = "newsTarget";

  private static final String       SELECTED_IDS_PREFERENCE    = "selectedArticleIds";

  private static final String       LIMIT_PREFERENCE           = "limit";

  private static final String       SEE_ALL_URL_PREFERENCE     = "seeAllUrl";

  private static final String       SHOW_SEE_ALL_PREFERENCE    = "showSeeAll";

  /** The drawer's switches: option name to preference name */
  private static final Map<String, String> SWITCHES            = new LinkedHashMap<>();

  static {
    SWITCHES.put(SHOW_HEADER, "showHeader");
    SWITCHES.put(SHOW_SEE_ALL, SHOW_SEE_ALL_PREFERENCE);
    SWITCHES.put(SHOW_ARTICLE_TITLE, "showArticleTitle");
    SWITCHES.put(SHOW_ARTICLE_SUMMARY, "showArticleSummary");
    SWITCHES.put(SHOW_ARTICLE_IMAGE, "showArticleImage");
    SWITCHES.put(SHOW_ARTICLE_AUTHOR, "showArticleAuthor");
    SWITCHES.put(SHOW_ARTICLE_SPACE, "showArticleSpace");
    SWITCHES.put(SHOW_ARTICLE_REACTIONS, "showArticleReactions");
    SWITCHES.put(SHOW_ARTICLE_DATE, "showArticleDate");
  }

  @Autowired
  private NewsService          newsService;

  @Autowired
  private NewsTargetingService newsTargetingService;

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public Map<String, Object> getSettingsSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(VIEW_TEMPLATE, enumSchema(VIEW_TEMPLATES, "Display template"));
    properties.put(SOURCE, enumSchema(SOURCES, "Articles shown: posted ones, those of news_target, or selected_article_ids"));
    properties.put(NEWS_TARGET, stringSchema("Name of a publication target the user may use, for the target source; empty for the other sources; a stored target sent back unchanged is kept"));
    properties.put(SELECTED_ARTICLE_IDS, idsSchema("Article ids, in order, for the selectedList source, at most 3 for NewsMosaic and 4 for NewsLatest when given; the limit follows their count"));
    properties.put(LIMIT, integerSchema(1, MAX_LIMIT, "Number of articles, at most 3 for NewsMosaic and 4 for NewsLatest"));
    properties.put(SHOW_HEADER, booleanSchema("Shows the header"));
    properties.put(SHOW_SEE_ALL, booleanSchema("Shows the see all button, which needs see_all_url"));
    properties.put(SEE_ALL_URL, stringSchema("See all target: an http(s) URL or a path of the platform; an empty text removes it"));
    properties.put(SHOW_ARTICLE_TITLE, booleanSchema("Shows the article titles"));
    properties.put(SHOW_ARTICLE_SUMMARY, booleanSchema("Shows the article summaries"));
    properties.put(SHOW_ARTICLE_IMAGE, booleanSchema("Shows the article images"));
    properties.put(SHOW_ARTICLE_AUTHOR, booleanSchema("Shows the article authors"));
    properties.put(SHOW_ARTICLE_SPACE, booleanSchema("Shows the article spaces"));
    properties.put(SHOW_ARTICLE_REACTIONS, booleanSchema("Shows the article reactions"));
    properties.put(SHOW_ARTICLE_DATE, booleanSchema("Shows the article dates"));
    properties.put(HEADER_TITLE,
                   translationsSchema(HEADER_MAX_LENGTH,
                                      "Header title per language tag, replacing every language, required in the default language; not in a site container"));
    return objectSchema(properties);
  }

  @Override
  protected String getCmsType() {
    return NewsUtils.NEWS_LIST_VIEW_CMS_TYPE;
  }

  @Override
  protected String getTranslationObjectType() {
    return NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE;
  }

  @Override
  protected String getHeaderField() {
    return NewsUtils.NEWS_LIST_HEADER_NAME_FIELD;
  }

  @Override
  protected int getHeaderMaxLength() {
    return HEADER_MAX_LENGTH;
  }

  @Override
  protected Context getMappingContext() {
    return NewsUtils.NEWS_LIST_VIEW_CONTEXT;
  }

  @Override
  protected Scope getMappingScope() {
    return NewsUtils.NEWS_LIST_VIEW_SCOPE;
  }

  @Override
  protected List<String> getOptions() {
    List<String> options = new ArrayList<>(Arrays.asList(VIEW_TEMPLATE,
                                                         SOURCE,
                                                         NEWS_TARGET,
                                                         SELECTED_ARTICLE_IDS,
                                                         LIMIT,
                                                         SEE_ALL_URL,
                                                         HEADER_TITLE));
    options.addAll(SWITCHES.keySet());
    return options;
  }

  @Override
  protected Map<String, Object> readPreferences(Map<String, String> preferences) {
    Map<String, Object> settings = new LinkedHashMap<>();
    String viewTemplate = getViewTemplate(preferences);
    settings.put(VIEW_TEMPLATE, viewTemplate);
    settings.put(SOURCE, getSource(preferences));
    settings.put(NEWS_TARGET, StringUtils.defaultString(getStoredTarget(preferences)));
    settings.put(SELECTED_ARTICLE_IDS, parseIds(preferences.get(SELECTED_IDS_PREFERENCE)));
    settings.put(LIMIT, getLimit(preferences, viewTemplate));
    SWITCHES.forEach((option, preference) -> settings.put(option, !"false".equals(preferences.get(preference))));
    settings.put(SHOW_SEE_ALL, isSeeAllShown(preferences));
    settings.put(SEE_ALL_URL, StringUtils.defaultString(preferences.get(SEE_ALL_URL_PREFERENCE)));
    return settings;
  }

  @Override
  protected Map<String, String> toPreferences(ApplicationSettingsContext context,
                                              Map<String, Object> settings,
                                              Identity identity) throws IllegalAccessException, ObjectNotFoundException {
    Map<String, String> stored = context.getPreferences();
    Map<String, String> preferences = new LinkedHashMap<>();
    String viewTemplate = settings.containsKey(VIEW_TEMPLATE) ? toValue(settings.get(VIEW_TEMPLATE), VIEW_TEMPLATE, VIEW_TEMPLATES)
                                                              : getViewTemplate(stored);
    if (settings.containsKey(VIEW_TEMPLATE)) {
      preferences.put(VIEW_TEMPLATE_PREFERENCE, viewTemplate);
    }
    String source = settings.containsKey(SOURCE) ? toValue(settings.get(SOURCE), SOURCE, SOURCES) : getSource(stored);
    if (settings.containsKey(SOURCE)) {
      preferences.put(SOURCE_PREFERENCE, source);
      if (!SOURCE_TARGET.equals(source)) {
        preferences.put(NEWS_TARGET_PREFERENCE, "");
      }
    }
    if (settings.containsKey(NEWS_TARGET)) {
      Object target = settings.get(NEWS_TARGET);
      if (!SOURCE_TARGET.equals(source)) {
        if (!"".equals(target)) {
          throw notAccepted(NEWS_TARGET);
        }
        preferences.put(NEWS_TARGET_PREFERENCE, "");
      } else if (!target.equals(getStoredTarget(stored))) {
        preferences.put(NEWS_TARGET_PREFERENCE, toNewsTarget(target, identity));
      }
    }
    if (SOURCE_TARGET.equals(source)
        && StringUtils.isBlank(preferences.containsKey(NEWS_TARGET_PREFERENCE) ? preferences.get(NEWS_TARGET_PREFERENCE)
                                                                             : getStoredTarget(stored))) {
      throw notAccepted(NEWS_TARGET);
    }
    if (settings.containsKey(SELECTED_ARTICLE_IDS)) {
      List<Long> ids = toIds(settings.get(SELECTED_ARTICLE_IDS), SELECTED_ARTICLE_IDS);
      List<Long> storedIds = parseIds(stored.get(SELECTED_IDS_PREFERENCE));
      requireViewableArticles(ids.stream().filter(id -> !storedIds.contains(id)).toList(), context.getUsername());
      preferences.put(SELECTED_IDS_PREFERENCE, StringUtils.join(ids, ","));
    }
    List<Long> selectedIds = parseIds(preferences.getOrDefault(SELECTED_IDS_PREFERENCE, stored.get(SELECTED_IDS_PREFERENCE)));
    if (SOURCE_SELECTED_LIST.equals(source)) {
      if (settings.containsKey(LIMIT)) {
        throw notAccepted(LIMIT);
      } else if (selectedIds.isEmpty()) {
        throw notAccepted(SELECTED_ARTICLE_IDS);
      } else if ((settings.containsKey(SELECTED_ARTICLE_IDS) || settings.containsKey(SOURCE))
                 && selectedIds.size() > getMaxLimit(viewTemplate)) {
        throw notAccepted(settings.containsKey(SELECTED_ARTICLE_IDS) ? SELECTED_ARTICLE_IDS : SOURCE);
      }
      if (settings.containsKey(SELECTED_ARTICLE_IDS) || settings.containsKey(SOURCE) || settings.containsKey(VIEW_TEMPLATE)) {
        preferences.put(LIMIT_PREFERENCE, String.valueOf(Math.min(selectedIds.size(), getMaxLimit(viewTemplate))));
      }
    } else if (settings.containsKey(LIMIT)) {
      preferences.put(LIMIT_PREFERENCE, String.valueOf(toInt(settings.get(LIMIT), LIMIT, 1, getMaxLimit(viewTemplate))));
    } else if (settings.containsKey(VIEW_TEMPLATE) && getLimit(stored, viewTemplate) > getMaxLimit(viewTemplate)) {
      preferences.put(LIMIT_PREFERENCE, String.valueOf(getMaxLimit(viewTemplate)));
    }
    for (Map.Entry<String, String> entry : SWITCHES.entrySet()) {
      if (settings.containsKey(entry.getKey())) {
        preferences.put(entry.getValue(), String.valueOf(toBoolean(settings.get(entry.getKey()), entry.getKey())));
      }
    }
    if (settings.containsKey(SEE_ALL_URL)) {
      Object seeAllUrl = settings.get(SEE_ALL_URL);
      if ("".equals(seeAllUrl)) {
        preferences.put(SEE_ALL_URL_PREFERENCE, "");
      } else if (!seeAllUrl.equals(stored.get(SEE_ALL_URL_PREFERENCE))) {
        preferences.put(SEE_ALL_URL_PREFERENCE, toUrl(seeAllUrl, SEE_ALL_URL));
      }
    }
    boolean showSeeAll = settings.containsKey(SHOW_SEE_ALL) ? Boolean.parseBoolean(preferences.get(SHOW_SEE_ALL_PREFERENCE))
                                                            : isSeeAllShown(stored);
    if (settings.containsKey(SEE_ALL_URL) && !settings.containsKey(SHOW_SEE_ALL)) {
      preferences.put(SHOW_SEE_ALL_PREFERENCE, String.valueOf(showSeeAll));
    }
    if (showSeeAll && StringUtils.isBlank(preferences.getOrDefault(SEE_ALL_URL_PREFERENCE, stored.get(SEE_ALL_URL_PREFERENCE)))) {
      throw notAccepted(SEE_ALL_URL);
    }
    return preferences;
  }

  private String toNewsTarget(Object value, Identity identity) throws ObjectNotFoundException {
    if (!(value instanceof String name) || StringUtils.isBlank(name)) {
      throw notAccepted(NEWS_TARGET);
    }
    boolean allowed = newsTargetingService.getAllowedTargets(identity)
                                          .stream()
                                          .map(NewsTargetingEntity::getName)
                                          .anyMatch(name::equals);
    if (!allowed) {
      throw new ObjectNotFoundException(TARGET_NOT_FOUND);
    }
    return name;
  }

  private void requireViewableArticles(List<Long> ids, String username) throws ObjectNotFoundException {
    for (long id : ids) {
      News news = newsService.getNewsArticleById(String.valueOf(id));
      if (news == null || !newsService.canViewNews(news, username)) {
        throw new ObjectNotFoundException(ARTICLE_NOT_FOUND);
      }
    }
  }

  private String getViewTemplate(Map<String, String> preferences) {
    String viewTemplate = preferences.get(VIEW_TEMPLATE_PREFERENCE);
    return StringUtils.isBlank(viewTemplate) ? DEFAULT_VIEW_TEMPLATE : viewTemplate;
  }

  /**
   * @return whether the view shows the see all button, as news-list-view's
   *         main.js decides it: the switch not off and a target stored
   */
  private boolean isSeeAllShown(Map<String, String> preferences) {
    return !"false".equals(preferences.get(SHOW_SEE_ALL_PREFERENCE))
           && StringUtils.isNotBlank(preferences.get(SEE_ALL_URL_PREFERENCE));
  }

  /**
   * @return the stored target, null when none: the drawer stores a cleared
   *         target as the text "null", which the view reads as none
   */
  private String getStoredTarget(Map<String, String> preferences) {
    String target = preferences.get(NEWS_TARGET_PREFERENCE);
    return StringUtils.isBlank(target) || "null".equals(target) ? null : target;
  }

  private String getSource(Map<String, String> preferences) {
    if (getStoredTarget(preferences) != null) {
      return SOURCE_TARGET;
    }
    String source = preferences.get(SOURCE_PREFERENCE);
    return SOURCES.contains(source) ? source : SOURCE_POSTED;
  }

  private int getLimit(Map<String, String> preferences, String viewTemplate) {
    return parseInt(preferences.get(LIMIT_PREFERENCE), MOSAIC_VIEW_TEMPLATE.equals(viewTemplate) ? MOSAIC_MAX_LIMIT : DEFAULT_LIMIT);
  }

  private int getMaxLimit(String viewTemplate) {
    if (MOSAIC_VIEW_TEMPLATE.equals(viewTemplate)) {
      return MOSAIC_MAX_LIMIT;
    } else if (LATEST_VIEW_TEMPLATE.equals(viewTemplate)) {
      return LATEST_MAX_LIMIT;
    } else {
      return MAX_LIMIT;
    }
  }

}
