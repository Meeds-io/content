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
package io.meeds.content.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.content.utils.ContentUtils;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Profile("layout")
public class ContentListPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final Log       LOG                            = ExoLogger.getLogger(ContentListPortletInstancePreferencePlugin.class);

  private static final String    APPLICATION_ID_PREFERENCE_NAME = "applicationId";

  private static final String    DATA_INIT_PREFERENCE_NAME   = "data.init";

  private static final String    CMS_SETTING_PREFERENCE_NAME = "name";

  private static final String    CATEGORY_IDS_PREFERENCE_NAME = "categoryIds";

  private static final String    EXCLUDE_CATEGORY_IDS_PREFERENCE_NAME = "excludeCategoryIds";

  @Autowired
  private TranslationService    translationService;

  @Autowired(required = false)
  private PortletInstanceService portletInstanceService;

  @PostConstruct
  public void init() {
    if (portletInstanceService == null) {
      portletInstanceService = ExoContainerContext.getService(PortletInstanceService.class);
    }
    if (portletInstanceService == null) {
      LOG.warn("PortletInstanceService is not available - Content List instances won't carry their header title over when created from a saved instance");
      return;
    }
    portletInstanceService.addPortletInstancePreferencePlugin(this);
  }

  @Override
  public String getPortletName() {
    return "ContentListView";
  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    List<PortletInstancePreference> result = copyPreferences(preferences, portletInstanceContext);
    String applicationId = getApplicationId(preferences);
    if (StringUtils.isBlank(applicationId)) {
      if (preferences != null && preferences.getPreference(DATA_INIT_PREFERENCE_NAME) != null) {
        result.add(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME,
                                                 preferences.getPreference(DATA_INIT_PREFERENCE_NAME).getValue()));
      }
      return result;
    }
    TranslationField translationField = translationService.getTranslationField(ContentUtils.CONTENT_LIST_VIEW_TYPE,
                                                                               applicationId,
                                                                               ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD);
    if (MapUtils.isNotEmpty(translationField.getLabels())) {
      result.add(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME, JsonUtils.toJsonString(translationField)));
    }
    return result;
  }

  /**
   * Copies the display settings of the instance, but not the preferences that
   * identify it: the CMS setting name, the application id keying its header
   * title translation, and the header title payload, regenerated by the caller.
   * An export also leaves out the category ids, which only exist on the exporting
   * platform
   */
  private List<PortletInstancePreference> copyPreferences(Portlet preferences,
                                                         PortletInstanceContext portletInstanceContext) {
    boolean export = portletInstanceContext != null && portletInstanceContext.isExport();
    List<PortletInstancePreference> result = new ArrayList<>();
    if (preferences != null) {
      StreamSupport.stream(preferences.spliterator(), false)
                   .filter(preference -> !StringUtils.equalsAny(preference.getName(),
                                                                CMS_SETTING_PREFERENCE_NAME,
                                                                APPLICATION_ID_PREFERENCE_NAME,
                                                                DATA_INIT_PREFERENCE_NAME))
                   .filter(preference -> !export || !StringUtils.equalsAny(preference.getName(),
                                                                           CATEGORY_IDS_PREFERENCE_NAME,
                                                                           EXCLUDE_CATEGORY_IDS_PREFERENCE_NAME))
                   .forEach(preference -> result.add(new PortletInstancePreference(preference.getName(),
                                                                                   preference.getValue())));
    }
    return result;
  }

  private String getApplicationId(Portlet preferences) {
    if (preferences == null) {
      return null;
    }
    Preference applicationIdPreference = preferences.getPreference(APPLICATION_ID_PREFERENCE_NAME);
    return applicationIdPreference == null ? null : applicationIdPreference.getValue();
  }

}
