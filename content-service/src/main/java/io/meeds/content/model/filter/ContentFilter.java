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
package io.meeds.content.model.filter;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A filter used to list content merged across the content types registered
 * via {@link io.meeds.content.plugin.ContentTypePlugin} (News articles and
 * Notes today; a new content type only needs a new registered plugin, not a
 * change here).
 */
@Data
@NoArgsConstructor
public class ContentFilter {

  private List<String> contentTypes;

  private String        status;

  private List<String>  spaces;

  private Long           categoryId;

  private String        searchText;

  private int            offset;

  private int            limit;

  /**
   * Admin-configured category restriction (portlet preferences, see task 10):
   * when set, and no explicit {@link #categoryId} browse is active, only
   * content linked to one of these categories is listed.
   */
  private List<Long>    includeCategoryIds;

  /**
   * Admin-configured category exclusion (portlet preferences): content
   * linked to any of these categories is always dropped from the list.
   */
  private List<Long>    excludeCategoryIds;

  public boolean hasContentType(String contentType) {
    return contentTypes == null || contentTypes.isEmpty() || contentTypes.contains(contentType);
  }

}
