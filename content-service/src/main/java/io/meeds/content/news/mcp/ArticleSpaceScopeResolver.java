/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.news.mcp;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;
import io.meeds.mcp.server.plugin.SpaceScopeResolver;

/**
 * Tells which space a news article belongs to, for the space scope of the
 * news tools that reach an article by its id: the space whose notebook holds
 * the article, as long as the article stays inside it. An article that isn't
 * posted (a draft or a scheduled one, which only its author and the space
 * publishers see), a deleted one, one published to the platform's publication
 * targets, one shared into other spaces, one outside any space, a missing
 * article, an id that isn't one and any failure belong to no space. The
 * article is read through {@link NewsService#getNewsArticleById(String)}, and
 * the decision reads no viewer-dependent field.
 */
@Service("articleSpaceScopeResolver")
@Profile("mcp-server")
public class ArticleSpaceScopeResolver implements SpaceScopeResolver {

  /** The name the news tools' {@code space_scope.resolver} gives. */
  public static final String NAME = "article";

  /** The logger. */
  private static final Log   LOG  = ExoLogger.getLogger(ArticleSpaceScopeResolver.class);

  /** Reads the article. */
  @Autowired
  private NewsService        newsService;

  /**
   * @return {@value #NAME}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * @param objectId the article id
   * @return the id of the space the article stays in, or null for an article
   *         that doesn't stay in one space (see the class description), a
   *         missing article, an id that isn't one, or a lookup that fails
   */
  @Override
  public Long spaceOf(String objectId) {
    if (!StringUtils.isNumeric(objectId)) {
      return null;
    }
    try {
      News article = newsService.getNewsArticleById(objectId);
      return article == null ? null : spaceOfArticle(article);
    } catch (Exception e) {
      LOG.debug("The space of article '{}' couldn't be read", objectId, e);
      return null;
    }
  }

  /**
   * @param article an article as {@link NewsService#getNewsArticleById(String)}
   *          builds it, its publication targets included
   * @return the id of the space the article stays in, or null when it is not
   *         posted, deleted, published to a target, shared into another
   *         space or outside any space
   */
  private Long spaceOfArticle(News article) {
    if (article.isDeleted()
        || !NewsService.POSTED.equals(article.getPublicationState())
        || article.isPublished()
        || CollectionUtils.isNotEmpty(article.getTargets())
        || CollectionUtils.isNotEmpty(article.getSharedInSpacesList())
        || !StringUtils.isNumeric(article.getSpaceId())) {
      return null;
    }
    return Long.valueOf(article.getSpaceId());
  }

}
