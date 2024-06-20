/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.news.listener;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.exoplatform.services.listener.ListenerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.social.metadata.model.MetadataItem;

import io.meeds.news.model.News;
import io.meeds.news.search.NewsIndexingServiceConnector;
import io.meeds.news.service.NewsService;
import io.meeds.news.utils.NewsUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class MetadataItemModifiedTest {

  @Mock
  private IndexingService       indexingService;

  @Mock
  private NewsService           newsService;

  @Mock
  private CachedActivityStorage activityStorage;

  @Mock
  private ListenerService        listenerService;

  @InjectMocks
  MetadataItemModified           metadataItemModified;

  @Before
  public void setUp() {
    metadataItemModified.init();
  }

  @Test
  public void testNoInteractionWhenMetadataNotForNews() throws Exception {
    MetadataItem metadataItem = mock(MetadataItem.class);
    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);
    when(event.getData().getObjectType()).thenReturn("activity");
    when(metadataItem.getObjectId()).thenReturn("1");

    metadataItemModified.onEvent(event);

    verifyNoInteractions(newsService);
  }

  @Test
  public void testReindexNewsWhenNewsSetAsFavorite() throws Exception {
    String newsId = "100";

    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getObjectType()).thenReturn(NewsUtils.NEWS_METADATA_OBJECT_TYPE);
    when(metadataItem.getObjectId()).thenReturn(newsId);

    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);

    News news = new News();
    news.setId(newsId);
    when(newsService.getNewsById(eq(newsId), anyBoolean())).thenReturn(news);

    metadataItemModified.onEvent(event);
    verify(newsService, times(1)).getNewsById(newsId, false);
    verify(indexingService, times(1)).reindex(NewsIndexingServiceConnector.TYPE, newsId);
  }

  @Test
  public void testCleanNewsActivityCacheWhenMarkAsFavorite() throws Exception {
    String newsId = "200";

    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getObjectType()).thenReturn(NewsUtils.NEWS_METADATA_OBJECT_TYPE);
    when(metadataItem.getObjectId()).thenReturn(newsId);

    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);

    String activityId = "activityId";
    News news = new News();
    news.setId(newsId);
    news.setActivityId(activityId);
    when(newsService.getNewsById(eq(newsId), anyBoolean())).thenReturn(news);

    metadataItemModified.onEvent(event);
    verify(newsService, times(1)).getNewsById(newsId, false);
    verify(indexingService, times(1)).reindex(NewsIndexingServiceConnector.TYPE, newsId);
    verify(activityStorage, times(1)).clearActivityCached(activityId);
  }

}
