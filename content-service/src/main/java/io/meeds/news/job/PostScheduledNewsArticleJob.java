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
package io.meeds.news.job;

import io.meeds.common.ContainerTransactional;
import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import org.apache.commons.collections4.MapUtils;
import org.exoplatform.social.metadata.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import static io.meeds.news.service.impl.NewsServiceImpl.*;

@Component
public class PostScheduledNewsArticleJob {

  private static final Logger LOG = LoggerFactory.getLogger(PostScheduledNewsArticleJob.class);

  @Autowired
  private NewsService         newsService;

  @Autowired
  private MetadataService     metadataService;


  @Scheduled(cron = "${meeds.content.postScheduledNewsArticle.job.cron:15 */2 * * * ?}")
  @ContainerTransactional
  public void postScheduledNewsArticle() {

    metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndMetadataItemProperty(NEWS_METADATA_NAME,
                                                                                          NEWS_METADATA_TYPE.getName(),
                                                                                          NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                                                          NEWS_PUBLICATION_STATE,
                                                                                          STAGED,
                                                                                          0,
                                                                                          0)
                   .stream()
                   .filter(scheduledArticleMetadataItem -> {
                     try {
                       if (scheduledArticleMetadataItem.getProperties() != null && !scheduledArticleMetadataItem.getProperties().isEmpty()) {
                         String articleScheduleDate = scheduledArticleMetadataItem.getProperties().getOrDefault(SCHEDULE_POST_DATE, null);
                         if (articleScheduleDate != null) {

                           if (isScheduleDatePassed(articleScheduleDate)) {
                             // return only the metadata items with a schedule date property equals or prior to
                             // the current date to be mapped to news articles and then post them.
                             return true;
                           }
                         }
                       }
                     } catch (Exception e) {
                       return false;
                     }
                     return false; // Return false if conditions are not met or exception occurred
                   })
                   .map(scheduledArticle -> newsService.getNewsArticleById(scheduledArticle.getObjectId()))
                   .forEach(article -> {
                     try {
                       if (article != null) {
                         article = newsService.postNews(article, article.getAuthor());
                         LOG.info("News scheduled news posted to {}", article.getUrl());
                       }
                     } catch (Exception e) {
                       LOG.error("Error posting scheduled news article", e);
                     }
                   });
  }

  @Scheduled(cron = "${meeds.content.postScheduledNewsArticle.job.cron:15 */2 * * * ?}")
  @ContainerTransactional
  public void unpublishArticle() {
    metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndMetadataItemProperty(NEWS_METADATA_NAME,
                                                                                          NEWS_METADATA_TYPE.getName(),
                                                                                          NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                                                          UNPUBLISH_SCHEDULED,
                                                                                          "true",
                                                                                          0,
                                                                                          0)
                   .stream()
                   .filter(item -> {
                     if (MapUtils.isNotEmpty(item.getProperties())) {
                       String scheduledUnpublishDate = item.getProperties().getOrDefault(UNPUBLISH_SCHEDULED_DATE, null);
                       return scheduledUnpublishDate != null && isScheduleDatePassed(scheduledUnpublishDate);
                     }
                     return false;
                   })
                   .map(article -> newsService.getNewsArticleById(article.getObjectId()))
                   .forEach(article -> {
                     if (article != null) {
                       try {
                         newsService.unpublishNews(article.getId(), article.getAuthor());
                         LOG.info("Unpublish schedule executed articleId: {}, articleTitle: {}",
                                  article.getId(),
                                  article.getTitle());
                       } catch (Exception e) {
                         LOG.error("Error unpublishing scheduled news article", e);
                       }
                     }
                   });
  }

  private boolean isScheduleDatePassed(String schedulePostDateString) {
    Calendar schedulePostDate = Calendar.getInstance();
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      format.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
      schedulePostDate.setTime(format.parse(schedulePostDateString));
    } catch (ParseException e) {
      // Handle exception as needed
      LOG.error("Error parsing schedule post date", e);
      return false;
    }

    Calendar now = Calendar.getInstance();
    return schedulePostDate.before(now) || now.equals(schedulePostDate);
  }
}
