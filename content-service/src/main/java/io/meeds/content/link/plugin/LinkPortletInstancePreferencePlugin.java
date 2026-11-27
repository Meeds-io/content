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
package io.meeds.content.link.plugin;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.content.link.model.LinkData;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.service.LinkService;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Profile("layout")
public class LinkPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final String    CMS_SETTING_PREFERENCE_NAME = "name";

  private static final String    DATA_INIT_PREFERENCE_NAME   = "data.init";

  @Autowired
  private LinkService            linkService;

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
    return "Links";
  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    String settingName = getCmsSettingName(preferences);
    boolean draft = isDraft(portletInstanceContext);
    if (StringUtils.isBlank(settingName)) {
      if (preferences != null && preferences.getPreference(DATA_INIT_PREFERENCE_NAME) != null) {
        String data = preferences.getPreference(DATA_INIT_PREFERENCE_NAME).getValue();
        LinkData linkData = parse(data);
        return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME,
                                                                       linkData == null ? data
                                                                                        : JsonUtils.toJsonString(toExport(linkData,
                                                                                                                          draft ? linkData.getSourceName()
                                                                                                                                : null))));
      } else {
        return Collections.emptyList();
      }
    }
    LinkData linkData = linkService.getLinkDataByName(settingName);
    return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME,
                                                                   JsonUtils.toJsonString(toExport(linkData,
                                                                                                   draft ? settingName : null))));
  }

  /**
   * Only a site draft's window replaces the window it is exported from, once
   * published: its data names that source, so that the users who hid it keep
   * it hidden. A duplicated section, or a page, site or space made from a
   * template, sits next to its source or elsewhere, and starts shown to all.
   */
  private boolean isDraft(PortletInstanceContext portletInstanceContext) {
    return portletInstanceContext != null
           && portletInstanceContext.getProperties() != null
           && Boolean.parseBoolean(portletInstanceContext.getProperties().get(PortletInstanceContext.DRAFT_PROPERTY));
  }

  /**
   * The exported data never names a list in its setting: only the
   * {@link LinkData#getSourceName()} a site draft's export sets does
   */
  private LinkData toExport(LinkData linkData, String sourceName) {
    LinkSetting linkSetting = linkData.getLinkSetting() == null ? null : linkData.getLinkSetting().clone();
    if (linkSetting != null) {
      linkSetting.setName(null);
    }
    return new LinkData(linkSetting, linkData.getLinks(), sourceName);
  }

  private LinkData parse(String data) {
    try {
      return StringUtils.isBlank(data) ? null : JsonUtils.fromJsonString(data, LinkData.class);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private String getCmsSettingName(Portlet preferences) {
    if (preferences == null) {
      return null;
    }
    Preference settingNamePreference = preferences.getPreference(CMS_SETTING_PREFERENCE_NAME);
    return settingNamePreference == null ? null : settingNamePreference.getValue();
  }

}
