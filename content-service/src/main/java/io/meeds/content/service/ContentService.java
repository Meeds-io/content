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
import io.meeds.social.activity.plugin.ActivityCategoryPlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryService;

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

  @Autowired
  private CategoryService           categoryService;

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
    Set<String> categoryLinkedIds = null;
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
      entries.addAll(plugin.search(filter, fetchLimit, currentIdentity, categoryLinkedIds));
    }

    if (CollectionUtils.isNotEmpty(filter.getExcludeCategoryIds())) {
      Set<Long> excludedCategoryIds = new HashSet<>(filter.getExcludeCategoryIds());
      entries = entries.stream()
                       .filter(entry -> entry.getCategoryIds() == null || entry.getCategoryIds()
                                                                                .stream()
                                                                                .noneMatch(excludedCategoryIds::contains))
                       .collect(Collectors.toList());
    }

    Comparator<ContentEntry> dateDesc = Comparator.comparing(ContentEntry::getDate, Comparator.nullsLast(Comparator.<Date> reverseOrder()));
    if (byIncludedCategories) {
      // The admin-configured "Per category" list order (already reflected
      // in filter.getIncludeCategoryIds() - its order is exactly the one
      // set/reordered in the settings drawer) drives the display order of
      // items themselves, not just of the category pills: items matching an
      // earlier-ranked category come first, and within the same rank items
      // are still sorted by date desc.
      List<Long> includeCategoryIds = filter.getIncludeCategoryIds();
      entries.sort(Comparator.comparingInt((ContentEntry entry) -> categoryRank(entry, includeCategoryIds)).thenComparing(dateDesc));
    } else {
      entries.sort(dateDesc);
    }
    return entries.stream().skip(offset).limit(limit).collect(Collectors.toList());
  }

  private int categoryRank(ContentEntry entry, List<Long> includeCategoryIds) {
    if (entry.getCategoryIds() == null) {
      return Integer.MAX_VALUE;
    }
    int rank = Integer.MAX_VALUE;
    for (int i = 0; i < includeCategoryIds.size() && i < rank; i++) {
      if (entry.getCategoryIds().contains(includeCategoryIds.get(i))) {
        rank = i;
      }
    }
    return rank;
  }

  public void deleteContent(String id, String contentType, String status, Identity currentIdentity) throws Exception {
    ContentTypePlugin plugin = contentTypePluginsByType.get(contentType);
    if (plugin == null) {
      throw new ObjectNotFoundException("Content type '" + contentType + "' is not registered");
    }
    plugin.delete(id, status, currentIdentity);
  }

  private Set<String> resolveCategoryLinkedIds(ContentFilter filter, List<Long> categoryIds) {
    List<String> types = contentTypePlugins.stream()
                                           .map(ContentTypePlugin::getType)
                                           .filter(filter::hasContentType)
                                           .collect(Collectors.toList());
    if (types.isEmpty()) {
      return Collections.emptySet();
    }
    // Once a wiki-page-backed item (News/Notes) is posted to a space feed,
    // its category link is stored under whichever type its shared Activity's
    // *specific metadata object* currently resolves to (see
    // ActivityCategoryPlugin#getObject / ExoSocialActivityImpl#getMetadataObject)
    // - for News/Notes that is always "news" once published
    // (NewsService#updateNewsActivity/postNewsActivity overwrite it), never
    // literally "activity", regardless of which of the two content types the
    // category was actually added from. Since News and Notes are the very
    // same wiki Page row (same id) once linked to the same Activity, ids are
    // matched here as one flat union across every queried type - not
    // partitioned per content type - so a link recorded under "news" still
    // matches the corresponding Notes candidate with the same id, and vice
    // versa. Also querying the generic "activity" type as a fallback, for
    // any future content type whose Activity never sets a specific
    // metadata object.
    List<String> queryTypes = new ArrayList<>(types);
    queryTypes.add(ActivityCategoryPlugin.OBJECT_TYPE);
    Set<String> linkedIds = new HashSet<>();
    for (Long categoryId : categoryIds) {
      for (Long id : withSubcategoryIds(categoryId)) {
        List<CategoryObject> linkedObjects = categoryLinkService.getLinkedObjects(id, queryTypes, 0, CATEGORY_LINKS_FETCH_CAP);
        for (CategoryObject linkedObject : linkedObjects) {
          linkedIds.add(linkedObject.getId());
        }
      }
    }
    return linkedIds;
  }

  private Set<Long> withSubcategoryIds(Long categoryId) {
    Set<Long> ids = new HashSet<>(categoryService.getSubcategoryIds(categoryId, 0, 0, -1));
    ids.add(categoryId);
    return ids;
  }

}
