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
package io.meeds.content.link.storage.cache;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.content.link.model.Link;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.storage.LinkStorage;

import jakarta.annotation.PostConstruct;

@Component
public class CachedLinkStorage extends LinkStorage {

  public static final String                                CACHE_NAME = "social.linkSettings";

  @Autowired
  private CacheService                                      cacheService;

  private FutureExoCache<Serializable, LinkSetting, Object> futureCache;

  @PostConstruct
  public void init() {
    ExoCache<Serializable, LinkSetting> cache = cacheService.getCacheInstance(CACHE_NAME);
    Loader<Serializable, LinkSetting, Object> loader = new Loader<>() {
      @Override
      public LinkSetting retrieve(Object context, Serializable key) throws Exception {
        return switch (key) {
        case String name -> CachedLinkStorage.super.getLinkSetting(name);
        case Long id -> CachedLinkStorage.super.getLinkSetting(id);
        default -> null;
        };
      }
    };
    this.futureCache = new FutureExoCache<>(loader, cache);
  }

  @Override
  public LinkSetting getLinkSetting(String name) {
    return clone(futureCache.get(null, name));
  }

  @Override
  public LinkSetting getLinkSetting(Long id) {
    return clone(futureCache.get(null, id));
  }

  @Override
  public LinkSetting initLinkSetting(String name, String pageId, long spaceId) {
    try {
      return super.initLinkSetting(name, pageId, spaceId);
    } finally {
      clearLinkSetting(name);
    }
  }

  @Override
  public LinkSetting saveLinkSetting(LinkSetting linkSetting) {
    try {
      return super.saveLinkSetting(linkSetting);
    } finally {
      futureCache.remove(linkSetting.getName());
    }
  }

  @Override
  public Link createLink(String linkSettingName, Link link) {
    try {
      return super.createLink(linkSettingName, link);
    } finally {
      clearLinkSetting(linkSettingName);
    }
  }

  @Override
  public Link updateLink(String linkSettingName, Link link) {
    try {
      return super.updateLink(linkSettingName, link);
    } finally {
      clearLinkSetting(linkSettingName);
    }
  }

  @Override
  public void deleteLink(String linkSettingName, long id) {
    try {
      super.deleteLink(linkSettingName, id);
    } finally {
      clearLinkSetting(linkSettingName);
    }
  }

  private void clearLinkSetting(String name) {
    LinkSetting linkSetting = futureCache.get(name);
    if (linkSetting != null) {
      futureCache.remove(name);
      futureCache.remove(linkSetting.getId());
    }
  }

  private LinkSetting clone(LinkSetting linkSetting) {
    return linkSetting == null ? null : linkSetting.clone();
  }

}
