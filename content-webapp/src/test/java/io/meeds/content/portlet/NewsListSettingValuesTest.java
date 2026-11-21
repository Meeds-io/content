/*
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
package io.meeds.content.portlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.meeds.content.news.rest.model.NewsTargetingEntity;
import io.meeds.content.news.service.NewsTargetingService;

class NewsListSettingValuesTest {

  @Test
  void limitIsAPositiveNumber() {
    assertTrue(NewsListSettingValues.isValidLimit(null));
    assertTrue(NewsListSettingValues.isValidLimit(""));
    assertTrue(NewsListSettingValues.isValidLimit("1"));
    // the templates' own maximums, and a selected list longer than any
    assertTrue(NewsListSettingValues.isValidLimit("3"));
    assertTrue(NewsListSettingValues.isValidLimit("4"));
    assertTrue(NewsListSettingValues.isValidLimit("250"));
    assertTrue(NewsListSettingValues.isValidLimit("1234"));
    assertFalse(NewsListSettingValues.isValidLimit("0"));
    assertFalse(NewsListSettingValues.isValidLimit("-3"));
    assertFalse(NewsListSettingValues.isValidLimit("4a"));
    assertFalse(NewsListSettingValues.isValidLimit("2.5"));
    assertFalse(NewsListSettingValues.isValidLimit("99999999999"));
    assertEquals("6", NewsListSettingValues.limitOrDefault("6"));
    assertEquals("250", NewsListSettingValues.limitOrDefault("250"));
    // an invalid stored limit stands for none: the view applies its template's own default
    assertEquals("", NewsListSettingValues.limitOrDefault("many"));
  }

  @Test
  void seeAllUrlIsRefusedOnlyForAnotherScheme() {
    assertTrue(NewsListSettingValues.isValidSeeAllUrl(null));
    assertTrue(NewsListSettingValues.isValidSeeAllUrl(""));
    assertTrue(NewsListSettingValues.isValidSeeAllUrl("https://example.org/news?page=2"));
    assertTrue(NewsListSettingValues.isValidSeeAllUrl("HTTP://example.org"));
    assertTrue(NewsListSettingValues.isValidSeeAllUrl("/portal/dw/news"));
    assertTrue(NewsListSettingValues.isValidSeeAllUrl("www.example.org"));
    assertTrue(NewsListSettingValues.isValidSeeAllUrl("news/all"));
    assertFalse(NewsListSettingValues.isValidSeeAllUrl("FTP://example.org"));
    assertEquals("news/all", NewsListSettingValues.seeAllUrlOrDefault("news/all"));
    assertEquals("", NewsListSettingValues.seeAllUrlOrDefault("ftp://example.org"));
  }

  @Test
  void targetIsOneOfTheInstanceTargets() {
    NewsTargetingService service = mock(NewsTargetingService.class);
    NewsTargetingEntity target = new NewsTargetingEntity();
    target.setName("sliderNews");
    when(service.getAllTargets()).thenReturn(List.of(target));

    assertTrue(NewsListSettingValues.isKnownTarget(null, service));
    assertTrue(NewsListSettingValues.isKnownTarget("", service));
    assertTrue(NewsListSettingValues.isKnownTarget("null", service));
    assertTrue(NewsListSettingValues.isKnownTarget("sliderNews", service));
    assertFalse(NewsListSettingValues.isKnownTarget("otherNews", service));
    assertEquals("sliderNews", NewsListSettingValues.targetOrDefault("sliderNews", service));
    assertEquals("", NewsListSettingValues.targetOrDefault("otherNews", service));
    assertEquals("", NewsListSettingValues.targetOrDefault("null", service));
  }

}
