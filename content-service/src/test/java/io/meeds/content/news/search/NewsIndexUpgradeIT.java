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
import static org.mockito.ArgumentMatchers.anyLong;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ActivityStreamImpl;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.MetadataFilter;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;

import io.meeds.content.elasticsearch.AbstractElasticsearchIT;
import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

/**
 * Plays the upgrade of the news index against a real engine: the previous
 * index is copied into the new one (what the engine-side reindex does), then
 * the documents the connector lists with {@code getAllIds} are built again
 * from the database and indexed (what {@code reindexOnUpgrade} asks the
 * indexing processor for). An article the engine rejected before the fix is
 * absent from the previous index: only the second step brings it back.
 */
public class NewsIndexUpgradeIT extends AbstractElasticsearchIT {

  private static final String     SUFFIX          = UUID.randomUUID().toString().replace("-", "");

  private static final String     PREVIOUS_INDEX  = "it_news_previous_" + SUFFIX;

  private static final String     CURRENT_INDEX   = "it_news_current_" + SUFFIX;

  private static final String     INDEXED_ID      = "10";

  private static final String     MISSING_ID      = "11";

  private static final HttpClient HTTP_CLIENT     = HttpClient.newHttpClient();

  @AfterAll
  static void deleteIndexes() throws Exception {
    send("DELETE", "/" + PREVIOUS_INDEX + "," + CURRENT_INDEX, null);
  }

  @Test
  void testArticleMissingFromThePreviousIndexIsIndexedAgainAtUpgrade() throws Exception {
    String settings = resource("es-default-index-settings.json").replace("shard.number", "1")
                                                                .replace("replica.number", "0")
                                                                .replace("max_regex.length", "65536");
    String mapping = resource("news-es-mapping.json");
    send("PUT", "/" + PREVIOUS_INDEX, "{\"settings\":" + settings + ",\"mappings\":" + mapping + "}");
    send("PUT", "/" + CURRENT_INDEX, "{\"settings\":" + settings + ",\"mappings\":" + mapping + "}");
    send("PUT",
         "/" + PREVIOUS_INDEX + "/_doc/" + INDEXED_ID + "?refresh=true",
         "{\"id\": \"10\", \"title\": \"Indexed before\", \"permissions\": [5], \"lastUpdatedTime\": 1}");

    // Engine-side copy of the previous index
    send("POST",
         "/_reindex?refresh=true",
         "{\"source\":{\"index\":\"" + PREVIOUS_INDEX + "\"},\"dest\":{\"index\":\"" + CURRENT_INDEX + "\"}}");
    assertEquals(List.of(INDEXED_ID), idsOf(CURRENT_INDEX));

    // Rebuild from the database, as the indexing processor does for each listed id
    NewsIndexingServiceConnector connector = connector();
    List<String> listed = new ArrayList<>();
    int offset = 0;
    List<String> page;
    do {
      page = connector.getAllIds(offset, 2);
      listed.addAll(page);
      offset += 2;
    } while (page.size() == 2);
    assertEquals(List.of(INDEXED_ID, MISSING_ID, MISSING_ID + "-fr"), listed);
    for (String id : listed) {
      Document document = connector.create(id);
      send("PUT", "/" + CURRENT_INDEX + "/_doc/" + id + "?refresh=true", document.toJSON());
    }

    assertEquals(List.of(INDEXED_ID, MISSING_ID, MISSING_ID + "-fr"), idsOf(CURRENT_INDEX));
  }

  private NewsIndexingServiceConnector connector() throws Exception {
    NewsService newsService = mock(NewsService.class);
    IdentityManager identityManager = mock(IdentityManager.class);
    ActivityManager activityManager = mock(ActivityManager.class);
    MetadataService metadataService = mock(MetadataService.class);

    when(metadataService.getMetadataItemsByFilter(any(MetadataFilter.class), eq(0L), anyLong())).thenReturn(List.of(item(INDEXED_ID),
                                                                                                                  item(MISSING_ID)));
    when(newsService.getArticleLanguages(INDEXED_ID, false)).thenReturn(List.of());
    when(newsService.getArticleLanguages(MISSING_ID, false)).thenReturn(List.of("fr"));
    // Longer than the 32766 bytes the engine accepts for one indexed term
    String longText = "longform ".repeat(5000) + "zebracorn";
    when(newsService.getNewsArticleById(INDEXED_ID)).thenReturn(news(INDEXED_ID, "Indexed before", "Short text", null));
    when(newsService.getNewsArticleById(MISSING_ID)).thenReturn(news(MISSING_ID, "Annual report", longText, null));
    when(newsService.getNewsArticleByIdAndLang(MISSING_ID, "fr")).thenReturn(news(MISSING_ID, "Rapport annuel", longText, "fr"));

    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setId("1");
    ActivityStreamImpl activityStream = new ActivityStreamImpl();
    activityStream.setPrettyId("prettyId");
    activityStream.setType(ActivityStream.Type.SPACE);
    activity.setActivityStream(activityStream);
    when(activityManager.getActivity("1")).thenReturn(activity);
    when(identityManager.getOrCreateIdentity(ActivityStream.Type.SPACE.getProviderId(), "prettyId")).thenReturn(new Identity("5"));

    InitParams params = new InitParams();
    PropertiesParam propertiesParam = new PropertiesParam();
    propertiesParam.setName("constructor.params");
    propertiesParam.setProperty("index_current", CURRENT_INDEX);
    params.addParameter(propertiesParam);
    return new NewsIndexingServiceConnector(identityManager, params, newsService, activityManager, metadataService);
  }

  private static MetadataItem item(String articleId) {
    MetadataItem item = new MetadataItem();
    item.setObjectId(articleId);
    return item;
  }

  private static News news(String id, String title, String body, String lang) {
    News news = new News();
    news.setId(id);
    news.setTitle(title);
    news.setBody(body);
    news.setLang(lang);
    news.setCreationDate(new Date());
    news.setUpdateDate(new Date());
    news.setActivities("1:1");
    return news;
  }

  private static List<String> idsOf(String index) throws Exception {
    send("POST", "/" + index + "/_refresh", "");
    HttpResponse<String> response = HTTP_CLIENT.send(request("POST",
                                                             "/" + index + "/_search",
                                                             "{\"size\":10,\"_source\":false}"),
                                                     BodyHandlers.ofString());
    assertTrue(response.statusCode() < 300, response::body);
    List<String> ids = new ArrayList<>();
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"_id\":\"([^\"]+)\"").matcher(response.body());
    while (matcher.find()) {
      ids.add(matcher.group(1));
    }
    return ids.stream().sorted().toList();
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
    try (InputStream inputStream = NewsIndexUpgradeIT.class.getClassLoader().getResourceAsStream(name)) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

}
