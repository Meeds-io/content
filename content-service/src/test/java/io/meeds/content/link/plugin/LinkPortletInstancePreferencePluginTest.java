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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.content.link.model.LinkData;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.service.LinkService;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.social.util.JsonUtils;

/**
 * A Links window's exported data names the list it comes from only for a site
 * draft, whose window replaces its source once published: the users who hid
 * the source keep it hidden there, and only there.
 */
@ExtendWith(MockitoExtension.class)
class LinkPortletInstancePreferencePluginTest {

  private static final String                 SOURCE_NAME = "Untitled-3-source";

  @Mock
  private LinkService                         linkService;

  @Mock
  private PortletInstanceService              portletInstanceService;

  @InjectMocks
  private LinkPortletInstancePreferencePlugin plugin;

  private LinkSetting                         sourceSetting;

  @Test
  void aSiteDraftExportNamesTheSourceListInItsMarkerOnly() throws Exception {
    mockSourceData();

    LinkData exported = export(namedWindow(), draft(true));

    assertEquals(SOURCE_NAME, exported.getSourceName());
    assertNull(exported.getLinkSetting().getName(), "The setting itself never names a list");
    assertEquals(SOURCE_NAME, sourceSetting.getName(), "The source list itself keeps its name");
  }

  @Test
  void anyOtherExportNamesNoSourceList() throws Exception {
    mockSourceData();

    LinkData duplicated = export(namedWindow(), draft(false));
    LinkData fromTemplate = export(namedWindow(), new PortletInstanceContext(true, null));
    LinkData withoutContext = export(namedWindow(), null);

    for (LinkData exported : List.of(duplicated, fromTemplate, withoutContext)) {
      assertNull(exported.getSourceName(), "A duplicated section's or a template's list starts shown to all");
      assertNull(exported.getLinkSetting().getName());
    }
    assertEquals(SOURCE_NAME, sourceSetting.getName(), "The source list itself keeps its name");
  }

  @Test
  void aWindowStillCarryingItsDataKeepsItsMarkerForASiteDraftOnly() {
    String data = JsonUtils.toJsonString(new LinkData(namedSetting(), Collections.emptyList(), SOURCE_NAME));
    Portlet preferences = new Portlet().putPreference(new Preference("data.init", data, false));

    assertEquals(SOURCE_NAME, export(preferences, draft(true)).getSourceName());
    assertNull(export(preferences, draft(false)).getSourceName());
    assertNull(export(preferences, draft(true)).getLinkSetting().getName());
  }

  @Test
  void dataStoredBeforeTheMarkerNamesNoSourceEvenInASiteDraft() {
    // The shape a template saved before the marker existed stores: the name in the setting only
    String data = JsonUtils.toJsonString(new LinkData(namedSetting(), Collections.emptyList()));
    Portlet preferences = new Portlet().putPreference(new Preference("data.init", data, false));

    LinkData exported = export(preferences, draft(true));

    assertNull(exported.getSourceName());
    assertNull(exported.getLinkSetting().getName());
  }

  private Portlet namedWindow() {
    return new Portlet().putPreference(new Preference("name", SOURCE_NAME, false));
  }

  private void mockSourceData() throws Exception {
    sourceSetting = namedSetting();
    when(linkService.getLinkDataByName(SOURCE_NAME)).thenReturn(new LinkData(sourceSetting, Collections.emptyList()));
  }

  private LinkSetting namedSetting() {
    LinkSetting linkSetting = new LinkSetting();
    linkSetting.setName(SOURCE_NAME);
    linkSetting.setAllowHide(true);
    return linkSetting;
  }

  private LinkData export(Portlet preferences, PortletInstanceContext context) {
    List<PortletInstancePreference> exported = plugin.generatePreferences(mock(Application.class), preferences, context);
    assertEquals(1, exported.size());
    return JsonUtils.fromJsonString(exported.get(0).getValue(), LinkData.class);
  }

  private PortletInstanceContext draft(boolean draft) {
    return new PortletInstanceContext(true,
                                      draft ? Map.of(PortletInstanceContext.DRAFT_PROPERTY, "true") : Collections.emptyMap());
  }

}
