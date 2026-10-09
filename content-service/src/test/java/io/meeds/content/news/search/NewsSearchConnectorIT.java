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
package io.meeds.content.news.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.api.ActivityStorage;

import io.meeds.content.elasticsearch.AbstractElasticsearchIT;
import io.meeds.content.news.model.filter.NewsFilter;

/**
 * Executes the query {@link NewsSearchConnector} builds from a searched text,
 * through the real {@code news-search-query.json}, against an index carrying
 * the platform's analyzers ({@code es-default-index-settings.json}, commons)
 * and the news mapping ({@code news-es-mapping.json}). A searched text that
 * the query_string parser refuses makes the engine answer an error, which the
 * connector returns as an empty result: only the engine can tell the two
 * apart.
 */
public class NewsSearchConnectorIT extends AbstractElasticsearchIT {

  private static final String     INDEX           = "it_news_" + UUID.randomUUID().toString().replace("-", "");

  private static final long       STREAM_OWNER_ID = 1L;

  private static final String     SLASH_TITLE_ID  = "1";

  private static final String     WORD_TITLE_ID   = "2";

  private static final String     SLASH_BODY_ID   = "3";

  private static final HttpClient HTTP_CLIENT     = HttpClient.newHttpClient();

  private NewsSearchConnector     newsSearchConnector;

  private int                     lastStatus;

  @BeforeAll
  static void createIndex() throws Exception {
    String settings = resource("es-default-index-settings.json").replace("shard.number", "1")
                                                                .replace("replica.number", "0")
                                                                .replace("max_regex.length", "65536");
    send("PUT", "/" + INDEX, "{\"settings\":" + settings + ",\"mappings\":" + resource("news-es-mapping.json") + "}");
    // The title is analyzed by whitespace, the other fields by the standard
    // tokenizer: each article holds its searched words in one field only
    indexArticle(SLASH_TITLE_ID, "a/b test", "Results");
    indexArticle(WORD_TITLE_ID, "Roadmap items", "Nothing here");
    indexArticle(SLASH_BODY_ID, "Release notes", "Results of the c/d test");
    send("POST", "/" + INDEX + "/_refresh", "");
  }

  @AfterAll
  static void deleteIndex() throws Exception {
    send("DELETE", "/" + INDEX, null);
  }

  @BeforeEach
  void setUp() throws Exception {
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    when(configurationManager.getInputStream(anyString())).thenAnswer(invocation -> resourceStream("news-search-query.json"));
    ActivityStorage activityStorage = mock(ActivityStorage.class);
    when(activityStorage.getStreamFeedOwnerIds(any())).thenAnswer(invocation -> new HashSet<>(Set.of(STREAM_OWNER_ID)));
    ElasticSearchingClient client = mock(ElasticSearchingClient.class);
    when(client.sendRequest(anyString(), eq(INDEX))).thenAnswer(invocation -> {
      HttpResponse<String> response = HTTP_CLIENT.send(request("POST", "/" + INDEX + "/_search", invocation.getArgument(0)),
                                                       BodyHandlers.ofString());
      lastStatus = response.statusCode();
      return response.body();
    });

    newsSearchConnector = new NewsSearchConnector();
    ReflectionTestUtils.setField(newsSearchConnector, "configurationManager", configurationManager);
    ReflectionTestUtils.setField(newsSearchConnector, "activityStorage", activityStorage);
    ReflectionTestUtils.setField(newsSearchConnector, "client", client);
    ReflectionTestUtils.setField(newsSearchConnector, "index", INDEX);
    ReflectionTestUtils.setField(newsSearchConnector, "searchQueryFilePath", "jar:/news-search-query.json");
  }

  @Test
  void testSlashIsSearchedAsTypedInTheTitle() {
    assertEquals(List.of(SLASH_TITLE_ID), search("a/b"));
    assertEquals(200, lastStatus);
  }

  @Test
  void testSlashIsSearchedAsTypedInTheBody() {
    assertEquals(List.of(SLASH_BODY_ID), search("c/d"));
    assertEquals(200, lastStatus);
    assertEquals(List.of(SLASH_BODY_ID), search("c d"));
    assertEquals(200, lastStatus);
  }

