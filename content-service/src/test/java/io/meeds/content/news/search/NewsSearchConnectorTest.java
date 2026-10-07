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
package io.meeds.content.news.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.jpa.search.ActivitySearchConnector;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.ActivityStorage;

import io.meeds.content.news.model.filter.NewsFilter;

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

  @InjectMocks
  NewsSearchConnector         newsSearchConnector;

  String                      searchResult    = null;

  boolean                     developingValue = false;

  @Before
  public void setUp() throws Exception {// NOSONAR
    openMocks(this);
    // set filed injected by the @Value annotation
    ReflectionTestUtils.setField(newsSearchConnector, "index", "news_alias");
    ReflectionTestUtils.setField(newsSearchConnector, "searchType", "news");
    ReflectionTestUtils.setField(newsSearchConnector, "searchQueryFilePath", "jar:/news-search-query.json");
    searchResult = IOUtil.getStreamContentAsString(getClass().getClassLoader().getResourceAsStream("news-search-result.json"));

    try {
      Mockito.reset(configurationManager);
      lenient().when(configurationManager.getInputStream(anyString()))
               .thenReturn(new ByteArrayInputStream(FAKE_ES_QUERY.getBytes()));
    } catch (Exception e) {
      throw new IllegalStateException("Error retrieving ES Query content", e);
    }
    developingValue = PropertyManager.isDevelopping();
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "false");
    PropertyManager.refresh();
    newsSearchConnector.init();
  }

  @After
  public void tearDown() {
    PropertyManager.setProperty(PropertyManager.DEVELOPING, String.valueOf(developingValue));
    PropertyManager.refresh();
  }

  @Test
  public void testSearchArguments() {
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

    List<NewsESSearchResult> result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(2, result.size());

    NewsESSearchResult newsESSearchResult = result.iterator().next();
    assertEquals("6", newsESSearchResult.getId());
    assertNotNull(newsESSearchResult.getExcerpts());
  }

  @Test
  public void testSearchWithIdentityResult() throws IOException {// NOSONAR
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
    String expectedSearchResult = IOUtil.getStreamContentAsString(getClass().getClassLoader()
                                                             .getResourceAsStream("news-search-result-by-identity.json"));
    lenient().when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX))).thenReturn(expectedSearchResult);

    List<NewsESSearchResult> result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(1, result.size());

    NewsESSearchResult newsESSearchResult = result.iterator().next();
    assertEquals("6", newsESSearchResult.getId());
    assertNotNull(newsESSearchResult.getExcerpts());
    assertEquals(0, newsESSearchResult.getExcerpts().size());
  }

  @Test
  public void testSearchWithSpaceResult() {// NOSONAR
    NewsFilter filter = new NewsFilter();
    filter.setSearchText("john");
    filter.setLimit(10);
    filter.setOffset(0);

    Long allowedSpaceStreamOwnerId = 10L;
    Long notAllowedSpaceStreamOwnerId = 50L;

    filter.setSpaces(Arrays.asList(Long.toString(allowedSpaceStreamOwnerId)));

    HashSet<Long> permissions = new HashSet<>(Arrays.asList(10L, 20L, 30L));
    Identity identity = mock(Identity.class);
    lenient().when(identity.getId()).thenReturn("1");
    lenient().when(activityStorage.getStreamFeedOwnerIds(eq(identity))).thenReturn(permissions);
    Set<Long> expectedPermissionsSet = new HashSet<>(Arrays.asList(allowedSpaceStreamOwnerId));
    String expectedESQuery = FAKE_ES_QUERY
                                          .replaceAll("@term_query@",
                                                      ActivitySearchConnector.SEARCH_QUERY_TERM.replace("@term@",
                                                                                                        filter.getSearchText())
                                                                                               .replace("@term_query@",
                                                                                                        filter.getSearchText()))
                                          .replaceAll("@permissions@", StringUtils.join(expectedPermissionsSet, ","))
                                          .replaceAll("@offset@", "0")
                                          .replaceAll("@limit@", "10");
    lenient().when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX))).thenReturn(searchResult);

    List<NewsESSearchResult> result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(2, result.size());

    //
    filter.setSpaces(Arrays.asList(Long.toString(notAllowedSpaceStreamOwnerId)));
    expectedPermissionsSet = new HashSet<>();

    expectedESQuery = FAKE_ES_QUERY
                                   .replaceAll("@term_query@",
                                               ActivitySearchConnector.SEARCH_QUERY_TERM.replace("@term@", filter.getSearchText())
                                                                                        .replace("@term_query@",
                                                                                                 filter.getSearchText()))
                                   .replaceAll("@permissions@", StringUtils.join(expectedPermissionsSet, ","))
                                   .replaceAll("@offset@", "0")
                                   .replaceAll("@limit@", "10");
    lenient().when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX))).thenReturn("{}");

    result = newsSearchConnector.search(identity, filter);
    assertNotNull(result);
    assertEquals(0, result.size());

  }

  @Test
  public void testBuildTermQueryStatementAddsWildcardPerWord() {
    // Without a trailing wildcard on each word, query_string only matches a
    // whole token - searching by the first few letters of a title/body/
    // summary word (while the user is still typing) would return nothing.
    String termQuery = ReflectionTestUtils.invokeMethod(newsSearchConnector, "buildTermQueryStatement", "ab cd");

    assertNotNull(termQuery);
    String expectedFragment = NewsSearchConnector.SEARCH_QUERY_TERM.replace("@term@", "ab* cd*");
    assertEquals(expectedFragment, termQuery);
  }

  @Test
  public void testBuildTermQueryStatementReturnsEmptyForBlankTerm() {
    String termQuery = ReflectionTestUtils.invokeMethod(newsSearchConnector, "buildTermQueryStatement", "   ");
    assertEquals("", termQuery);
  }

  @Test
  public void testBuildTermQueryStatementAnalyzesWildcardSoCapitalizedTermsStillMatch() {
    // query_string defaults to analyze_wildcard=false, so a wildcarded term
    // (added above for prefix matching) would otherwise skip the field
    // analyzer's lowercasing and never match a capitalized search term
    // (e.g. mobile keyboards auto-capitalizing the first word) against the
    // lowercased index tokens.
    String termQuery = ReflectionTestUtils.invokeMethod(newsSearchConnector, "buildTermQueryStatement", "Meeds");

    assertNotNull(termQuery);
    assertTrue(termQuery.contains("\"analyze_wildcard\": true"));
  }

  @Test
  public void testBuildTermQueryStatementEscapesJsonSpecialCharacters() throws Exception {// NOSONAR
    // The term lands inside a JSON string: an unescaped quote would close it
    // and let the searched text rewrite the query around the permissions
    String term = "a\"b\\c\"}},{\"match_all\":{";
    assertEquals("a\\\"b\\\\c\\\"\\}\\},\\{\\\"match_all*", queryStringOf(term));
  }

  @Test
  public void testBuildTermQueryStatementSearchesQueryStringSyntaxAsTyped() {
    // A slash opens a regular expression, an unbalanced quote or parenthesis
    // fails the query: the engine refuses it and the search shows nothing
    assertEquals("a\\/b* a\\\"b* a\\(b*", queryStringOf("a/b a\"b a(b"));
  }

  @Test
  public void testBuildTermQueryStatementEscapesEachReservedCharacterOnce() {
    String reserved = "\\+-=&|!(){}[]^\"~*?:/";
    StringBuilder expected = new StringBuilder("a");
    reserved.chars().forEach(c -> expected.append('\\').append((char) c));
    assertEquals(expected + "b*", queryStringOf("a" + reserved + "b"));
  }

  @Test
  public void testBuildTermQueryStatementSeparatesWordsOnReservedCharactersAroundThem() {
    // The title field keeps punctuation in its tokens: a quoted word searched
    // with its quotes would miss a title holding the word
    assertEquals("roadmap* roadmap* roadmap* roadmap* roadmap* relea*",
                 queryStringOf("\"roadmap\" (roadmap) +roadmap -roadmap roadmap? relea*"));
  }

  @Test
  public void testBuildTermQueryStatementSplitsWordsOnEveryWhitespace() {
    // query_string also separates terms on these: OR joined to b by an
    // ideographic space would reach it as a bare operator
    assertEquals("OR* b* c* d* e*", queryStringOf("OR\u3000b\tc\nd\r\ne"));
  }

  @Test
  public void testBuildTermQueryStatementCutsTheSearchedTextAtItsMaximumLength() {
    // 256 characters: 85 words "ab", then the first letter of the 86th
    assertEquals("ab* ".repeat(85) + "a*", queryStringOf("ab ".repeat(200)));
    // Counted in code points: a character outside the BMP is never split
    String emoji = new String(Character.toChars(0x1F600));
    assertEquals(emoji.repeat(256) + "*", queryStringOf(emoji.repeat(300)));
    // 400 chars but 200 code points: under the maximum length, not cut
    assertEquals(emoji.repeat(200) + "*", queryStringOf(emoji.repeat(200)));
  }

  @Test
  public void testBuildTermQueryStatementDropsWordsMadeOfReservedCharactersOnly() {
    assertEquals("a* b*", queryStringOf("a - \"\" b"));
    assertEquals("", queryStringOf("- ( \\ *"));
  }

  @Test
  public void testBuildTermQueryStatementSeparatesWordsOnCharactersQueryStringCannotEscape() {
    assertEquals("a* b* c*", queryStringOf("a<b>c"));
  }

  @Test
  public void testBuildTagsQueryStatementEscapesJsonSpecialCharacters() throws Exception {// NOSONAR
    String tag = "tag\"}}],\"must_not\":[{\"x";
    String tagsQuery = ReflectionTestUtils.invokeMethod(newsSearchConnector,
                                                        "buildTagsQueryStatement",
                                                        Collections.singletonList(tag));

    JSONObject parsed = (JSONObject) new JSONParser().parse("{" + tagsQuery.substring(1) + "}");
    JSONArray should = (JSONArray) parsed.get("should");
    assertEquals(1, should.size());
    JSONObject tagTerm = (JSONObject) ((JSONObject) ((JSONObject) should.get(0)).get("term"))
                                                                              .get("metadatas.tags.metadataName.keyword");
    assertEquals(tag, tagTerm.get("value"));
    assertNull(parsed.get("must_not"));
  }

  @Test
  public void testBuildQueryStatementDoesNotResubstitutePlaceholdersTypedByTheUser() throws Exception {// NOSONAR
    String template = IOUtil.getStreamContentAsString(getClass().getClassLoader().getResourceAsStream("news-search-query.json"));
    ReflectionTestUtils.setField(newsSearchConnector, "searchQuery", template);
    NewsFilter filter = new NewsFilter();
    filter.setSearchText("@tags_query@");
    filter.setTagNames(Collections.singletonList("@sortQuery@"));
    filter.setLimit(10);
    Identity identity = mock(Identity.class);

    String query = ReflectionTestUtils.invokeMethod(newsSearchConnector,
                                                    "buildQueryStatement",
                                                    identity,
                                                    new HashSet<>(Arrays.asList(10L, 20L)),
                                                    filter);

    JSONObject bool = (JSONObject) ((JSONObject) ((JSONObject) new JSONParser().parse(query)).get("query")).get("bool");
    JSONObject queryString = (JSONObject) ((JSONObject) bool.get("must")).get("query_string");
    assertEquals("@tags_query@*", queryString.get("query"));
    JSONObject tagTerm = (JSONObject) ((JSONObject) ((JSONObject) ((JSONArray) bool.get("should")).get(0)).get("term"))
                                                                                                          .get("metadatas.tags.metadataName.keyword");
    assertEquals("@sortQuery@", tagTerm.get("value"));
    JSONObject permissions = (JSONObject) ((JSONObject) ((JSONArray) bool.get("filter")).get(0)).get("terms");
    assertTrue(((JSONArray) permissions.get("permissions")).containsAll(Arrays.asList(10L, 20L)));
  }

  @Test
  public void testBuildSortQueryStatementAcceptsOnlyAscOrDesc() throws Exception {// NOSONAR
    NewsFilter filter = new NewsFilter();
    filter.setSortField("date");

    filter.setSortDirection("asc\"}},{\"_script\":{\"x\":\"");
    assertEquals("desc", getSortOrder(filter));
    filter.setSortDirection(null);
    assertEquals("desc", getSortOrder(filter));
    filter.setSortDirection("ASC");
    assertEquals("asc", getSortOrder(filter));
    filter.setSortDirection("desc");
    assertEquals("desc", getSortOrder(filter));
  }

  private String queryStringOf(String term) {
    String termQuery = ReflectionTestUtils.invokeMethod(newsSearchConnector, "buildTermQueryStatement", term);
    JsonNode parsed = new ObjectMapper().readTree("{" + termQuery.substring(0, termQuery.lastIndexOf(',')) + "}");
    return parsed.path("must").path("query_string").path("query").asString();
  }

  private String getSortOrder(NewsFilter filter) throws Exception {
    String sortQuery = ReflectionTestUtils.invokeMethod(newsSearchConnector, "buildSortQueryStatement", filter);
    JSONArray sort = (JSONArray) new JSONParser().parse("[" + sortQuery + "]");
    assertEquals(2, sort.size());
    return (String) ((JSONObject) ((JSONObject) sort.get(0)).get("lastUpdatedDate")).get("order");
  }
}
