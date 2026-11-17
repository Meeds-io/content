/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.news.model;

/**
 * The data of {@code content.article.id.changed}: an article now stored under
 * another object than before. Posting or scheduling a new article's draft
 * creates its page, then deletes the draft; unscheduling a staged article
 * creates a new draft, then deletes the page. The event is broadcast once the
 * old object is deleted.
 *
 * @param fromId the id of the object the article was stored under
 * @param fromDraft true when that object is a draft of a new article, false
 *          for an article page
 * @param toId the id of the object the article is now stored under
 * @param toDraft true when that object is a draft of a new article, false for
 *          an article page
 * @param spaceId the id of the article's space
 */
public record ArticleIdChange(String fromId, boolean fromDraft, String toId, boolean toDraft, String spaceId) {
}
