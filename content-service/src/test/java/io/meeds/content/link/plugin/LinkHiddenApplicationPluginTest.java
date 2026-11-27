/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.service.LinkService;
import io.meeds.social.hidden.model.HiddenApplication;

@RunWith(MockitoJUnitRunner.class)
public class LinkHiddenApplicationPluginTest {

  private static final String         NAME           = "Untitled-1-abc";

  private static final String         PAGE_REFERENCE = "portal::dw::overview";

  @Mock
  private LinkService                 linkService;

  @Mock
  private ResourceBundleService       resourceBundleService;

  @Mock
  private LayoutService               layoutService;

  @InjectMocks
  private LinkHiddenApplicationPlugin plugin;

  @Test
  public void getApplicationOfAnUnknownWindowIsNull() {
    assertNull(plugin.getApplication(NAME, "fr"));
  }

  @Test
  public void getApplicationTitleIsTheHeaderInTheRequestedLanguage() {
    mockLinkSetting(Map.of("en", "Getting started", "fr", "Premiers pas"), true, "fr");
    mockBundle(Locale.FRENCH, "links.label.defaultListName", "Liste");

    HiddenApplication application = plugin.getApplication(NAME, "fr");

    assertEquals(LinkService.LINK_APPLICATION_TYPE, application.getType());
    assertEquals(NAME, application.getName());
    assertEquals("Premiers pas", application.getTitle());
    assertEquals(PAGE_REFERENCE, application.getPageReference());
    assertTrue(application.isAllowHide());
  }

  @Test
  public void getApplicationTitleFallsBackToTheEnglishHeader() {
    mockLinkSetting(Map.of("en", "Getting started"), false, "de");
    mockBundle(Locale.GERMAN, "links.label.defaultListName", "Liste");

    HiddenApplication application = plugin.getApplication(NAME, "de");

    assertEquals("Getting started", application.getTitle());
    assertFalse(application.isAllowHide());
  }

  @Test
  public void getApplicationTitleTranslatesAHeaderGivenAsALabelKey() {
    mockLinkSetting(Map.of("en", "links.label.gettingStarted"), true, "fr");
    mockBundle(Locale.FRENCH, "links.label.gettingStarted", "Premiers pas");

    assertEquals("Premiers pas", plugin.getApplication(NAME, "fr").getTitle());
  }

  @Test
  public void getApplicationTitleWithoutHeaderIsTheDefaultListName() {
    mockLinkSetting(Map.of(), true, "fr");
    mockBundle(Locale.FRENCH, "links.label.defaultListName", "Liste");

    assertEquals("Liste", plugin.getApplication(NAME, "fr").getTitle());
  }

  @Test
  public void canViewIsTheAccessPermissionOfTheApplication() {
    Identity identity = new Identity("user");
    when(linkService.hasAccessPermission(NAME, identity)).thenReturn(true);
    assertTrue(plugin.canView(NAME, identity));
    assertFalse(plugin.canView(NAME, new Identity("other")));
  }

  private void mockLinkSetting(Map<String, String> header, boolean allowHide, String language) {
    LinkSetting linkSetting = new LinkSetting();
    linkSetting.setName(NAME);
    linkSetting.setPageReference(PAGE_REFERENCE);
    linkSetting.setHeader(header);
    linkSetting.setAllowHide(allowHide);
    when(linkService.getLinkSettingByName(NAME, language, true)).thenReturn(linkSetting);
  }

  private void mockBundle(Locale locale, String key, String value) {
    when(resourceBundleService.getResourceBundle(eq(LinkHiddenApplicationPlugin.LINKS_BUNDLE_NAME),
                                                 any(Locale.class))).thenAnswer(invocation -> locale.equals(invocation.getArgument(1)) ? new ListResourceBundle() {
                                                   @Override
                                                   protected Object[][] getContents() {
                                                     return new Object[][] { { key, value } };
                                                   }
                                                 } : null);
  }

}
