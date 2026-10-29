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

import java.util.List;

import io.meeds.social.translation.service.TranslationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.content.utils.ListViewPreferenceUtils;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;

import jakarta.annotation.PostConstruct;

@Component
@Profile("layout")
public class NewsListPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final String    SELECTED_ARTICLE_IDS_PREFERENCE_NAME = "selectedArticleIds";

  private static final String    ARTICLES_SOURCE_PREFERENCE_NAME      = "articlesSourceOption";

  private static final String    SELECTED_LIST_ARTICLES_SOURCE        = "selectedList";

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
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    return ListViewPreferenceUtils.generatePreferences(preferences,
                                                       portletInstanceContext,
                                                       translationService,
                                                       NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                       NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD,
                                                       NewsListPortletInstancePreferencePlugin::isExportExcluded);
  }

  /**
   * An export leaves out the selected article ids, which only exist on the
   * exporting platform, and the selected articles source that cannot work
   * without them, so the imported list shows the posted articles
   */
  private static boolean isExportExcluded(Preference preference) {
    return StringUtils.equals(preference.getName(), SELECTED_ARTICLE_IDS_PREFERENCE_NAME)
           || (StringUtils.equals(preference.getName(), ARTICLES_SOURCE_PREFERENCE_NAME)
               && StringUtils.equals(preference.getValue(), SELECTED_LIST_ARTICLES_SOURCE));
  }

}
