/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.content.link.rest;

import static io.meeds.social.util.JsonUtils.toJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.mock.MockUploadService;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.upload.UploadService;

import io.meeds.content.AbstractSpringConfigurationTest;
import io.meeds.content.link.constant.LinkAlignType;
import io.meeds.content.link.constant.LinkDisplayType;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.rest.model.LinkRestEntity;
import io.meeds.content.link.rest.model.LinkSettingRestEntity;
import io.meeds.content.link.service.LinkService;
import io.meeds.social.util.JsonUtils;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

public class LinkRestTest extends AbstractSpringConfigurationTest { // NOSONAR

  private static final String            REST_PATH            = "/links";                    // NOSONAR

  private static MockedStatic<RestUtils> REST_UTILS;                                         // NOSONAR

  private static final String            USERNAME             = "simple";

  private static final String            TEST_PASSWORD        = "testPassword";

  private static final String            USERS_GROUP          = "*:/platform/users";

  private static final String            ADMINISTRATORS_GROUP = "*:/platform/administrators";

  private static final String            LINK_SETTING_NAME    = "linkSettingName";

  private static final String            MIME_TYPE            = "image/png";

  private static final String            FILE_NAME            = "cover.png";

  private static final String            UPLOAD_ID            = "1234";

  private static final String            SVG_MIME_TYPE        = "image/svg+xml";

  private static final String            SVG_FILE_NAME        = "icon.svg";

  private static final String            SVG_UPLOAD_ID        = "5678";

  private static final String            SVG_CONTENT          =
                                                    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect width=\"10\" height=\"10\"/></svg>";

  private LayoutService                  layoutService;

  private IdentityRegistry               identityRegistry;

  private MockUploadService              uploadService;

  @Before
  @Override
  public void setUp() {
    super.setUp();
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();

    linkService = getContainer().getComponentInstanceOfType(LinkService.class);
    layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    uploadService = (MockUploadService) getContainer().getComponentInstanceOfType(UploadService.class);
    REST_UTILS = mockStatic(RestUtils.class); // NOSONAR
  }

  @After
  @Override
  public void tearDown() {
    super.setUp();
    REST_UTILS.close(); // NOSONAR
  }

  @Test
  @SneakyThrows
  public void testSaveLink() {
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, "testSaveLink", true);
    assertNotNull(linkSetting);

    ResultActions response = saveLink(false, false);
    response.andExpect(status().isForbidden());

    registerInternalUser(USERNAME);
    response = saveLink(true, false);
    response.andExpect(status().isUnauthorized());

    registerAdministratorUser(USERNAME);
    response = saveLink(false, true);
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  public void testGetLinkResponseCode() {
    String pageName = "testGetLink";
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertNotNull(linkSetting);

    registerAdministratorUser(USERNAME);
    ResultActions response = saveLink(false, true);
    response.andExpect(status().isOk());

    registerAnonymousUser();
    response = getLink(false, false);
    response.andExpect(status().isOk());

    linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, false);
    response = getLink(false, false);
    response.andExpect(status().isUnauthorized());

    registerInternalUser(USERNAME);
    response = getLink(true, false);
    response.andExpect(status().isOk());

    String eTagValue = getETagValue(response);
    response = getLinkWithETag(eTagValue, true, false);
    response.andExpect(status().isNotModified());

    Thread.sleep(2); // NOSONAR wait for 10 milliseconds to have a different
    // modification timestamp after saving the link setting
    registerAdministratorUser(USERNAME);
    response = saveLink(true, true);
    response.andExpect(status().isOk());

    LinkSetting modifiedLinkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertTrue(modifiedLinkSetting.getLastModified() > linkSetting.getLastModified());

    registerInternalUser(USERNAME);
    response = getLinkWithETag(eTagValue, true, false);
    response.andExpect(status().isOk());

