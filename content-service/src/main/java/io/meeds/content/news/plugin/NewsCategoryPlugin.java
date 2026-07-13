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
package io.meeds.content.news.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;
import io.meeds.social.category.model.CategoryEntryItem;
import io.meeds.social.category.plugin.CategoryPlugin;
import io.meeds.social.category.service.CategoryPluginService;

import jakarta.annotation.PostConstruct;

@Component
public class NewsCategoryPlugin implements CategoryPlugin {

  private static final String ICON = "fa-newspaper";

  public static final String  OBJECT_TYPE = NewsPermanentLinkPlugin.OBJECT_TYPE;

  @Autowired
  private PortalContainer      container;

  @Autowired
  private NewsService          newsService;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(CategoryPluginService.class).addPlugin(this);
  }

  @Override
  public String getType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canAccess(String objectId, String username) {
    News news = newsService.getNewsArticleById(objectId);
    return news != null && newsService.canViewNews(news, username);
  }

  @Override
  public boolean canEdit(String objectId, String username) {
    News news = newsService.getNewsArticleById(objectId);
    return news != null && newsService.canEditNews(news, username);
  }

  @Override
  public CategoryEntryItem getEntryItem(String objectId, String username) {
    News news = newsService.getNewsArticleById(objectId);
    if (news == null) {
      return null;
    }
    return new CategoryEntryItem(news.getId(),
                                   OBJECT_TYPE,
                                   ICON,
                                   news.getTitle(),
                                   news.getProperties() == null ? null : news.getProperties().getSummary(),
                                   news.getIllustrationURL(),
                                   news.getUrl(),
                                   news.getAuthorDisplayName(),
                                   news.getAuthorAvatarUrl(),
                                   news.getSpaceDisplayName(),
                                   news.getSpaceAvatarUrl(),
                                   news.getUpdateDate(),
                                   news.getLikesCount(),
                                   news.getCommentsCount(),
                                   news.getViewsCount() == null ? 0 : news.getViewsCount(),
                                   news.getCategories());
  }

}
