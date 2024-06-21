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
package io.meeds.news.listener;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.social.metadata.model.MetadataItem;

import io.meeds.news.model.News;
import io.meeds.news.search.NewsIndexingServiceConnector;
import io.meeds.news.service.NewsService;
import io.meeds.news.utils.NewsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class MetadataItemModified extends Listener<Long, MetadataItem> {

  @Autowired
  private IndexingService       indexingService;

  @Autowired
  private NewsService           newsService;

  @Autowired
  private ActivityStorage       activityStorage;

  @Autowired
  private ListenerService        listenerService;

  private CachedActivityStorage cachedActivityStorage;

  private String[] LISTENER_EVENTS = { "social.metadataItem.updated", "social.metadataItem.created", "social.metadataItem.deleted" };

  @PostConstruct
  public void init() {
    if (activityStorage instanceof CachedActivityStorage) {
      this.cachedActivityStorage = (CachedActivityStorage) activityStorage;
    }
    for (String listener : LISTENER_EVENTS) {
      listenerService.addListener(listener, this);
    }
  }

  @Override
  public void onEvent(Event<Long, MetadataItem> event) throws Exception {
    MetadataItem metadataItem = event.getData();
    String objectType = metadataItem.getObjectType();
    String objectId = metadataItem.getObjectId();
    if (isNewsEvent(objectType)) {
      // Ensure to re-execute all ActivityProcessors to compute & cache
      // metadatas of the activity again
      News news = newsService.getNewsArticleById(objectId);
      if (news != null) {
        if (StringUtils.isNotBlank(news.getActivityId())) {
          clearCache(news.getActivityId());
        }
        reindexNews(objectId);
      }
    }
  }

  protected boolean isNewsEvent(String objectType) {
    return StringUtils.equals(objectType, NewsUtils.NEWS_METADATA_OBJECT_TYPE);
  }

  private void clearCache(String activityId) {
    if (cachedActivityStorage != null) {
      cachedActivityStorage.clearActivityCached(activityId);
    }
  }

  private void reindexNews(String newsId) {
    indexingService.reindex(NewsIndexingServiceConnector.TYPE, newsId);
  }

}
