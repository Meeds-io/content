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
package io.meeds.content.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ListViewSettingValuesTest {

  @Test
  void flagIsTrueOrFalse() {
    assertTrue(ListViewSettingValues.isValidFlag(null));
    assertTrue(ListViewSettingValues.isValidFlag("true"));
    assertTrue(ListViewSettingValues.isValidFlag("false"));
    assertFalse(ListViewSettingValues.isValidFlag(""));
    assertFalse(ListViewSettingValues.isValidFlag("TRUE"));
    assertFalse(ListViewSettingValues.isValidFlag("1"));
  }

  @Test
  void viewTemplateIsAName() {
    assertTrue(ListViewSettingValues.isValidViewTemplate(null));
    assertTrue(ListViewSettingValues.isValidViewTemplate(""));
    assertTrue(ListViewSettingValues.isValidViewTemplate("NewsCards"));
    assertTrue(ListViewSettingValues.isValidViewTemplate("Custom_View-2"));
    assertFalse(ListViewSettingValues.isValidViewTemplate("News Cards"));
    assertFalse(ListViewSettingValues.isValidViewTemplate("1Cards"));
    assertFalse(ListViewSettingValues.isValidViewTemplate("a<b"));
    assertFalse(ListViewSettingValues.isValidViewTemplate("A".repeat(65)));
  }

  @Test
  void sourceOptionIsOneOfTheDrawerOptions() {
    assertTrue(ListViewSettingValues.isValidSourceOption(null));
    assertTrue(ListViewSettingValues.isValidSourceOption(""));
    assertTrue(ListViewSettingValues.isValidSourceOption("posted"));
    assertTrue(ListViewSettingValues.isValidSourceOption("target"));
    assertTrue(ListViewSettingValues.isValidSourceOption("selectedList"));
    assertFalse(ListViewSettingValues.isValidSourceOption("all"));
    assertFalse(ListViewSettingValues.isValidSourceOption("Posted"));
  }

  @Test
  void idListIsWholeNumbersSeparatedByCommas() {
    assertTrue(ListViewSettingValues.isValidIdList(null));
    assertTrue(ListViewSettingValues.isValidIdList(""));
    assertTrue(ListViewSettingValues.isValidIdList("12"));
    assertTrue(ListViewSettingValues.isValidIdList("12,345,6"));
    assertFalse(ListViewSettingValues.isValidIdList("12,"));
    assertFalse(ListViewSettingValues.isValidIdList(",12"));
    assertFalse(ListViewSettingValues.isValidIdList("12,,3"));
    assertFalse(ListViewSettingValues.isValidIdList("1.5"));
    assertFalse(ListViewSettingValues.isValidIdList("-5"));
    assertFalse(ListViewSettingValues.isValidIdList("0x1A"));
    assertFalse(ListViewSettingValues.isValidIdList("12 3"));
    assertFalse(ListViewSettingValues.isValidIdList("1".repeat(19)));
  }

  @Test
  void categoryDepthIsWithinWhatTheDrawerOffers() {
    assertTrue(ListViewSettingValues.isValidCategoryDepth(null));
    assertTrue(ListViewSettingValues.isValidCategoryDepth(""));
    assertTrue(ListViewSettingValues.isValidCategoryDepth("0"));
    assertTrue(ListViewSettingValues.isValidCategoryDepth("4"));
    assertTrue(ListViewSettingValues.isValidCategoryDepth("50"));
    assertFalse(ListViewSettingValues.isValidCategoryDepth("51"));
    assertFalse(ListViewSettingValues.isValidCategoryDepth("99"));
    assertFalse(ListViewSettingValues.isValidCategoryDepth("100"));
    assertFalse(ListViewSettingValues.isValidCategoryDepth("-1"));
    assertFalse(ListViewSettingValues.isValidCategoryDepth("2.5"));
  }

}
