/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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

import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.ARTICLE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

import jakarta.annotation.PostConstruct;

@Component
public class NewsPageAttachmentPlugin extends AttachmentPlugin {

  @Autowired
  private AttachmentService  attachmentService;

  @Autowired
  private NewsService        newsService;

  public static final String OBJECT_TYPE = "articlePage";

  @PostConstruct
  public void init() {
    attachmentService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(Identity identity, String articleId) throws ObjectNotFoundException {
    News news = newsService.getNewsArticleById(articleId);
    return news != null && newsService.canViewNews(news, identity.getUserId());
  }

  @Override
  public boolean hasEditPermission(Identity identity, String articleId) throws ObjectNotFoundException {
    News news = null;
    try {
      news = newsService.getNewsById(articleId, identity, false, ARTICLE.name());
    } catch (IllegalAccessException e) {
      return false;
    }
    return news != null && news.isCanEdit();
  }

  @Override
  public long getAudienceId(String s) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(String articleId) throws ObjectNotFoundException {
    News news = newsService.getNewsArticleById(articleId);
    return news != null ? Long.parseLong(news.getSpaceId()) : 0;
  }
}
