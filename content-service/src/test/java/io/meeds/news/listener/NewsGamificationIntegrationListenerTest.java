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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import io.meeds.news.utils.NewsUtils;

@RunWith(MockitoJUnitRunner.class)
public class NewsGamificationIntegrationListenerTest {

  @Mock
  ListenerService listenerService;

  @Mock
  NewsService     newsService;

  @Test
  public void testAddGamificationPointsAfterCreatingAnArticle() throws Exception { // NOSONAR
    News news = new News();
    news.setTitle("title");
    news.setAuthor("jean");
    news.setId("id123");
    news.setSpaceId("3");
    news.setActivities("3:39;1:11");
    news.setActivityId("10");

    AtomicBoolean executeListener = new AtomicBoolean(true);
    listenerService.addListener(NewsUtils.POST_NEWS, new Listener<Long, Long>() {
      @Override
      public void onEvent(Event<Long, Long> event) throws Exception {
        executeListener.set(true);
      }
    });
    newsService.postNews(news, "root");
    assertTrue(executeListener.get());
  }

}
