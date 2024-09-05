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

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.tag.TagService;

import io.meeds.news.filter.NewsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NewsSearchConnector {

  @Autowired
  private ConfigurationManager   configurationManager;

  @Autowired
  private IdentityManager        identityManager;

  @Autowired
  private ActivityStorage        activityStorage;

  @Autowired
  private ElasticSearchingClient client;

  @Value("${content.es.index:news_alias}")
  private String                 index;

  @Value("${content.search.type:news}")
  private String                 searchType;

  @Value("${content.es.query.path:jar:/news-search-query.json}")
  private String                       searchQueryFilePath;

  private String                       searchQuery;

  private static final Log             LOG                          = ExoLogger.getLogger(NewsSearchConnector.class);

  public static final String           SEARCH_QUERY_TERM            = """
                                                                      "must":{ "query_string" :{
                                                                              "fields": ["body", "posterName", "summary","title"],
                                                                              "default_operator": "AND",
                                                                              "query": "@term@"}
                                                                              },""";

  @PostConstruct
  public void init() {
    retrieveSearchQuery();
  }
  public List<NewsESSearchResult> search(Identity viewerIdentity, NewsFilter filter) {
    if (viewerIdentity == null) {
      throw new IllegalArgumentException("Viewer identity is mandatory");
    }
    if (filter.getOffset() < 0) {
      throw new IllegalArgumentException("Offset must be positive");
    }
    if (filter.getLimit() < 0) {
      throw new IllegalArgumentException("Limit must be positive");
    }
    if (StringUtils.isBlank(filter.getSearchText()) && !filter.isFavorites() && CollectionUtils.isEmpty(filter.getTagNames())) {
      throw new IllegalArgumentException("Filter term is mandatory");
    }
    Set<Long> streamFeedOwnerIds = this.activityStorage.getStreamFeedOwnerIds(viewerIdentity);
    String esQuery = buildQueryStatement(viewerIdentity, streamFeedOwnerIds, filter);
    String jsonResponse = this.client.sendRequest(esQuery, this.index);
    return buildResult(jsonResponse);
  }

  private String buildQueryStatement(Identity viewerIdentity, Set<Long> streamFeedOwnerIds, NewsFilter filter) {
    Map<String, List<String>> metadataFilters = buildMetadataFilter(filter, viewerIdentity);
    String termQuery = buildTermQueryStatement(filter.getSearchText());
    String favoriteQuery = buildFavoriteQueryStatement(metadataFilters.get(FavoriteService.METADATA_TYPE.getName()));
    String tagsQuery = buildTagsQueryStatement(metadataFilters.get(TagService.METADATA_TYPE.getName()));
    return retrieveSearchQuery().replace("@term_query@", termQuery)
                                .replace("@favorite_query@", favoriteQuery)
                                .replace("@tags_query@", tagsQuery)
                                .replace("@permissions@", StringUtils.join(streamFeedOwnerIds, ","))
                                .replace("@offset@", String.valueOf(filter.getOffset()))
                                .replace("@limit@", String.valueOf(filter.getLimit()));
  }

  @SuppressWarnings("rawtypes")
  private List<NewsESSearchResult> buildResult(String jsonResponse) {
    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    List<NewsESSearchResult> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult == null) {
      return results;
    }

    //
    JSONArray jsonHits = (JSONArray) jsonResult.get("hits");
    for (Object jsonHit : jsonHits) {
      try {
        NewsESSearchResult newsSearchResult = new NewsESSearchResult();
        JSONObject jsonHitObject = (JSONObject) jsonHit;
        JSONObject hitSource = (JSONObject) jsonHitObject.get("_source");
        String id = (String) hitSource.get("id");
        String posterId = (String) hitSource.get("posterId");
        String spaceDisplayName = (String) hitSource.get("spaceDisplayName");
        String newsActivityId = (String) hitSource.get("newsActivityId");
        String language = (String) hitSource.get("lang");

        Long postedTime = parseLong(hitSource, "postedTime");
        Long lastUpdatedTime = parseLong(hitSource, "lastUpdatedTime");

        String title = (String) hitSource.get("title");
        String body = (String) hitSource.get("body");
        JSONObject highlightSource = (JSONObject) jsonHitObject.get("highlight");
        List<String> excerpts = new ArrayList<>();
        if (highlightSource != null) {
          JSONArray bodyExcepts = (JSONArray) highlightSource.get("body");
          if (bodyExcepts != null) {
            excerpts = Arrays.asList((String[]) bodyExcepts.toArray(new String[0]));
          }
        }
        newsSearchResult.setId(id);
        newsSearchResult.setLang(language);
        newsSearchResult.setTitle(title);
        if (posterId != null) {
          Identity posterIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, posterId);
          newsSearchResult.setPoster(posterIdentity);
        }
        newsSearchResult.setPostedTime(postedTime);
        newsSearchResult.setLastUpdatedTime(lastUpdatedTime);
        newsSearchResult.setSpaceDisplayName(spaceDisplayName);
        newsSearchResult.setActivityId(newsActivityId);

        String portalName = PortalContainer.getCurrentPortalContainerName();
        String portalOwner = CommonsUtils.getCurrentPortalOwner();
        newsSearchResult.setNewsUrl("/" + portalName + "/" + portalOwner + "/activity?id=" + newsActivityId);
        if (language != null) {
          newsSearchResult.setNewsUrl(newsSearchResult.getNewsUrl().concat("&lang=" + language));
        }
        newsSearchResult.setBody(body);
        newsSearchResult.setExcerpts(excerpts);

        results.add(newsSearchResult);
      } catch (Exception e) {
        LOG.warn("Error processing news search result item, ignore it from results", e);
      }
    }
    return results;
  }

  private String buildTermQueryStatement(String term) {
    if (StringUtils.isBlank(term)) {
      return term;
    }
    term = removeSpecialCharacters(term);
    return SEARCH_QUERY_TERM.replace("@term@", term);
  }

  private Long parseLong(JSONObject hitSource, String key) {
    String value = (String) hitSource.get(key);
    return StringUtils.isBlank(value) ? null : Long.parseLong(value);
  }

  private String retrieveSearchQuery() {
    if (StringUtils.isBlank(this.searchQuery) || PropertyManager.isDevelopping()) {
      try {
        InputStream queryFileIS = this.configurationManager.getInputStream(searchQueryFilePath);
        this.searchQuery = IOUtil.getStreamContentAsString(queryFileIS);
      } catch (Exception e) {
        throw new IllegalStateException("Error retrieving search query from file: " + searchQueryFilePath, e);
      }
    }
    return this.searchQuery;
  }

  private String removeSpecialCharacters(String string) {
    string = Normalizer.normalize(string, Normalizer.Form.NFD);
    string = string.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replaceAll("'", " ");
    return string;
  }

  private String buildFavoriteQueryStatement(List<String> values) {
    if (CollectionUtils.isEmpty(values)) {
      return "";
    }
    return new StringBuilder().append("{\"terms\":{")
                              .append("\"metadatas.favorites.metadataName.keyword\": [\"")
                              .append(StringUtils.join(values, "\",\""))
                              .append("\"]}},")
                              .toString();
  }

  private String buildTagsQueryStatement(List<String> values) {
    if (CollectionUtils.isEmpty(values)) {
      return "";
    }
    List<String> tagsQueryParts =
                                values.stream()
                                      .map(value -> new StringBuilder().append("{\"term\": {\n")
                                                                       .append("            \"metadatas.tags.metadataName.keyword\": {\n")
                                                                       .append("              \"value\": \"")
                                                                       .append(value)
                                                                       .append("\",\n")
                                                                       .append("              \"case_insensitive\":true\n")
                                                                       .append("            }\n")
                                                                       .append("          }}")
                                                                       .toString())
                                      .collect(Collectors.toList());
    return new StringBuilder().append(",\"should\": [\n")
                              .append(StringUtils.join(tagsQueryParts, ","))
                              .append("      ],\n")
                              .append("      \"minimum_should_match\": 1")
                              .toString();
  }

  private Map<String, List<String>> buildMetadataFilter(NewsFilter filter, Identity viewerIdentity) {
    Map<String, List<String>> metadataFilters = new HashMap<>();
    if (filter.isFavorites()) {
      metadataFilters.put(FavoriteService.METADATA_TYPE.getName(), Collections.singletonList(viewerIdentity.getId()));
    }
    if (CollectionUtils.isNotEmpty(filter.getTagNames())) {
      metadataFilters.put(TagService.METADATA_TYPE.getName(), filter.getTagNames());
    }
    return metadataFilters;
  }
}
