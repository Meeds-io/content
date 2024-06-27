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

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.news.model.News;
import io.meeds.news.utils.NewsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.meeds.news.utils.NewsUtils.POST_NEWS_ARTICLE;
import static io.meeds.news.utils.NewsUtils.PUBLISH_NEWS;

@Component
public class NewsGamificationIntegrationListener extends Listener<String, News> {
  private static final Log   LOG                                          =
                                 ExoLogger.getLogger(NewsGamificationIntegrationListener.class);

  private String[] LISTENERS = {POST_NEWS_ARTICLE, PUBLISH_NEWS};

  public static final String GAMIFICATION_GENERIC_EVENT                   = "exo.gamification.generic.action";

  public static final String GAMIFICATION_POST_NEWS_ARTICLE_RULE_TITLE    = "PostArticle";

  public static final String GAMIFICATION_PUBLISH_NEWS_ARTICLE_RULE_TITLE = "PublishArticle";

  String                     ACTIVITY_OBJECT_TYPE                         = "news";

  String                     OBJECT_ID_PARAM                              = "objectId";

  String                     OBJECT_TYPE_PARAM                            = "objectType";

  @Autowired
  private PortalContainer    container;

  @Autowired
  private ListenerService    listenerService;

  @PostConstruct
  public void init() {
    for (String listener : LISTENERS) {
      listenerService.addListener(listener, this);
    }
  }
  @Override
  public void onEvent(Event<String, News> event) throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String eventName = event.getEventName();
      News news = event.getData();
      String ruleTitle = "";
      if (StringUtils.equals(eventName, POST_NEWS_ARTICLE)) {
        ruleTitle = GAMIFICATION_POST_NEWS_ARTICLE_RULE_TITLE;
      } else if (StringUtils.equals(eventName, NewsUtils.PUBLISH_NEWS)) {
        ruleTitle = GAMIFICATION_PUBLISH_NEWS_ARTICLE_RULE_TITLE;
      }
      try {
        Map<String, String> gamificationMap = new HashMap<>();
        gamificationMap.put("ruleTitle", ruleTitle);
        gamificationMap.put("object", news.getUrl());
        gamificationMap.put("senderId", news.getAuthor()); // matches the
                                                           // gamification's
                                                           // earner id
        gamificationMap.put("receiverId", news.getAuthor());
        gamificationMap.put(OBJECT_ID_PARAM, news.getActivityId());
        gamificationMap.put(OBJECT_TYPE_PARAM, ACTIVITY_OBJECT_TYPE);
        listenerService.broadcast(GAMIFICATION_GENERIC_EVENT, gamificationMap, news.getId());
      } catch (Exception e) {
        LOG.error("Cannot broadcast gamification event");
      }
    } finally {
      RequestLifeCycle.end();
    }
  }
}
