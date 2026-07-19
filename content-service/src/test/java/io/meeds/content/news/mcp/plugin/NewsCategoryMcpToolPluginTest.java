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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NewsCategoryMcpToolPluginTest {

  private static final String       NEWS_TYPE  = "news";

  private static final String       NEWS_ID    = "newsId";

  private static final String       TEST_USER  = "testUser";

  @InjectMocks
  private NewsCategoryMcpToolPlugin  plugin;

  @Test
  public void matchNewsType() {
    assertTrue(plugin.match(NEWS_TYPE, NEWS_ID));
  }

  @Test
  public void doNotMatchOtherTypes() {
    assertFalse(plugin.match("note", NEWS_ID));
    assertFalse(plugin.match("activity", NEWS_ID));
    assertFalse(plugin.match(null, NEWS_ID));
  }

  @Test
  public void addCategoryFailsClearly() {
    UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                                                          () -> plugin.addCategory(1L, NEWS_TYPE, NEWS_ID, TEST_USER));
    assertNotNull(exception.getMessage());
    assertTrue("Error message must explain that news articles can't be categorized yet",
               exception.getMessage().toLowerCase().contains("categor"));
  }

}
