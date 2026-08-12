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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.content.utils.ContentUtils;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.portlet.CMSPortlet;

public class ContentListViewPortlet extends CMSPortlet {

  private static final String OBJECT_TYPE    = "contentListViewPortlet";

  private static final String APPLICATION_ID = "applicationId";

  private static final Random RANDOM         = new Random();

  private SettingService settingService;

  private TranslationService translationService;

  private CMSService     cmsService;

  @Override
  public void init(PortletConfig config) throws PortletException {
    super.init(config);
    this.contentType = OBJECT_TYPE;
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    PortletPreferences preferences = request.getPreferences();
    if (!canEditCurrentInstance(preferences)) {
      // The write is blocked and a server-side trace is left, but the HTTP
      // response still ends up 200: PortalRequestHandler.processRequest
      // catches any exception escaping processAction, logs it and commits
      // the response normally, so the frontend's resp.ok check can't see
      // this failure. Acceptable here because the settings drawer is hidden
      // without edit permission - only a forged request reaches this path,
      // and it gets no state change and no information either way.
      throw new PortletException("Current user is not authorized to edit this Content List instance");
    }
    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String name = parameterNames.nextElement();
      if (StringUtils.equals(name, "action") || StringUtils.contains(name, "portal:")) {
        continue;
      }
      String value = request.getParameter(name);
      preferences.setValue(name, value);
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
  @SneakyThrows
  protected void postSettingInit(PortletPreferences preferences, String name) {
    String data = preferences.getValue(DATA_INIT_PREFERENCE_NAME, null);
    String applicationId = getOrCreateApplicationId(preferences);
    if (StringUtils.isNotBlank(data)) {
      TranslationField translationField = JsonUtils.fromJsonString(data, TranslationField.class);
      if (translationField != null && MapUtils.isNotEmpty(translationField.getLabels())) {
        getTranslationService().saveTranslationLabels(translationField.getObjectType(),
                                                       applicationId,
                                                       translationField.getFieldName(),
                                                       translationField.getLabels(),
                                                       false);
      }
      savePreference(DATA_INIT_PREFERENCE_NAME, null);
    }
    initContentListHeaderTranslationSettings(applicationId, name);
  }

  private boolean canEditCurrentInstance(PortletPreferences preferences) {
    String name = preferences.getValue(NAME, null);
    if (StringUtils.isBlank(name)) {
      return false;
    }
    ConversationState conversationState = ConversationState.getCurrent();
    Identity currentIdentity = conversationState == null ? null : conversationState.getIdentity();
    return getCmsService().hasEditPermission(currentIdentity, contentType, name);
  }

  private String getOrCreateApplicationId(PortletPreferences preferences) {
    String applicationId = preferences.getValue(APPLICATION_ID, null);
    if (applicationId == null) {
      applicationId = String.valueOf(Math.abs(RANDOM.nextLong()));
      savePreference(APPLICATION_ID, applicationId);
    }
    return applicationId;
  }

  private void initContentListHeaderTranslationSettings(String applicationId, String settingName) {
    SettingValue<?> storedSettingNameValue = getSettingService().get(ContentUtils.CONTENT_LIST_VIEW_CONTEXT,
                                                                     ContentUtils.CONTENT_LIST_VIEW_SCOPE,
                                                                     applicationId);
    String storedSettingName = storedSettingNameValue != null ? storedSettingNameValue.getValue().toString() : null;
    if (storedSettingName == null || !storedSettingName.equals(settingName)) {
      getSettingService().set(ContentUtils.CONTENT_LIST_VIEW_CONTEXT,
                              ContentUtils.CONTENT_LIST_VIEW_SCOPE,
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

  private CMSService getCmsService() {
    if (cmsService == null) {
      cmsService = ExoContainerContext.getService(CMSService.class);
    }
    return cmsService;
  }

}
