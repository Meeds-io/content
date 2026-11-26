/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Random;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;
import lombok.SneakyThrows;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import io.meeds.content.news.plugin.NewsListHiddenApplicationPlugin;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.social.hidden.service.HiddenApplicationService;
import io.meeds.social.portlet.CMSPortlet;

public class NewsListViewPortlet extends CMSPortlet {

  private static final Log    LOG            = ExoLogger.getLogger(NewsListViewPortlet.class);

  private static final String OBJECT_TYPE    = "newsListViewPortlet";

  private static final String APPLICATION_ID = "applicationId";

  private static final String ALLOW_HIDE_PARAMETER = "allowHide";

  private SettingService settingService;

  private TranslationService translationService;

  private HiddenApplicationService hiddenApplicationService;

  private NewsListHiddenApplicationPlugin newsListHiddenApplicationPlugin;

  @Override
  public void init(PortletConfig config) throws PortletException {
    super.init(config);
    this.contentType = OBJECT_TYPE;
  }

  @Override
  @SneakyThrows
  protected void postSettingInit(PortletPreferences preferences, String name) {
    String data = preferences.getValue(DATA_INIT_PREFERENCE_NAME, null);
    String applicationId = getOrCreateApplicationId(preferences);
    if (StringUtils.isNotBlank(data)) {
      TranslationField translationField = JsonUtils.fromJsonString(data, TranslationField.class);
      if (translationField != null && MapUtils.isNotEmpty(translationField.getLabels())) {
        getTranslationService().saveTranslationLabels(translationField.getObjectType(), applicationId, translationField.getFieldName(), translationField.getLabels(), false);
      }
      savePreference(DATA_INIT_PREFERENCE_NAME, null);
    }
    initNewsListHeaderTranslationSettings(applicationId, name);
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    PortletPreferences preferences = request.getPreferences();
    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String name = parameterNames.nextElement();
      if (StringUtils.equals(name, "action") || StringUtils.contains(name, "portal:")) {
        continue;
      }
      String value = request.getParameter(name);
      if (StringUtils.equals(name, ALLOW_HIDE_PARAMETER)) {
        saveHideAllowed(preferences.getValue(NAME, null), Boolean.parseBoolean(value));
      } else {
        preferences.setValue(name, value);
      }
    }
    preferences.store();
  }

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
    String applicationId = getOrCreateApplicationId(request.getPreferences());
    request.getPreferences().setValue(APPLICATION_ID, applicationId);
    super.doView(request, response);
  }

  @Override
  protected void setViewRequestAttributes(String name, RenderRequest request, RenderResponse response) {
    boolean allowHide = getHiddenApplicationService().isHideAllowed(NewsListHiddenApplicationPlugin.APPLICATION_TYPE, name);
    request.setAttribute(ALLOW_HIDE_PARAMETER, allowHide);
    request.setAttribute("hidden",
                         allowHide && getHiddenApplicationService().isHidden(NewsListHiddenApplicationPlugin.APPLICATION_TYPE,
                                                                             name,
                                                                             request.getRemoteUser()));
  }

  private void saveHideAllowed(String name, boolean allowed) {
    ConversationState conversationState = ConversationState.getCurrent();
    try {
      getNewsListHiddenApplicationPlugin().saveHideAllowed(name,
                                                           allowed,
                                                           conversationState == null ? null : conversationState.getIdentity());
    } catch (IllegalAccessException | IllegalArgumentException e) {
      // The option is kept for the window, not for the page: only its editors
      // change it, whoever else may save the other display options
      LOG.debug("News list hiding option not saved", e);
    }
  }

  private String getOrCreateApplicationId(PortletPreferences preferences) {
    String applicationId = preferences.getValue(APPLICATION_ID, null);
    if (applicationId == null) {
      Random random = new Random();
      applicationId = String.valueOf(Math.abs(random.nextLong()));
      savePreference(APPLICATION_ID, applicationId);
    }
    return applicationId;
  }

  private void initNewsListHeaderTranslationSettings(String applicationId, String settingName) {
    SettingValue<?> storedSettingNameValue = getSettingService().get(NewsUtils.NEWS_LIST_VIEW_CONTEXT,
                                                                     NewsUtils.NEWS_LIST_VIEW_SCOPE,
                                                                     applicationId);
    String storedSettingName = storedSettingNameValue != null ? storedSettingNameValue.getValue().toString() : null;
    if (storedSettingName == null || !storedSettingName.equals(settingName)) {
      getSettingService().set(NewsUtils.NEWS_LIST_VIEW_CONTEXT,
                              NewsUtils.NEWS_LIST_VIEW_SCOPE,
                              applicationId,
                              SettingValue.create(settingName));
    }
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = ExoContainerContext.getService(SettingService.class);
    }
    return settingService;
  }

  private HiddenApplicationService getHiddenApplicationService() {
    if (hiddenApplicationService == null) {
      hiddenApplicationService = ExoContainerContext.getService(HiddenApplicationService.class);
    }
    return hiddenApplicationService;
  }

  private NewsListHiddenApplicationPlugin getNewsListHiddenApplicationPlugin() {
    if (newsListHiddenApplicationPlugin == null) {
      newsListHiddenApplicationPlugin = ExoContainerContext.getService(NewsListHiddenApplicationPlugin.class);
    }
    return newsListHiddenApplicationPlugin;
  }

  private TranslationService getTranslationService() {
    if (translationService == null) {
      translationService = ExoContainerContext.getService(TranslationService.class);
    }
    return translationService;
  }
}
