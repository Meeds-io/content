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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.social.core.manager.ActivityManager;

import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils;

@RunWith(MockitoJUnitRunner.class)
public class NewsActivityTypePluginTest {

  private static final String    NEWS_ID     = "123";

  private static final String    ACTIVITY_ID = "456";

  @Mock
  private NewsService            newsService;

  @Mock
  private ActivityManager        activityManager;

  @InjectMocks
  private NewsActivityTypePlugin newsActivityTypePlugin;

  @Test
  public void testPluginDeclaration() {
    assertEquals("news", newsActivityTypePlugin.getActivityType());
    assertEquals(NewsUtils.NEWS_METADATA_OBJECT_TYPE, newsActivityTypePlugin.getMetadataObjectType());
    assertEquals(false, newsActivityTypePlugin.isEnableNotification());

    newsActivityTypePlugin.init();
    verify(activityManager).addActivityTypePlugin(newsActivityTypePlugin);
  }

  @Test
  public void testGetActivityIds() {
    when(newsService.getNewsActivityId(NEWS_ID)).thenReturn(ACTIVITY_ID);

    List<String> activityIds = newsActivityTypePlugin.getActivityIds(List.of(NEWS_ID));
    assertEquals(List.of(ACTIVITY_ID), activityIds);
  }

  @Test
  public void testGetActivityIdsStripsLanguageSuffixAndDeduplicates() {
    when(newsService.getNewsActivityId(NEWS_ID)).thenReturn(ACTIVITY_ID);
    // Would resolve the suffixed id if the plugin didn't strip the language
    // suffix before resolving
    lenient().when(newsService.getNewsActivityId(NEWS_ID + "-fr")).thenReturn("unexpectedActivityId");

    // A favorite on a translated article carries a '-<lang>' suffixed id and
    // resolves to the same activity as the base article, once
    List<String> activityIds = newsActivityTypePlugin.getActivityIds(Arrays.asList(NEWS_ID, NEWS_ID + "-fr"));
    assertEquals(List.of(ACTIVITY_ID), activityIds);
  }

  @Test
  public void testGetActivityIdsOmitsUnresolvableNews() {
    when(newsService.getNewsActivityId(NEWS_ID)).thenReturn(ACTIVITY_ID);
    when(newsService.getNewsActivityId("999")).thenReturn(null);
    when(newsService.getNewsActivityId("888")).thenThrow(new IllegalStateException("unexpected error"));

    List<String> activityIds = newsActivityTypePlugin.getActivityIds(Arrays.asList("999", "888", NEWS_ID));
    assertEquals(List.of(ACTIVITY_ID), activityIds);
  }
}
