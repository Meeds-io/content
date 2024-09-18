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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import io.meeds.news.utils.NewsUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import static io.meeds.news.utils.NewsUtils.NewsObjectType.ARTICLE;

/**
 * A triggered listener class about activity lifecyles. This class is used to
 * propagate sharing activity in News elements to let targeted space members to
 * access News
 */
@Component
public class NewsActivityListener extends ActivityListenerPlugin {

  private static final Log    LOG     = ExoLogger.getLogger(NewsActivityListener.class);

  private static final String NEWS_ID = "newsId";

  @Autowired
  private ActivityManager     activityManager;

  @Autowired
  private IdentityManager     identityManager;

  @Autowired
  private SpaceService        spaceService;

  @Autowired
  private NewsService         newsService;

  @PostConstruct
  public void init() {
    activityManager.addActivityEventListener(this);
  }
  @Override
  public void shareActivity(ActivityLifeCycleEvent event) {
    ExoSocialActivity sharedActivity = event.getActivity();
    if (sharedActivity != null && sharedActivity.getTemplateParams() != null
        && sharedActivity.getTemplateParams().containsKey("originalActivityId")) {
      String originalActivityId = sharedActivity.getTemplateParams().get("originalActivityId");
      ExoSocialActivity originalActivity = activityManager.getActivity(originalActivityId);
      if (originalActivity != null && originalActivity.getTemplateParams() != null
          && originalActivity.getTemplateParams().containsKey(NEWS_ID)) {
        String newsId = originalActivity.getTemplateParams().get(NEWS_ID);
        org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
        try {
          News news = newsService.getNewsById(newsId, currentIdentity, false, ARTICLE.name().toLowerCase());
          if (news != null && !news.isDeleted()) {
            Identity posterIdentity = getIdentity(sharedActivity);
            Space space = getSpace(sharedActivity);
            newsService.shareNews(news, space, posterIdentity, sharedActivity.getId());
          }
        } catch (Exception e) {
          LOG.error("Error while sharing news {} to activity {}", newsId, sharedActivity.getId(), e);
        }
      }
    }
  }

  @Override
  public void likeActivity(ActivityLifeCycleEvent event) {
    ExoSocialActivity activity = activityManager.getActivity(event.getActivity().getId());
    if (activity != null && activity.getTemplateParams() != null && activity.getTemplateParams().containsKey(NEWS_ID)) {
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      try {
        News news = newsService.getNewsByActivityId(activity.getId(), currentIdentity);
        NewsUtils.broadcastEvent(NewsUtils.LIKE_NEWS, currentIdentity.getUserId(), news);
      } catch (Exception e) {
        LOG.error("Error broadcast like news event", e);
      }
    }
  }

  @Override
  public void saveComment(ActivityLifeCycleEvent event) {
    ExoSocialActivity activity = activityManager.getActivity(event.getActivity().getParentId());
    if (activity != null && activity.getTemplateParams() != null && activity.getTemplateParams().containsKey(NEWS_ID)) {
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      try {
        News news = newsService.getNewsByActivityId(activity.getId(), currentIdentity);
        NewsUtils.broadcastEvent(NewsUtils.COMMENT_NEWS, currentIdentity.getUserId(), news);
      } catch (Exception e) {
        LOG.error("Error broadcast comment news event", e);
      }
    }
  }

  private Identity getIdentity(ExoSocialActivity sharedActivity) {
    String posterIdentityId = sharedActivity.getPosterId();
    return identityManager.getIdentity(posterIdentityId);
  }

  private Space getSpace(ExoSocialActivity sharedActivity) {
    String spacePrettyName = sharedActivity.getActivityStream().getPrettyId();
    return spaceService.getSpaceByPrettyName(spacePrettyName);
  }

}
