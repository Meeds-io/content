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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.content.news.utils.NewsUtils;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.hidden.plugin.HiddenApplicationPlugin;
import io.meeds.social.hidden.service.HiddenApplicationService;
import io.meeds.social.translation.service.TranslationService;

/**
 * Lets each user hide a news list application whose editor allowed it. The
 * news list keeps its display options as portlet preferences, so whether a
 * window allows hiding is kept by {@link HiddenApplicationService}.
 */
@Service("newsListHiddenApplicationPlugin")
public class NewsListHiddenApplicationPlugin implements HiddenApplicationPlugin {

  public static final String         APPLICATION_TYPE     = "newsList";

  public static final String         SETTING_TYPE         = "newsListViewPortlet";

  public static final String         NEWS_BUNDLE_NAME     = "locale.portlet.news.News";

  public static final String         DEFAULT_NAME_KEY     = "news.list.label.defaultName";

  @Autowired
  private CMSService                 cmsService;

  @Autowired
  private HiddenApplicationService   hiddenApplicationService;

  @Autowired
  private SettingService             settingService;

  @Autowired
  private TranslationService         translationService;

  @Autowired
  private ResourceBundleService      resourceBundleService;

  @Override
  public String getApplicationType() {
    return APPLICATION_TYPE;
  }

  @Override
  public HiddenApplication getApplication(String name, String language) {
    CMSSetting setting = cmsService.getSetting(SETTING_TYPE, name);
    if (setting == null) {
      return null;
    }
    return new HiddenApplication(APPLICATION_TYPE,
                                 name,
                                 getTitle(name, language),
                                 setting.getPageReference(),
                                 hiddenApplicationService.isHideAllowed(APPLICATION_TYPE, name));
  }

  @Override
  public boolean canView(String name, Identity identity) {
    return cmsService.hasAccessPermission(identity, SETTING_TYPE, name);
  }

  /**
   * Allows each user to hide a news list window, or forbids it, which shows
   * the window again to every user who hid it. Social's hidden applications
   * check no right for it: only an editor of the window changes it.
   *
   * @param name window setting name
   * @param allowed whether each user may hide the window
   * @param identity the user changing the option
   * @throws IllegalAccessException when the user can't edit the window
   */
  public void saveHideAllowed(String name, boolean allowed, Identity identity) throws IllegalAccessException {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("News list setting name is mandatory");
    }
    if (identity == null || !cmsService.hasEditPermission(identity, SETTING_TYPE, name)) {
      throw new IllegalAccessException(String.format("User %s can't edit the news list %s",
                                                     identity == null ? null : identity.getUserId(),
                                                     name));
    }
    hiddenApplicationService.setHideAllowed(APPLICATION_TYPE, name, allowed);
  }

  private String getTitle(String name, String language) {
    Locale locale = StringUtils.isBlank(language) ? Locale.ENGLISH : Locale.forLanguageTag(language);
    String applicationId = getApplicationId(name);
    String title = applicationId == null ? null : getHeader(applicationId, locale);
    if (StringUtils.isNotBlank(title)) {
      return title;
    }
    ResourceBundle bundle = resourceBundleService.getResourceBundle(NEWS_BUNDLE_NAME, locale);
    return bundle != null && bundle.containsKey(DEFAULT_NAME_KEY) ? bundle.getString(DEFAULT_NAME_KEY) : null;
  }

  private String getHeader(String applicationId, Locale locale) {
    String header = translationService.getTranslationLabel(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                            applicationId,
                                                            NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD,
                                                            locale);
    return StringUtils.isNotBlank(header) ? header :
                                          translationService.getTranslationLabel(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                                                 applicationId,
                                                                                 NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD,
                                                                                 Locale.ENGLISH);
  }

  /**
   * @param name window setting name
   * @return the identifier the header translations of the window are kept
   *         with, which the news list maps to its setting name. The news list
   *         stores that mapping one way only, identifier to name, so this
   *         scans every news list's mapping; when a copied window re-pointed
   *         an older identifier to the same name, either one is kept.
   */
  private String getApplicationId(String name) {
    Map<String, SettingValue> applications =
                                           settingService.getSettingsByContextAndScope(NewsUtils.NEWS_LIST_VIEW_CONTEXT.getName(),
                                                                                       NewsUtils.NEWS_LIST_VIEW_CONTEXT.getId(),
                                                                                       NewsUtils.NEWS_LIST_VIEW_SCOPE.getName(),
                                                                                       NewsUtils.NEWS_LIST_VIEW_SCOPE.getId());
    if (MapUtils.isEmpty(applications)) {
      return null;
    }
    return applications.entrySet()
                       .stream()
                       .filter(entry -> entry.getValue() != null
                                        && Objects.equals(name, String.valueOf(entry.getValue().getValue())))
                       .map(Map.Entry::getKey)
                       .findFirst()
                       .orElse(null);
  }

}
