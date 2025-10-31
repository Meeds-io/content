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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import io.meeds.content.AbstractSpringConfigurationTest;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

public class LinkSettingTranslationPluginTest extends AbstractSpringConfigurationTest { // NOSONAR

  private static final String FIELD_NAME           = "header";

  private static final String USERS_GROUP          = "*:/platform/users";

  private static final String ADMINISTRATORS_GROUP = "*:/platform/administrators";

  private static final String USERNAME             = "testuser";

  private static final String LINK_SETTING_NAME    = "linkSettingName";

  private LayoutService       layoutService;

  private TranslationService  translationService;

  private IdentityRegistry    identityRegistry;

  @Before
  @Override
  public void setUp() {
    super.setUp();
    layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    translationService = getContainer().getComponentInstanceOfType(TranslationService.class);
  }

  @Test
  @SneakyThrows
  public void testLinkSettingHeaderTranslation() {
    String pageId = createPage("testLinkSettingHeaderTranslation1", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageId, 0l);

    LinkSetting linkSetting = linkService.getLinkSettingByName(LINK_SETTING_NAME);
    assertNotNull(linkSetting);
    assertTrue(linkSetting.getId() > 0);

    TranslationField translationField =
                                      translationService.getTranslationField(LinkSettingTranslationPlugin.LINK_SETTINGS_OBJECT_TYPE,
                                                                             linkSetting.getId(),
                                                                             FIELD_NAME,
                                                                             null);
    assertNotNull(translationField);
    assertNotNull(translationField.getLabels());
    assertTrue(translationField.getLabels().isEmpty());

    pageId = createPage("testLinkSettingHeaderTranslation1", USERS_GROUP, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageId, 0l);
    assertThrows(IllegalAccessException.class,
                 () -> translationService.getTranslationField(LinkSettingTranslationPlugin.LINK_SETTINGS_OBJECT_TYPE,
                                                              linkSetting.getId(),
                                                              FIELD_NAME,
                                                              null));

    registerInternalUser(USERNAME);
    translationField = translationService.getTranslationField(LinkSettingTranslationPlugin.LINK_SETTINGS_OBJECT_TYPE,
                                                              linkSetting.getId(),
                                                              FIELD_NAME,
                                                              USERNAME);
    assertNotNull(translationField);
    assertNotNull(translationField.getLabels());
    assertTrue(translationField.getLabels().isEmpty());

    String translationValue = "value1";
    assertThrows(IllegalAccessException.class,
                 () -> translationService.saveTranslationLabels(LinkSettingTranslationPlugin.LINK_SETTINGS_OBJECT_TYPE,
                                                                linkSetting.getId(),
                                                                FIELD_NAME,
                                                                Collections.singletonMap(Locale.ENGLISH, translationValue),
                                                                USERNAME,
                                                                false));

    registerAdministratorUser(USERNAME);
    translationService.saveTranslationLabels(LinkSettingTranslationPlugin.LINK_SETTINGS_OBJECT_TYPE,
                                             linkSetting.getId(),
                                             FIELD_NAME,
                                             Collections.singletonMap(Locale.ENGLISH, translationValue),
                                             USERNAME,
                                             false);

    registerInternalUser(USERNAME);
    translationField = translationService.getTranslationField(LinkSettingTranslationPlugin.LINK_SETTINGS_OBJECT_TYPE,
                                                              linkSetting.getId(),
                                                              FIELD_NAME,
                                                              USERNAME);
    assertNotNull(translationField);
    assertNotNull(translationField.getLabels());
    assertFalse(translationField.getLabels().isEmpty());
    assertEquals(translationValue, translationField.getLabels().get(Locale.ENGLISH));
  }

  private String createPage(String pageName, String accessPermission, String editPermission) {
    String siteType = "portal";
    String siteName = "test";
    if (layoutService.getPortalConfig(siteName) == null) {
      PortalConfig portal = new PortalConfig();
      portal.setType(siteType);
      portal.setName(siteName);
      portal.setLocale("en");
      portal.setLabel("Test");
      portal.setDescription("Test");
      portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
      layoutService.create(portal);
    }

    PageKey pageKey = new PageKey(siteType, siteName, pageName);
    PageState pageState = new PageState(pageName,
                                        null,
                                        false,
                                        null,
                                        Collections.singletonList(accessPermission),
                                        editPermission);
    layoutService.save(new PageContext(pageKey, pageState));
    return pageKey.format();
  }

  private org.exoplatform.services.security.Identity registerAdministratorUser(String user) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(user,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/administrators")));
    identityRegistry.register(identity);
    return identity;
  }

  private org.exoplatform.services.security.Identity registerInternalUser(String username) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(username,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/users")));
    identityRegistry.register(identity);
    return identity;
  }

}
