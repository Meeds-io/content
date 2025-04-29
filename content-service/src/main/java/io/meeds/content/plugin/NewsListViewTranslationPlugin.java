/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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

import io.meeds.content.service.NewsService;
import io.meeds.content.utils.NewsUtils;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewsListViewTranslationPlugin extends TranslationPlugin {

  public static final String        NEWS_LIST_VIEW_OBJECT_TYPE      = "newsListView";
  

  @Setter
  private IdentityRegistry    identityRegistry;

  @Autowired
  private OrganizationService organizationService;

  @Autowired
  private TranslationService  translationService;

  @Autowired
  private CMSService          cmsService;

  @Autowired
  private SettingService      settingService;

  @Autowired
  private NewsService         newsService;

  @PostConstruct
  public void init() {
    setIdentityRegistry(ExoContainerContext.getService(IdentityRegistry.class));
    translationService.addPlugin(this);
  }

  @Override
  public String getName() {
    return NEWS_LIST_VIEW_OBJECT_TYPE;
  }

  @Override
  public String getObjectType() {
    return NEWS_LIST_VIEW_OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(long objectId, String username) {
    return true;
  }

  @Override
  public boolean hasEditPermission(long objectId, String username) {
    try {
      SettingValue<?> settingNameValue = settingService.get(NewsUtils.NEWS_LIST_VIEW_CONTEXT,
                                                            NewsUtils.NEWS_LIST_VIEW_SCOPE,
                                                            String.valueOf(objectId));
      String settingName = settingNameValue != null ? settingNameValue.getValue().toString() : null;
      return getIdentity(username) != null
          && (cmsService.hasEditPermission(getIdentity(username), "newsListViewPortlet", settingName));
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public long getAudienceId(long objectId) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(long objectId) throws ObjectNotFoundException {
    return 0;
  }

  private Identity getIdentity(String username) throws Exception {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    Identity aclIdentity = identityRegistry.getIdentity(username);
    if (aclIdentity == null) {
      List<MembershipEntry> entries = organizationService.getMembershipHandler()
                                                         .findMembershipsByUser(username)
                                                         .stream()
                                                         .map(membership -> new MembershipEntry(membership.getGroupId(),
                                                                                                membership.getMembershipType()))
                                                         .toList();
      aclIdentity = new Identity(username, entries);
      identityRegistry.register(aclIdentity);
    }
    return aclIdentity;
  }
}
