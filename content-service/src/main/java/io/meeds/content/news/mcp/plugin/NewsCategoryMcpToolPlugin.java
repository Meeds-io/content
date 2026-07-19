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
package io.meeds.content.news.mcp.plugin;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.content.news.plugin.NewsPermanentLinkPlugin;
import io.meeds.mcp.server.tool.plugin.CategoryMcpToolPlugin;

/**
 * {@link CategoryMcpToolPlugin} bound to the {@code news} content type. News
 * articles are not categorizable yet (only notes are, and there is currently no
 * note category plugin either), so this plugin exists solely to intercept the
 * {@code news} type in the generic {@code add_content_to_category} MCP tool and
 * reject the operation with a clear, user-facing error. Without it, the tool
 * would fall back to writing a raw {@code CategoryObject("news", newsId)} link
 * that nothing reads back, reporting a false success to the calling agent.
 */
@Service
@Profile("mcp-server")
public class NewsCategoryMcpToolPlugin implements CategoryMcpToolPlugin {

  /**
   * Human-readable, user-facing message returned to the calling agent when it
   * tries to categorize a news article. Carried by an
   * {@link UnsupportedOperationException} whose message the MCP tool contract
   * surfaces as clean text (rather than a raw 500).
   */
  protected static final String NOT_CATEGORIZABLE_MESSAGE =
                                                          "News articles can't be categorized yet: categories currently apply to notes only, "
                                                              + "not to the 'news' content type. This operation is not supported.";

  /**
   * Matches the {@code news} content type so that this plugin intercepts every
   * {@code add_content_to_category} call targeting a news article, preventing
   * the generic tool from silently creating an unusable category link.
   *
   * @param contentType the content type passed to the MCP tool
   * @param contentId the content identifier passed to the MCP tool (unused here,
   *          matching is done on the type only)
   * @return {@code true} when {@code contentType} is the news object type
   */
  @Override
  public boolean match(String contentType, String contentId) {
    return NewsPermanentLinkPlugin.OBJECT_TYPE.equals(contentType);
  }

  /**
   * Rejects any attempt to link a news article to a category. News articles are
   * not categorizable yet, so instead of silently linking the article's backing
   * activity (which produced a false success that no read path could see), this
   * always throws an {@link UnsupportedOperationException} carrying a clear,
   * user-facing message.
   *
   * @param categoryId the target category identifier (ignored)
   * @param contentType the content type, always the news object type here
   *          (ignored)
   * @param contentId the news article identifier (ignored)
   * @param username the acting user (ignored)
   * @throws UnsupportedOperationException always, since news articles can't be
   *           categorized yet
   */
  @Override
  public void addCategory(long categoryId,
                          String contentType,
                          String contentId,
                          String username) throws IllegalAccessException,
                                           ObjectNotFoundException {
    throw new UnsupportedOperationException(NOT_CATEGORIZABLE_MESSAGE);
  }

}
