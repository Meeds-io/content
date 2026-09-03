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
package io.meeds.content.news.digest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.commons.digest.model.DigestItem;
import io.meeds.commons.digest.model.DigestLine;
import io.meeds.commons.digest.plugin.DigestLineContext;
import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class NewsDigestLinePluginTest {

  private static final DigestLineContext CONTEXT = new DigestLineContext("ayoub", Locale.ENGLISH, ZoneId.of("Europe/Paris"));

  @Mock
  private NewsService                    newsService;

  @Mock
  private SpaceService                   spaceService;

  private NewsDigestLinePlugin           plugin;

  @Before
  public void setUp() {
    InitParams params = new InitParams();
    ValuesParam pluginIds = new ValuesParam();
    pluginIds.setName("pluginIds");
    pluginIds.setValues(new ArrayList<>(List.of(NewsDigestLinePlugin.POST_NEWS_PLUGIN)));
    params.addParameter(pluginIds);
    plugin = new NewsDigestLinePlugin(params, newsService, spaceService);
  }

  @Test
  public void testPublishedArticleLine() {
    News news = new News();
    news.setId("12");
    news.setTitle("Quarterly results");
    news.setSpaceDisplayName("Finance");
    news.setUrl("https://platform/portal/g/finance/news/12");
    when(newsService.getNewsArticleById("12")).thenReturn(news);

    DigestLine line = plugin.buildLine(item("12"), CONTEXT);
    assertNotNull(line);
    assertEquals("digest.line.PostNewsNotificationPlugin", line.getLabelKey());
    assertEquals(List.of("Finance", "Quarterly results"), line.getArgs());
    assertEquals("https://platform/portal/g/finance/news/12", line.getUrl());
  }

  @Test
  public void testSpaceNameIsLookedUpWhenTheArticleDoesNotCarryIt() {
    News news = new News();
    news.setId("12");
    news.setTitle("Quarterly results");
    news.setSpaceId("42");
    when(newsService.getNewsArticleById("12")).thenReturn(news);
    Space space = new Space();
    space.setDisplayName("Finance");
    lenient().when(spaceService.getSpaceById("42")).thenReturn(space);

    DigestLine line = plugin.buildLine(item("12"), CONTEXT);
    assertNotNull(line);
    assertEquals("Finance", line.getArgs().get(0));
    assertNull(line.getUrl());
  }

  @Test
  public void testVanishedArticleGivesNoLine() {
    assertNull(plugin.buildLine(item("404"), CONTEXT));
    assertNull(plugin.buildLine(new DigestItem(1, "ayoub", NewsDigestLinePlugin.POST_NEWS_PLUGIN, "news", Instant.now(), Map.of()),
                                CONTEXT));
  }

  @Test
  public void testOtherTypesGiveNoLine() {
    assertNull(plugin.buildLine(new DigestItem(1, "ayoub", "MentionInNewsNotificationPlugin", "news", Instant.now(), Map.of("NEWS_ID", "12")),
                                CONTEXT));
  }

  private static DigestItem item(String newsId) {
    return new DigestItem(1, "ayoub", NewsDigestLinePlugin.POST_NEWS_PLUGIN, "news", Instant.now(), Map.of("NEWS_ID", newsId));
  }

}
