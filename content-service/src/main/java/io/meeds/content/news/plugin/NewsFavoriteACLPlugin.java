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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.security.Identity;
import org.exoplatform.social.metadata.FavoriteACLPlugin;
import org.exoplatform.social.metadata.favorite.FavoriteService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils;

import jakarta.annotation.PostConstruct;

/**
 * Allows to mark a news as favorite only for users who can view it
 */
@Component
public class NewsFavoriteACLPlugin extends FavoriteACLPlugin {

  @Autowired
  private FavoriteService favoriteService;

  @Autowired
  private NewsService     newsService;

  @PostConstruct
  public void init() {
    favoriteService.addFavoriteACLPlugin(this);
  }

  @Override
  public String getEntityType() {
    return NewsUtils.NEWS_METADATA_OBJECT_TYPE;
  }

  @Override
  public boolean canCreateFavorite(Identity userIdentity, String entityId) {
    if (userIdentity == null || StringUtils.isBlank(entityId)) {
      return false;
    }
    // A news Metadata object id can carry a '-<lang>' suffix for a
    // translated article, resolve it to its base article id
    String newsId = StringUtils.substringBefore(entityId, "-");
    if (!StringUtils.isNumeric(newsId)) {
      return false;
    }
    News news = newsService.getNewsArticleById(newsId);
    return news != null && !news.isDeleted() && newsService.canViewNews(news, userIdentity.getUserId());
  }

}
