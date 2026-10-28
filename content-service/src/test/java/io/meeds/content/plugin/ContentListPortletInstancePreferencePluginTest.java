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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.content.utils.ContentUtils;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

@ExtendWith(MockitoExtension.class)
class ContentListPortletInstancePreferencePluginTest {

  private static final String APPLICATION_ID = "123456";

  @Mock
  private TranslationService translationService;

  @InjectMocks
  private ContentListPortletInstancePreferencePlugin plugin;

  @Test
  void testGeneratePreferencesCopiesDisplaySettings() throws Exception {
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
    assertFalse(result.containsKey("name"), "The copy must not share the original's CMS setting");
    assertFalse(result.containsKey("applicationId"), "The copy must not share the original's header title key");
    assertEquals(JsonUtils.toJsonString(translationField), result.get("data.init"));
  }

  @Test
  void testGeneratePreferencesWithoutHeaderTitle() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("applicationId", APPLICATION_ID);
    preferences.setValue("showHeader", "false");
    when(translationService.getTranslationField(ContentUtils.CONTENT_LIST_VIEW_TYPE, APPLICATION_ID, ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD)).thenReturn(new TranslationField());

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("false", result.get("showHeader"));
    assertFalse(result.containsKey("data.init"), "No header title payload without labels");
  }

  @Test
  void testGeneratePreferencesWithoutApplicationIdKeepsDataInit() throws Exception {
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
  void testGeneratePreferencesWithoutPreferences() {
    assertTrue(plugin.generatePreferences(null, null, new PortletInstanceContext()).isEmpty());
    assertTrue(plugin.generatePreferences(null, new Portlet(), new PortletInstanceContext()).isEmpty());
  }

  @Test
  void testGeneratePreferencesOnExportDropsCategoryIds() {
    Portlet preferences = new Portlet();
    preferences.setValue("showHeader", "false");
    preferences.setValue("categoryIds", "1,2");
    preferences.setValue("excludeCategoryIds", "3");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext(true, null)));

    assertEquals("false", result.get("showHeader"));
    assertFalse(result.containsKey("categoryIds"), "An export must not carry ids of the exporting platform");
    assertFalse(result.containsKey("excludeCategoryIds"), "An export must not carry ids of the exporting platform");
  }

  @Test
  void testGeneratePreferencesWithoutContextKeepsCategoryIds() {
    Portlet preferences = new Portlet();
    preferences.setValue("showHeader", "false");
    preferences.setValue("categoryIds", "1,2");
    preferences.setValue("excludeCategoryIds", "3");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, null));

    assertEquals("false", result.get("showHeader"));
    assertEquals("1,2", result.get("categoryIds"));
    assertEquals("3", result.get("excludeCategoryIds"));
  }

  private Map<String, String> toMap(List<PortletInstancePreference> preferences) {
    return preferences.stream()
                      .collect(Collectors.toMap(PortletInstancePreference::getName, PortletInstancePreference::getValue));
  }

}
