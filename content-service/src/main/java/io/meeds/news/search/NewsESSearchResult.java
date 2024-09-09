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
package io.meeds.news.search;

import java.util.List;

import lombok.Data;
import org.exoplatform.social.core.identity.model.Identity;

@Data
public class NewsESSearchResult {

  private String       id;

  private String       title;

  private Identity     poster;

  private String       body;

  private String       spaceDisplayName;

  private String       newsUrl;

  private List<String> excerpts;

  private long         postedTime;

  private long         lastUpdatedTime;

  private String       activityId;

  private String       lang;
}
