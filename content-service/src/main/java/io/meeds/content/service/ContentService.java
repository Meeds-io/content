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
package io.meeds.content.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.ContentType;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.plugin.ContentTypePlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

/**
 * Merges content coming from every registered {@link ContentTypePlugin} (News
 * articles, Notes, and any content type a future addon registers) into a
 * single, uniform, paginated list. Each content type is fetched independently
 * then merged/sorted/sliced in memory: this is an accepted MVP tradeoff since
 * no content type shares a common filter/pagination type with another.
 */
@Service
public class ContentService {

  // Upper bound on how many objects linked to a category are considered when
  // filtering the merged list by category. Combining a category filter with
  // each content type's own independent pagination has no exact solution
  // without a shared index; this cap keeps it correct for any reasonably-sized
  // category.
  private static final int CATEGORY_LINKS_FETCH_CAP = 500;

  @Autowired
  private ApplicationContext        applicationContext;

  @Autowired
  private CategoryLinkService       categoryLinkService;

  private List<ContentTypePlugin>   contentTypePlugins;

  private Map<String, ContentTypePlugin> contentTypePluginsByType;

  @PostConstruct
  public void init() {
    contentTypePlugins = new ArrayList<>(applicationContext.getBeansOfType(ContentTypePlugin.class).values());
    contentTypePlugins.sort(Comparator.comparingInt(ContentTypePlugin::getOrder).thenComparing(ContentTypePlugin::getType));
    contentTypePluginsByType = contentTypePlugins.stream()
                                                 .collect(Collectors.toMap(ContentTypePlugin::getType, plugin -> plugin));
  }

  public List<ContentType> getContentTypes() {
    return contentTypePlugins.stream()
                             .map(plugin -> new ContentType(plugin.getType(), plugin.getLabelKey()))
                             .collect(Collectors.toList());
  }

  public List<ContentEntry> getContentList(ContentFilter filter, Identity currentIdentity) throws Exception {
    int offset = filter.getOffset();
    int limit = filter.getLimit();
    boolean byCategory = filter.getCategoryId() != null;
    boolean byIncludedCategories = !byCategory && CollectionUtils.isNotEmpty(filter.getIncludeCategoryIds());
    // Each type is fetched from 0 up to a bound since no type can be
    // paginated against another's results ahead of the merge: when
    // filtering by category, the bound is the category-links cap (so any
    // in-category item can surface regardless of its date-sort position);
    // otherwise it is simply (offset + limit).
    int fetchLimit = (byCategory || byIncludedCategories) ? CATEGORY_LINKS_FETCH_CAP : offset + limit;
    Map<String, Set<String>> categoryLinkedIds = null;
    if (byCategory) {
      categoryLinkedIds = resolveCategoryLinkedIds(filter, List.of(filter.getCategoryId()));
    } else if (byIncludedCategories) {
      categoryLinkedIds = resolveCategoryLinkedIds(filter, filter.getIncludeCategoryIds());
    }

    List<ContentEntry> entries = new ArrayList<>();
    for (ContentTypePlugin plugin : contentTypePlugins) {
      if (!filter.hasContentType(plugin.getType())) {
        continue;
      }
      // getOrDefault (not get): when a category filter is active, a type
      // with no linked ids must be told "zero allowed" (empty set), not
      // "unrestricted" (null) - the two collapse to the same `null` only
      // when no category filter is active at all (categoryLinkedIds itself
      // is null).
      Set<String> allowedIds = categoryLinkedIds == null ? null : categoryLinkedIds.getOrDefault(plugin.getType(), Collections.emptySet());
      entries.addAll(plugin.search(filter, fetchLimit, currentIdentity, allowedIds));
    }

    if (CollectionUtils.isNotEmpty(filter.getExcludeCategoryIds())) {
      Set<Long> excludedCategoryIds = new HashSet<>(filter.getExcludeCategoryIds());
      entries = entries.stream()
                       .filter(entry -> entry.getCategoryIds() == null || entry.getCategoryIds()
                                                                                .stream()
                                                                                .noneMatch(excludedCategoryIds::contains))
                       .collect(Collectors.toList());
    }

    entries.sort(Comparator.comparing(ContentEntry::getDate, Comparator.nullsLast(Comparator.<Date> reverseOrder())));
    return entries.stream().skip(offset).limit(limit).collect(Collectors.toList());
  }

  public void deleteContent(String id, String contentType, String status, Identity currentIdentity) throws Exception {
    ContentTypePlugin plugin = contentTypePluginsByType.get(contentType);
    if (plugin == null) {
      throw new ObjectNotFoundException("Content type '" + contentType + "' is not registered");
    }
    plugin.delete(id, status, currentIdentity);
  }

  private Map<String, Set<String>> resolveCategoryLinkedIds(ContentFilter filter, List<Long> categoryIds) {
    List<String> types = contentTypePlugins.stream()
                                           .map(ContentTypePlugin::getType)
                                           .filter(filter::hasContentType)
                                           .collect(Collectors.toList());
    if (types.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Set<String>> linkedIdsByType = new HashMap<>();
    for (Long categoryId : categoryIds) {
      List<CategoryObject> linkedObjects = categoryLinkService.getLinkedObjects(categoryId, types, 0, CATEGORY_LINKS_FETCH_CAP);
      for (CategoryObject linkedObject : linkedObjects) {
        linkedIdsByType.computeIfAbsent(linkedObject.getType(), key -> new HashSet<>()).add(linkedObject.getId());
      }
    }
    return linkedIdsByType;
  }

}