  @Test
  void testReservedCharactersAroundAWordFindTheTitleHoldingTheWord() {
    List<String> terms = List.of("roadmap", "\"roadmap\"", "(roadmap)", "[roadmap]", "+roadmap", "-roadmap", "!roadmap",
                                 "roadmap?", "roadmap:", "roadma*", "\"(roadmap)\"");
    for (String term : terms) {
      assertEquals(List.of(WORD_TITLE_ID), search(term), () -> "Searched text: " + term);
      assertEquals(200, lastStatus, () -> "The engine refused the query built from: " + term);
    }
  }

  @Test
  void testLongTextIsSearchedByItsFirstWords() {
    // 40000 words would exceed the engine's clause limit, 1024 in the harness
    assertEquals(List.of(WORD_TITLE_ID), search("roadmap items ".repeat(20000)));
    assertEquals(200, lastStatus);
  }

  @Test
  void testCostliestTextStaysUnderTheClauseLimit() {
    // One word of distinct ideographs costs three clauses per character: cut
    // at its maximum length, it stays under the engine's 1024-clause floor
    StringBuilder word = new StringBuilder();
    for (int i = 0; i < 400; i++) {
      word.appendCodePoint(0x4E00 + i);
    }
    search(word.toString());
    assertEquals(200, lastStatus);
  }

  @Test
  void testTextWithoutAnyWordFindsNothing() {
    List<String> terms = List.of("-", "!", "\"", "\"\"", "(", "()", "*", "?", "\\", "/", "&&", "<", "> <", "- * /");
    for (String term : terms) {
      assertEquals(List.of(), search(term), () -> "Searched text: " + term);
      assertEquals(200, lastStatus, () -> "The engine refused the query built from: " + term);
    }
  }

  @Test
  void testNoSearchedTextMakesTheEngineRefuseTheQuery() {
    List<String> terms = List.of("a\"b", "\"a", "a(b", "a)b", "(", "a[b", "a]b", "a{b", "a}b", "a\\", "\\", "a\\b", "a:b",
                                 "title:a", "a~", "a~2", "a^2", "a!b", "!", "-", "+", "a-b", "a+b", "a&&b", "a||b", "&", "|",
                                 "a<b", "a>b", "<", ">", "a=b", "=", "a*b", "*", "a?b", "?", "/", "//", "/a/", "a/b/c",
                                 "AND", "OR", "NOT", "a AND", "OR b", "a\"b(c/d\\e", "OR\u3000b", "OR\tb", "OR\nb",
                                 "a\u3000OR\u3000OR\u3000b", "AND\r\nb");
    for (String term : terms) {
      search(term);
      assertEquals(200, lastStatus, () -> "The engine refused the query built from: " + term);
    }
  }

  private List<String> search(String term) {
    NewsFilter filter = new NewsFilter();
    filter.setSearchText(term);
    filter.setLimit(10);
    lastStatus = 0;
    return newsSearchConnector.search(mock(Identity.class), filter).stream().map(NewsESSearchResult::getId).toList();
  }

  private static void indexArticle(String id, String title, String body) throws Exception {
    send("PUT",
         "/" + INDEX + "/_doc/" + id,
         """
             {"id": "%s", "title": "%s", "body": "%s", "summary": "", "posterName": "Root Root",
              "permissions": [%d], "lastUpdatedTime": 1}
             """.formatted(id, title, body, STREAM_OWNER_ID));
  }

  private static void send(String method, String path, String body) throws Exception {
    HttpResponse<String> response = HTTP_CLIENT.send(request(method, path, body), BodyHandlers.ofString());
    assertTrue(response.statusCode() < 300, () -> method + " " + path + " answered " + response.body());
  }

  private static HttpRequest request(String method, String path, String body) {
    return HttpRequest.newBuilder(URI.create(elasticsearchUrl() + path))
                      .header("Content-Type", "application/json")
                      .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body))
                      .build();
  }

  private static String resource(String name) throws Exception {
    try (InputStream inputStream = resourceStream(name)) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static InputStream resourceStream(String name) {
    return NewsSearchConnectorIT.class.getClassLoader().getResourceAsStream(name);
  }

}
