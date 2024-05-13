/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.news.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.jpa.search.ActivitySearchConnector;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.ActivityStorage;

import io.meeds.news.filter.NewsFilter;

@RunWith(MockitoJUnitRunner.class)
public class NewsSearchConnectorTest {

  private static final String ES_INDEX        = "news_alias";

  public static final String  FAKE_ES_QUERY   =
                                            "{offset: @offset@, limit: @limit@, term1: @term@, term2: @term@, permissions: @permissions@}";

  @Mock
  IdentityManager             identityManager;

  @Mock
  ActivityStorage             activityStorage;

  @Mock
  ConfigurationManager        configurationManager;

  @Mock
  ElasticSearchingClient      client;

  String                      searchResult    = null;

  boolean                     developingValue = false;

  @Before
  public void setUp() throws Exception {// NOSONAR
    searchResult = IOUtil.getStreamContentAsString(getClass().getClassLoader().getResourceAsStream("news-search-result.json"));

    try {
      Mockito.reset(configurationManager);
      lenient().when(configurationManager.getInputStream("FILE_PATH"))
               .thenReturn(new ByteArrayInputStream(FAKE_ES_QUERY.getBytes()));
    } catch (Exception e) {
      throw new IllegalStateException("Error retrieving ES Query content", e);
    }
    developingValue = PropertyManager.isDevelopping();
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "false");
    PropertyManager.refresh();
  }

  @After
  public void tearDown() {
    PropertyManager.setProperty(PropertyManager.DEVELOPING, String.valueOf(developingValue));
    PropertyManager.refresh();
  }

  @Test
  public void testSearchArguments() {
    NewsSearchConnector newsSearchConnector = new NewsSearchConnector(configurationManager,
                                                                            identityManager,
                                                                            activityStorage,
                                                                            client,
                                                                            getParams());
    NewsFilter filter = new NewsFilter();
    filter.setSearchText("term");
    filter.setLimit(0);
    filter.setOffset(10);
    try {
      newsSearchConnector.search(null, filter);
      fail("Should throw IllegalArgumentException: viewer identity is mandatory");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    Identity identity = mock(Identity.class);
    lenient().when(identity.getId()).thenReturn("1");
    try {
      NewsFilter filter2 = new NewsFilter();
      filter.setSearchText("term");
      filter.setLimit(-1);
      filter.setOffset(10);
      newsSearchConnector.search(identity, filter2);
      fail("Should throw IllegalArgumentException: limit should be positive");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      NewsFilter filter3 = new NewsFilter();
      filter.setSearchText("term");
      filter.setLimit(0);
      filter.setOffset(-1);
      newsSearchConnector.search(identity, filter3);
      fail("Should throw IllegalArgumentException: offset should be positive");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testSearchNoResult() {
    NewsSearchConnector newsSearchConnector = new NewsSearchConnector(configurationManager,
                                                                            identityManager,
                                                                            activityStorage,
                                                                            client,
                                                                            getParams());

    NewsFilter filter = new NewsFilter();
    filter.setSearchText("term");
    filter.setLimit(10);
    filter.setOffset(0);

    HashSet<Long> permissions = new HashSet<>(Arrays.asList(10L, 20L, 30L));
    Identity identity = mock(Identity.class);
    lenient().when(identity.getId()).thenReturn("1");
    lenient().when(activityStorage.getStreamFeedOwnerIds(eq(identity))).thenReturn(permissions);
    String expectedESQuery = FAKE_ES_QUERY
                                          .replaceAll("@term_query@",
                                                      ActivitySearchConnector.SEARCH_QUERY_TERM.replace("@term@",
                                                                                                        filter.getSearchText())
                                                                                               .replace("@term_query@",
                                                                                                        filter.getSearchText()))
                                          .replaceAll("@permissions@", StringUtils.join(permissions, ","))
                                          .replaceAll("@offset@", "0")
                                          .replaceAll("@limit@", "10");
    lenient().when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX))).thenReturn("{}");

    List<NewsESSearchResult> result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testSearchWithResult() {
    NewsSearchConnector newsSearchConnector = new NewsSearchConnector(configurationManager,
                                                                            identityManager,
                                                                            activityStorage,
                                                                            client,
                                                                            getParams());

    NewsFilter filter = new NewsFilter();
    filter.setSearchText("term");
    filter.setLimit(10);
    filter.setOffset(0);

    HashSet<Long> permissions = new HashSet<>(Arrays.asList(10L, 20L, 30L));
    Identity identity = mock(Identity.class);
    lenient().when(identity.getId()).thenReturn("1");
    lenient().when(activityStorage.getStreamFeedOwnerIds(eq(identity))).thenReturn(permissions);
    String expectedESQuery = FAKE_ES_QUERY
                                          .replaceAll("@term_query@",
                                                      ActivitySearchConnector.SEARCH_QUERY_TERM.replace("@term@",
                                                                                                        filter.getSearchText())
                                                                                               .replace("@term_query@",
                                                                                                        filter.getSearchText()))
                                          .replaceAll("@permissions@", StringUtils.join(permissions, ","))
                                          .replaceAll("@offset@", "0")
                                          .replaceAll("@limit@", "10");
    lenient().when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX))).thenReturn(searchResult);

    Identity rootIdentity = new Identity("organization", "root");
    lenient().when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "posterId")).thenReturn(rootIdentity);

    List<NewsESSearchResult> result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(2, result.size());

    NewsESSearchResult newsESSearchResult = result.iterator().next();
    assertEquals("6", newsESSearchResult.getId());
    assertEquals(1592227545758L, newsESSearchResult.getPostedTime());
    assertEquals(1592227545758L, newsESSearchResult.getLastUpdatedTime());
    assertNotNull(newsESSearchResult.getExcerpts());
  }

  @Test
  public void testSearchWithIdentityResult() throws IOException {// NOSONAR
    NewsSearchConnector newsSearchConnector = new NewsSearchConnector(configurationManager,
                                                                            identityManager,
                                                                            activityStorage,
                                                                            client,
                                                                            getParams());

    NewsFilter filter = new NewsFilter();
    filter.setSearchText("john");
    filter.setLimit(10);
    filter.setOffset(0);

    HashSet<Long> permissions = new HashSet<>(Arrays.asList(10L, 20L, 30L));
    Identity identity = mock(Identity.class);
    lenient().when(identity.getId()).thenReturn("1");
    lenient().when(activityStorage.getStreamFeedOwnerIds(eq(identity))).thenReturn(permissions);
    String expectedESQuery = FAKE_ES_QUERY
                                          .replaceAll("@term_query@",
                                                      ActivitySearchConnector.SEARCH_QUERY_TERM.replace("@term@",
                                                                                                        filter.getSearchText())
                                                                                               .replace("@term_query@",
                                                                                                        filter.getSearchText()))
                                          .replaceAll("@permissions@", StringUtils.join(permissions, ","))
                                          .replaceAll("@offset@", "0")
                                          .replaceAll("@limit@", "10");
    searchResult = IOUtil.getStreamContentAsString(getClass().getClassLoader()
                                                             .getResourceAsStream("news-search-result-by-identity.json"));
    lenient().when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX))).thenReturn(searchResult);

    Identity poster = new Identity(OrganizationIdentityProvider.NAME, "posterId");
    lenient().when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "posterId")).thenReturn(poster);

    List<NewsESSearchResult> result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(1, result.size());

    NewsESSearchResult newsESSearchResult = result.iterator().next();
    assertEquals("6", newsESSearchResult.getId());
    assertEquals(1592227545758L, newsESSearchResult.getPostedTime());
    assertEquals(1592227545758L, newsESSearchResult.getLastUpdatedTime());
    assertNotNull(newsESSearchResult.getExcerpts());
    assertEquals(0, newsESSearchResult.getExcerpts().size());
  }

  private InitParams getParams() {
    InitParams params = new InitParams();
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setName("constructor.params");
    propertiesParam.setProperty("index", ES_INDEX);

    ValueParam valueParam = new ValueParam();
    valueParam.setName("query.file.path");
    valueParam.setValue("FILE_PATH");

    params.addParameter(propertiesParam);
    params.addParameter(valueParam);
    return params;
  }

}
