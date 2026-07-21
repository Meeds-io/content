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
package io.meeds.content.model;

import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A uniform, display-ready representation of a piece of content listed by
 * the Content List app, whatever its underlying content type (News article,
 * Note...). The field set mirrors
 * {@code io.meeds.social.category.model.CategoryEntryItem} (the shape already
 * used by {@code NewsCategoryPlugin}/{@code NoteCategoryPlugin}) so that the
 * same conventions are reused, extended with the status/permission flags
 * needed to drive the per-item options menu.
 */
@Data
@NoArgsConstructor
public class ContentEntry {

  private String       id;

  private String       contentType;

  private String       icon;

  private String       title;

  private String       summary;

  private String       illustrationUrl;

  private String       url;

  private String       authorUsername;

  private String       authorDisplayName;

  private String       authorAvatarUrl;

  private String       spaceId;

  private String       spaceDisplayName;

  private String       spaceAvatarUrl;

  private Date         date;

  private long         likesCount;

  private long         commentsCount;

  private long         viewsCount;

  private long         attachmentsCount;

  private List<Long>   categoryIds;

  private boolean      published;

  private boolean      draft;

  private boolean      scheduled;

  private boolean      canEdit;

  private boolean      canDelete;

  private boolean      canPublish;

  private boolean      canSchedule;

}
