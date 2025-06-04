/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.content.news.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.upgrade.UpgradePluginExecutionContext;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

public class CleanDuplicatedEsIndexesUpgrade extends UpgradeProductPlugin {

  private static final Log       LOG                                 =
                                     ExoLogger.getLogger(CleanDuplicatedEsIndexesUpgrade.class.getName());

  private ElasticSearchingClient elasticSearchingClient;

  private IndexingService        indexingService;

  private NewsService            newsService;

  private SettingService         settingService;

  private int                    cleanedDuplicatedNewsEsIndexesCount = 0;

  private boolean                cleanupFailed                       = false;

  private static final String    PLUGIN_NAME                         = "CleanDuplicatedEsIndexesUpgrade";

  private static final String    PLUGIN_EXECUTED_KEY                 = "cleanDuplicatedEsIndexesUpgradeExecuted";

  public CleanDuplicatedEsIndexesUpgrade(InitParams initParams,
                                         NewsService newsService,
                                         ElasticSearchingClient elasticSearchingClient,
                                         IndexingService indexingService,
                                         SettingService settingService) {
    super(initParams);
    this.newsService = newsService;
    this.elasticSearchingClient = elasticSearchingClient;
    this.indexingService = indexingService;
    this.settingService = settingService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long startupTime = System.currentTimeMillis();
    LOG.info("Start cleanup of duplicated News ES indexes");
    int notCleanedDuplicatedNewsEsIndexesCount = 0;
    int processedDuplicatedNewsEsIndexesCount = 0;
    int totalDuplicatedNewsEsIndexesCount = 0;
    try {
      String matchAllQuery = "{\n" + "  \"query\": {\n" + "    \"match_all\": {}\n" + "  },\n" + "  \"size\": 10000\n" + "}";

      String notesMatchAllQueryResponse = this.elasticSearchingClient.sendRequest(matchAllQuery, "notes_v1");
      List<String> duplicatedNewsEsIndexes = getDuplicatedNewsEsIndexes(notesMatchAllQueryResponse);
      if (duplicatedNewsEsIndexes != null) {
        totalDuplicatedNewsEsIndexesCount = duplicatedNewsEsIndexes.size();
        LOG.info("Total number of duplicated News ES indexes to be cleaned: {}", totalDuplicatedNewsEsIndexesCount);
        if (duplicatedNewsEsIndexes != null) {
          for (List<String> duplicatedNewsEsIndexesChunk : ListUtils.partition(duplicatedNewsEsIndexes, 10)) {
            int processedDuplicatedNewsEsIndexesCountByTransaction = duplicatedNewsEsIndexesChunk.size();
            int notCleanedDuplicatedNewsEsIndexesCountByTransaction = manageDuplicatedNewsEsIndexes(duplicatedNewsEsIndexesChunk);
            processedDuplicatedNewsEsIndexesCount += processedDuplicatedNewsEsIndexesCountByTransaction;
            cleanedDuplicatedNewsEsIndexesCount += processedDuplicatedNewsEsIndexesCountByTransaction
                - notCleanedDuplicatedNewsEsIndexesCountByTransaction;
            notCleanedDuplicatedNewsEsIndexesCount += notCleanedDuplicatedNewsEsIndexesCountByTransaction;
            LOG.info("Duplicated News ES indexes cleanup progress: processed={}/{} succeeded={} error={}",
                     processedDuplicatedNewsEsIndexesCount,
                     totalDuplicatedNewsEsIndexesCount,
                     cleanedDuplicatedNewsEsIndexesCount,
                     notCleanedDuplicatedNewsEsIndexesCount);
          }
        }
      }
    } catch (Exception e) {
      this.cleanupFailed = true;
      LOG.error("An error occurred when cleanup duplicated News ES indexes:", e);
    }
    if (!this.cleanupFailed && totalDuplicatedNewsEsIndexesCount == cleanedDuplicatedNewsEsIndexesCount) {
      LOG.info("End duplicated News ES indexes successful cleanup: total={} succeeded={} error={}. It tooks {} ms.",
               totalDuplicatedNewsEsIndexesCount,
               cleanedDuplicatedNewsEsIndexesCount,
               notCleanedDuplicatedNewsEsIndexesCount,
               (System.currentTimeMillis() - startupTime));
    } else {
      LOG.warn("End duplicated News ES indexes cleanup with some errors: total={} succeeded={} error={}. It tooks {} ms."
          + " The not cleaned news articles will be processed again next startup.",
               totalDuplicatedNewsEsIndexesCount,
               cleanedDuplicatedNewsEsIndexesCount,
               notCleanedDuplicatedNewsEsIndexesCount,
               (System.currentTimeMillis() - startupTime));
      this.cleanupFailed = true;
      throw new IllegalStateException("Some duplicated News ES indexes wasn't executed successfully. It will be re-attempted next startup");
    }
  }

