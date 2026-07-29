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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;

import io.meeds.content.utils.ContentUtils;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Component
public class ContentListViewTranslationPlugin extends TranslationPlugin {

  public static final String   CONTENT_LIST_VIEW_OBJECT_TYPE = "contentListView";

  @Autowired
  private TranslationService translationService;

  @Autowired
  private CMSService          cmsService;

  @Autowired
  private SettingService     settingService;

  @Autowired
  private UserACL             userAcl;

  @PostConstruct
  public void init() {
    translationService.addPlugin(this);
  }

  @Override
  public String getName() {
    return CONTENT_LIST_VIEW_OBJECT_TYPE;
  }

  @Override
  public String getObjectType() {
    return CONTENT_LIST_VIEW_OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(String objectId, String username) {
    return true;
  }

  @Override
  public boolean hasEditPermission(String objectId, String username) {
    try {
      SettingValue<?> settingNameValue = settingService.get(ContentUtils.CONTENT_LIST_VIEW_CONTEXT,
                                                            ContentUtils.CONTENT_LIST_VIEW_SCOPE,
                                                            objectId);
      String settingName = settingNameValue != null ? settingNameValue.getValue().toString() : null;
      Identity userAclIdentity = userAcl.getUserIdentity(username);
      return userAclIdentity != null
             && (cmsService.hasEditPermission(userAclIdentity, "contentListViewPortlet", settingName));
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public long getAudienceId(String objectId) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(String objectId) throws ObjectNotFoundException {
    return 0;
  }

}
