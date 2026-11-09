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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;

import io.meeds.content.link.constant.LinkAlignType;
import io.meeds.content.link.constant.LinkDisplayType;
import io.meeds.content.link.model.Link;
import io.meeds.content.link.model.LinkData;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.model.LinkWithIconAttachment;
import io.meeds.content.link.model.LinkWithImageContent;
import io.meeds.content.link.service.LinkService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class LinkApplicationSettingsPluginTest {

  private static final String        USERNAME       = "mary";

  private static final String        LIVE_PAGE      = "portal::classic::home";

  private static final String        LIVE_SETTING   = "Untitled-12-live";

  private static final String        SITE_HOME      = "portal::classic::welcome";

  private static final String        SITE_WITHOUT_PAGE = "not configurable before the site has a page: application";

  private static final long          SPACE_ID       = 0;

  private static final long          WINDOW_ID      = 45;

  @Mock
  private LinkService                linkService;

  @Mock
  private CMSService                 cmsService;

  @Mock
  private UserACL                    userAcl;

  @Mock
  private LocaleConfigService        localeConfigService;

  @Mock
  private ApplicationSettingsService applicationSettingsService;

  @Mock
  private Identity                   identity;

  @InjectMocks
  private LinkApplicationSettingsPlugin plugin;

  @BeforeEach
  void setUp() throws Exception {
    LocaleConfig english = localeConfig(Locale.ENGLISH);
    LocaleConfig french = localeConfig(Locale.FRENCH);
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(english, french));
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(english);
    lenient().when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    lenient().when(cmsService.hasEditPermission(identity, LIVE_PAGE, SPACE_ID)).thenReturn(true);
    // The copy is the service's: its own steps stay observable on the mock
    lenient().when(linkService.getLinkCopies(anyString())).thenCallRealMethod();
    lenient().when(linkService.toLinkCopies(any())).thenCallRealMethod();
    lenient().when(linkService.copyLinkSetting(anyString(), anyString(), anyLong(), any(), any(), any())).thenCallRealMethod();
  }

  @Test
  void testRegistersFromPostConstruct() throws NoSuchMethodException {
    plugin.init();
    verify(applicationSettingsService).addPlugin(plugin);
    assertTrue(LinkApplicationSettingsPlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class),
               "the annotation-removed mutant leaves the plugin unregistered");
  }

  @Test
  void testSchemaExposesTheDrawerVocabulary() {
    Map<String, Object> schema = plugin.getSettingsSchema();
    Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
    assertEquals(Arrays.asList("ROW", "CARD", "COLUMN"), ((Map<?, ?>) properties.get("type")).get("enum"));
    assertEquals("integer", ((Map<?, ?>) properties.get("icon_size")).get("type"));
    assertEquals(null, ((Map<?, ?>) properties.get("icon_size")).get("minimum"),
                 "a stored size below the drawer's bounds is read and accepted back");
    assertEquals(null, ((Map<?, ?>) properties.get("icon_size")).get("maximum"),
                 "a stored size above the drawer's bounds is read and accepted back");
    assertEquals(false, schema.get("additionalProperties"));
    assertEquals("Links", plugin.getPortletName());
    assertTrue(plugin.isConfigurableInSiteContainer());
  }

  @Test
  void testValidateRefusesValuesTheDrawerRefuses() {
    ApplicationSettingsContext context = context(false, Collections.emptyMap());
    assertRefused(context, Map.of("type", "GRID"), "value is not accepted for type");
    assertRefused(context, Map.of("icon_size", 11), "value is not accepted for icon_size");
    assertRefused(context, Map.of("icon_size", 61), "value is not accepted for icon_size");
    assertRefused(context, Map.of("icon_size", 20.5), "value is not accepted for icon_size");
    assertRefused(context, Map.of("show_icon", "yes"), "value is not accepted for show_icon");
    assertRefused(context, Map.of("h_align", "LEFT"), "value is not accepted for h_align");
    assertRefused(context, Map.of("header", Map.of("en", "x".repeat(51))), "value is not accepted for header");
    assertRefused(context, Map.of("header", Map.of("de", "Kopf")), "value is not accepted for header");
    assertRefused(context, Map.of("see_more", "javascript:alert(1)"), "value is not accepted for see_more");
    assertRefused(context, Map.of("unknown", true), "value is not accepted for unknown");
    assertRefused(context, Map.of("links", List.of(link(null, "fr", "Lien", "/portal"))), "value is not accepted for name");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "a b"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "intranet"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "foo.invalidtld"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "mailto:team@meeds.io"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "tel:+33612345678"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "https://-x"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "meeds.io"))), "value is not accepted for url");
    assertRefused(context, Map.of("links", List.of(link(null, "en", "Link", "ftp://meeds.io"))), "value is not accepted for url");
    assertRefused(context, Map.of("see_more", "meeds"), "value is not accepted for see_more");
    assertRefused(context, Map.of("see_more", false), "value is not accepted for see_more");
    assertRefused(context, Map.of("see_more", 5), "value is not accepted for see_more");
    assertRefused(context, Map.of("links", List.of(Map.of("name", Map.of("en", "Link")))), "value is not accepted for url");
  }

  @Test
  void testValidateAcceptsTheDrawerBounds() throws Exception {
    ApplicationSettingsContext context = context(false, Collections.emptyMap());
    Map<String, Object> settings = new HashMap<>();
    settings.put("icon_size", 60);
    settings.put("header", Map.of("en", "x".repeat(50), "fr", ""));
    settings.put("see_more", "");
    settings.put("links",
                 List.of(link(null, "en", "Path", "/portal/dw"),
                         link(null, "en", "Site", "https://meeds.io"),
                         link(null, "en", "Host", "https://intranet"),
                         link(null, "en", "Relative", "./news")));
    plugin.validate(context, settings);
  }

  @Test
  void testValidateRefusesAPageNonEditor() {
    when(cmsService.hasEditPermission(identity, LIVE_PAGE, SPACE_ID)).thenReturn(false);
    ApplicationSettingsContext context = context(false, Map.of("name", LIVE_SETTING));
    assertThrows(IllegalAccessException.class, () -> plugin.validate(context, Map.of("show_name", true)));
  }

  @Test
  void testValidateRefusesASourceTheUserCannotRead() {
    when(linkService.hasLinkSetting(LIVE_SETTING)).thenReturn(true);
    when(linkService.hasAccessPermission(LIVE_SETTING, identity)).thenReturn(false);
    ApplicationSettingsContext context = context(false, Map.of("name", LIVE_SETTING));
    assertThrows(IllegalAccessException.class, () -> plugin.validate(context, Map.of("show_name", true)));
  }

  @Test
  void testValidateRefusesALinkIdOfAnotherSetting() {
    mockSource(LIVE_PAGE, storedLink(7, 0));
    ApplicationSettingsContext context = context(false, Map.of("name", LIVE_SETTING));
    ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class,
                                             () -> plugin.validate(context,
                                                                   Map.of("links", List.of(link(8L, "en", "Link", "/a")))));
    assertEquals("does not exist: link", e.getMessage());
    assertRefused(context,
                  Map.of("links", List.of(link(7L, "en", "Link", "/a"), link(7L, "en", "Again", "/b"))),
                  "value is not accepted for id");
  }

  /**
   * The drawer stores what Autolinker links, a bare host or a mailto address
   * included: the read returns it, and sending it back unchanged keeps it
   */
  @Test
  void testStoredTargetsSentBackUnchangedAreKept() throws Exception {
    Link stored = storedLink(7, 0);
    stored.setUrl("mailto:team@meeds.io");
    mockSource(LIVE_PAGE, stored);
    LinkSetting setting = liveSetting(LIVE_PAGE);
    setting.setSeeMore("meeds.io");
    when(linkService.getLinkSettingByName(LIVE_SETTING, null, true)).thenReturn(setting);
    ApplicationSettingsContext context = context(false, Map.of("name", LIVE_SETTING));

    plugin.validate(context, Map.of("see_more", "meeds.io", "links", List.of(link(7L, "en", "Mail", "mailto:team@meeds.io"))));
    assertRefused(context, Map.of("see_more", "www.meeds.io"), "value is not accepted for see_more");
    assertRefused(context, Map.of("links", List.of(link(7L, "en", "Mail", "mailto:other@meeds.io"))), "value is not accepted for url");
  }

  @Test
  void testFirstDraftWriteCopiesTheSettingAndLeavesTheLiveOneUntouched() throws Exception {
    mockSource(LIVE_PAGE, storedLink(7, 99));
    LinkWithImageContent stored = new LinkWithImageContent(storedLink(7, 99));
    stored.setImageContent(Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 }));
    when(linkService.getLinkDataByName(LIVE_SETTING)).thenReturn(new LinkData(liveSetting(LIVE_PAGE), List.of(stored)));
    when(applicationSettingsService.isOwner(any())).thenReturn(false);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, Map.of("type", "CARD"));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(linkService).initLinkSetting(name.capture(), eq(LIVE_PAGE), eq(SPACE_ID));
    assertNotEquals(LIVE_SETTING, name.getValue());
    assertTrue(name.getValue().startsWith("Untitled-" + WINDOW_ID + "-"));
    ArgumentCaptor<LinkSetting> setting = ArgumentCaptor.forClass(LinkSetting.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Link>> links = ArgumentCaptor.forClass(List.class);
    verify(linkService).saveLinkSetting(setting.capture(), links.capture(), eq(identity));
    assertEquals(name.getValue(), setting.getValue().getName());
    assertEquals(LinkDisplayType.CARD, setting.getValue().getType());
    assertEquals(LinkAlignType.START, setting.getValue().getHAlign());
    assertEquals(1, links.getValue().size());
    Link copy = links.getValue().get(0);
    assertEquals(0, copy.getId());
    assertEquals(0, copy.getIconFileId());
    InputStream icon = assertInstanceOf(LinkWithIconAttachment.class, copy).getInputStream();
    assertNotNull(icon);
    verify(applicationSettingsService).markOwner(context, Map.of("name", name.getValue()));
  }

  /**
   * The values the read returns, sent back on a window that does not own its
   * setting, change nothing: no setting is created, no link copied, the
   * window keeps its keys
   */
  @Test
  void testReadValuesSentBackOnAWindowItDoesNotOwnWriteNothing() throws Exception {
    mockSource(LIVE_PAGE, storedLink(7, 99));
    lenient().when(linkService.getLinkSettingByName(LIVE_SETTING, null, identity)).thenReturn(liveSetting(LIVE_PAGE));
    when(linkService.getLinkDataByName(LIVE_SETTING)).thenReturn(new LinkData(liveSetting(LIVE_PAGE),
                                                                              List.of(new LinkWithImageContent(storedLink(7, 99)))));
    lenient().when(applicationSettingsService.isOwner(any())).thenReturn(false);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, plugin.read(context));
    plugin.apply(context, Map.of());

    verify(linkService, never()).initLinkSetting(anyString(), anyString(), anyLong());
    verify(linkService, never()).saveLinkSetting(any(), any(), any());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  @Test
  void testReadValuesSentBackOnAnUnkeyedWindowWriteNothing() throws Exception {
    ApplicationSettingsContext context = context(true, Map.of("data.init", initialData()));

    plugin.apply(context, plugin.read(context));

    verify(linkService, never()).initLinkSetting(anyString(), anyString(), anyLong());
    verify(linkService, never()).saveLinkSetting(any(), any(), any());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  @Test
  void testOwnedWindowIsWrittenInPlace() throws Exception {
    mockSource(LIVE_PAGE, storedLink(7, 99));
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, Map.of("links", List.of(link(7L, "en", "Renamed", "/b"), link(null, "en", "New", "/c"))));

    ArgumentCaptor<LinkSetting> setting = ArgumentCaptor.forClass(LinkSetting.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Link>> links = ArgumentCaptor.forClass(List.class);
    verify(linkService).saveLinkSetting(setting.capture(), links.capture(), eq(identity));
    assertEquals(LIVE_SETTING, setting.getValue().getName());
    assertEquals(7, links.getValue().get(0).getId());
    assertEquals(99, links.getValue().get(0).getIconFileId());
    assertEquals("Renamed", links.getValue().get(0).getName().get("en"));
    assertEquals(0, links.getValue().get(0).getOrder());
    assertEquals(0, links.getValue().get(1).getId());
    assertEquals(1, links.getValue().get(1).getOrder());
    verify(linkService, never()).initLinkSetting(anyString(), anyString(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  @Test
  void testOwnedWindowNamingAnotherPagesSettingIsCopied() throws Exception {
    mockSource("portal::classic::other", storedLink(7, 0));
    when(linkService.getLinkDataByName(LIVE_SETTING)).thenReturn(new LinkData(liveSetting("portal::classic::other"),
                                                                              List.of(new LinkWithImageContent(storedLink(7, 0)))));
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, Map.of("show_name", true));

    verify(linkService).initLinkSetting(anyString(), eq(LIVE_PAGE), eq(SPACE_ID));
    verify(applicationSettingsService).markOwner(eq(context), anyMap());
  }

  @Test
  void testNeverRenderedWindowGetsItsOwnSetting() throws Exception {
    ApplicationSettingsContext context = context(true, Collections.emptyMap());

    plugin.apply(context, Map.of("links", List.of(link(null, "en", "Docs", "/docs"))));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(linkService).initLinkSetting(name.capture(), eq(LIVE_PAGE), eq(SPACE_ID));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Link>> links = ArgumentCaptor.forClass(List.class);
    verify(linkService).saveLinkSetting(any(LinkSetting.class), links.capture(), eq(identity));
    assertEquals("/docs", links.getValue().get(0).getUrl());
    verify(applicationSettingsService).markOwner(context, Map.of("name", name.getValue()));
  }

  /**
   * In a site container the setting hangs on the site's home page the
   * context carries: the right is that page's, and the copy is keyed on it
   */
  @Test
  void testSiteContainerWindowIsKeyedOnTheSiteHomePage() throws Exception {
    when(cmsService.hasEditPermission(identity, SITE_HOME, SPACE_ID)).thenReturn(true);
    ApplicationSettingsContext context = siteContext(SITE_HOME, Collections.emptyMap());

    plugin.validate(context, Map.of("links", List.of(link(null, "en", "Docs", "/docs"))));
    plugin.apply(context, Map.of("links", List.of(link(null, "en", "Docs", "/docs"))));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(linkService).initLinkSetting(name.capture(), eq(SITE_HOME), eq(SPACE_ID));
    verify(linkService).saveLinkSetting(any(LinkSetting.class), anyList(), eq(identity));
    verify(applicationSettingsService).markOwner(context, Map.of("name", name.getValue()));
  }

  @Test
  void testSiteContainerWindowOfASiteWithoutPageIsRefusedBeforeAnyCheck() throws Exception {
    ApplicationSettingsContext context = siteContext(null, Map.of("name", LIVE_SETTING));
    Map<String, Object> settings = Map.of("links", List.of(link(null, "en", "Docs", "/docs")));

    assertRefused(context, settings, SITE_WITHOUT_PAGE);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.apply(context, settings));
    assertEquals(SITE_WITHOUT_PAGE, e.getMessage());
    verify(cmsService, never()).hasEditPermission(any(), any(), anyLong());
    verify(linkService, never()).initLinkSetting(anyString(), any(), anyLong());
    verify(linkService, never()).saveLinkSetting(any(), anyList(), any());
  }

  /**
   * A window a site draft or a copy left with no name carries the source's
   * links in data.init, as the portlet imports them at the first render: the
   * first write starts from them, its copy keeps their icons, and the
   * preference is cleared with the name the window gets
   */
  @Test
  void testFirstWriteOnAnUnkeyedWindowImportsItsInitialData() throws Exception {
    ApplicationSettingsContext context = context(true, Map.of("data.init", initialData()));

    assertEquals(List.of(Map.of("id", 7L, "name", Map.of("en", "Link"), "description", Map.of(), "url", "/a", "same_tab", false)),
                 plugin.read(context).get("links"));
    assertEquals("CARD", plugin.read(context).get("type"));
    plugin.validate(context, Map.of("links", List.of(link(7L, "en", "Renamed", "/a"), link(null, "en", "New", "/new"))));
    plugin.apply(context, Map.of("show_name", true));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(linkService).initLinkSetting(name.capture(), eq(LIVE_PAGE), eq(SPACE_ID));
    ArgumentCaptor<LinkSetting> setting = ArgumentCaptor.forClass(LinkSetting.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Link>> links = ArgumentCaptor.forClass(List.class);
    verify(linkService).saveLinkSetting(setting.capture(), links.capture(), eq(identity));
    assertEquals(name.getValue(), setting.getValue().getName());
    assertEquals(LinkDisplayType.CARD, setting.getValue().getType());
    assertTrue(setting.getValue().isShowName());
    assertEquals(1, links.getValue().size());
    Link copy = links.getValue().get(0);
    assertEquals(0, copy.getId());
    assertEquals("/a", copy.getUrl());
    assertNotNull(assertInstanceOf(LinkWithIconAttachment.class, copy).getInputStream());
    Map<String, String> keys = new HashMap<>();
    keys.put("name", name.getValue());
    keys.put("data.init", null);
    verify(applicationSettingsService).markOwner(context, keys);
    verify(linkService, never()).getLinkDataByName(anyString());
  }

  @Test
  void testOmittedHeaderAndLinksKeepTheStoredOnes() throws Exception {
    mockSource(LIVE_PAGE, storedLink(7, 99));
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, Map.of("show_name", true));

    ArgumentCaptor<LinkSetting> setting = ArgumentCaptor.forClass(LinkSetting.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Link>> links = ArgumentCaptor.forClass(List.class);
    verify(linkService).saveLinkSetting(setting.capture(), links.capture(), eq(identity));
    assertEquals(Map.of("en", "Header"), setting.getValue().getHeader());
    assertTrue(setting.getValue().isShowName());
    assertEquals(1, links.getValue().size());
    assertEquals(7, links.getValue().get(0).getId());
  }

  @Test
  void testEmptyHeaderAndSeeMoreClearThem() throws Exception {
    mockSource(LIVE_PAGE);
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, Map.of("header", Map.of(), "see_more", ""));

    ArgumentCaptor<LinkSetting> setting = ArgumentCaptor.forClass(LinkSetting.class);
    verify(linkService).saveLinkSetting(setting.capture(), any(), eq(identity));
    assertEquals(Collections.emptyMap(), setting.getValue().getHeader());
    assertEquals(null, setting.getValue().getSeeMore());
  }

  @Test
  void testReadReturnsTheDrawerOptions() throws Exception {
    LinkSetting setting = liveSetting(LIVE_PAGE);
    when(linkService.getLinkSettingByName(LIVE_SETTING, null, identity)).thenReturn(setting);
    when(linkService.getLinks(LIVE_SETTING, null, true)).thenReturn(List.of(storedLink(7, 99)));

    Map<String, Object> settings = plugin.read(context(true, Map.of("name", LIVE_SETTING)));

    assertEquals("ROW", settings.get("type"));
    assertEquals(Map.of("en", "Header"), settings.get("header"));
    assertEquals("START", settings.get("h_align"));
    List<?> links = (List<?>) settings.get("links");
    assertEquals(7L, ((Map<?, ?>) links.get(0)).get("id"));
    assertEquals("/a", ((Map<?, ?>) links.get(0)).get("url"));
  }

  @Test
  void testReadOfASettingStoredBeforeTheIconSizeShowsTheDrawersSize() throws Exception {
    LinkSetting setting = liveSetting(LIVE_PAGE);
    setting.setIconSize(0);
    when(linkService.getLinkSettingByName(LIVE_SETTING, null, identity)).thenReturn(setting);
    assertEquals(34, plugin.read(context(true, Map.of("name", LIVE_SETTING))).get("icon_size"));
    setting.setLargeIcon(true);
    assertEquals(48, plugin.read(context(true, Map.of("name", LIVE_SETTING))).get("icon_size"));
  }

  /**
   * The REST save stores any size: one outside the drawer's bounds is read as
   * stored and kept when sent back unchanged, a changed one must be in bounds
   */
  @Test
  void testStoredIconSizeSentBackUnchangedIsKept() throws Exception {
    LinkSetting stored = liveSetting(LIVE_PAGE);
    stored.setIconSize(0);
    stored.setLargeIcon(true);
    mockSource(LIVE_PAGE);
    when(linkService.getLinkSettingByName(LIVE_SETTING, null, true)).thenReturn(stored);
    when(applicationSettingsService.isOwner(any())).thenReturn(true);
    ApplicationSettingsContext context = context(true, Map.of("name", LIVE_SETTING));

    plugin.apply(context, Map.of("icon_size", 48));

    ArgumentCaptor<LinkSetting> setting = ArgumentCaptor.forClass(LinkSetting.class);
    verify(linkService).saveLinkSetting(setting.capture(), any(), eq(identity));
    assertEquals(0, setting.getValue().getIconSize());

    LinkSetting tiny = liveSetting(LIVE_PAGE);
    tiny.setIconSize(8);
    when(linkService.getLinkSettingByName(LIVE_SETTING, null, true)).thenReturn(tiny);
    plugin.validate(context, Map.of("icon_size", 8));
    assertRefused(context, Map.of("icon_size", 9), "value is not accepted for icon_size");
    tiny.setIconSize(-5);
    plugin.validate(context, Map.of("icon_size", -5));
  }

  @Test
  void testReadOfANeverRenderedWindowIsEmpty() throws Exception {
    assertEquals(null, plugin.read(context(true, Collections.emptyMap())));
  }

  private void assertRefused(ApplicationSettingsContext context, Map<String, Object> settings, String message) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    assertEquals(message, e.getMessage());
  }

  private void mockSource(String pageReference, Link... links) {
    when(linkService.hasLinkSetting(LIVE_SETTING)).thenReturn(true);
    when(linkService.hasAccessPermission(LIVE_SETTING, identity)).thenReturn(true);
    when(linkService.getLinkSettingByName(LIVE_SETTING, null, true)).thenReturn(liveSetting(pageReference));
    lenient().when(linkService.getLinks(LIVE_SETTING, null, true)).thenReturn(new ArrayList<>(Arrays.asList(links)));
  }

  private String initialData() {
    LinkSetting setting = liveSetting(LIVE_PAGE);
    setting.setType(LinkDisplayType.CARD);
    LinkWithImageContent link = new LinkWithImageContent(storedLink(7, 99));
    link.setImageContent(Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 }));
    return JsonUtils.toJsonString(new LinkData(setting, List.of(link)));
  }

  private LinkSetting liveSetting(String pageReference) {
    LinkSetting setting = new LinkSetting();
    setting.setId(3);
    setting.setName(LIVE_SETTING);
    setting.setPageReference(pageReference);
    setting.setType(LinkDisplayType.ROW);
    setting.setHAlign(LinkAlignType.START);
    setting.setVAlign(LinkAlignType.CENTER);
    setting.setHeader(new HashMap<>(Map.of("en", "Header")));
    return setting;
  }

  private Link storedLink(long id, long iconFileId) {
    return new Link(id, new HashMap<>(Map.of("en", "Link")), new HashMap<>(), "/a", null, false, 0, iconFileId);
  }

  private Map<String, Object> link(Long id, String language, String name, String url) {
    Map<String, Object> link = new LinkedHashMap<>();
    if (id != null) {
      link.put("id", id);
    }
    link.put("name", Map.of(language, name));
    link.put("url", url);
    return link;
  }

  private ApplicationSettingsContext siteContext(String homePageReference, Map<String, String> preferences) {
    return new ApplicationSettingsContext(WINDOW_ID,
                                          "content/Links",
                                          new HashMap<>(preferences),
                                          true,
                                          true,
                                          null,
                                          null,
                                          homePageReference,
                                          SPACE_ID,
                                          USERNAME);
  }

  private ApplicationSettingsContext context(boolean draft, Map<String, String> preferences) {
    return new ApplicationSettingsContext(WINDOW_ID,
                                          "content/Links",
                                          new HashMap<>(preferences),
                                          false,
                                          draft,
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
