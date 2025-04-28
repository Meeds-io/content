/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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
package io.meeds.content.storage;

import static io.meeds.content.storage.util.EntityMapper.fromModel;
import static io.meeds.content.storage.util.EntityMapper.toModel;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import io.meeds.content.constant.LinkAlignType;
import io.meeds.content.constant.LinkDisplayType;
import io.meeds.content.dao.LinkDAO;
import io.meeds.content.dao.LinkSettingDAO;
import io.meeds.content.entity.LinkEntity;
import io.meeds.content.entity.LinkSettingEntity;
import io.meeds.content.model.Link;
import io.meeds.content.model.LinkSetting;
import io.meeds.content.storage.util.EntityMapper;

public abstract class LinkStorage {

  @Autowired
  private LinkSettingDAO linkSettingDAO;

  @Autowired
  private LinkDAO        linkDAO;

  public LinkSetting getLinkSetting(String name) {
    LinkSettingEntity linkSettingEntity = linkSettingDAO.findByName(name);
    return toModel(linkSettingEntity);
  }

  public LinkSetting getLinkSetting(Long linkSettingId) {
    LinkSettingEntity linkSettingEntity = linkSettingDAO.findById(linkSettingId).orElse(null);
    return toModel(linkSettingEntity);
  }

  public boolean hasLinkSetting(String linkSettingName) {
    // Reuse cached DTO instead of requesting Database using DAO
    return getLinkSetting(linkSettingName) != null;
  }

  public LinkSetting initLinkSetting(String name, String pageReference, long spaceId) {
    LinkSettingEntity linkSettingEntity = linkSettingDAO.findByName(name);
    if (linkSettingEntity == null) {
      linkSettingEntity = new LinkSettingEntity();
      linkSettingEntity.setName(name);
      linkSettingEntity.setPageReference(pageReference);
      linkSettingEntity.setSpaceId(spaceId);
      linkSettingEntity.setShowIcon(true);
      linkSettingEntity.setIconSize(34);
      linkSettingEntity.setType(LinkDisplayType.ROW);
      linkSettingEntity.setVAlign(LinkAlignType.CENTER);
      linkSettingEntity.setHAlign(LinkAlignType.CENTER);
      linkSettingEntity.setLastModified(Instant.now());
      return toModel(linkSettingDAO.save(linkSettingEntity));
    } else {
      linkSettingEntity.setPageReference(pageReference);
      return toModel(linkSettingDAO.save(linkSettingEntity));
    }
  }

  public LinkSetting saveLinkSetting(LinkSetting linkSetting) {
    LinkSettingEntity existingLinkSettingEntity = linkSettingDAO.findByName(linkSetting.getName());
    LinkSettingEntity linkSettingEntity = fromModel(linkSetting, existingLinkSettingEntity);
    return toModel(linkSettingDAO.save(linkSettingEntity));
  }

  public List<Link> getLinks(String linkSettingName) {
    List<LinkEntity> linkEntities = linkDAO.getLinks(linkSettingName);
    return linkEntities.stream().map(EntityMapper::toModel).toList();
  }

  public Link createLink(String linkSettingName, Link link) {
    LinkSettingEntity linkSettingEntity = linkSettingDAO.findByName(linkSettingName);
    LinkEntity linkEntity = fromModel(link, linkSettingEntity);
    linkEntity.setId(null);
    linkEntity = linkDAO.save(linkEntity);
    updateLastModifiedTime(linkSettingEntity);
    return toModel(linkEntity);
  }

  public Link updateLink(String linkSettingName, Link link) {
    LinkSettingEntity linkSettingEntity = linkSettingDAO.findByName(linkSettingName);
    LinkEntity linkEntity = fromModel(link, linkSettingEntity);
    linkEntity = linkDAO.save(linkEntity);
    updateLastModifiedTime(linkSettingEntity);
    return toModel(linkEntity);
  }

  public void deleteLink(String linkSettingName, long id) {
    LinkSettingEntity linkSettingEntity = linkSettingDAO.findByName(linkSettingName);
    linkDAO.deleteById(id);
    updateLastModifiedTime(linkSettingEntity);
  }

  private void updateLastModifiedTime(LinkSettingEntity linkSettingEntity) {
    linkSettingEntity.setLastModified(Instant.now());
    linkSettingDAO.save(linkSettingEntity);
  }

  public LinkSetting getLinkSettingByLinkId(long linkId) {
    long linkSettingId = linkDAO.getLinkSettingByLinkId(linkId);
    return getLinkSetting(linkSettingId);
  }

}
