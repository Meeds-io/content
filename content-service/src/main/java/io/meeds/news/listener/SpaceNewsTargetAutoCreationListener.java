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

import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;

import io.meeds.news.rest.NewsTargetingEntity;
import io.meeds.news.service.NewsTargetingService;

public class SpaceNewsTargetAutoCreationListener extends SpaceListenerPlugin {

  private static final Log     LOG = ExoLogger.getLogger(SpaceNewsTargetAutoCreationListener.class);

  private NewsTargetingService newsTargetingService;

  public SpaceNewsTargetAutoCreationListener(NewsTargetingService newsTargetingService) {
    this.newsTargetingService = newsTargetingService;
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    Space space = event.getSpace();
    NewsTargetingEntity spaceNewsTargetEntity = new NewsTargetingEntity();
    spaceNewsTargetEntity.setName(space.getDisplayName());
    spaceNewsTargetEntity.setProperties(Map.of("label", space.getDisplayName(), "permissions", "space:" + space.getId()));
    try {
      newsTargetingService.createNewsTarget(spaceNewsTargetEntity, currentIdentity, false);
    } catch (Exception e) {
      LOG.warn("Can't create space {} news target", space.getPrettyName());
    }
  }
}
