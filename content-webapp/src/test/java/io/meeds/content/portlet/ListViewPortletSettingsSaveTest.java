/*
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
package io.meeds.content.portlet;

import static io.meeds.social.portlet.CMSPortlet.NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.content.news.service.NewsTargetingService;
import io.meeds.social.cms.service.CMSService;

class ListViewPortletSettingsSaveTest {

  private static final String         SETTING_NAME = "Untitled-1-settings";

  private static final String         USERNAME     = "editor";

  private static final String         SPACE_ID     = "3";

  // the publish right reads the space service once and keeps it
  private static final SpaceService   SPACE_SERVICE = mock(SpaceService.class);

  private MockedStatic<ExoContainerContext> containerContext;

  private CMSService                  cmsService;

  private PortletPreferences          preferences;

  private Map<String, String>         parameters;

  private ActionRequest               request;

  @BeforeEach
  void setUp() {
    cmsService = mock(CMSService.class);
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(CMSService.class)).thenReturn(cmsService);
    preferences = mock(PortletPreferences.class);
    when(preferences.getValue(NAME, null)).thenReturn(SETTING_NAME);
    parameters = new HashMap<>();
    request = mock(ActionRequest.class);
    when(request.getPreferences()).thenReturn(preferences);
    when(request.getParameter(anyString())).thenAnswer(invocation -> parameters.get(invocation.<String> getArgument(0)));
    when(request.getParameterNames()).thenAnswer(invocation -> Collections.enumeration(parameters.keySet()));
    ConversationState.setCurrent(new ConversationState(new Identity(USERNAME)));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
    containerContext.close();
  }

  @Test
  void theContentListSaveStoresOnlyItsDrawerSettings() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    postDrawerSettingsAndOthers("showHeader", "headerTitle", "allowFilteringPerCategory", "categoryDepth", "categoryIds", "excludeCategoryIds");

    new ContentListViewPortlet().processAction(request, mock(ActionResponse.class));

    verifyOnlyDrawerSettingsStored("showHeader", "headerTitle", "allowFilteringPerCategory", "categoryDepth", "categoryIds", "excludeCategoryIds");
  }

  @Test
  void theNewsListSaveStoresOnlyItsDrawerSettings() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    String[] drawerSettings = { "viewTemplate", "newsTarget", "selectedArticleIds", "articlesSourceOption", "showHeader",
        "showSeeAll", "showArticleTitle", "showArticleSummary", "showArticleImage", "showArticleAuthor", "showArticleSpace",
        "showArticleReactions", "showArticleDate", "seeAllUrl", "limit" };
    postDrawerSettingsAndOthers(drawerSettings);

    new NewsListViewPortlet().processAction(request, mock(ActionResponse.class));

    for (String setting : drawerSettings) {
      verify(preferences).setValue(setting, drawerValue(setting));
    }
    verifyOtherSettingsNeverStored();
    verify(preferences).store();
  }

  @Test
  void theNewsListSaveIsAcceptedFromAnEditorOfTheInstance() throws Exception {
    when(cmsService.hasEditPermission(argThat(identity -> identity != null && USERNAME.equals(identity.getUserId())),
                                      any(),
                                      eq(SETTING_NAME))).thenReturn(true);
    parameters.put("limit", "6");

    new NewsListViewPortlet().processAction(request, mock(ActionResponse.class));

    verify(preferences).setValue("limit", "6");
    // a setting the save does not post keeps its stored value
    verify(preferences, never()).setValue(eq("viewTemplate"), nullable(String.class));
    verify(preferences).store();
  }

  /**
   * The save reads the instance's edit right and no separate publish right
   */
  @Test
  void theNewsListSaveIsRefusedToAPublisherWithoutTheInstanceRight() throws Exception {
    Space space = new Space();
    space.setId(SPACE_ID);
    reset(SPACE_SERVICE);
    lenient().when(SPACE_SERVICE.getSpaceById(SPACE_ID)).thenReturn(space);
    lenient().when(SPACE_SERVICE.canPublishOnSpace(any(), any())).thenReturn(true);
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(false);
    parameters.put("limit", "6");
    NewsListViewPortlet portlet = new NewsListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    try (MockedStatic<SpaceUtils> spaceUtils = mockStatic(SpaceUtils.class);
        MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class)) {
      spaceUtils.when(SpaceUtils::getSpaceByContext).thenReturn(space);
      commonsUtils.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(SPACE_SERVICE);

      assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    }
    verify(preferences, never()).store();
  }

  @Test
  void theNewsListSaveIsRefusedForABlankInstanceName() throws Exception {
    when(preferences.getValue(NAME, null)).thenReturn(" ");
    when(cmsService.hasEditPermission(any(), any(), nullable(String.class))).thenReturn(true);
    parameters.put("limit", "6");
    NewsListViewPortlet portlet = new NewsListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    verify(preferences, never()).store();
  }

  @Test
  void theContentListSaveKeepsTheSettingsItDoesNotPost() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    parameters.put("categoryDepth", "2");

    new ContentListViewPortlet().processAction(request, mock(ActionResponse.class));

    verify(preferences).setValue("categoryDepth", "2");
    verify(preferences, never()).setValue(eq("headerTitle"), nullable(String.class));
    verify(preferences).store();
  }

  @Test
  void theNewsListViewTakesItsSettingsRightFromTheInstanceCanEdit() throws Exception {
    String view = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/newsListView.jsp"), StandardCharsets.UTF_8);
    assertTrue(view.contains("boolean hasEditPermission = Boolean.TRUE.equals(request.getAttribute(\"canEdit\"));"));
    assertFalse(view.contains("UserACL"));
  }

  @Test
  void theNewsListSaveIsRefusedToAnyoneElse() throws Exception {
    // the right is held by a signed-in identity only
    when(cmsService.hasEditPermission(argThat(identity -> identity != null), any(), eq(SETTING_NAME))).thenReturn(true);
    parameters.put("limit", "6");
    NewsListViewPortlet portlet = new NewsListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    // a visitor who is not signed in
    ConversationState.setCurrent(null);
    assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    verify(preferences, never()).setValue(nullable(String.class), nullable(String.class));
    verify(preferences, never()).store();
  }

  @Test
  void theNewsListSaveIsRefusedForAValueOfAnotherShape() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    NewsTargetingService targetingService = mock(NewsTargetingService.class);
    when(targetingService.isTarget(anyString())).thenReturn(false);
    containerContext.when(() -> ExoContainerContext.getService(NewsTargetingService.class)).thenReturn(targetingService);
    NewsListViewPortlet portlet = new NewsListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    for (String[] setting : new String[][] { { "limit", "many" }, { "seeAllUrl", "ftp://example.org" }, { "newsTarget", "unknown" } }) {
      parameters.clear();
      parameters.put("showHeader", "true");
      parameters.put(setting[0], setting[1]);
      assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    }
    verify(preferences, never()).setValue(nullable(String.class), nullable(String.class));
    verify(preferences, never()).store();
  }

  @Test
  void theNewsListSaveIsRefusedForEachSettingOfAnotherShape() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    NewsListViewPortlet portlet = new NewsListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    List<String[]> refused = new ArrayList<>(List.of(new String[][] { { "viewTemplate", "News Cards<" },
        { "articlesSourceOption", "everything" }, { "selectedArticleIds", "12,abc" }, { "selectedArticleIds", "12," } }));
    for (String flag : List.of("showHeader", "showSeeAll", "showArticleTitle", "showArticleSummary", "showArticleImage",
                               "showArticleAuthor", "showArticleSpace", "showArticleReactions", "showArticleDate")) {
      refused.add(new String[] { flag, "yes" });
    }
    for (String[] setting : refused) {
      parameters.clear();
      parameters.put("limit", "5");
      parameters.put(setting[0], setting[1]);
      assertThrows(PortletException.class, () -> portlet.processAction(request, response), setting[0]);
    }
    verify(preferences, never()).setValue(nullable(String.class), nullable(String.class));
    verify(preferences, never()).store();
  }

  @Test
  void theContentListSaveIsRefusedForEachSettingOfAnotherShape() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    ContentListViewPortlet portlet = new ContentListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    for (String[] setting : new String[][] { { "showHeader", "1" }, { "allowFilteringPerCategory", "maybe" },
        { "categoryDepth", "-1" }, { "categoryDepth", "51" }, { "categoryDepth", "2.5" }, { "categoryIds", "1.5" },
        { "categoryIds", "0x1A" }, { "excludeCategoryIds", "3,x" } }) {
      parameters.clear();
      parameters.put("headerTitle", "A title");
      parameters.put(setting[0], setting[1]);
      assertThrows(PortletException.class, () -> portlet.processAction(request, response), setting[0]);
    }
    verify(preferences, never()).setValue(nullable(String.class), nullable(String.class));
    verify(preferences, never()).store();
  }

  @Test
  void theContentListSaveKeepsAFreeHeaderTitle() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    parameters.put("headerTitle", "Our <b>news</b> & more");

    new ContentListViewPortlet().processAction(request, mock(ActionResponse.class));

    verify(preferences).setValue("headerTitle", "Our <b>news</b> & more");
    verify(preferences).store();
  }

  @Test
  void theNewsListSaveAcceptsTheValuesTheDrawerPosts() throws Exception {
    when(cmsService.hasEditPermission(any(), any(), eq(SETTING_NAME))).thenReturn(true);
    NewsTargetingService targetingService = mock(NewsTargetingService.class);
    when(targetingService.isTarget(anyString())).thenReturn(false);
    containerContext.when(() -> ExoContainerContext.getService(NewsTargetingService.class)).thenReturn(targetingService);
    parameters.put("newsTarget", "null");
    parameters.put("seeAllUrl", "www.example.org");
    parameters.put("limit", "250");

    new NewsListViewPortlet().processAction(request, mock(ActionResponse.class));

    verify(preferences).setValue("newsTarget", "");
    verify(preferences).setValue("seeAllUrl", "www.example.org");
    verify(preferences).setValue("limit", "250");
    verify(preferences).store();
  }

  @Test
  void theNewsListViewReadsItsStoredValuesThroughTheirRules() throws Exception {
    String view = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/newsListView.jsp"), StandardCharsets.UTF_8);
    assertTrue(view.contains("NewsListSettingValues.targetOrDefault("));
    assertTrue(view.contains("NewsListSettingValues.limitOrDefault("));
    assertTrue(view.contains("NewsListSettingValues.sourceOrDefault("));
    assertTrue(view.contains("&& !limit.isEmpty()) {"));
    assertTrue(view.contains("NewsListSettingValues.seeAllUrlOrDefault("));
  }

  @Test
  void theContentListSaveIsRefusedToANonEditor() throws Exception {
    parameters.put("headerTitle", "x");
    ContentListViewPortlet portlet = new ContentListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    verify(preferences, never()).store();
  }

  private void postDrawerSettingsAndOthers(String... drawerSettings) {
    for (String setting : drawerSettings) {
      parameters.put(setting, drawerValue(setting));
    }
    parameters.put(NAME, "another-setting");
    parameters.put("applicationId", "4");
    parameters.put("header", "a header");
    parameters.put("data.init", "{}");
  }

  private void verifyOnlyDrawerSettingsStored(String... drawerSettings) throws Exception {
    for (String setting : drawerSettings) {
      verify(preferences).setValue(setting, drawerValue(setting));
    }
    verifyOtherSettingsNeverStored();
    verify(preferences).store();
  }

  // a value of the shape the drawer posts for the setting
  private static String drawerValue(String setting) {
    return switch (setting) {
      case "newsTarget" -> "";
      case "seeAllUrl" -> "/portal/news";
      case "limit" -> "5";
      case "viewTemplate" -> "NewsCards";
      case "articlesSourceOption" -> "posted";
      case "selectedArticleIds", "categoryIds", "excludeCategoryIds" -> "12,34";
      case "categoryDepth" -> "4";
      case "headerTitle" -> "headerTitle-value";
      default -> "true";
    };
  }

  private void verifyOtherSettingsNeverStored() throws Exception {
    verify(preferences, never()).setValue(eq(NAME), anyString());
    verify(preferences, never()).setValue(eq("applicationId"), anyString());
    verify(preferences, never()).setValue(eq("header"), anyString());
    verify(preferences, never()).setValue(eq("data.init"), anyString());
  }

}
