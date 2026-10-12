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

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.tag.TagService;

import io.meeds.content.news.model.filter.NewsFilter;

import jakarta.annotation.PostConstruct;

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

  private static final Log       LOG                   = ExoLogger.getLogger(NewsSearchConnector.class);

  public static final String     SEARCH_QUERY_TERM     = """
      "must":{ "query_string" :{
              "fields": ["body", "posterName", "summary","title"],
              "default_operator": "AND",
              "analyze_wildcard": true,
              "query": "@term@"}
              },""";

  public static final String     DEFAULT_SORTING_QUERY = """
          {
            "_score": {
              "order": "desc"
            }
          }
      """;

  public static final String     SORTING_QUERY         = """
          {
            "@sortField@": {
              "order": "@sortOrder@"
            }
          },
          "_score"
      """;

  // The query_string reserved characters, the backslash included, see
  // https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-query-string-query#_reserved_characters
  private static final Pattern   QUERY_STRING_RESERVED_CHARACTERS = Pattern.compile("[\\\\+\\-=&|!(){}\\[\\]^\"~*?:/]");

  // Around a word, reserved characters are punctuation: quotes, brackets, a
  // leading operator or a trailing wildcard
  private static final Pattern   QUERY_STRING_RESERVED_WORD_EDGES =
                                                                  Pattern.compile("^" + QUERY_STRING_RESERVED_CHARACTERS.pattern()
                                                                      + "+|" + QUERY_STRING_RESERVED_CHARACTERS.pattern() + "+$");

  // Reserved too, but query_string cannot escape them: they separate words
  private static final Pattern   QUERY_STRING_UNESCAPABLE_CHARACTERS = Pattern.compile("[<>]");

  // Elasticsearch refuses a query beyond its clause limit, 1024 clauses at
  // least, more on a larger heap. The costliest searched text, one word of
  // ideographs or emoji, costs a clause per character in each of the three
  // fields the standard tokenizer splits, and reaches 1024 clauses at 341
  // characters
  private static final int       MAX_SEARCHED_TEXT_LENGTH            = 256;

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
    if (!CollectionUtils.isEmpty(filter.getSpaces())) {
      Set<Long> spaceIdentityIds = filter.getSpaces().stream()
              .map(Long::parseLong)
              .collect(Collectors.toSet());

      streamFeedOwnerIds.retainAll(spaceIdentityIds);
      if (CollectionUtils.isEmpty(streamFeedOwnerIds)) {
        return Collections.emptyList();
      }
    }
    String esQuery = buildQueryStatement(viewerIdentity, streamFeedOwnerIds, filter);
    String jsonResponse = this.client.sendRequest(esQuery, this.index);
    return buildResult(jsonResponse);
  }

  private String buildQueryStatement(Identity viewerIdentity, Set<Long> streamFeedOwnerIds, NewsFilter filter) {
    Map<String, List<String>> metadataFilters = buildMetadataFilter(filter, viewerIdentity);
    String termQuery = buildTermQueryStatement(filter.getSearchText());
    String favoriteQuery = buildFavoriteQueryStatement(metadataFilters.get(FavoriteService.METADATA_TYPE.getName()));
    String tagsQuery = buildTagsQueryStatement(metadataFilters.get(TagService.METADATA_TYPE.getName()));
    String sortQuery = buildSortQueryStatement(filter);
    // One pass: a replaced fragment is never scanned again, so a placeholder
    // name typed in the term or a tag stays text
    return StringUtils.replaceEach(retrieveSearchQuery(),
                                   new String[] { "@term_query@", "@favorite_query@", "@tags_query@", "@permissions@",
                                       "@sortQuery@", "@offset@", "@limit@" },
                                   new String[] { termQuery, favoriteQuery, tagsQuery, StringUtils.join(streamFeedOwnerIds, ","),
                                       sortQuery, String.valueOf(filter.getOffset()), String.valueOf(filter.getLimit()) });
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
        String language = (String) hitSource.get("lang");
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
      return "";
    }
    if (term.codePointCount(0, term.length()) > MAX_SEARCHED_TEXT_LENGTH) {
      term = term.substring(0, term.offsetByCodePoints(0, MAX_SEARCHED_TEXT_LENGTH));
    }
    term = removeSpecialCharacters(term);
    // A trailing wildcard is required on each word for query_string to match
    // a partial/prefix term (e.g. while the user is still typing) - without
    // it, query_string only matches a whole token, so searching by the
    // first few letters of a title/body/summary word returns nothing.
    // Words are split on every whitespace query_string separates terms on,
    // tab, line feed and the ideographic space included: a word left joined
    // to its neighbour by one of them would keep an operator bare
    String wildcardTerm = Arrays.stream(StringUtils.split(term))
                                .map(this::toQueryStringWord)
                                .filter(StringUtils::isNotBlank)
                                .map(word -> word + "*")
                                .collect(Collectors.joining(" "));
    // The term is spliced into a JSON string: escape it so that a quote or a
    // backslash typed by the user can't close the string and alter the query
    return SEARCH_QUERY_TERM.replace("@term@", JSONValue.escape(wildcardTerm));
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
    return QUERY_STRING_UNESCAPABLE_CHARACTERS.matcher(string).replaceAll(" ");
  }

  /**
   * Turns a searched word into query_string text searched as typed: a slash
   * would otherwise open a regular expression, and an unbalanced quote or
   * parenthesis would make the engine refuse the whole query. The reserved
   * characters around the word are removed, since the title field keeps them
   * in its tokens and {@code "word"} would then miss a title holding
   * {@code word}; those inside the word are escaped once, in a single pass,
   * so a typed backslash is not escaped twice.
   *
   * @param word a searched word, without {@code <} nor {@code >}
   * @return the query_string text of the word, empty when it holds reserved
   *         characters only
   */
  private String toQueryStringWord(String word) {
    String innerWord = QUERY_STRING_RESERVED_WORD_EDGES.matcher(word.trim()).replaceAll("");
    return QUERY_STRING_RESERVED_CHARACTERS.matcher(innerWord).replaceAll("\\\\$0");
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
                                                                       .append(JSONValue.escape(value))
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

  private String buildSortQueryStatement(NewsFilter newsFilter) {
    String sortFiled = newsFilter.getSortField();
    String sortDirection = newsFilter.getSortDirection();

    if (StringUtils.isBlank(sortFiled)) {
      return DEFAULT_SORTING_QUERY;
    }
    return switch (sortFiled) {
      case "date" -> SORTING_QUERY.replace("@sortField@", "lastUpdatedDate").replace("@sortOrder@", getSortOrder(sortDirection));
      default -> DEFAULT_SORTING_QUERY;
    };
  }

  private String getSortOrder(String sortDirection) {
    // Only the two values Elasticsearch knows are spliced into the query
    return StringUtils.equalsIgnoreCase(sortDirection, "asc") ? "asc" : "desc";
  }

}
