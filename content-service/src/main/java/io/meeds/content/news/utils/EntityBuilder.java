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
package io.meeds.content.news.utils;

import io.meeds.content.news.model.ArticleTarget;
import io.meeds.content.news.model.News;
import org.apache.commons.collections4.CollectionUtils;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataItem;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataItem;

import io.meeds.content.news.model.ArticleTarget;
import io.meeds.content.news.rest.model.NewsSearchResultEntity;
import io.meeds.content.news.search.NewsESSearchResult;

public class EntityBuilder {

  public static NewsSearchResultEntity toSearchResult(News news) {
    if (news == null) return null;

    NewsSearchResultEntity result = new NewsSearchResultEntity();
    result.setId(news.getId());
    result.setTitle(news.getTitle());
    result.setBody(news.getBody());
    result.setSummary(news.getProperties() != null ? news.getProperties().getSummary() : null);
    result.setLang(news.getLang());
    result.setSpaceId(news.getSpaceId());
    result.setNewsUrl(news.getUrl());
    result.setLastUpdatedTime(news.getUpdateDate() != null ? news.getUpdateDate().getTime() : 0L);
    result.setFavorite(news.isFavorite());
    result.setActivityId(news.getActivityId());
    result.setUpdaterUserName(news.getUpdater() != null ? news.getUpdater() : news.getAuthor());
    result.setSpaceAvatar(news.getSpaceAvatarUrl());
    result.setSpaceDisplayName(news.getSpaceDisplayName());
    return result;
  }

  public static ArticleTarget toArticleTarget(MetadataItem metadataItem) {
    if (metadataItem == null) {
      return null;
    }
    Metadata metadata = metadataItem.getMetadata();
    Map<String, String> properties = metadataItem.getProperties();
    String publishedDate = properties != null ? properties.getOrDefault(NewsUtils.PUBLISHED_DATE, "0") : "0";
    return new ArticleTarget(metadata.getName(), Long.parseLong(publishedDate));
  }
  
  public static List<ArticleTarget> toArticleTargets(List<MetadataItem> metadataItems) {
    if (CollectionUtils.isEmpty(metadataItems)) {
      return null;
    }
    return metadataItems.stream().map(EntityBuilder::toArticleTarget).toList();
  }
}
