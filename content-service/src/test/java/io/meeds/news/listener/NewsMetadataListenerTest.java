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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.exoplatform.services.listener.ListenerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;

import io.meeds.news.model.News;
import io.meeds.news.search.NewsIndexingServiceConnector;
import io.meeds.news.utils.NewsUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class NewsMetadataListenerTest {

  private static final String USERNAME = "testuser";

  @Mock
  private IndexingService     indexingService;

  @Mock
  private SpaceService        spaceService;

  @Mock
  private IdentityManager     identityManager;

  @Mock
  private TagService          tagService;

  @Mock
  private ListenerService     listenerService;

  @InjectMocks
  NewsMetadataListener        newsMetadataListener;

  @Before
  public void setUp() {
    newsMetadataListener.init();
  }
  @Test
  public void testCreateNewsTagsWhenNewsSaved() throws Exception {
    String newsId = "newsId";
    String spaceId = "spaceId";
    String content = "Test #tag1 Test #tag2.";

    News news = mock(News.class);
    when(news.getId()).thenReturn(newsId);
    when(news.getSpaceId()).thenReturn(spaceId);
    when(news.getBody()).thenReturn(content);

    Event<String, News> event = mock(Event.class);
    when(event.getData()).thenReturn(news);
    when(event.getSource()).thenReturn(USERNAME);

    HashSet<TagName> contentTags = new HashSet<>(Arrays.asList(new TagName("tag1"), new TagName("tag2")));

    String spacePrettyName = "spacePrettyName";

    Space space = mock(Space.class);
    when(space.getPrettyName()).thenReturn(spacePrettyName);
    when(spaceService.getSpaceById(spaceId)).thenReturn(space);

    String spaceIdentityId = "200";
    Identity spaceIdentity = mock(Identity.class);
    when(spaceIdentity.getId()).thenReturn(spaceIdentityId);

    when(identityManager.getOrCreateSpaceIdentity(spacePrettyName)).thenReturn(spaceIdentity);

    String userIdentityId = "300";
    Identity userIdentity = mock(Identity.class);
    when(userIdentity.getId()).thenReturn(userIdentityId);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);

    when(tagService.detectTagNames(content)).thenReturn(contentTags);

    newsMetadataListener.onEvent(event);

    verify(indexingService, times(1)).reindex(NewsIndexingServiceConnector.TYPE, newsId);
    verify(tagService, times(1)).saveTags(new TagObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE, news.getId(), null),
                                          contentTags,
                                          Long.parseLong(spaceIdentityId),
                                          Long.parseLong(userIdentityId));
  }

}
