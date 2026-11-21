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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

import io.meeds.social.cms.service.CMSService;

class ListViewPortletSettingsSaveTest {

  private static final String         SETTING_NAME = "Untitled-1-settings";

  private static final String         USERNAME     = "editor";

  private static final String         SPACE_ID     = "3";

  // the publish right reads the space service once and keeps it
  private static final SpaceService   SPACE_SERVICE = mock(SpaceService.class);

  private MockedStatic<ExoContainerContext> containerContext;

  private MockedStatic<SpaceUtils>    spaceUtils;

  private MockedStatic<CommonsUtils>  commonsUtils;

  private CMSService                  cmsService;

  private PortletPreferences          preferences;

  private Map<String, String>         parameters;

  private ActionRequest               request;

  @BeforeEach
  void setUp() {
    cmsService = mock(CMSService.class);
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(CMSService.class)).thenReturn(cmsService);
    spaceUtils = mockStatic(SpaceUtils.class);
    commonsUtils = mockStatic(CommonsUtils.class);
    commonsUtils.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(SPACE_SERVICE);
    reset(SPACE_SERVICE);
    preferences = mock(PortletPreferences.class);
    when(preferences.getValue(NAME, null)).thenReturn(SETTING_NAME);
    parameters = new HashMap<>();
    request = mock(ActionRequest.class);
    when(request.getPreferences()).thenReturn(preferences);
    when(request.getParameter(anyString())).thenAnswer(invocation -> parameters.get(invocation.<String> getArgument(0)));
    ConversationState.setCurrent(new ConversationState(new Identity(USERNAME)));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
    commonsUtils.close();
    spaceUtils.close();
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

    verifyOnlyDrawerSettingsStored(drawerSettings);
  }

  @Test
  void theNewsListSaveIsAcceptedFromAPublisherOfThePageSpace() throws Exception {
    Space space = new Space();
    space.setId(SPACE_ID);
    spaceUtils.when(SpaceUtils::getSpaceByContext).thenReturn(space);
    when(SPACE_SERVICE.getSpaceById(SPACE_ID)).thenReturn(space);
    when(SPACE_SERVICE.canPublishOnSpace(space, USERNAME)).thenReturn(true);
    parameters.put("limit", "6");

    new NewsListViewPortlet().processAction(request, mock(ActionResponse.class));

    verify(preferences).setValue("limit", "6");
    // a setting the save does not post keeps its stored value
    verify(preferences, never()).setValue(eq("viewTemplate"), nullable(String.class));
    verify(preferences).store();
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
  void theNewsListViewShowsItsSettingsToTheEditorsTheSaveAccepts() throws Exception {
    String view = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/newsListView.jsp"), StandardCharsets.UTF_8);
    assertTrue(view.contains("boolean hasEditPermission = Boolean.TRUE.equals(request.getAttribute(\"canEdit\"));"));
    assertFalse(view.contains("UserACL"));
  }

  @Test
  void theNewsListSaveIsRefusedToAnyoneElse() throws Exception {
    Space space = new Space();
    space.setId(SPACE_ID);
    spaceUtils.when(SpaceUtils::getSpaceByContext).thenReturn(space);
    // a signed-in member of the page's space who may not publish there
    when(SPACE_SERVICE.getSpaceById(SPACE_ID)).thenReturn(space);
    when(SPACE_SERVICE.canPublishOnSpace(space, USERNAME)).thenReturn(false);
    parameters.put("limit", "6");
    NewsListViewPortlet portlet = new NewsListViewPortlet();
    ActionResponse response = mock(ActionResponse.class);

    assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    verify(preferences, never()).setValue(nullable(String.class), nullable(String.class));
    verify(preferences, never()).store();

    verify(SPACE_SERVICE).canPublishOnSpace(space, USERNAME);

    // and a visitor who is not signed in
    ConversationState.setCurrent(null);
    when(SPACE_SERVICE.canPublishOnSpace(any(), any())).thenReturn(true);
    assertThrows(PortletException.class, () -> portlet.processAction(request, response));
    verify(preferences, never()).store();
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
      parameters.put(setting, setting + "-value");
    }
    parameters.put(NAME, "another-setting");
    parameters.put("applicationId", "4");
    parameters.put("header", "a header");
    parameters.put("data.init", "{}");
  }

  private void verifyOnlyDrawerSettingsStored(String... drawerSettings) throws Exception {
    for (String setting : drawerSettings) {
      verify(preferences).setValue(setting, setting + "-value");
    }
    verify(preferences, never()).setValue(eq(NAME), anyString());
    verify(preferences, never()).setValue(eq("applicationId"), anyString());
    verify(preferences, never()).setValue(eq("header"), anyString());
    verify(preferences, never()).setValue(eq("data.init"), anyString());
    verify(preferences).store();
  }

}
