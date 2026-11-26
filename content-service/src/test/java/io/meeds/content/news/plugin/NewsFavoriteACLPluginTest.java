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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.security.Identity;
import org.exoplatform.social.metadata.favorite.FavoriteService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class NewsFavoriteACLPluginTest {

  private static final String   USER = "john";

  @Mock
  private FavoriteService       favoriteService;

  @Mock
  private NewsService           newsService;

  @Mock
  private News                  news;

  @InjectMocks
  private NewsFavoriteACLPlugin plugin;

  private Identity              identity = new Identity(USER);

  @BeforeEach
  void setUp() {
    plugin.init();
  }

  @Test
  void testRegistration() {
    verify(favoriteService).addFavoriteACLPlugin(plugin);
    assertEquals("news", plugin.getEntityType());
  }

  /**
   * Guards the registration running at startup: without
   * {@code @PostConstruct} on init(), the ACL plugin is never registered.
   */
  @Test
  void testRegistrationRunsAtStartup() throws Exception {
    assertTrue(NewsFavoriteACLPlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
  }

  @Test
  void testViewableNews() {
    when(newsService.getNewsArticleById("12")).thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(true);
    assertTrue(plugin.canCreateFavorite(identity, "12"));
  }

  @Test
  void testNotViewableNews() {
    when(newsService.getNewsArticleById("12")).thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(false);
    assertFalse(plugin.canCreateFavorite(identity, "12"));
  }

  @Test
  void testDeletedNews() {
    when(newsService.getNewsArticleById("12")).thenReturn(news);
    when(news.isDeleted()).thenReturn(true);
    assertFalse(plugin.canCreateFavorite(identity, "12"));
    verify(newsService, never()).canViewNews(any(News.class), nullable(String.class));
  }

  @Test
  void testTranslatedNewsId() {
    when(newsService.getNewsArticleById("12")).thenReturn(news);
    when(newsService.canViewNews(news, USER)).thenReturn(true);
    assertTrue(plugin.canCreateFavorite(identity, "12-fr"));
  }

  @Test
  void testMissingNews() {
    when(newsService.getNewsArticleById("12")).thenReturn(null);
    assertFalse(plugin.canCreateFavorite(identity, "12"));
  }

  @Test
  void testInvalidIds() {
    assertFalse(plugin.canCreateFavorite(identity, "abc"));
    assertFalse(plugin.canCreateFavorite(identity, "abc-fr"));
    assertFalse(plugin.canCreateFavorite(identity, "-fr"));
    assertFalse(plugin.canCreateFavorite(identity, ""));
    assertFalse(plugin.canCreateFavorite(identity, null));
    assertFalse(plugin.canCreateFavorite(null, "12"));
    verify(newsService, never()).getNewsArticleById(nullable(String.class));
  }

}
