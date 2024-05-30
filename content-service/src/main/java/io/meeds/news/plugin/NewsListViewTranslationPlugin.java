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
package io.meeds.news.plugin;

import io.meeds.social.translation.plugin.TranslationPlugin;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import java.util.List;
import java.util.Objects;

public class NewsListViewTranslationPlugin extends TranslationPlugin {

  public static final String        NEWS_LIST_VIEW_OBJECT_TYPE      = "newsListView";

  private static final String       PUBLISHER_MEMBERSHIP_NAME       = "publisher";

  private static final String       PLATFORM_WEB_CONTRIBUTORS_GROUP = "/platform/web-contributors";

  private final IdentityRegistry    identityRegistry;

  private final OrganizationService organizationService;

  public NewsListViewTranslationPlugin(IdentityRegistry identityRegistry, OrganizationService organizationService) {
    this.identityRegistry = identityRegistry;
    this.organizationService = organizationService;
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
      return getIdentity(username) != null
          && Objects.requireNonNull(getIdentity(username)).isMemberOf(PLATFORM_WEB_CONTRIBUTORS_GROUP, PUBLISHER_MEMBERSHIP_NAME);
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
