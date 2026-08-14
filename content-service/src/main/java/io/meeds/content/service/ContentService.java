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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.ContentPage;
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
 * Plugins self-register via {@link #addPlugin(ContentTypePlugin)} (same
 * pattern as {@code CategoryPluginService}) rather than being snapshotted
 * once via {@code getBeansOfType} - a plugin declared in another WAR, whose
 * Spring context the Kernel bridge merges in after this one boots, still
 * registers correctly.
 */
@Service
public class ContentService {

  private static final Log LOG = ExoLogger.getLogger(ContentService.class);

  // Upper bound on how many objects linked to a category are considered when
  // filtering the merged list by category. Combining a category filter with
  // each content type's own independent pagination has no exact solution
  // without a shared index; this cap keeps it correct for any reasonably-sized
  // category.
  private static final int CATEGORY_LINKS_FETCH_CAP = 500;

  // Upper bound on how many raw rows are fetched per content type on the
  // non-category path, independent of CATEGORY_LINKS_FETCH_CAP (that one is
  // specifically about category-link resolution, not a generic fetch
  // ceiling).
  private static final int CONTENT_FETCH_CAP        = 500;

  // Flat safety margin - not a multiplier - added on top of (offset + limit)
  // when fetching each content type's raw rows, to absorb rows a plugin
  // still drops after the fetch (permission checks, non-space notes, "My
  // Content" author filtering...) without under-filling the requested page.
  // Deliberately additive rather than a factor of (offset + limit): a
  // multiplier makes every content type's per-item N+1 cost grow with page
  // depth (page 5 at limit=20 would otherwise re-fetch/re-check 300 rows
  // just to render 20), while a flat margin keeps that cost bounded.
  private static final int OVER_FETCH_MARGIN        = 20;

  @Autowired
  private CategoryLinkService       categoryLinkService;

  @Autowired
  private CategoryService           categoryService;

  private final Map<String, ContentTypePlugin> contentTypePluginsByType = new ConcurrentHashMap<>();

  public void addPlugin(ContentTypePlugin plugin) {
    ContentTypePlugin existing = contentTypePluginsByType.putIfAbsent(plugin.getType(), plugin);
    if (existing != null) {
      LOG.warn("Content type '{}' is already registered by {} - ignoring registration from {}",
              plugin.getType(),
              existing.getClass().getName(),
              plugin.getClass().getName());
    }
  }

  private List<ContentTypePlugin> getContentTypePlugins() {
    List<ContentTypePlugin> plugins = new ArrayList<>(contentTypePluginsByType.values());
    plugins.sort(Comparator.comparingInt(ContentTypePlugin::getOrder).thenComparing(ContentTypePlugin::getType));
    return plugins;
  }

  public List<ContentType> getContentTypes() {
    return getContentTypePlugins().stream()
                             .map(plugin -> new ContentType(plugin.getType(), plugin.getLabelKey()))
                             .collect(Collectors.toList());
  }

  public ContentPage getContentList(ContentFilter filter, Identity currentIdentity) throws Exception {
    int offset = filter.getOffset();
    int limit = filter.getLimit();
    boolean byCategory = filter.getCategoryId() != null;
    boolean byIncludedCategories = !byCategory && CollectionUtils.isNotEmpty(filter.getIncludeCategoryIds());
    // Each type is fetched from 0 up to a bound since no type can be
    // paginated against another's results ahead of the merge: when
    // filtering by category, the bound is the category-links cap (so any
    // in-category item can surface regardless of its date-sort position);
    // otherwise it is (offset + limit) with a flat safety margin, since a
    // plugin may still drop rows after this raw fetch (permission checks,
    // non-space notes, "My Content" author filtering...) - fetching only
    // exactly (offset + limit) would silently under-fill the page in that
    // case.
    int fetchLimit = (byCategory || byIncludedCategories) ? CATEGORY_LINKS_FETCH_CAP
                                                          : Math.min(offset + limit + OVER_FETCH_MARGIN, CONTENT_FETCH_CAP);
    Set<String> categoryLinkedIds = null;
    if (byCategory) {
      categoryLinkedIds = resolveCategoryLinkedIds(filter, List.of(filter.getCategoryId()));
    } else if (byIncludedCategories) {
      categoryLinkedIds = resolveCategoryLinkedIds(filter, filter.getIncludeCategoryIds());
    }

    List<ContentEntry> entries = new ArrayList<>();
    for (ContentTypePlugin plugin : getContentTypePlugins()) {
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
    // One extra row beyond the requested page, when available, tells us
    // there is a next page without relying on the page itself coming back
    // full-sized (a filtered-down page can be smaller than "limit" while
    // more content still exists further down) - trimmed off before
    // returning, only its presence is exposed, via ContentPage#hasMore.
    List<ContentEntry> page = entries.stream().skip(offset).limit(limit + 1L).collect(Collectors.toList());
    boolean hasMore = page.size() > limit;
    if (hasMore) {
      page = page.subList(0, limit);
    }
    return new ContentPage(page, hasMore);
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
    List<String> types = getContentTypePlugins().stream()
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
        if (linkedObjects.size() >= CATEGORY_LINKS_FETCH_CAP) {
          LOG.debug("Category {} has at least {} linked objects - the category filter's fetch cap was hit, some content may be missing from it",
                    id,
                    CATEGORY_LINKS_FETCH_CAP);
        }
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
