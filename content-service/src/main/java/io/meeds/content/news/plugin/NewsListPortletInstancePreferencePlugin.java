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
package io.meeds.content.news.plugin;

import java.util.Collections;
import java.util.List;

import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Profile("layout")
public class NewsListPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final String    APPLICATION_ID_PREFERENCE_NAME = "applicationId";

  private static final String    DATA_INIT_PREFERENCE_NAME   = "data.init";

  @Autowired
  private TranslationService    translationService;

  @Autowired(required = false)
  private PortletInstanceService portletInstanceService;

  @PostConstruct
  public void init() {
    if (portletInstanceService == null) {
      portletInstanceService = ExoContainerContext.getService(PortletInstanceService.class);
    }
    portletInstanceService.addPortletInstancePreferencePlugin(this);
  }

  @Override
  public String getPortletName() {
    return "NewsListView";
  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    String settingName = getCmsSettingName(preferences);
    if (StringUtils.isBlank(settingName)) {
      if (preferences != null && preferences.getPreference(DATA_INIT_PREFERENCE_NAME) != null) {
        return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME,
                                                                       preferences.getPreference(DATA_INIT_PREFERENCE_NAME)
                                                                                  .getValue()));
      } else {
        return Collections.emptyList();
      }
    }
    TranslationField translationField = translationService.getTranslationField("newsListView", settingName, "headerNameInput");
    if (translationField != null) {
      return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME,
                                                                     JsonUtils.toJsonString(translationField)));
    } else {
      return Collections.emptyList();
    }
  }

  private String getCmsSettingName(Portlet preferences) {
    if (preferences == null) {
      return null;
    }
    Preference settingNamePreference = preferences.getPreference(APPLICATION_ID_PREFERENCE_NAME);
    return settingNamePreference == null ? null : settingNamePreference.getValue();
  }

}
