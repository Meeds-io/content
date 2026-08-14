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
package io.meeds.content.plugin;

import java.util.List;
import java.util.Set;

import org.exoplatform.services.security.Identity;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.filter.ContentFilter;

/**
 * A content-producing addon registers one {@link ContentTypePlugin} bean to
 * contribute its own content type (News, Notes...) to the merged Content List.
 * {@link io.meeds.content.service.ContentService} discovers every registered
 * plugin (Spring {@code getBeansOfType}) instead of hardcoding one branch per
 * type, so a new addon can add its own content type by depending on
 * {@code content-service} and registering a plugin - no change needed here.
 */
public interface ContentTypePlugin {

  /**
   * @return the content type identifier (e.g. "news", "notes"); becomes
   *         {@link ContentEntry}'s contentType field and the value used in
   *         {@link ContentFilter}'s contentTypes field.
   */
  String getType();

  /**
   * @return the i18n key of this content type's display label in the
   *         Content List filter drawer.
   */
  String getLabelKey();

  /**
   * @return display order among registered content types (ascending); ties
   *         are broken on {@link #getType()}.
   */
  default int getOrder() {
    return 0;
  }

  /**
   * Fetches and maps this content type's own entries matching the filter,
   * already permission-checked for the current user. Implementations decide
   * for themselves how (or whether) each {@link ContentFilter} field applies
   * - e.g. a type with no draft/scheduled workflow simply returns an empty
   * list for those statuses.
   *
   * @param filter            the aggregate filter as configured by the caller
   * @param fetchLimit        upper bound of this (unpaginated, unmerged)
   *                            fetch - pagination is applied once, after all
   *                            types are merged
   * @param currentIdentity   the current user, for ACL and "My Content"
   *                            resolution
   * @param categoryLinkedIds ids of objects linked to the category being
   *                            filtered by - a flat union resolved across
   *                            every registered content type (not just this
   *                            plugin's own type), since a wiki-page-backed
   *                            item's link ends up recorded under whichever
   *                            type its shared Activity currently resolves
   *                            to, not necessarily its own; matching a
   *                            candidate's own id against this set is always
   *                            correct since ids are never reused across
   *                            content types backed by the same wiki Page.
   *                            {@code null} when no category filter is active
   * @return the matching entries
   */
  List<ContentEntry> search(ContentFilter filter,
                            int fetchLimit,
                            Identity currentIdentity,
                            Set<String> categoryLinkedIds) throws Exception;

  /**
   * Deletes the content item with the given id.
   *
   * @param id              the content id, as previously returned in
   *                          {@link ContentEntry}'s id field
   * @param status          the content's status, when relevant to resolve
   *                          how to delete it (e.g. News draft vs published)
   * @param currentIdentity the current user, for permission checks
   */
  void delete(String id, String status, Identity currentIdentity) throws Exception;

}
