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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.service.LinkService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Component
public class LinkSettingTranslationPlugin extends TranslationPlugin {

  public static final String    LINK_SETTINGS_OBJECT_TYPE = "link-settings";

  private static final Log      LOG                       = ExoLogger.getLogger(LinkSettingTranslationPlugin.class);

  @Autowired
  protected LinkService         linkService;

  @Autowired
  protected IdentityRegistry    identityRegistry;

  @Autowired
  protected OrganizationService organizationService;

  @Autowired
  protected TranslationService  translationService;

  @PostConstruct
  public void init() {
    translationService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return LINK_SETTINGS_OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(String linkSettingId, String username) throws ObjectNotFoundException {
    try {
      LinkSetting linkSetting = linkService.getLinkSettingById(Long.parseLong(linkSettingId));
      return linkSetting != null && linkService.hasAccessPermission(linkSetting.getName(), getIdentity(username));
    } catch (Exception e) {
      LOG.warn("Error checking access permission on link setting with id {} for user {}", linkSettingId, username, e);
      return false;
    }
  }

  @Override
  public boolean hasEditPermission(String linkSettingId, String username) throws ObjectNotFoundException {
    try {
      LinkSetting linkSetting = linkService.getLinkSettingById(Long.parseLong(linkSettingId));
      return linkSetting != null && linkService.hasEditPermission(linkSetting.getName(), getIdentity(username));
    } catch (Exception e) {
      LOG.warn("Error checking edit permission on link setting with id {} for user {}", linkSettingId, username, e);
      return false;
    }
  }

  @Override
  public long getAudienceId(String linkSettingId) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(String linkSettingId) throws ObjectNotFoundException {
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
