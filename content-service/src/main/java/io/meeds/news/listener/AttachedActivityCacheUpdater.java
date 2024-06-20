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

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;

import io.meeds.news.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * A listener to clear cached news inside
 * {@link ExoSocialActivity#getLinkedProcessedEntities()} after any modification
 * made on {@link News}
 */
@Component
public class AttachedActivityCacheUpdater extends Listener<String, News> {

  @Autowired
  private ActivityStorage activityStorage;

  @Autowired
  private ListenerService listenerService;

  private CachedActivityStorage cachedActivityStorage;

  private String[] LISTENER_EVENTS = { "exo.news.postArticle", "exo.news.updateArticle", "exo.news.shareArticle", "exo.news.scheduleArticle", "exo.news.unscheduleArticle" };

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
  public void onEvent(Event<String, News> event) throws Exception {
    if (cachedActivityStorage != null && event.getData() != null && StringUtils.isNotBlank(event.getData().getActivityId())) {
      cachedActivityStorage.clearActivityCached(event.getData().getActivityId());
    }
  }

}
