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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.social.attachment.AttachmentService;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class NewsAttachmentPluginTest {

  @Mock
  private NewsService                        newsService;

  @Mock
  private AttachmentService                  attachmentService;

  @InjectMocks
  private NewsPageAttachmentPlugin plugin;

  @Before
  public void setUp() {
    plugin.init();
  }

  @Test
  public void testGetObjectType() {
    Assert.assertEquals("articlePage", plugin.getObjectType());
  }

  @Test
  public void testHasAccessPermission() throws Exception {
    org.exoplatform.services.security.Identity userIdentity = mock(org.exoplatform.services.security.Identity.class);
    News news = mock(News.class);

    when(newsService.getNewsArticleById(anyString())).thenReturn(news);
    when(newsService.canViewNews(news, userIdentity.getUserId())).thenReturn(true);

    assertTrue(plugin.hasAccessPermission(userIdentity, "1"));
  }

  @Test
  public void testHasEditPermission() throws Exception {
    org.exoplatform.services.security.Identity userIdentity = mock(org.exoplatform.services.security.Identity.class);
    News news = mock(News.class);

    when(newsService.getNewsById("1", userIdentity, false, ARTICLE.name())).thenReturn(news);
    when(news.isCanEdit()).thenReturn(true);

    assertTrue(plugin.hasEditPermission(userIdentity, "1"));
  }

}
