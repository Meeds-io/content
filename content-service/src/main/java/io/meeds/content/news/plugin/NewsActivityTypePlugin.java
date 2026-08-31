/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.news.plugin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.ActivityTypePlugin;
import org.exoplatform.social.core.manager.ActivityManager;

import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils;

import jakarta.annotation.PostConstruct;

/**
 * {@link ActivityTypePlugin} of news activities. News activities redirect
 * their Metadata facts (favorites by example) to the news object itself, thus
 * this plugin declares the news Metadata Object Type and resolves news objects
 * back to their displaying activities (used by the favorite activities stream
 * filter by example).
 */
@Component
public class NewsActivityTypePlugin extends ActivityTypePlugin {

  private static final Log LOG = ExoLogger.getLogger(NewsActivityTypePlugin.class);

  @Autowired
  private NewsService      newsService;

  @Autowired
  private ActivityManager  activityManager;

  public NewsActivityTypePlugin() {
    super(getInitParams());
  }

  @PostConstruct
  public void init() {
    activityManager.addActivityTypePlugin(this);
  }

  @Override
  public String getMetadataObjectType() {
    return NewsUtils.NEWS_METADATA_OBJECT_TYPE;
  }

  @Override
  public List<String> getActivityIds(List<String> metadataObjectIds) {
    return metadataObjectIds.stream()
                            // A news Metadata object id can carry a
                            // '-<lang>' suffix for a translated article,
                            // resolve it to its base article id
                            .map(objectId -> StringUtils.substringBefore(objectId, "-"))
                            .distinct()
                            .map(this::getNewsActivityId)
                            .filter(StringUtils::isNotBlank)
                            .toList();
  }

  private String getNewsActivityId(String newsId) {
    try {
      return newsService.getNewsActivityId(newsId);
    } catch (Exception e) {
      // One unresolvable news must not break the whole favorite activities
      // listing, thus warn and drop it
      LOG.warn("Error retrieving news with id {} while resolving its displaying activity", newsId, e);
      return null;
    }
  }

  private static InitParams getInitParams() {
    InitParams initParams = new InitParams();
    ValueParam typeParam = new ValueParam();
    typeParam.setName(ACTIVITY_TYPE_PARAM);
    typeParam.setValue("news");
    initParams.addParameter(typeParam);
    ValueParam enableNotificationParam = new ValueParam();
    enableNotificationParam.setName(ENABLE_NOTIFICATION_PARAM);
    enableNotificationParam.setValue("false");
    initParams.addParameter(enableNotificationParam);
    return initParams;
  }
}
