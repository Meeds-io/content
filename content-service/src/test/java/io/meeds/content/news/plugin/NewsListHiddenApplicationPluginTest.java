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

import static io.meeds.content.news.plugin.NewsListHiddenApplicationPlugin.APPLICATION_TYPE;
import static io.meeds.content.news.plugin.NewsListHiddenApplicationPlugin.SETTING_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.content.news.utils.NewsUtils;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.hidden.service.HiddenApplicationService;
import io.meeds.social.translation.service.TranslationService;

@RunWith(MockitoJUnitRunner.class)
public class NewsListHiddenApplicationPluginTest {

  private static final String             NAME           = "Untitled-1-abc";

  private static final String             APPLICATION_ID = "12345";

  private static final String             PAGE_REFERENCE = "portal::dw::overview";

  @Mock
  private CMSService                      cmsService;

  @Mock
  private HiddenApplicationService        hiddenApplicationService;

  @Mock
  private SettingService                  settingService;

  @Mock
  private TranslationService              translationService;

  @Mock
  private ResourceBundleService           resourceBundleService;

  @InjectMocks
  private NewsListHiddenApplicationPlugin plugin;

  @Before
  public void setUp() {
    lenient().when(resourceBundleService.getResourceBundle(eq(NewsListHiddenApplicationPlugin.NEWS_BUNDLE_NAME),
                                                           any(Locale.class)))
             .thenReturn(new ListResourceBundle() {
               @Override
               protected Object[][] getContents() {
                 return new Object[][] { { NewsListHiddenApplicationPlugin.DEFAULT_NAME_KEY, "News block" } };
               }
             });
  }

  @Test
  public void getApplicationOfAnUnknownWindowIsNull() {
    assertNull(plugin.getApplication(NAME, "fr"));
  }

  @Test
  public void getApplicationTitleIsTheHeaderOfTheWindowInTheRequestedLanguage() throws Exception {
    mockSetting();
    when(hiddenApplicationService.isHideAllowed(APPLICATION_TYPE, NAME)).thenReturn(true);
    when(settingService.getSettingsByContextAndScope(NewsUtils.NEWS_LIST_VIEW_CONTEXT.getName(),
                                                     NewsUtils.NEWS_LIST_VIEW_CONTEXT.getId(),
                                                     NewsUtils.NEWS_LIST_VIEW_SCOPE.getName(),
                                                     NewsUtils.NEWS_LIST_VIEW_SCOPE.getId()))
                                                                                              .thenReturn(Map.of("999",
                                                                                                                 SettingValue.create("Other"),
                                                                                                                 APPLICATION_ID,
                                                                                                                 SettingValue.create(NAME)));
    when(translationService.getTranslationLabel(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                APPLICATION_ID,
                                                NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD,
                                                Locale.FRENCH)).thenReturn("Actualités");

    HiddenApplication application = plugin.getApplication(NAME, "fr");

    assertEquals(APPLICATION_TYPE, application.getType());
    assertEquals(NAME, application.getName());
    assertEquals("Actualités", application.getTitle());
    assertEquals(PAGE_REFERENCE, application.getPageReference());
    assertTrue(application.isAllowHide());
  }

  @Test
  public void getApplicationTitleFallsBackToTheEnglishHeader() throws Exception {
    mockSetting();
    when(settingService.getSettingsByContextAndScope(NewsUtils.NEWS_LIST_VIEW_CONTEXT.getName(),
                                                     NewsUtils.NEWS_LIST_VIEW_CONTEXT.getId(),
                                                     NewsUtils.NEWS_LIST_VIEW_SCOPE.getName(),
                                                     NewsUtils.NEWS_LIST_VIEW_SCOPE.getId()))
                                                                                              .thenReturn(Map.of(APPLICATION_ID,
                                                                                                                 SettingValue.create(NAME)));
    when(translationService.getTranslationLabel(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                APPLICATION_ID,
                                                NewsListViewTranslationPlugin.NEWS_LIST_HEADER_NAME_FIELD,
                                                Locale.ENGLISH)).thenReturn("News");

    assertEquals("News", plugin.getApplication(NAME, "fr").getTitle());
  }

  @Test
  public void getApplicationTitleWithoutHeaderIsTheDefaultName() {
    mockSetting();

    HiddenApplication application = plugin.getApplication(NAME, "fr");

    assertEquals("News block", application.getTitle());
    assertFalse("The option is off until an editor turns it on", application.isAllowHide());
  }

  @Test
  public void canViewIsTheAccessPermissionOfTheWindow() {
    Identity identity = new Identity("user");
    when(cmsService.hasAccessPermission(identity, SETTING_TYPE, NAME)).thenReturn(true);
    assertTrue(plugin.canView(NAME, identity));
    assertFalse(plugin.canView(NAME, new Identity("other")));
  }

  @Test
  public void saveHideAllowedTurnsTheOptionOnAndOffForAnEditorOfTheWindow() throws Exception {
    Identity editor = new Identity("editor");
    when(cmsService.hasEditPermission(editor, SETTING_TYPE, NAME)).thenReturn(true);

    plugin.saveHideAllowed(NAME, true, editor);
    verify(hiddenApplicationService).setHideAllowed(APPLICATION_TYPE, NAME, true);

    plugin.saveHideAllowed(NAME, false, editor);
    verify(hiddenApplicationService).setHideAllowed(APPLICATION_TYPE, NAME, false);
  }

  @Test
  public void saveHideAllowedIsRefusedToAUserWhoCantEditTheWindow() {
    Identity reader = new Identity("reader");
    lenient().when(cmsService.hasAccessPermission(reader, SETTING_TYPE, NAME)).thenReturn(true);

    assertThrows(IllegalAccessException.class, () -> plugin.saveHideAllowed(NAME, true, reader));
    verify(hiddenApplicationService, never()).setHideAllowed(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void saveHideAllowedIsRefusedWithoutAUser() {
    lenient().when(cmsService.hasEditPermission(any(), eq(SETTING_TYPE), eq(NAME))).thenReturn(true);

    assertThrows(IllegalAccessException.class, () -> plugin.saveHideAllowed(NAME, false, null));
    verify(hiddenApplicationService, never()).setHideAllowed(anyString(), anyString(), anyBoolean());
  }

  @Test
  public void saveHideAllowedRefusesABlankName() {
    Identity editor = new Identity("editor");
    lenient().when(cmsService.hasEditPermission(any(Identity.class), eq(SETTING_TYPE), any())).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () -> plugin.saveHideAllowed(" ", true, editor));
    assertThrows(IllegalArgumentException.class, () -> plugin.saveHideAllowed(null, true, editor));
    verify(hiddenApplicationService, never()).setHideAllowed(anyString(), anyString(), anyBoolean());
  }

  private void mockSetting() {
    when(cmsService.getSetting(SETTING_TYPE, NAME)).thenReturn(new CMSSetting(SETTING_TYPE, NAME, PAGE_REFERENCE, 0));
  }

}
