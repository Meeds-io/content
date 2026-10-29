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
package io.meeds.content.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.meeds.content.news.listener.AnalyticsNewsListener;
import io.meeds.content.news.service.NewsService;

import tools.jackson.databind.ObjectMapper;

/**
 * The collaborative activity declaration analytics reads from this jar: an
 * article counts once, through the content event recorded when it is posted,
 * on the feed or in a dedicated section; the stream post of an article is left
 * out by its activity type. The strings are read from the constants of the
 * code that emits them: the analytics listener for the operation, NewsService
 * for the type it gives the post. The label keys are matched by hand against
 * content-webapp's Portlets_en.properties.
 */
class CollaborativeActivityDeclarationTest {

  private static final String DECLARATION_FILE = "analytics-collaborative-activities.json";

  @Test
  @SuppressWarnings("unchecked")
  void theDeclarationCountsThePostingAndExcludesTheNewsStreamPost() throws IOException {
    Map<String, Object> declaration;
    try (InputStream content = getClass().getClassLoader().getResourceAsStream(DECLARATION_FILE)) {
      assertNotNull(content, "the declaration ships at the root of content-service, which lands in /lib");
      declaration = new ObjectMapper().readValue(content, Map.class);
    }

    assertEquals(List.of("contents"), declaration.get("modules"));
    assertEquals(List.of(AnalyticsNewsListener.CREATE_CONTENT_OPERATION_NAME), declaration.get("operations"));
    assertEquals(List.of(NewsService.NEWS_ACTIVITY_TYPE), declaration.get("excludedActivityTypes"));
    assertFalse(declaration.containsKey("legacyOperations"), "no name was ever emitted for an article under another operation");
    assertFalse(declaration.containsKey("browserOperations"));

    Map<String, Object> labels = (Map<String, Object>) declaration.get("labels");
    assertEquals("content.collaborativeActivity.help.title", labels.get("title"));
    assertEquals(List.of("content.collaborativeActivity.help.counted"), labels.get("counted"));
  }

}
