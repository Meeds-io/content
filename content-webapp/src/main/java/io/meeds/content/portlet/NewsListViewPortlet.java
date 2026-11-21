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
import java.util.List;
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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;

import io.meeds.content.news.utils.NewsUtils;
import io.meeds.social.portlet.CMSPortlet;

public class NewsListViewPortlet extends CMSPortlet {

  private static final String OBJECT_TYPE    = "newsListViewPortlet";

  private static final String APPLICATION_ID = "applicationId";

  // the settings the News List settings drawer saves
  private static final List<String> SETTING_NAMES = List.of("viewTemplate",
                                                            "newsTarget",
                                                            "selectedArticleIds",
                                                            "articlesSourceOption",
                                                            "showHeader",
                                                            "showSeeAll",
                                                            "showArticleTitle",
                                                            "showArticleSummary",
                                                            "showArticleImage",
                                                            "showArticleAuthor",
                                                            "showArticleSpace",
                                                            "showArticleReactions",
                                                            "showArticleDate",
                                                            "seeAllUrl",
                                                            "limit");

  private SettingService settingService;

  private TranslationService translationService;

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
    if (!canEditSettings(preferences)) {
      // Refused like the Content List save; the view shows the settings drawer
      // only to those this check accepts
      throw new PortletException("Current user is not authorized to edit this News List instance");
    }
    for (String settingName : SETTING_NAMES) {
      String value = request.getParameter(settingName);
      if (value != null) {
        preferences.setValue(settingName, value);
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

  // the editors of the instance, as the view's canEdit, and the publishers of
  // the page's space, as the view's canPublishNews
  private boolean canEditSettings(PortletPreferences preferences) {
    String name = preferences.getValue(NAME, null);
    if (StringUtils.isNotBlank(name) && canEdit(name, null)) {
      return true;
    }
    ConversationState conversationState = ConversationState.getCurrent();
    Identity currentIdentity = conversationState == null ? null : conversationState.getIdentity();
    Space space = SpaceUtils.getSpaceByContext();
    return space != null && NewsUtils.canPublishNews(space.getId(), currentIdentity);
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

  private TranslationService getTranslationService() {
    if (translationService == null) {
      translationService = ExoContainerContext.getService(TranslationService.class);
    }
    return translationService;
  }
}
