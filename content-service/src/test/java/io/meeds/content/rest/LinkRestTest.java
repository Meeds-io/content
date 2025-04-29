/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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
package io.meeds.content.rest;

import static io.meeds.social.util.JsonUtils.toJsonString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.content.constant.LinkAlignType;
import io.meeds.content.constant.LinkDisplayType;
import io.meeds.content.model.LinkSetting;
import io.meeds.content.rest.model.LinkRestEntity;
import io.meeds.content.rest.model.LinkSettingRestEntity;
import io.meeds.content.service.LinkService;
import io.meeds.social.util.JsonUtils;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { LinkRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@RunWith(SpringRunner.class)
public class LinkRestTest { // NOSONAR

  private static final String            REST_PATH         = "/links";         // NOSONAR

  private static MockedStatic<RestUtils> REST_UTILS;                           // NOSONAR

  private static final String            SIMPLE_USER       = "simple";

  private static final String            TEST_PASSWORD     = "testPassword";

  private static final String            LINK_SETTING_NAME = "linkSettingName";

  private static final String            UPLOAD_ID         = "1234";

  @Autowired
  private SecurityFilterChain            filterChain;

  @Autowired
  private WebApplicationContext          context;

  @MockBean
  private LinkService                    linkService;

  private MockMvc                        mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
    REST_UTILS = mockStatic(RestUtils.class); // NOSONAR
  }

  @After
  public void tearDown() {
    REST_UTILS.close(); // NOSONAR
  }

  @Test
  @SneakyThrows
  public void testSaveLink() {
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, "testSaveLink", true);
    assertNotNull(linkSetting);

    ResultActions response = saveLink(false, false);
    response.andExpect(status().isForbidden());

    response = saveLink(true, false);
    response.andExpect(status().isUnauthorized());

    response = saveLink(false, true);
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  public void testGetLinkResponseCode() {
    String pageName = "testGetLink";
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertNotNull(linkSetting);

    ResultActions response = saveLink(false, true);
    response.andExpect(status().isOk());

    response = getLink(false, false);
    response.andExpect(status().isOk());

    linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, false);
    response = getLink(false, false);
    response.andExpect(status().isUnauthorized());

    response = getLink(true, false);
    response.andExpect(status().isOk());

    String eTagValue = getETagValue(response);
    response = getLinkWithETag(eTagValue, true, false);
    response.andExpect(status().isNotModified());

    response = saveLink(true, true);
    response.andExpect(status().isOk());

    LinkSetting modifiedLinkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertTrue(modifiedLinkSetting.getLastModified() > linkSetting.getLastModified());

    response = getLinkWithETag(eTagValue, true, false);
    response.andExpect(status().isOk());

    response = getLink(false, false);
    response.andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  public void testGetLinkResponseEntity() {
    String pageName = "testGetLinkResponseEntity";
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, pageName, true);
    assertNotNull(linkSetting);

    ResultActions response = saveLink(true, true);
    response.andExpect(status().isOk());

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

    ResultActions response = saveLink(true, true);
    response.andExpect(status().isOk());

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

    response = saveLink(true, true);
    response.andExpect(status().isOk());

    linkSettingRestEntity = getLinkSetting(response);
    link = linkSettingRestEntity.getLinks().get(1);

    response = getByUrlWithETag(link.getIconUrl(), eTagValue, false, false);
    response.andExpect(status().isOk());
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
    MockHttpServletRequestBuilder responseBuilder = get(url);
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
    MockHttpServletRequestBuilder responseBuilder = get(url).header("If-None-Match", eTagValue);
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

  private LinkSettingRestEntity newLinkSettingRestEntity() {
    Map<String, String> linkNames = new HashMap<>();
    linkNames.put("en", "Name-en");
    linkNames.put("fr", "Name-fr");
    Map<String, String> linkDescriptions = new HashMap<>();
    linkDescriptions.put("en", "Description-en");
    linkDescriptions.put("fr", "Description-fr");

    List<LinkRestEntity> links = new ArrayList<>();
    links.add(new LinkRestEntity(0, linkNames, linkDescriptions, "url1", "icon1", true, 2, null, 0, UPLOAD_ID));
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
    LinkSetting linkSetting = new LinkSetting(2l,
                                              linkSettingName,
                                              pageName,
                                              0l,
                                              null,
                                              LinkDisplayType.CARD,
                                              LinkAlignType.CENTER,
                                              LinkAlignType.CENTER,
                                              false,
                                              0,
                                              false,
                                              false,
                                              false,
                                              null,
                                              System.currentTimeMillis());
    if (!anonymous) {
      lenient().when(linkService.getLinkSetting(eq(linkSettingName),
                                                anyString(),
                                                argThat(identity -> identity == null || identity.getGroups().isEmpty())))
               .thenThrow(IllegalAccessException.class);
    }
    return linkSetting;
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

  private RequestPostProcessor testAdminUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("administrators"), new SimpleGrantedAuthority("users"));
  }

}
