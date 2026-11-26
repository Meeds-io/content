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

import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.ARTICLE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

import jakarta.annotation.PostConstruct;

@Component
public class NewsPageAttachmentPlugin extends AttachmentPlugin {

  public static final String OBJECT_TYPE = "articlePage";

  private static final Log   LOG         = ExoLogger.getLogger(NewsPageAttachmentPlugin.class);

  @Autowired
  private AttachmentService  attachmentService;

  @Autowired
  private NewsService        newsService;

  @Autowired
  private NoteService        noteService;

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
    Boolean articlePage = isArticlePage(articleId);
    if (articlePage == null) {
      return false;
    } else if (!articlePage.booleanValue()) {
      Page note = getKeptNote(articleId, identity);
      return note != null && note.isCanView();
    }
    News news = newsService.getNewsArticleById(articleId);
    return news != null && newsService.canViewNews(news, identity.getUserId());
  }

  @Override
  public boolean hasEditPermission(Identity identity, String articleId) throws ObjectNotFoundException {
    Boolean articlePage = isArticlePage(articleId);
    if (articlePage == null) {
      return false;
    } else if (!articlePage.booleanValue()) {
      Page note = getKeptNote(articleId, identity);
      return note != null && note.isCanManage();
    }
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

  /**
   * @return whether the page is still an article, or null when that cannot be
   *         read: the caller then refuses, rather than granting the note's
   *         rights to an article whose item could not be read
   */
  private Boolean isArticlePage(String pageId) {
    try {
      return newsService.isArticlePage(pageId);
    } catch (Exception e) {
      LOG.warn("Cannot read the article item of page {}", pageId, e);
      return null; // NOSONAR null means unknown, which the callers refuse
    }
  }

  /**
   * An article deleted while keeping its note leaves its images and files
   * under this object type and the page id: once the page is no longer an
   * article, they follow the note's own rights.
   */
  private Page getKeptNote(String pageId, Identity identity) {
    try {
      Page note = noteService.getNoteById(pageId, identity);
      return note == null || note.isDeleted() ? null : note;
    } catch (Exception e) {
      return null;
    }
  }
}
