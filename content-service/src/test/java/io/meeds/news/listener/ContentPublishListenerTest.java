/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
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

import static org.mockito.Mockito.*;

import io.meeds.news.model.ContentPublishEvent;
import io.meeds.news.model.News;
import io.meeds.news.service.NewsTargetingService;
import io.meeds.notes.model.NotePageProperties;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentPublishListenerTest {

  @Mock
  private SpaceService           spaceService;

  @Mock
  private ListenerService        listenerService;

  @Mock
  private IdentityManager        identityManager;

  @Mock
  private NewsTargetingService   newsTargetingService;

  @InjectMocks
  private ContentPublishListener contentPublishListener;

  @BeforeEach
  void setUp() {
    contentPublishListener.init();
  }

  @Test
  void testInitAddsListeners() {
    verify(listenerService, times(1)).addListener("createPublishContent", contentPublishListener);
    verify(listenerService, times(1)).addListener("updatePublishContent", contentPublishListener);
  }

  @Test
  void testOnEventValidPublish() throws Exception {
    Event<String, ContentPublishEvent> event = mock(Event.class);
    ContentPublishEvent eventData = mock(ContentPublishEvent.class);

    NotePageProperties properties = new NotePageProperties();
    properties.setHideAuthor(false);

    News originalArticle = mock(News.class);
    News updatedArticle = mock(News.class);
    Identity identity = mock(Identity.class);

    when(updatedArticle.getProperties()).thenReturn(properties);

    when(event.getSource()).thenReturn("user123");
    when(identityManager.getOrCreateUserIdentity("user123")).thenReturn(identity);
    when(identity.getId()).thenReturn("42");
    when(event.getData()).thenReturn(eventData);
    when(event.getEventName()).thenReturn("updatePublishContent");
    when(eventData.getOriginalArticle()).thenReturn(originalArticle);
    when(eventData.getUpdatedArticle()).thenReturn(updatedArticle);
    when(originalArticle.isPublished()).thenReturn(false);
    when(updatedArticle.isPublished()).thenReturn(true);

    contentPublishListener.onEvent(event);

    verify(updatedArticle, times(1)).getTitle();
  }
}
