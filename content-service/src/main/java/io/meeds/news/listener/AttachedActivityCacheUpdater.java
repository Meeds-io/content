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
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;

import io.meeds.news.model.News;

/**
 * A listener to clear cached news inside
 * {@link ExoSocialActivity#getLinkedProcessedEntities()} after any modification
 * made on {@link News}
 */
public class AttachedActivityCacheUpdater extends Listener<String, News> {

  private CachedActivityStorage cachedActivityStorage;

  public AttachedActivityCacheUpdater(ActivityStorage activityStorage) {
    if (activityStorage instanceof CachedActivityStorage) {
      this.cachedActivityStorage = (CachedActivityStorage) activityStorage;
    }
  }

  @Override
  public void onEvent(Event<String, News> event) throws Exception {
    if (cachedActivityStorage != null && event.getData() != null && StringUtils.isNotBlank(event.getData().getActivityId())) {
      cachedActivityStorage.clearActivityCached(event.getData().getActivityId());
    }
  }

}