  @Override
  public void afterUpgrade() {
    if (!cleanupFailed) {
      settingService.set(Context.GLOBAL.id(PLUGIN_NAME),
                         Scope.APPLICATION.id(PLUGIN_NAME),
                         PLUGIN_EXECUTED_KEY,
                         SettingValue.create(true));
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion,
                                        String previousGroupVersion,
                                        UpgradePluginExecutionContext upgradePluginExecutionContext) {
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL.id(PLUGIN_NAME),
                                                      Scope.APPLICATION.id(PLUGIN_NAME),
                                                      PLUGIN_EXECUTED_KEY);
    boolean shouldUpgrade = super.shouldProceedToUpgrade(newVersion, previousGroupVersion, upgradePluginExecutionContext);
    if (!shouldUpgrade && settingValue == null) {
      settingService.set(Context.GLOBAL.id(PLUGIN_NAME),
                         Scope.APPLICATION.id(PLUGIN_NAME),
                         PLUGIN_EXECUTED_KEY,
                         SettingValue.create(true));
    }
    return shouldUpgrade;
  }

  public int manageDuplicatedNewsEsIndexes(List<String> duplicatedNewsEsIndexes) {
    int notCleanedDuplicatedNewsEsIndexesCountByTransaction = 0;
    for (String duplicatedNewsEsIndex : duplicatedNewsEsIndexes) {
      try {
        LOG.info("Cleaning duplicated News ES index with id '{}'", duplicatedNewsEsIndex);
        indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, duplicatedNewsEsIndex);
        LOG.info("Success cleaning News ES index with id '{}'", duplicatedNewsEsIndex);
      } catch (Exception e) {
        notCleanedDuplicatedNewsEsIndexesCountByTransaction++;
        LOG.warn("Error cleaning duplicated news ES index with id '{}'. Continue to migrate other items",
                 duplicatedNewsEsIndex,
                 e);
      }
    }
    return notCleanedDuplicatedNewsEsIndexesCountByTransaction;
  }

  public int getCleanedDuplicatedNewsEsIndexesCount() {
    return cleanedDuplicatedNewsEsIndexesCount;
  }

  private List<String> getDuplicatedNewsEsIndexes(String queryResponse) {

    List<String> duplicatedNewsEsIndexes = new ArrayList<>();
    JSONParser parser = new JSONParser();
    Map queryJsonResponse;
    try {
      queryJsonResponse = (Map) parser.parse(queryResponse);
    } catch (ParseException e) {
      return null;
    }

    JSONObject queryJsonResult = (JSONObject) queryJsonResponse.get("hits");
    JSONArray queryJsonHits = (JSONArray) queryJsonResult.get("hits");

    for (Object queryJsonHit : queryJsonHits) {
      String noteId = ((JSONObject) queryJsonHit).get("_id").toString();
      String articleId = noteId;
      if (noteId.contains("-")) {
        articleId = noteId.substring(0, noteId.indexOf("-"));
      }
      News article = null;
      try {
        article = newsService.buildArticle(articleId);
        if (article != null && !article.isReferred() && !article.isFromExternalPage()) {
          duplicatedNewsEsIndexes.add(noteId);
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving article id '{}'", noteId);
      }
    }
    return duplicatedNewsEsIndexes;
  }
}
