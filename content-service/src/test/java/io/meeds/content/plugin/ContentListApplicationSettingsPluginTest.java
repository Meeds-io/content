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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.content.utils.ContentUtils;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.category.service.CategoryService;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.service.TranslationService;

@ExtendWith(MockitoExtension.class)
class ContentListApplicationSettingsPluginTest {

  private static final String                  USERNAME    = "mary";

  private static final String                  LIVE_PAGE   = "portal::classic::contents";

  private static final long                    SPACE_ID    = 0;

  @Mock
  private CMSService                           cmsService;

  @Mock
  private TranslationService                   translationService;

  @Mock
  private SettingService                       settingService;

  @Mock
  private IdentityManager                      identityManager;

  @Mock
  private UserACL                              userAcl;

  @Mock
  private LocaleConfigService                  localeConfigService;

  @Mock
  private ApplicationSettingsService           applicationSettingsService;

  @Mock
  private CategoryService                      categoryService;

  @Mock
  private Identity                             identity;

  @InjectMocks
  private ContentListApplicationSettingsPlugin plugin;

  @BeforeEach
  void setUp() {
    LocaleConfig english = localeConfig(Locale.ENGLISH);
    LocaleConfig french = localeConfig(Locale.FRENCH);
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(english, french));
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(english);
    lenient().when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    lenient().when(cmsService.hasEditPermission(identity, LIVE_PAGE, SPACE_ID)).thenReturn(true);
  }

  @Test
  void testValidateRefusesValuesTheDrawerRefuses() {
    ApplicationSettingsContext context = context(Map.of());
    assertRefused(context, Map.of("category_depth", 51), "value is not accepted for category_depth");
    assertRefused(context, Map.of("category_depth", -1), "value is not accepted for category_depth");
    assertRefused(context, Map.of("category_ids", List.of(3, 3)), "value is not accepted for category_ids");
    assertRefused(context, Map.of("exclude_category_ids", "3,4"), "value is not accepted for exclude_category_ids");
    assertRefused(context, Map.of("header_title", Map.of("en", "x".repeat(151))), "value is not accepted for header_title");
    assertRefused(context, Map.of("view_template", "NewsCards"), "value is not accepted for view_template");
  }

  @Test
  void testValidateRefusesAnUnknownCategory() throws Exception {
    when(categoryService.getCategory(9L, USERNAME, Locale.ENGLISH)).thenThrow(new ObjectNotFoundException("category"));
    ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class,
                                             () -> plugin.validate(context(Map.of()), Map.of("category_ids", List.of(9))));
    assertEquals("does not exist: category", e.getMessage());
  }

  @Test
  void testOptionsAreWrittenAsTheActionStoresThem() throws Exception {
    ApplicationSettingsContext context = context(Map.of());
    plugin.apply(context,
                 Map.of("show_header", false,
                        "allow_filtering_per_category", true,
                        "category_depth", 0,
                        "category_ids", List.of(12, 4),
                        "exclude_category_ids", List.of()));
    verify(applicationSettingsService).updatePreferences(context,
                                                         Map.of("showHeader",
                                                                "false",
                                                                "allowFilteringPerCategory",
                                                                "true",
                                                                "categoryDepth",
                                                                "0",
                                                                "categoryIds",
                                                                "12,4",
                                                                "excludeCategoryIds",
                                                                ""));
  }

  @Test
  void testHeaderRekeysOnTheContentListKeysAndStoresTheTitlePreference() throws Exception {
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(new org.exoplatform.social.core.identity.model.Identity("9"));
    when(applicationSettingsService.isOwner(any())).thenReturn(false);
    ApplicationSettingsContext context = context(Map.of("name", "Untitled-live", "applicationId", "777"));

    plugin.apply(context, Map.of("header_title", Map.of("en", "Contents", "fr", "Contenus")));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(cmsService).saveSettingName(eq(ContentUtils.CONTENT_LIST_VIEW_CMS_TYPE), name.capture(), eq(LIVE_PAGE), eq(SPACE_ID), eq(9L));
    ArgumentCaptor<String> applicationId = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue<?>> mapped = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(eq(ContentUtils.CONTENT_LIST_VIEW_CONTEXT),
                               eq(ContentUtils.CONTENT_LIST_VIEW_SCOPE),
                               applicationId.capture(),
                               mapped.capture());
    assertEquals(name.getValue(), mapped.getValue().getValue());
    verify(translationService).saveTranslationLabels(eq(ContentUtils.CONTENT_LIST_VIEW_TYPE),
                                                     eq(applicationId.getValue()),
                                                     eq(ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD),
                                                     anyMap(),
                                                     eq(USERNAME),
                                                     eq(false));
    InOrder order = inOrder(cmsService, settingService, translationService, applicationSettingsService);
    order.verify(cmsService).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    order.verify(settingService).set(any(), any(), anyString(), any());
    order.verify(translationService).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), eq(false));
    order.verify(applicationSettingsService).markOwner(any(), anyMap());
    verify(applicationSettingsService).markOwner(context,
                                                 Map.of("headerTitle",
                                                        "Contents",
                                                        "name",
                                                        name.getValue(),
                                                        "applicationId",
                                                        applicationId.getValue()));
  }

  @Test
  void testReadAppliesTheDrawerDefaults() throws Exception {
    when(translationService.getTranslationField(ContentUtils.CONTENT_LIST_VIEW_TYPE,
                                                "777",
                                                ContentUtils.CONTENT_LIST_HEADER_TITLE_FIELD,
                                                USERNAME)).thenReturn(null);
    Map<String, Object> settings = plugin.read(context(Map.of("applicationId", "777", "categoryIds", "5,2")));
    assertEquals(true, settings.get("show_header"));
    assertEquals(true, settings.get("allow_filtering_per_category"));
    assertEquals(4, settings.get("category_depth"));
    assertEquals(List.of(5L, 2L), settings.get("category_ids"));
    assertEquals(List.of(), settings.get("exclude_category_ids"));
    assertEquals(Map.of(), settings.get("header_title"));
    assertEquals("ContentListView", plugin.getPortletName());
    assertEquals(ContentUtils.CONTENT_LIST_VIEW_CMS_TYPE, plugin.getCmsType());
    verify(translationService).getTranslationField(anyString(), anyString(), anyString(), eq(USERNAME));
  }

  private void assertRefused(ApplicationSettingsContext context, Map<String, Object> settings, String message) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    assertEquals(message, e.getMessage());
  }

  private ApplicationSettingsContext context(Map<String, String> preferences) {
    return new ApplicationSettingsContext(45,
                                          "content/ContentListView",
                                          new HashMap<>(preferences),
                                          false,
                                          true,
                                          null,
                                          null,
                                          LIVE_PAGE,
                                          SPACE_ID,
                                          USERNAME);
  }

  private LocaleConfig localeConfig(Locale locale) {
    LocaleConfig config = mock(LocaleConfig.class);
    lenient().when(config.getLocale()).thenReturn(locale);
    return config;
  }

}
