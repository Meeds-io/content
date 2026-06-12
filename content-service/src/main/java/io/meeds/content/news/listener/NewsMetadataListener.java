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
package io.meeds.content.news.listener;

import static io.meeds.content.news.utils.NewsUtils.POST_NEWS;
import static io.meeds.content.news.utils.NewsUtils.SHARE_NEWS;
import static io.meeds.content.news.utils.NewsUtils.UPDATE_NEWS;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;

import io.meeds.content.news.model.News;
import io.meeds.content.news.search.NewsIndexingServiceConnector;
import io.meeds.content.news.utils.NewsUtils;

import jakarta.annotation.PostConstruct;

@Component
public class NewsMetadataListener extends Listener<Object, News> {

  @Autowired
  private IndexingService indexingService;

  @Autowired
  private IdentityManager identityManager;

  @Autowired
  private SpaceService    spaceService;

  @Autowired
  private TagService            tagService;

  @Autowired
  private ListenerService       listenerService;

  private String[] LISTENERS = {POST_NEWS, UPDATE_NEWS, SHARE_NEWS};

  @PostConstruct
  public void init() {
    for (String listener : LISTENERS) {
      listenerService.addListener(listener, this);
    }
  }
  @Override
  public void onEvent(Event<Object, News> event) throws Exception {
    News news = event.getData();
    Object userId = event.getSource();
    String username = null;
    if (userId instanceof String s) {
      username = s;
    } else if (userId instanceof Long l) {
      Identity identity = identityManager.getIdentity(l);
      if (identity != null && identity.isUser()) {
        username = identity.getRemoteId();
      }
    }

    if (username != null) {
      saveTags(news, username);
    }

    indexingService.reindex(NewsIndexingServiceConnector.TYPE, news.getId());
  }

  private void saveTags(News news, String username) {
    long creatorId = getPosterId(username);
    long audienceId = getStreamOwnerId(news.getSpaceId(), username);

    Set<TagName> tagNames = tagService.detectTagNames(news.getBody());
    tagService.saveTags(new TagObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE, news.getId(), null), tagNames, audienceId, creatorId);
  }

  private long getStreamOwnerId(String spaceId, String username) {
    Space space = spaceService.getSpaceById(spaceId);
    return space == null ? getPosterId(username)
                         : Long.parseLong(identityManager.getOrCreateSpaceIdentity(space.getPrettyName()).getId());
  }

  private long getPosterId(String username) {
    return StringUtils.isBlank(username) ? 0 : Long.parseLong(identityManager.getOrCreateUserIdentity(username).getId());
  }

}
