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

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import io.meeds.social.space.service.SpaceServiceImpl;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;

import io.meeds.news.rest.NewsTargetingEntity;
import io.meeds.news.service.NewsTargetingService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpaceNewsTargetAutoCreationListener extends SpaceListenerPlugin {

  private static final Log     LOG = ExoLogger.getLogger(SpaceNewsTargetAutoCreationListener.class);

  @Autowired
  private NewsTargetingService newsTargetingService;

  @Autowired
  private SpaceService spaceService;

  private SpaceServiceImpl spaceServiceImpl;

  @PostConstruct
  public void init() {
    if (spaceService instanceof SpaceServiceImpl) {
      this.spaceServiceImpl = (SpaceServiceImpl) spaceService;
    }
    spaceServiceImpl.addSpaceListener(this);
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    Space space = event.getSpace();
    NewsTargetingEntity spaceNewsTargetEntity = new NewsTargetingEntity();
    String spaceGroupId = space.getGroupId();
    String spaceGroupName = spaceGroupId.substring(spaceGroupId.lastIndexOf("/") + 1);
    spaceNewsTargetEntity.setName(spaceGroupName);
    Map<String, String> properties = new HashMap<>();
    properties.put("label", space.getDisplayName());
    properties.put("permissions", "space:" + space.getId());
    spaceNewsTargetEntity.setProperties(properties);
    try {
      newsTargetingService.createNewsTarget(spaceNewsTargetEntity, currentIdentity, false);
    } catch (Exception e) {
      LOG.warn("Can't create space {} news target", space.getPrettyName(), e);
    }
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    Space space = event.getSpace();
    NewsTargetingEntity spaceNewsTargetEntity = new NewsTargetingEntity();
    Map<String, String> properties = new HashMap<>();
    properties.put("label", space.getDisplayName());
    properties.put("permissions", "space:" + space.getId());
    spaceNewsTargetEntity.setProperties(properties);
    String spaceGroupId = space.getGroupId();
    String spaceGroupName = spaceGroupId.substring(spaceGroupId.lastIndexOf("/") + 1);
    spaceNewsTargetEntity.setName(spaceGroupName);
    try {
      newsTargetingService.updateNewsTargets(spaceGroupName, spaceNewsTargetEntity, currentIdentity);
    } catch (Exception e) {
      LOG.warn("Can't rename space {} news target", space.getPrettyName());
    }
  }
}