    registerAnonymousUser();
    response = getLink(false, false);
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  public void testGetLinkResponseEntity() {
    String pageName = "testGetLinkResponseEntity";
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertNotNull(linkSetting);

    registerAdministratorUser(USERNAME);
    ResultActions response = saveLink(true, true);
    response.andExpect(status().isOk());

    registerAnonymousUser();
    response = getLink(true, false);
    response.andExpect(status().isOk());

    LinkSettingRestEntity linkSettingRestEntity = getLinkSetting(response);
    assertTrue(linkSettingRestEntity.getId() > 0);
    assertEquals(LINK_SETTING_NAME, linkSettingRestEntity.getName());

    LinkSettingRestEntity newLinkSettingRestEntity = newLinkSettingRestEntity();
    assertEquals(newLinkSettingRestEntity.getName(), linkSettingRestEntity.getName());
    assertEquals(newLinkSettingRestEntity.getHeader(), linkSettingRestEntity.getHeader());
    assertEquals(newLinkSettingRestEntity.getSeeMore(), linkSettingRestEntity.getSeeMore());
    assertEquals(newLinkSettingRestEntity.getType(), linkSettingRestEntity.getType());
    assertEquals(newLinkSettingRestEntity.isLargeIcon(), linkSettingRestEntity.isLargeIcon());
    assertEquals(newLinkSettingRestEntity.getIconSize(), linkSettingRestEntity.getIconSize());
    assertEquals(newLinkSettingRestEntity.isShowName(), linkSettingRestEntity.isShowName());
    assertEquals(newLinkSettingRestEntity.isShowDescription(), linkSettingRestEntity.isShowDescription());

    List<LinkRestEntity> newLinks = newLinkSettingRestEntity.getLinks();
    List<LinkRestEntity> links = linkSettingRestEntity.getLinks();
    assertNotNull(links);
    assertEquals(2, links.size());
    LinkRestEntity savedLink1 = links.get(0);
    assertNotNull(savedLink1);
    assertTrue(savedLink1.getId() > 0);
    assertEquals(0l, savedLink1.getIconFileId());
    assertNull(savedLink1.getIconUrl());
    LinkRestEntity newLink1 = newLinks.get(1);
    assertEquals(newLink1.getOrder(), savedLink1.getOrder());
    assertEquals(newLink1.getName(), savedLink1.getName());
    assertEquals(newLink1.getDescription(), savedLink1.getDescription());
    assertEquals(newLink1.getUrl(), savedLink1.getUrl());
    assertEquals(newLink1.isSameTab(), savedLink1.isSameTab());

    LinkRestEntity savedLink2 = links.get(1);
    assertNotNull(savedLink2);
    assertTrue(savedLink2.getId() > 0);
    assertTrue(savedLink2.getIconFileId() > 0);
    assertNotNull(savedLink2.getIconUrl());
    LinkRestEntity newLink2 = newLinks.get(0);
    assertEquals(newLink2.getOrder(), savedLink2.getOrder());
    assertEquals(newLink2.getName(), savedLink2.getName());
    assertEquals(newLink2.getDescription(), savedLink2.getDescription());
    assertEquals(newLink2.getUrl(), savedLink2.getUrl());
    assertEquals(newLink2.isSameTab(), savedLink2.isSameTab());

    response = getLinkWithLang("fr", true, false);
    response.andExpect(status().isOk());

    linkSettingRestEntity = getLinkSetting(response);
    assertEquals(Collections.singletonMap("fr", newLinkSettingRestEntity.getHeader().get("fr")),
                 linkSettingRestEntity.getHeader());
    links = linkSettingRestEntity.getLinks();

    savedLink1 = links.get(0);
    assertEquals(Collections.singletonMap("fr", newLink1.getName().get("fr")), savedLink1.getName());
    assertEquals(Collections.singletonMap("fr", newLink1.getDescription().get("fr")), savedLink1.getDescription());

    savedLink2 = links.get(1);
    assertEquals(Collections.singletonMap("fr", newLink2.getName().get("fr")), savedLink2.getName());
    assertEquals(Collections.singletonMap("fr", newLink2.getDescription().get("fr")), savedLink2.getDescription());
  }

