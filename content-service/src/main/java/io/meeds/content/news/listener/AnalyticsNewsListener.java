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
package io.meeds.content.news.listener;

import static io.meeds.analytics.utils.AnalyticsUtils.addSpaceStatistics;
import static io.meeds.content.news.utils.NewsUtils.ADD_ARTICLE_TRANSLATION;
import static io.meeds.content.news.utils.NewsUtils.COMMENT_NEWS;
import static io.meeds.content.news.utils.NewsUtils.DELETE_NEWS;
import static io.meeds.content.news.utils.NewsUtils.LIKE_NEWS;
import static io.meeds.content.news.utils.NewsUtils.POST_NEWS;
import static io.meeds.content.news.utils.NewsUtils.REMOVE_ARTICLE_TRANSLATION;
import static io.meeds.content.news.utils.NewsUtils.SHARE_NEWS;
import static io.meeds.content.news.utils.NewsUtils.UPDATE_NEWS;
import static io.meeds.content.news.utils.NewsUtils.VIEW_NEWS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import io.meeds.content.news.model.News;
import io.meeds.content.news.utils.NewsUtils;

import jakarta.annotation.PostConstruct;

@Asynchronous
@Component
@Profile("analytics")
public class AnalyticsNewsListener extends Listener<Object, News> {

  private static final String   CREATE_CONTENT_OPERATION_NAME  = "createContent";

  private static final String   UPDATE_CONTENT_OPERATION_NAME  = "updateContent";

  private static final String   DELETE_CONTENT_OPERATION_NAME  = "deleteContent";

  private static final String   VIEW_CONTENT_OPERATION_NAME    = "viewContent";

  private static final String   SHARE_CONTENT_OPERATION_NAME   = "shareContent";

  private static final String   LIKE_CONTENT_OPERATION_NAME    = "likeContent";

  private static final String   COMMENT_CONTENT_OPERATION_NAME = "commentContent";

  private static final String[] LISTENER_EVENTS                = { POST_NEWS, UPDATE_NEWS, DELETE_NEWS, VIEW_NEWS, SHARE_NEWS,
      COMMENT_NEWS, LIKE_NEWS, ADD_ARTICLE_TRANSLATION, REMOVE_ARTICLE_TRANSLATION };

  @Autowired
  private IdentityManager       identityManager;

  @Autowired
  private SpaceService          spaceService;

  @Autowired
  private ListenerService       listenerService;

  @PostConstruct
  public void init() {
    for (String listener : LISTENER_EVENTS) {
      listenerService.addListener(listener, this);
    }
  }

  @Override
  public void onEvent(Event<Object, News> event) throws Exception {
    News news = event.getData();
    Object updaterId = event.getSource();
    String username = null;
    if (updaterId instanceof String s) {
      username = s;
    } else if (updaterId instanceof Long l) {
      Identity identity = identityManager.getIdentity(l);
      if (identity != null && identity.isUser()) {
        username = identity.getRemoteId();
      }
    }
    if (username == null) {
      return;
    }

    String operation = mapEventNameToOperation(event.getEventName());
    long userId = 0;
    Identity identity = getIdentityManager().getOrCreateUserIdentity(username);
    if (identity != null) {
      userId = Long.parseLong(identity.getId());
    }
    StatisticData statisticData = new StatisticData();

    statisticData.setModule("contents");
    statisticData.setSubModule("contents");
    statisticData.setOperation(operation);
    statisticData.setUserId(userId);
    statisticData.addKeyword("contentId", news.getId());
    statisticData.addKeyword("contentTitle", news.getTitle());
    if (operation.equals(VIEW_CONTENT_OPERATION_NAME) || operation.equals(UPDATE_CONTENT_OPERATION_NAME)
        || operation.equals(DELETE_CONTENT_OPERATION_NAME)) {
      statisticData.addKeyword("contentLanguage", news.getLang() != null ? news.getLang() : "originalVersion");
    }
    statisticData.addKeyword("contentCreator", news.getOwner());
    statisticData.addKeyword("contentLastModifier", username);
    statisticData.addKeyword("contentType", "News");
    statisticData.addDate("contentUpdatedDate", news.getUpdateDate());
    statisticData.addDate("contentCreationDate", news.getCreationDate());
    Space space = getSpaceService().getSpaceById(news.getSpaceId());
    if (space != null) {
      addSpaceStatistics(statisticData, space);
    }
    AnalyticsUtils.addStatisticData(statisticData);
  }

  private String mapEventNameToOperation(String eventName) {
    return switch (eventName) {
    case "exo.news.postArticle" -> CREATE_CONTENT_OPERATION_NAME;
    case "exo.news.updateArticle", NewsUtils.ADD_ARTICLE_TRANSLATION -> UPDATE_CONTENT_OPERATION_NAME;
    case "exo.news.deleteArticle", NewsUtils.REMOVE_ARTICLE_TRANSLATION -> DELETE_CONTENT_OPERATION_NAME;
    case "exo.news.viewArticle" -> VIEW_CONTENT_OPERATION_NAME;
    case "exo.news.shareArticle" -> SHARE_CONTENT_OPERATION_NAME;
    case "exo.news.commentArticle" -> COMMENT_CONTENT_OPERATION_NAME;
    case "exo.news.likeArticle" -> LIKE_CONTENT_OPERATION_NAME;
    default -> throw new IllegalArgumentException("Unknown event: " + eventName);
    };
  }

  public IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = ExoContainerContext.getService(IdentityManager.class);
    }
    return identityManager;
  }

  public SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = ExoContainerContext.getService(SpaceService.class);
    }
    return spaceService;
  }
}
