/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
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

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import jakarta.annotation.PostConstruct;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.wiki.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExternalArticlePageListener extends Listener<Object, Page> {

  private final ListenerService listenerService;

  private final NewsService     newsService;

  private CachedActivityStorage cachedActivityStorage;

  private static final String   NOTE_DELETED    = "note.deleted";

  private static final String   NOTE_UPDATED    = "note.updated";

  private static final String   ARTICLE         = "article";

  private static final String[] LISTENER_EVENTS = { NOTE_DELETED, NOTE_UPDATED };

  @Autowired
  public ExternalArticlePageListener(ListenerService listenerService, NewsService newsService, ActivityStorage activityStorage) {
    this.listenerService = listenerService;
    this.newsService = newsService;
    if (activityStorage instanceof CachedActivityStorage) {
      this.cachedActivityStorage = (CachedActivityStorage) activityStorage;
    }
  }

  @PostConstruct
  public void init() {
    for (String event : LISTENER_EVENTS) {
      listenerService.addListener(event, this);
    }
  }

  @Override
  public void onEvent(Event<Object, Page> event) throws Exception {
    Page page = event.getData();
    if (page != null && !page.getOwner().equals("__system")) {
      if (event.getEventName().equals(NOTE_UPDATED)) {
        News news = newsService.getNewsArticleById(page.getId());
        if (news != null && news.getActivityId() != null) {
          cachedActivityStorage.clearActivityCached(news.getActivityId());
        }
      } else if (event.getEventName().equals(NOTE_DELETED) && newsService.getNewsArticleById(page.getId()) != null) {
        Identity identity = (Identity) event.getSource();
        newsService.deleteNews(page.getId(), identity, ARTICLE);
      }
    }
  }
}
