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
package io.meeds.content.news.digest;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.commons.digest.model.DigestItem;
import io.meeds.commons.digest.model.DigestLine;
import io.meeds.commons.digest.plugin.DigestLineContext;
import io.meeds.commons.digest.plugin.DigestLinePlugin;
import io.meeds.content.news.model.News;
import io.meeds.content.news.notification.utils.NotificationConstants;
import io.meeds.content.news.service.NewsService;

/**
 * The digest email line of a published article: "{space} published
 * "{title}"", read fresh from the article id. An article that no longer
 * exists gives no line.
 */
public class NewsDigestLinePlugin extends DigestLinePlugin {

  public static final String  POST_NEWS_PLUGIN = "PostNewsNotificationPlugin";

  private static final String LINE_KEY_PREFIX  = "digest.line.";

  private NewsService         newsService;

  private SpaceService        spaceService;

  public NewsDigestLinePlugin(InitParams params) {
    super(params);
  }

  NewsDigestLinePlugin(InitParams params, NewsService newsService, SpaceService spaceService) {
    super(params);
    this.newsService = newsService;
    this.spaceService = spaceService;
  }

  @Override
  public DigestLine buildLine(DigestItem item, DigestLineContext context) {
    if (!POST_NEWS_PLUGIN.equals(item.getPluginId())) {
      return null;
    }
    String newsId = item.getParam(NotificationConstants.NEWS_ID);
    News news = StringUtils.isBlank(newsId) ? null : getNewsService().getNewsArticleById(newsId);
    if (news == null || StringUtils.isBlank(news.getTitle())) {
      return null;
    }
    return DigestLine.of(LINE_KEY_PREFIX + item.getPluginId(), spaceName(news), news.getTitle()).withUrl(url(news));
  }

  private String spaceName(News news) {
    if (StringUtils.isNotBlank(news.getSpaceDisplayName())) {
      return news.getSpaceDisplayName();
    }
    Space space = StringUtils.isBlank(news.getSpaceId()) ? null : getSpaceService().getSpaceById(news.getSpaceId());
    return space == null ? "" : space.getDisplayName();
  }

  /** The article link, made absolute the way the instant emails do */
  static String url(News news) {
    String url = news.getUrl();
    if (StringUtils.isBlank(url)) {
      return null;
    }
    return url.startsWith("http") ? url : CommonsUtils.getCurrentDomain() + url;
  }

  private NewsService getNewsService() {
    if (newsService == null) {
      newsService = ExoContainerContext.getService(NewsService.class);
    }
    return newsService;
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = ExoContainerContext.getService(SpaceService.class);
    }
    return spaceService;
  }

}
