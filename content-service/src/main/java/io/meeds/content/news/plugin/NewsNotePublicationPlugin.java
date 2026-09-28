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
package io.meeds.content.news.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.model.Page;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;
import io.meeds.notes.plugin.NotePublicationPlugin;

/**
 * Publishes a space note as a news article, as the Notes publication drawer
 * does, when the publication is requested outside the Notes UI.
 */
@Service
public class NewsNotePublicationPlugin implements NotePublicationPlugin {

  @Autowired
  private NewsService newsService;

  @Override
  public String publishNote(Page note, Identity identity) throws Exception { // NOSONAR
    News article = newsService.postNoteArticle(note, identity);
    return article == null ? null : article.getActivityId();
  }

}
