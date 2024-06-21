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

import static io.meeds.analytics.utils.AnalyticsUtils.addSpaceStatistics;
import static io.meeds.news.utils.NewsUtils.COMMENT_NEWS;
import static io.meeds.news.utils.NewsUtils.DELETE_NEWS;
import static io.meeds.news.utils.NewsUtils.LIKE_NEWS;
import static io.meeds.news.utils.NewsUtils.POST_NEWS;
import static io.meeds.news.utils.NewsUtils.SHARE_NEWS;
import static io.meeds.news.utils.NewsUtils.UPDATE_NEWS;
import static io.meeds.news.utils.NewsUtils.VIEW_NEWS;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import io.meeds.news.model.News;
import io.meeds.news.utils.NewsUtils;

@Asynchronous
@Component
@Profile("analytics")
public class AnalyticsNewsListener extends Listener<String, News> {

  private static final String   CREATE_CONTENT_OPERATION_NAME  = "createContent";

  private static final String   UPDATE_CONTENT_OPERATION_NAME  = "updateContent";

  private static final String   DELETE_CONTENT_OPERATION_NAME  = "deleteContent";

  private static final String   VIEW_CONTENT_OPERATION_NAME    = "viewContent";

  private static final String   SHARE_CONTENT_OPERATION_NAME   = "shareContent";

  private static final String   LIKE_CONTENT_OPERATION_NAME    = "likeContent";

  private static final String   COMMENT_CONTENT_OPERATION_NAME = "commentContent";

  private static final String[] LISTENER_EVENTS                = { POST_NEWS, UPDATE_NEWS, DELETE_NEWS, VIEW_NEWS, SHARE_NEWS, COMMENT_NEWS, LIKE_NEWS };

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
  public void onEvent(Event<String, News> event) throws Exception {
    News news = event.getData();
    String operation = mapEventNameToOperation(event.getEventName());
    long userId = 0;
    Identity identity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, event.getSource());
    if (identity != null) {
      userId = Long.parseLong(identity.getId());
    }
    StatisticData statisticData = new StatisticData();

    statisticData.setModule("contents");
    statisticData.setSubModule("contents");
    statisticData.setOperation(operation);
    statisticData.setUserId(userId);
    statisticData.addParameter("contentId", news.getId());
    statisticData.addParameter("contentTitle", news.getTitle());
    statisticData.addParameter("contentAuthor", news.getAuthor());
    statisticData.addParameter("contentLastModifier", news.getUpdater());
    statisticData.addParameter("contentType", "News");
    statisticData.addParameter("contentUpdatedDate", news.getUpdateDate());
    statisticData.addParameter("contentCreationDate", news.getCreationDate());
    statisticData.addParameter("contentPublication", news.isPublished() ? "Yes" : "No");
    if (news.isPublished()
        && (operation.equals(CREATE_CONTENT_OPERATION_NAME) || operation.equals(UPDATE_CONTENT_OPERATION_NAME))) {
      statisticData.addParameter("contentPublicationAudience",
                                 news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE) ? "All users" : "Only space members");
    }
    Space space = getSpaceService().getSpaceById(news.getSpaceId());
    if (space != null) {
      addSpaceStatistics(statisticData, space);
    }
    AnalyticsUtils.addStatisticData(statisticData);
  }

  private String mapEventNameToOperation(String eventName) {
    switch (eventName) {
    case "exo.news.postArticle":
      return CREATE_CONTENT_OPERATION_NAME;
    case "exo.news.updateArticle":
      return UPDATE_CONTENT_OPERATION_NAME;
    case "exo.news.deleteArticle":
      return DELETE_CONTENT_OPERATION_NAME;
    case "exo.news.viewArticle":
      return VIEW_CONTENT_OPERATION_NAME;
    case "exo.news.shareArticle":
      return SHARE_CONTENT_OPERATION_NAME;
    case "exo.news.commentArticle":
      return COMMENT_CONTENT_OPERATION_NAME;
    case "exo.news.likeArticle":
      return LIKE_CONTENT_OPERATION_NAME;
    default:
      throw new IllegalArgumentException("Unknown event: " + eventName);
    }
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
