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

  private EntityBuilder() {
  }

  public static NewsSearchResultEntity fromNewsSearchResult(FavoriteService favoriteService,
                                                            NewsESSearchResult newsESSearchResult,
                                                            Identity currentIdentity) {
    NewsSearchResultEntity newsSearchResultEntity = new NewsSearchResultEntity(newsESSearchResult);
    Favorite favorite = new Favorite(NewsUtils.NEWS_METADATA_OBJECT_TYPE,
                                     newsESSearchResult.getLang() != null ? newsESSearchResult.getId().concat("-").concat(newsESSearchResult.getLang()) : newsESSearchResult.getId(),
                                     null,
                                     Long.parseLong(currentIdentity.getId()));
    newsSearchResultEntity.setFavorite(favoriteService.isFavorite(favorite));

    return newsSearchResultEntity;
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
