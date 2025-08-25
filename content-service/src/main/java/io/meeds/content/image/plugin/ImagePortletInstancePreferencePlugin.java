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
package io.meeds.content.image.plugin;

import static io.meeds.content.image.plugin.ImageAttachmentPlugin.OBJECT_TYPE;

import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
@Profile("layout")
public class ImagePortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final String    CMS_SETTING_PREFERENCE_NAME = "name";

  private static final String    DATA_INIT_PREFERENCE_NAME   = "data.init";

  @Autowired
  private AttachmentService      attachmentService;

  @Autowired
  private CMSService             cmsService;

  @Autowired
  private FileService            fileService;

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
    return "Image";
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
    Long fileId = getImageFileId(settingName);
    if (fileId == null) {
      return Collections.emptyList();
    }
    FileItem file = fileService.getFile(fileId);
    if (file == null) {
      return Collections.emptyList();
    } else {
      String imageContent = Base64.encodeBase64String(file.getAsByte());
      return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME, imageContent));
    }
  }

  private Long getImageFileId(String settingName) {
    CMSSetting setting = cmsService.getSetting(OBJECT_TYPE, settingName);
    List<String> fileIds = attachmentService.getAttachmentFileIds(OBJECT_TYPE, setting.getName());
    if (CollectionUtils.isEmpty(fileIds)) {
      return null;
    } else {
      return Long.parseLong(fileIds.getFirst());
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
