/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import io.meeds.content.AbstractSpringConfigurationTest;
import io.meeds.content.model.Link;
import io.meeds.content.model.LinkSetting;
import io.meeds.kernel.test.AbstractSpringTest;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

public class LinkTranslationPluginTest extends AbstractSpringConfigurationTest { // NOSONAR

  private static final String FIELD_NAME           = "name";

  private static final String USERS_GROUP          = "*:/platform/users";

  private static final String ADMINISTRATORS_GROUP = "*:/platform/administrators";

  private static final String USERNAME             = "testuser";

  private static final String LINK_SETTING_NAME    = "linkSettingName";

  private LayoutService       layoutService;

  private TranslationService  translationService;

  private IdentityRegistry    identityRegistry;

  public LinkTranslationPluginTest() {
    AbstractSpringTest.setTestClass(this.getClass());
  }

  @Before
  public void setUp() {
    super.setUp();
    layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    translationService = getContainer().getComponentInstanceOfType(TranslationService.class);
  }

  @Test
  public void testLinkSettingHeaderTranslation() throws ObjectNotFoundException, IllegalAccessException {
    String pageId = createPage("testLinkSettingHeaderTranslation1", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageId, 0l);

    LinkSetting linkSetting = linkService.getLinkSetting(LINK_SETTING_NAME);
    assertNotNull(linkSetting);
    Link linkToSave = new Link(0,
                               Collections.singletonMap("en", "Website"),
                               Collections.singletonMap("en", "Website description"),
                               "https://localhost/",
                               "icon",
                               true,
                               5,
                               0);
    linkService.saveLinkSetting(linkSetting, Collections.singletonList(linkToSave), registerAdministratorUser(USERNAME));
    List<Link> links = linkService.getLinks(LINK_SETTING_NAME);
    assertNotNull(links);
    assertEquals(1, links.size());
    Link link = links.get(0);

    pageId = createPage("testLinkSettingHeaderTranslation1", USERS_GROUP, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageId, 0l);
    assertThrows(IllegalAccessException.class,
                 () -> translationService.getTranslationField(LinkTranslationPlugin.LINKS_OBJECT_TYPE,
                                                              link.getId(),
                                                              FIELD_NAME,
                                                              null));

    registerInternalUser(USERNAME);
    TranslationField nameTranslationField = translationService.getTranslationField(LinkTranslationPlugin.LINKS_OBJECT_TYPE,
                                                                                   link.getId(),
                                                                                   FIELD_NAME,
                                                                                   USERNAME);
    TranslationField descriptionTranslationField = translationService.getTranslationField(LinkTranslationPlugin.LINKS_OBJECT_TYPE,
                                                                                          link.getId(),
                                                                                          "description",
                                                                                          USERNAME);
    assertNotNull(nameTranslationField);
    assertNotNull(nameTranslationField.getLabels());
    assertFalse(nameTranslationField.getLabels().isEmpty());
    assertEquals(linkToSave.getName().get(Locale.ENGLISH.toLanguageTag()), nameTranslationField.getLabels().get(Locale.ENGLISH));
    assertEquals(linkToSave.getDescription().get(Locale.ENGLISH.toLanguageTag()),
                 descriptionTranslationField.getLabels().get(Locale.ENGLISH));
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
