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
package io.meeds.content.news.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.meeds.mcp.server.tool.model.SpaceModel;
import io.meeds.mcp.server.tool.model.UserModel;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_EMPTY)
public record NewsModel(
                        @JsonProperty("news_id")
                        long id,
                        String title,
                        String summary,
                        String content,
                        @JsonProperty("access_url")
                        String accessUrl,
                        @JsonProperty("edit_url")
                        String editUrl,
                        @JsonProperty("is_published")
                        boolean isPublished,
                        @JsonProperty("is_draft")
                        boolean isDraft,
                        @JsonProperty("publication_date")
                        String publicationDate,
                        @JsonProperty("number_of_comments")
                        int numberOfComments,
                        @JsonProperty("number_of_likes")
                        int numberOfLikes,
                        @JsonProperty("number_of_views")
                        int numberOfViews,
                        @JsonProperty("is_activity_posted")
                        boolean isActivityPosted,
                        @JsonProperty("activity_id")
                        long activityId,
                        UserModel publisher,
                        @JsonProperty("target_space")
                        SpaceModel targetSpace) {
}