  @Test
  @SneakyThrows
  public void testGetLinkIconStream() {
    String pageName = "testGetLinkResponseEntity";
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertNotNull(linkSetting);

    registerAdministratorUser(USERNAME);
    ResultActions response = saveLink(true, true);
    response.andExpect(status().isOk());

    registerAnonymousUser();
    response = getLink(false, false);
    response.andExpect(status().isOk());

    LinkSettingRestEntity linkSettingRestEntity = getLinkSetting(response);
    List<LinkRestEntity> links = linkSettingRestEntity.getLinks();
    assertNotNull(links);
    assertEquals(2, links.size());
    LinkRestEntity link = links.get(1);
    assertNotNull(link);
    assertNotNull(link.getIconUrl());

    response = getByUrl(link.getIconUrl(), true, true);
    response.andExpect(status().isOk());

    byte[] bytes = response.andReturn().getResponse().getContentAsByteArray();
    assertNotNull(bytes);

    String eTagValue = getETagValue(response);
    response = getByUrlWithETag(link.getIconUrl(), eTagValue, true, true);
    response.andExpect(status().isNotModified());

    registerAdministratorUser(USERNAME);
    response = saveLink(true, true);
    response.andExpect(status().isOk());

    linkSettingRestEntity = getLinkSetting(response);
    link = linkSettingRestEntity.getLinks().get(1);

    registerAnonymousUser();
    response = getByUrlWithETag(link.getIconUrl(), eTagValue, false, false);
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  public void testGetSvgLinkIcon() {
    String pageName = "testGetSvgLinkIcon";
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertNotNull(linkSetting);

    registerAdministratorUser(USERNAME);
    LinkSettingRestEntity linkSettingEntity = newSvgLinkSettingRestEntity();
    MockHttpServletRequestBuilder responseBuilder = put(getUrl()).content(toJsonString(linkSettingEntity))
                                                                 .contentType(MediaType.APPLICATION_JSON)
                                                                 .with(testAdminUser())
                                                                 .with(testSimpleUser());
    mockMvc.perform(responseBuilder).andExpect(status().isOk());

    registerAnonymousUser();
    ResultActions response = getLink(false, false);
    response.andExpect(status().isOk());
    LinkSettingRestEntity linkSettingRestEntity = getLinkSetting(response);
    LinkRestEntity link = linkSettingRestEntity.getLinks().get(0);
    assertNotNull(link);
    assertNotNull(link.getIconUrl());

    // An SVG icon must be served with its original content type, otherwise the
    // browser can't render it.
    response = getByUrl(link.getIconUrl(), true, true);
    response.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf(SVG_MIME_TYPE)));
    byte[] bytes = response.andReturn().getResponse().getContentAsByteArray();
    assertEquals(SVG_CONTENT, new String(bytes, StandardCharsets.UTF_8));
  }

  private String getUrl() {
    return REST_PATH + "/" + LINK_SETTING_NAME;
  }

  @SneakyThrows
  private ResultActions saveLink(boolean user, boolean admin) {
    LinkSettingRestEntity linkSettingEntity = newLinkSettingRestEntity();
    MockHttpServletRequestBuilder responseBuilder = put(getUrl()).content(toJsonString(linkSettingEntity))
                                                                 .contentType(MediaType.APPLICATION_JSON);
    if (admin) {
      responseBuilder.with(testAdminUser());
    }
    if (user) {
      responseBuilder.with(testSimpleUser());
    }
    return mockMvc.perform(responseBuilder);
  }

  private ResultActions getLink(boolean user, boolean admin) {
    return getByUrl(getUrl(), user, admin);
  }

  private ResultActions getLinkWithLang(String lang, boolean user, boolean admin) {
    return getByUrl(getUrl() + "?lang=" + lang, user, admin);
  }

  @SneakyThrows
  private ResultActions getByUrl(String url, boolean user, boolean admin) {
    MockHttpServletRequestBuilder responseBuilder = get(url.replace("/content/rest", ""));
    if (admin) {
      responseBuilder.with(testAdminUser());
    }
    if (user) {
      responseBuilder.with(testSimpleUser());
    }
    return mockMvc.perform(responseBuilder);
  }

  private ResultActions getLinkWithETag(String eTagValue, boolean user, boolean admin) {
    return getByUrlWithETag(getUrl(), eTagValue, user, admin);
  }

  @SneakyThrows
  private ResultActions getByUrlWithETag(String url, String eTagValue, boolean user, boolean admin) {
    MockHttpServletRequestBuilder responseBuilder = get(url.replace("/content/rest", "")).header("If-None-Match", eTagValue);
    if (admin) {
      responseBuilder.with(testAdminUser());
    }
    if (user) {
      responseBuilder.with(testSimpleUser());
    }
    return mockMvc.perform(responseBuilder);
  }

  private String getETagValue(ResultActions response) {
    return response.andReturn().getResponse().getHeader("eTag");
  }

  @SneakyThrows
  private LinkSettingRestEntity getLinkSetting(ResultActions response) {
    return getContent(response, LinkSettingRestEntity.class);
  }

  @SneakyThrows
  private <T> T getContent(ResultActions response, Class<T> type) {
    return JsonUtils.fromJsonString(response.andReturn().getResponse().getContentAsString(), type);
  }

  @SneakyThrows
  private LinkSettingRestEntity newLinkSettingRestEntity() {
    Map<String, String> linkNames = new HashMap<>();
    linkNames.put("en", "Name-en");
    linkNames.put("fr", "Name-fr");
    Map<String, String> linkDescriptions = new HashMap<>();
    linkDescriptions.put("en", "Description-en");
    linkDescriptions.put("fr", "Description-fr");

    List<LinkRestEntity> links = new ArrayList<>();
    links.add(new LinkRestEntity(0, linkNames, linkDescriptions, "url1", "icon1", true, 2, null, 0, UPLOAD_ID));
    uploadResource();
    links.add(new LinkRestEntity(0, linkNames, linkDescriptions, "url2", "icon2", false, 1, null, 0, null));

    Map<String, String> linkHeaders = new HashMap<>();
    linkHeaders.put("en", "Header-en");
    linkHeaders.put("fr", "Header-fr");
    return new LinkSettingRestEntity(0,
                                     LINK_SETTING_NAME,
                                     linkHeaders,
                                     LinkDisplayType.CARD,
                                     LinkAlignType.CENTER,
                                     LinkAlignType.END,
                                     true,
                                     12,
                                     true,
                                     true,
                                     true,
                                     "#SeeMore",
                                     links);
  }

  @SneakyThrows
  private LinkSetting initLinkSetting(String linkSettingName, String pageName, boolean anonymous) {
    String pageId = createPage(pageName, anonymous ? UserACL.EVERYONE : USERS_GROUP, ADMINISTRATORS_GROUP);
    return linkService.initLinkSetting(linkSettingName, pageId, 0l);
  }

  private String createPage(String pageName, String accessPermission, String editPermission) {
    String siteType = "portal";
    String siteName = "test";
    if (layoutService.getPortalConfig(siteName) == null) {
      PortalConfig portal = new PortalConfig();
      portal.setType(siteType);
      portal.setName(siteName);
      portal.setLocale("en");
      portal.setLabel("Test");
      portal.setDescription("Test");
      portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
      layoutService.create(portal);
    }

    PageKey pageKey = new PageKey(siteType, siteName, pageName);
    PageState pageState = new PageState(pageName,
                                        null,
                                        false,
                                        null,
                                        Collections.singletonList(accessPermission),
                                        editPermission);
    layoutService.save(new PageContext(pageKey, pageState));
    return pageKey.format();
  }

  @SneakyThrows
  private void uploadResource() {
    File tempFile = File.createTempFile("image", "temp");
    uploadService.createUploadResource(UPLOAD_ID, tempFile.getPath(), FILE_NAME, MIME_TYPE);
  }

  @SneakyThrows
  private void uploadSvgResource() {
    File tempFile = File.createTempFile("icon", ".svg");
    Files.writeString(tempFile.toPath(), SVG_CONTENT);
    uploadService.createUploadResource(SVG_UPLOAD_ID, tempFile.getPath(), SVG_FILE_NAME, SVG_MIME_TYPE);
  }

  @SneakyThrows
  private LinkSettingRestEntity newSvgLinkSettingRestEntity() {
    Map<String, String> linkNames = new HashMap<>();
    linkNames.put("en", "Name-en");
    Map<String, String> linkDescriptions = new HashMap<>();
    linkDescriptions.put("en", "Description-en");

    List<LinkRestEntity> links = new ArrayList<>();
    links.add(new LinkRestEntity(0, linkNames, linkDescriptions, "url1", "icon1", true, 1, null, 0, SVG_UPLOAD_ID));
    uploadSvgResource();

    Map<String, String> linkHeaders = new HashMap<>();
    linkHeaders.put("en", "Header-en");
    return new LinkSettingRestEntity(0,
                                     LINK_SETTING_NAME,
                                     linkHeaders,
                                     LinkDisplayType.CARD,
                                     LinkAlignType.CENTER,
                                     LinkAlignType.END,
                                     true,
                                     12,
                                     true,
                                     true,
                                     true,
                                     "#SeeMore",
                                     links);
  }

  private void registerAnonymousUser() {
    resetRestUtils();
  }

  private org.exoplatform.services.security.Identity registerAdministratorUser(String user) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(user,
                                                                                                       Arrays.asList(MembershipEntry.parse(ADMINISTRATORS_GROUP),
                                                                                                                     MembershipEntry.parse(USERS_GROUP)));
    identityRegistry.register(identity);
    ConversationState.setCurrent(new ConversationState(identity));
    resetRestUtils();
    REST_UTILS.when(RestUtils::getCurrentUser).thenReturn(USERNAME);
    REST_UTILS.when(RestUtils::getCurrentUserAclIdentity).thenReturn(identity);
    return identity;
  }

  private org.exoplatform.services.security.Identity registerInternalUser(String username) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(username,
                                                                                                       Arrays.asList(MembershipEntry.parse(USERS_GROUP)));
    identityRegistry.register(identity);
    ConversationState.setCurrent(new ConversationState(identity));
    resetRestUtils();
    REST_UTILS.when(RestUtils::getCurrentUser).thenReturn(USERNAME);
    REST_UTILS.when(RestUtils::getCurrentUserAclIdentity).thenReturn(identity);
    return identity;
  }

  private RequestPostProcessor testSimpleUser() {
    return user(USERNAME).password(TEST_PASSWORD)
                         .authorities(new SimpleGrantedAuthority("users"));
  }

  private RequestPostProcessor testAdminUser() {
    return user(USERNAME).password(TEST_PASSWORD)
                         .authorities(new SimpleGrantedAuthority("administrators"), new SimpleGrantedAuthority("users"));
  }

  private void resetRestUtils() {
    REST_UTILS.reset();
    REST_UTILS.when(RestUtils::getBaseRestUrl).thenReturn("");
  }

}
