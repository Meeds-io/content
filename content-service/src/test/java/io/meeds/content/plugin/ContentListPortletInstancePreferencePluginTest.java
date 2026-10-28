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
package io.meeds.content.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.content.utils.ContentUtils;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

@RunWith(MockitoJUnitRunner.class)
public class ContentListPortletInstancePreferencePluginTest {

  private static final String APPLICATION_ID = "123456";

  @Mock
  private TranslationService translationService;

  @InjectMocks
  private ContentListPortletInstancePreferencePlugin plugin;

  @Test
  public void testGeneratePreferencesCopiesDisplaySettings() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("showHeader", "false");
    preferences.setValue("allowFilteringPerCategory", "true");
    preferences.setValue("categoryDepth", "2");
    preferences.setValue("categoryIds", "1,2");
    preferences.setValue("excludeCategoryIds", "3");
    preferences.setValue("name", "Untitled-1-abc");
    preferences.setValue("applicationId", APPLICATION_ID);
    preferences.setValue("data.init", "stale");
    TranslationField translationField = new TranslationField();
    translationField.setObjectType(ContentUtils.CONTENT_LIST_VIEW_TYPE);
    translationField.setFieldName(ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD);
    translationField.setLabels(Map.of(Locale.ENGLISH, "Header"));
    when(translationService.getTranslationField(ContentUtils.CONTENT_LIST_VIEW_TYPE, APPLICATION_ID, ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD)).thenReturn(translationField);

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("false", result.get("showHeader"));
    assertEquals("true", result.get("allowFilteringPerCategory"));
    assertEquals("2", result.get("categoryDepth"));
    assertEquals("1,2", result.get("categoryIds"));
    assertEquals("3", result.get("excludeCategoryIds"));
    assertFalse("The copy must not share the original's CMS setting", result.containsKey("name"));
    assertFalse("The copy must not share the original's header title key", result.containsKey("applicationId"));
    assertEquals(JsonUtils.toJsonString(translationField), result.get("data.init"));
  }

  @Test
  public void testGeneratePreferencesWithoutHeaderTitle() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("applicationId", APPLICATION_ID);
    preferences.setValue("showHeader", "false");
    when(translationService.getTranslationField(ContentUtils.CONTENT_LIST_VIEW_TYPE, APPLICATION_ID, ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD)).thenReturn(new TranslationField());

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("false", result.get("showHeader"));
    assertFalse("No header title payload without labels", result.containsKey("data.init"));
  }

  @Test
  public void testGeneratePreferencesWithoutApplicationIdKeepsDataInit() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("showHeader", "false");
    preferences.setValue("allowFilteringPerCategory", "true");
    preferences.setValue("categoryDepth", "2");
    preferences.setValue("categoryIds", "1,2");
    preferences.setValue("excludeCategoryIds", "3");
    preferences.setValue("data.init", "payload");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("false", result.get("showHeader"));
    assertEquals("true", result.get("allowFilteringPerCategory"));
    assertEquals("2", result.get("categoryDepth"));
    assertEquals("1,2", result.get("categoryIds"));
    assertEquals("3", result.get("excludeCategoryIds"));
    assertEquals("payload", result.get("data.init"));
    verify(translationService, never()).getTranslationField(anyString(), anyString(), anyString());
  }

  @Test
  public void testGeneratePreferencesWithoutPreferences() {
    assertTrue(plugin.generatePreferences(null, null, new PortletInstanceContext()).isEmpty());
    assertTrue(plugin.generatePreferences(null, new Portlet(), new PortletInstanceContext()).isEmpty());
  }

  private Map<String, String> toMap(List<PortletInstancePreference> preferences) {
    return preferences.stream()
                      .collect(Collectors.toMap(PortletInstancePreference::getName, PortletInstancePreference::getValue));
  }

}
