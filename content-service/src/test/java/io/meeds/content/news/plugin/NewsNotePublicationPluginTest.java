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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.model.Page;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@ExtendWith(MockitoExtension.class)
class NewsNotePublicationPluginTest {

  private static final String       NOTE_ID     = "12";

  private static final String       ACTIVITY_ID = "55";

  @Mock
  private NewsService               newsService;

  @Mock
  private Identity                  identity;

  @InjectMocks
  private NewsNotePublicationPlugin plugin;

  @Test
  void publishNoteShouldReturnTheActivityOfThePostedArticle() throws Exception {
    Page note = note();
    News article = new News();
    article.setId(NOTE_ID);
    article.setActivityId(ACTIVITY_ID);
    when(newsService.postNoteArticle(note, identity)).thenReturn(article);

    assertEquals(ACTIVITY_ID, plugin.publishNote(note, identity));
  }

  @Test
  void publishNoteShouldNotHandleANoteThatIsNotPostedAsArticle() throws Exception {
    Page note = note();
    when(newsService.postNoteArticle(note, identity)).thenReturn(null);

    assertNull(plugin.publishNote(note, identity));
  }

  private Page note() {
    Page note = new Page("note");
    note.setId(NOTE_ID);
    return note;
  }

}
