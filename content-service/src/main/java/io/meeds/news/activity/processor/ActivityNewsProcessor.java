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
package io.meeds.news.activity.processor;

import java.util.HashMap;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.math.NumberUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import io.meeds.news.utils.NewsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityNewsProcessor extends BaseActivityProcessorPlugin {

  private static final Log    LOG                     = ExoLogger.getLogger(ActivityNewsProcessor.class);

  private static final String ACTIVITY_PROCESSOR_NAME = "ActivityNewsProcessor";

  private static final int    processorPriority       = 30;

  @Autowired
  private NewsService         newsService;

  @Autowired
  private ActivityManager     activityManager;

  public ActivityNewsProcessor() {
    super(getInitParams());
  }

  @PostConstruct
  public void init() {
    activityManager.addProcessorPlugin(this);
  }

  @Override
  public String getName() {
    return ACTIVITY_PROCESSOR_NAME;
  }

  @Override
  public void processActivity(ExoSocialActivity activity) {
    if (activity.isComment() || activity.getType() == null || !activity.getTemplateParams().containsKey("newsId")) {
      return;
    }
    if (activity.getLinkedProcessedEntities() == null) {
      activity.setLinkedProcessedEntities(new HashMap<>());
    }
    News news = (News) activity.getLinkedProcessedEntities().get("news");
    if (news == null) {
      try {
        if (!NumberUtils.isParsable(activity.getTemplateParams().get("newsId"))) {
          return;
        }
        news = newsService.getNewsArticleById(activity.getTemplateParams().get("newsId"));

        RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getCommentsWithListAccess(activity, true);
        news.setCommentsCount(listAccess.getSize());
        news.setLikesCount(activity.getLikeIdentityIds() == null ? 0 : activity.getLikeIdentityIds().length);

        activity.setMetadataObjectId(news.getId());
        activity.setMetadataObjectType(NewsUtils.NEWS_METADATA_OBJECT_TYPE);
        news.setIllustration(null);
      } catch (Exception e) {
        LOG.warn("Error retrieving news with id {}", activity.getTemplateParams().get("newsId"), e);
      }
      activity.getLinkedProcessedEntities().put("news", news);
    }
  }

  private static InitParams getInitParams() {
    InitParams initParams = new InitParams();
    ValueParam param = new ValueParam();
    param.setName("priority");
    param.setValue(String.valueOf(processorPriority));
    initParams.addParameter(param);
    return initParams;
  }
}
