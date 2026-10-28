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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import io.meeds.content.news.model.News;
import io.meeds.content.news.rest.model.NewsTargetingEntity;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.service.NewsTargetingService;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.plugin.ListViewApplicationSettingsPlugin;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class NewsListApplicationSettingsPluginTest {

  private static final String               USERNAME     = "mary";

  private static final String               LIVE_PAGE    = "portal::classic::news";

  private static final String               LIVE_NAME    = "Untitled-12-live";

  private static final String               LIVE_APP_ID  = "777";

  private static final long                 SPACE_ID     = 4;

  private static final long                 WINDOW_ID    = 45;

  @Mock
  private CMSService                        cmsService;

  @Mock
  private TranslationService                translationService;

  @Mock
  private SettingService                    settingService;

  @Mock
  private IdentityManager                   identityManager;

  @Mock
  private UserACL                           userAcl;

  @Mock
  private LocaleConfigService               localeConfigService;

  @Mock
  private ApplicationSettingsService        applicationSettingsService;

  @Mock
  private NewsService                       newsService;

  @Mock
  private NewsTargetingService              newsTargetingService;

  @Mock
  private Identity                          identity;

  @InjectMocks
  private NewsListApplicationSettingsPlugin plugin;

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
  void testRegistersFromPostConstruct() throws NoSuchMethodException {
    plugin.init();
    verify(applicationSettingsService).addPlugin(plugin);
    assertTrue(ListViewApplicationSettingsPlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class),
               "the annotation-removed mutant leaves the plugin unregistered");
    assertEquals("NewsListView", plugin.getPortletName());
    assertTrue(plugin.isConfigurableInSiteContainer());
  }

  @Test
  void testValidateRefusesValuesTheDrawerRefuses() {
    ApplicationSettingsContext context = pageContext(Map.of());
    assertRefused(context, Map.of("view_template", "NewsEmptyTemplate"), "value is not accepted for view_template");
    assertRefused(context, Map.of("view_template", "NewsMosaic", "limit", 4), "value is not accepted for limit");
    assertRefused(context, Map.of("view_template", "NewsLatest", "limit", 5), "value is not accepted for limit");
    assertRefused(context, Map.of("limit", 0), "value is not accepted for limit");
    assertRefused(context, Map.of("source", "all"), "value is not accepted for source");
    assertRefused(context, Map.of("source", "target"), "value is not accepted for news_target");
    assertRefused(context, Map.of("news_target", "home"), "value is not accepted for news_target");
    assertRefused(context, Map.of("source", "selectedList"), "value is not accepted for selected_article_ids");
    assertRefused(context, Map.of("show_see_all", true), "value is not accepted for see_all_url");
    assertRefused(context, Map.of("header_title", Map.of("fr", "Actualités")), "value is not accepted for header_title");
    assertRefused(context, Map.of("header_title", Map.of("en", "x".repeat(101))), "value is not accepted for header_title");
    assertRefused(context, Map.of("show_comments", true), "value is not accepted for show_comments");
  }

  @Test
  void testValidateAcceptsTheDrawerBounds() throws Exception {
    plugin.validate(pageContext(Map.of()), Map.of("view_template", "NewsMosaic", "limit", 3));
    plugin.validate(pageContext(Map.of()), Map.of("view_template", "NewsLatest", "limit", 4));
    plugin.validate(pageContext(Map.of()), Map.of("view_template", "NewsCards", "limit", 12));
    plugin.validate(pageContext(Map.of()), Map.of("show_see_all", true, "see_all_url", "/portal/dw/news"));
    plugin.validate(pageContext(Map.of()), Map.of("header_title", Map.of("en", "x".repeat(100))));
  }

  @Test
  void testValidateRefusesATargetTheUserMayNotUse() {
    NewsTargetingEntity target = new NewsTargetingEntity();
    target.setName("allowed");
    when(newsTargetingService.getAllowedTargets(identity)).thenReturn(List.of(target));
    ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class,
                                             () -> plugin.validate(pageContext(Map.of()),
                                                                   Map.of("source", "target", "news_target", "other")));
    assertEquals("does not exist: news_target", e.getMessage());
  }

  @Test
  void testValidateRefusesAnArticleTheUserCannotView() {
    News news = new News();
    when(newsService.getNewsArticleById("5")).thenReturn(news);
    when(newsService.canViewNews(news, USERNAME)).thenReturn(false);
    ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class,
                                             () -> plugin.validate(pageContext(Map.of()),
                                                                   Map.of("source", "selectedList", "selected_article_ids", List.of(5))));
    assertEquals("does not exist: article", e.getMessage());
  }

  /**
   * The read returns every stored id, an article another editor selected
   * included: sending it back keeps that article, only the added ones need
   * the user's view right
   */
  @Test
  void testStoredArticlesAreKeptWithoutTheUsersViewRight() throws Exception {
    News added = new News();
    when(newsService.getNewsArticleById("3")).thenReturn(added);
    when(newsService.canViewNews(added, USERNAME)).thenReturn(true);
    ApplicationSettingsContext context = pageContext(Map.of("articlesSourceOption", "selectedList", "selectedArticleIds", "5"));

    plugin.apply(context, Map.of("selected_article_ids", List.of(5, 3)));

    verify(newsService, never()).getNewsArticleById("5");
    verify(applicationSettingsService).updatePreferences(context, Map.of("selectedArticleIds", "5,3", "limit", "2"));
  }

  @Test
  void testUnreadableNumbersAreRefusedOrIgnored() throws Exception {
    assertRefused(pageContext(Map.of()),
                  Map.of("source", "selectedList", "selected_article_ids", List.of("99999999999999999999")),
                  "value is not accepted for selected_article_ids");
    Map<String, Object> settings = plugin.read(pageContext(Map.of("limit", "99999999999999999999",
                                                                  "selectedArticleIds", "4,99999999999999999999,x")));
    assertEquals(4, settings.get("limit"));
    assertEquals(List.of(4L), settings.get("selected_article_ids"));
  }

  @Test
  void testValidateRefusesAPageNonEditor() {
    when(cmsService.hasEditPermission(identity, LIVE_PAGE, SPACE_ID)).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> plugin.validate(pageContext(Map.of()), Map.of("limit", 2)));
  }

  @Test
  void testSiteContainerAcceptsOptionsButNotTheHeader() throws Exception {
    ApplicationSettingsContext context = siteContext();
    plugin.validate(context, Map.of("limit", 2));
    verify(cmsService, never()).hasEditPermission(any(Identity.class), anyString(), anyLong());
    assertRefused(context, Map.of("header_title", Map.of("en", "News")), "not configurable in a site container: header_title");
  }

  @Test
  void testOptionsAreWrittenAsTheActionStoresThemWithoutRekeying() throws Exception {
    ApplicationSettingsContext context = pageContext(Map.of("name", LIVE_NAME, "applicationId", LIVE_APP_ID));
    plugin.apply(context, Map.of("view_template", "NewsList", "show_article_date", false, "limit", 6));

    verify(applicationSettingsService).updatePreferences(context,
                                                         Map.of("viewTemplate", "NewsList", "showArticleDate", "false", "limit", "6"));
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  @Test
  void testTemplateChangeClampsTheStoredLimit() throws Exception {
    ApplicationSettingsContext context = pageContext(Map.of("limit", "8"));
    plugin.apply(context, Map.of("view_template", "NewsMosaic"));
    verify(applicationSettingsService).updatePreferences(context, Map.of("viewTemplate", "NewsMosaic", "limit", "3"));
  }

  @Test
  void testSelectedListWritesTheIdsAndTheirCount() throws Exception {
    News news = new News();
    when(newsService.getNewsArticleById(anyString())).thenReturn(news);
    when(newsService.canViewNews(news, USERNAME)).thenReturn(true);
    ApplicationSettingsContext context = pageContext(Map.of("newsTarget", "home"));
    plugin.apply(context, Map.of("source", "selectedList", "selected_article_ids", List.of(5, 3)));
    verify(applicationSettingsService).updatePreferences(context,
                                                         Map.of("articlesSourceOption",
                                                                "selectedList",
                                                                "newsTarget",
                                                                "",
                                                                "selectedArticleIds",
                                                                "5,3",
                                                                "limit",
                                                                "2"));
    assertRefused(context, Map.of("source", "selectedList", "selected_article_ids", List.of(5), "limit", 1),
                  "value is not accepted for limit");
  }

  /**
   * A draft window carries the live window's keys verbatim: they resolve to a
   * setting of the live page, and only the missing ownership marker tells
   * the plugin they are not its own
   */
  @Test
  void testSelectedListRespectsTheTemplateMaximum() {
    News news = new News();
    lenient().when(newsService.getNewsArticleById(anyString())).thenReturn(news);
    lenient().when(newsService.canViewNews(news, USERNAME)).thenReturn(true);
    assertRefused(pageContext(Map.of("viewTemplate", "NewsMosaic")),
                  Map.of("source", "selectedList", "selected_article_ids", List.of(1, 2, 3, 4)),
                  "value is not accepted for selected_article_ids");
    assertRefused(pageContext(Map.of("viewTemplate", "NewsLatest", "selectedArticleIds", "1,2,3,4,5")),
                  Map.of("source", "selectedList"),
                  "value is not accepted for source");
  }

  /**
   * The drawer keeps every selected article when the template changes and
   * clamps the limit only, so a stored list longer than the template's
   * maximum is valid and blocks no other option
   */
  @Test
  void testStoredSelectedListLongerThanTheTemplateMaximumIsKept() throws Exception {
    ApplicationSettingsContext mosaic = pageContext(Map.of("articlesSourceOption", "selectedList", "selectedArticleIds", "1,2,3,4,5"));
    plugin.apply(mosaic, Map.of("view_template", "NewsMosaic"));
    verify(applicationSettingsService).updatePreferences(mosaic, Map.of("viewTemplate", "NewsMosaic", "limit", "3"));

    ApplicationSettingsContext stored = pageContext(Map.of("viewTemplate",
                                                           "NewsMosaic",
                                                           "articlesSourceOption",
                                                           "selectedList",
                                                           "selectedArticleIds",
                                                           "1,2,3,4,5",
                                                           "limit",
                                                           "3"));
    plugin.apply(stored, Map.of("show_header", false));
    verify(applicationSettingsService).updatePreferences(stored, Map.of("showHeader", "false"));
  }

  @Test
  void testStoredSeeAllTargetSentBackUnchangedIsKept() throws Exception {
    ApplicationSettingsContext context = pageContext(Map.of("seeAllUrl", "meeds.io"));
    plugin.validate(context, Map.of("see_all_url", "meeds.io", "show_see_all", true));
    assertRefused(context, Map.of("see_all_url", "news.meeds.io"), "value is not accepted for see_all_url");
  }

  @Test
  void testSelectedListWithinTheTemplateMaximumIsAccepted() throws Exception {
    News news = new News();
    when(newsService.getNewsArticleById(anyString())).thenReturn(news);
    when(newsService.canViewNews(news, USERNAME)).thenReturn(true);
    plugin.validate(pageContext(Map.of("viewTemplate", "NewsMosaic")),
                    Map.of("source", "selectedList", "selected_article_ids", List.of(1, 2, 3)));
    plugin.validate(pageContext(Map.of("viewTemplate", "NewsLatest", "selectedArticleIds", "1,2,3,4")),
                    Map.of("source", "selectedList"));
  }

  @Test
  void testFirstHeaderWriteOnADraftRekeysTheWindow() throws Exception {
    org.exoplatform.social.core.identity.model.Identity userIdentity = new org.exoplatform.social.core.identity.model.Identity("9");
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);
    when(applicationSettingsService.isOwner(any())).thenReturn(false);
    lenient().when(settingService.get(NewsUtils.NEWS_LIST_VIEW_CONTEXT, NewsUtils.NEWS_LIST_VIEW_SCOPE, LIVE_APP_ID))
             .thenAnswer(i -> SettingValue.create(LIVE_NAME));
    lenient().when(cmsService.getSetting(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE, LIVE_NAME)).thenReturn(cmsSetting(LIVE_PAGE));
    ApplicationSettingsContext context = pageContext(Map.of("name", LIVE_NAME, "applicationId", LIVE_APP_ID));

    plugin.apply(context, Map.of("header_title", Map.of("en", "News", "fr", "Actualités")));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(cmsService).saveSettingName(eq(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE), name.capture(), eq(LIVE_PAGE), eq(SPACE_ID), eq(9L));
    assertNotEquals(LIVE_NAME, name.getValue());
    ArgumentCaptor<String> applicationId = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue<?>> mapped = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(eq(NewsUtils.NEWS_LIST_VIEW_CONTEXT),
                               eq(NewsUtils.NEWS_LIST_VIEW_SCOPE),
                               applicationId.capture(),
                               mapped.capture());
    assertNotEquals(LIVE_APP_ID, applicationId.getValue());
    assertEquals(name.getValue(), mapped.getValue().getValue());
    verify(translationService).saveTranslationLabels(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                     applicationId.getValue(),
                                                     NewsUtils.NEWS_LIST_HEADER_NAME_FIELD,
                                                     Map.of(Locale.ENGLISH, "News", Locale.FRENCH, "Actualités"),
                                                     USERNAME,
                                                     false);
    verify(translationService, never()).saveTranslationLabels(anyString(),
                                                              eq(LIVE_APP_ID),
                                                              anyString(),
                                                              anyMap(),
                                                              anyString(),
                                                              eq(false));
    verify(applicationSettingsService).markOwner(context,
                                                 Map.of("name", name.getValue(), "applicationId", applicationId.getValue()));
    InOrder order = inOrder(cmsService, settingService, translationService, applicationSettingsService);
    order.verify(cmsService).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    order.verify(settingService).set(any(), any(), anyString(), any());
    order.verify(translationService).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), eq(false));
    order.verify(applicationSettingsService).markOwner(any(), anyMap());
  }

  @Test
  void testOwnedWindowWritesItsHeaderInPlace() throws Exception {
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    when(settingService.get(NewsUtils.NEWS_LIST_VIEW_CONTEXT, NewsUtils.NEWS_LIST_VIEW_SCOPE, LIVE_APP_ID))
                                                                                                         .thenAnswer(i -> SettingValue.create(LIVE_NAME));
    when(cmsService.getSetting(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE, LIVE_NAME)).thenReturn(cmsSetting(LIVE_PAGE));
    ApplicationSettingsContext context = pageContext(Map.of("name", LIVE_NAME, "applicationId", LIVE_APP_ID));

    plugin.apply(context, Map.of("header_title", Map.of("en", "News")));

    verify(translationService).saveTranslationLabels(NewsListViewTranslationPlugin.NEWS_LIST_VIEW_OBJECT_TYPE,
                                                     LIVE_APP_ID,
                                                     NewsUtils.NEWS_LIST_HEADER_NAME_FIELD,
                                                     Map.of(Locale.ENGLISH, "News"),
                                                     USERNAME,
                                                     false);
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
  }

  @Test
  void testOwnedWindowWhoseSettingIsOnAnotherPageIsRekeyed() throws Exception {
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(new org.exoplatform.social.core.identity.model.Identity("9"));
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    when(settingService.get(NewsUtils.NEWS_LIST_VIEW_CONTEXT, NewsUtils.NEWS_LIST_VIEW_SCOPE, LIVE_APP_ID))
                                                                                                         .thenAnswer(i -> SettingValue.create(LIVE_NAME));
    when(cmsService.getSetting(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE, LIVE_NAME)).thenReturn(cmsSetting("portal::classic::other"));
    ApplicationSettingsContext context = pageContext(Map.of("name", LIVE_NAME, "applicationId", LIVE_APP_ID));

    plugin.apply(context, Map.of("header_title", Map.of("en", "News")));

    verify(cmsService).saveSettingName(eq(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE), anyString(), eq(LIVE_PAGE), eq(SPACE_ID), eq(9L));
    verify(applicationSettingsService).markOwner(eq(context), anyMap());
  }

  @Test
  void testOwnedWindowWhoseApplicationIdMapsElsewhereIsRekeyed() throws Exception {
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(new org.exoplatform.social.core.identity.model.Identity("9"));
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    when(settingService.get(NewsUtils.NEWS_LIST_VIEW_CONTEXT, NewsUtils.NEWS_LIST_VIEW_SCOPE, LIVE_APP_ID))
                                                                                                         .thenAnswer(i -> SettingValue.create("Untitled-other"));
    lenient().when(cmsService.getSetting(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE, LIVE_NAME)).thenReturn(cmsSetting(LIVE_PAGE));
    ApplicationSettingsContext context = pageContext(Map.of("name", LIVE_NAME, "applicationId", LIVE_APP_ID));

    plugin.apply(context, Map.of("header_title", Map.of("en", "News")));

    verify(cmsService).saveSettingName(eq(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE), anyString(), eq(LIVE_PAGE), eq(SPACE_ID), eq(9L));
  }

  @Test
  void testReadAppliesTheDrawerDefaults() throws Exception {
    Map<String, Object> settings = plugin.read(pageContext(Map.of("viewTemplate", "NewsMosaic", "showArticleDate", "false")));
    assertEquals("NewsMosaic", settings.get("view_template"));
    assertEquals("posted", settings.get("source"));
    assertEquals(3, settings.get("limit"));
    assertEquals(false, settings.get("show_article_date"));
    assertEquals(true, settings.get("show_header"));
    assertEquals(Map.of(), settings.get("header_title"));
  }

  private void assertRefused(ApplicationSettingsContext context, Map<String, Object> settings, String message) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    assertEquals(message, e.getMessage());
  }

  private CMSSetting cmsSetting(String pageReference) {
    return new CMSSetting(NewsUtils.NEWS_LIST_VIEW_CMS_TYPE, LIVE_NAME, pageReference, SPACE_ID);
  }

  private ApplicationSettingsContext pageContext(Map<String, String> preferences) {
    return new ApplicationSettingsContext(WINDOW_ID,
                                          "content/NewsListView",
                                          new HashMap<>(preferences),
                                          false,
                                          true,
                                          null,
                                          null,
                                          LIVE_PAGE,
                                          SPACE_ID,
                                          USERNAME);
  }

  private ApplicationSettingsContext siteContext() {
    return new ApplicationSettingsContext(WINDOW_ID,
                                          "content/NewsListView",
                                          new HashMap<>(),
                                          true,
                                          true,
                                          null,
                                          null,
                                          null,
                                          0,
                                          USERNAME);
  }

  private LocaleConfig localeConfig(Locale locale) {
    LocaleConfig config = mock(LocaleConfig.class);
    lenient().when(config.getLocale()).thenReturn(locale);
    return config;
  }

}
