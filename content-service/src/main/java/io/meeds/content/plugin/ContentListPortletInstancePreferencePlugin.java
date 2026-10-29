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

import java.util.List;

import io.meeds.social.translation.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.content.utils.ContentUtils;
import io.meeds.content.utils.ListViewPreferenceUtils;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;

import jakarta.annotation.PostConstruct;

@Component
@Profile("layout")
public class ContentListPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final Log       LOG                            = ExoLogger.getLogger(ContentListPortletInstancePreferencePlugin.class);

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
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    return ListViewPreferenceUtils.generatePreferences(preferences,
                                                       portletInstanceContext,
                                                       translationService,
                                                       ContentUtils.CONTENT_LIST_VIEW_TYPE,
                                                       ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD,
                                                       CATEGORY_IDS_PREFERENCE_NAME,
                                                       EXCLUDE_CATEGORY_IDS_PREFERENCE_NAME);
  }

}
