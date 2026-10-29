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
package io.meeds.content.news.plugin;

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

import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

@ExtendWith(MockitoExtension.class)
class NewsListPortletInstancePreferencePluginTest {

  private static final String APPLICATION_ID = "123456";

  @Mock
  private TranslationService translationService;

  @InjectMocks
  private NewsListPortletInstancePreferencePlugin plugin;

  @Test
  void testGeneratePreferencesCopiesDisplaySettings() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("viewTemplate", "NewsMosaic");
    preferences.setValue("newsTarget", "homeTarget");
    preferences.setValue("limit", "7");
    preferences.setValue("showArticleSummary", "false");
    preferences.setValue("showArticleImage", "false");
    preferences.setValue("name", "Untitled-1-abc");
    preferences.setValue("applicationId", APPLICATION_ID);
    preferences.setValue("data.init", "stale");
    TranslationField translationField = new TranslationField();
    translationField.setObjectType(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE);
    translationField.setFieldName(NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD);
    translationField.setLabels(Map.of(Locale.ENGLISH, "Header"));
    when(translationService.getTranslationField(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE, APPLICATION_ID, NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD)).thenReturn(translationField);

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("NewsMosaic", result.get("viewTemplate"));
    assertEquals("homeTarget", result.get("newsTarget"));
    assertEquals("7", result.get("limit"));
    assertEquals("false", result.get("showArticleSummary"));
    assertEquals("false", result.get("showArticleImage"));
    assertFalse(result.containsKey("name"), "The copy must not share the original's CMS setting");
    assertFalse(result.containsKey("applicationId"), "The copy must not share the original's header title key");
    assertEquals(JsonUtils.toJsonString(translationField), result.get("data.init"));
  }

  @Test
  void testGeneratePreferencesWithoutHeaderTitle() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("applicationId", APPLICATION_ID);
    preferences.setValue("showHeader", "false");
    when(translationService.getTranslationField(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE, APPLICATION_ID, NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD)).thenReturn(new TranslationField());

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("false", result.get("showHeader"));
    assertFalse(result.containsKey("data.init"), "No header title payload without labels");
  }

  @Test
  void testGeneratePreferencesWithoutApplicationIdKeepsDataInit() throws Exception {
    Portlet preferences = new Portlet();
    preferences.setValue("viewTemplate", "NewsMosaic");
    preferences.setValue("newsTarget", "homeTarget");
    preferences.setValue("limit", "7");
    preferences.setValue("showArticleSummary", "false");
    preferences.setValue("showArticleImage", "false");
    preferences.setValue("data.init", "payload");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("NewsMosaic", result.get("viewTemplate"));
    assertEquals("homeTarget", result.get("newsTarget"));
    assertEquals("7", result.get("limit"));
    assertEquals("false", result.get("showArticleSummary"));
    assertEquals("false", result.get("showArticleImage"));
    assertEquals("payload", result.get("data.init"));
    verify(translationService, never()).getTranslationField(anyString(), anyString(), anyString());
  }

  @Test
  void testGeneratePreferencesWithoutPreferences() {
    assertTrue(plugin.generatePreferences(null, null, new PortletInstanceContext()).isEmpty());
    assertTrue(plugin.generatePreferences(null, new Portlet(), new PortletInstanceContext()).isEmpty());
  }

  @Test
  void testGeneratePreferencesOnExportDropsArticleIds() {
    Portlet preferences = new Portlet();
    preferences.setValue("viewTemplate", "NewsMosaic");
    preferences.setValue("selectedArticleIds", "4,5");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext(true, null)));

    assertEquals("NewsMosaic", result.get("viewTemplate"));
    assertFalse(result.containsKey("selectedArticleIds"), "An export must not carry ids of the exporting platform");
  }

  @Test
  void testGeneratePreferencesOnExportDropsSelectedArticlesSource() {
    Portlet preferences = new Portlet();
    preferences.setValue("articlesSourceOption", "selectedList");
    preferences.setValue("selectedArticleIds", "4,5");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext(true, null)));

    assertFalse(result.containsKey("articlesSourceOption"),
                "A selected articles source without its ids must not be exported, the imported list falls back to posted articles");
    assertFalse(result.containsKey("selectedArticleIds"));
  }

  @Test
  void testGeneratePreferencesOnExportKeepsOtherArticlesSource() {
    Portlet preferences = new Portlet();
    preferences.setValue("articlesSourceOption", "target");
    preferences.setValue("newsTarget", "homeTarget");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext(true, null)));

    assertEquals("target", result.get("articlesSourceOption"));
    assertEquals("homeTarget", result.get("newsTarget"));
  }

  @Test
  void testGeneratePreferencesWithoutContextKeepsArticleIds() {
    Portlet preferences = new Portlet();
    preferences.setValue("viewTemplate", "NewsMosaic");
    preferences.setValue("selectedArticleIds", "4,5");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, null));

    assertEquals("NewsMosaic", result.get("viewTemplate"));
    assertEquals("4,5", result.get("selectedArticleIds"));
  }

  @Test
  void testGeneratePreferencesOnCopyKeepsSelectedArticlesSource() {
    Portlet preferences = new Portlet();
    preferences.setValue("articlesSourceOption", "selectedList");
    preferences.setValue("selectedArticleIds", "4,5");

    Map<String, String> result = toMap(plugin.generatePreferences(null, preferences, new PortletInstanceContext()));

    assertEquals("selectedList", result.get("articlesSourceOption"));
    assertEquals("4,5", result.get("selectedArticleIds"));
  }

  private Map<String, String> toMap(List<PortletInstancePreference> preferences) {
    return preferences.stream()
                      .collect(Collectors.toMap(PortletInstancePreference::getName, PortletInstancePreference::getValue));
  }

}
