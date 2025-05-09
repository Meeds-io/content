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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.service.NewsService;
import io.meeds.social.cms.model.ContentLinkExtension;
import io.meeds.social.cms.model.ContentLinkSearchResult;
import io.meeds.social.cms.service.ContentLinkPluginService;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public class NewsContentLinkPluginTest {

  private static final String      NEWS_TITLE = "newsTitle";

  private static final String      NEWS_ID    = "newsId";

  private static final String      TEST_USER  = "testUser";

  private static final String      QUERY      = "query";

  @Mock
  private ContentLinkPluginService contentLinkPluginService;

  @Mock
  private NewsService              newsService;

  @Mock
  private UserACL                  userAcl;

  @Mock
  private IdentityManager          identityManager;

  @Mock
  private Identity                 identity;

  @Mock
  private News                     news;

  @InjectMocks
  private NewsContentLinkPlugin    plugin;

  @Test
  public void getExtension() {
    ContentLinkExtension extension = plugin.getExtension();
    assertNotNull(extension);
    assertEquals("news", extension.getObjectType());
    assertEquals("article", extension.getCommand());
    assertEquals("fa fa-newspaper", extension.getIcon());
    assertEquals("contentLink.news", extension.getTitleKey());
  }

  @Test
  public void searchWhenEmpty() {
    when(userAcl.isAnonymousUser((Identity) null)).thenReturn(true);

    List<ContentLinkSearchResult> results = plugin.search(QUERY, null, null, 0, 10);
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  @SneakyThrows
  public void searchWithQuery() {
    NewsFilter filter = new NewsFilter();
    filter.setSearchText(QUERY);
    filter.setOffset(2);
    filter.setLimit(10);
    filter.setLang("en");

    when(identity.getUserId()).thenReturn(TEST_USER);
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity(TEST_USER)).thenReturn(userIdentity);
    when(news.getId()).thenReturn(NEWS_ID);
    when(news.getTitle()).thenReturn(NEWS_TITLE);

    List<ContentLinkSearchResult> results = plugin.search(QUERY, identity, Locale.ENGLISH, 2, 10);
    assertNotNull(results);
    assertTrue(results.isEmpty());

    when(newsService.searchNews(filter, userIdentity)).thenReturn(Collections.singletonList(news));
    results = plugin.search(QUERY, identity, Locale.ENGLISH, 2, 10);
    assertNotNull(results);
    assertEquals(1, results.size());
    ContentLinkSearchResult contentLinkSearchResult = results.get(0);
    assertEquals(plugin.getExtension().getObjectType(), contentLinkSearchResult.getObjectType());
    assertEquals(news.getId(), contentLinkSearchResult.getObjectId());
    assertEquals(plugin.getExtension().getIcon(), contentLinkSearchResult.getIcon());
    assertEquals(news.getTitle(), contentLinkSearchResult.getTitle());
  }

  @Test
  public void getContentTitleWhenNotFound() {
    assertNull(plugin.getContentTitle(NEWS_ID, null));
    assertNull(plugin.getContentTitle(NEWS_ID, Locale.ENGLISH));
  }

  @Test
  public void getContentTitleWhenNoLang() {
    when(newsService.getNewsArticleById(NEWS_ID)).thenReturn(news);
    when(news.getTitle()).thenReturn(NEWS_TITLE);
    assertEquals(NEWS_TITLE, plugin.getContentTitle(NEWS_ID, null));
    assertEquals(NEWS_TITLE, plugin.getContentTitle(NEWS_ID, Locale.ENGLISH));
  }

  @Test
  public void getContentTitle() {
    when(newsService.getNewsArticleByIdAndLang(eq(NEWS_ID), anyString())).thenReturn(news);
    when(news.getTitle()).thenReturn(NEWS_TITLE);
    assertEquals(NEWS_TITLE, plugin.getContentTitle(NEWS_ID, Locale.ENGLISH));
  }

}
