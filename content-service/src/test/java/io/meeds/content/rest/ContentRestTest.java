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
package io.meeds.content.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.ContentPage;
import io.meeds.content.model.ContentType;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.rest.model.ContentEntryList;
import io.meeds.content.service.ContentService;

@RunWith(MockitoJUnitRunner.class)
public class ContentRestTest {

  private static final String JOHN = "john";

  @Mock
  private ContentService      contentService;

  @InjectMocks
  private ContentRest         contentRest;

  @Before
  public void setUp() {
    ConversationState.setCurrent(new ConversationState(new Identity(JOHN)));
  }

  @Test
  public void testGetContentTypes() {
    List<ContentType> types = Arrays.asList(new ContentType("news", "content.list.filter.contentType.news"));
    when(contentService.getContentTypes()).thenReturn(types);

    ResponseEntity<List<ContentType>> response = contentRest.getContentTypes();

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(types, response.getBody());
  }

  @Test
  public void testGetContentListSuccess() throws Exception {
    ContentEntry entry = new ContentEntry();
    entry.setId("1");
    entry.setCategoryIds(Arrays.asList(1L, 2L));
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenReturn(new ContentPage(Arrays.asList(entry), false));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 20, null, null);

    assertEquals(200, response.getStatusCodeValue());
    ContentEntryList body = response.getBody();
    assertEquals(1, body.getSize());
    assertEquals(Arrays.asList(1L, 2L), body.getCategoryIds());
  }

  @Test
  public void testGetContentListClampsLimitToMax() throws Exception {
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenReturn(new ContentPage(Arrays.asList(), false));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 100000, null, null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(100, response.getBody().getLimit());
  }

  @Test
  public void testGetContentListPropagatesHasMore() throws Exception {
    ContentEntry entry = new ContentEntry();
    entry.setId("1");
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenReturn(new ContentPage(Arrays.asList(entry), true));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 20, null, null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(true, response.getBody().isHasMore());
  }

  @Test
  public void testGetContentListNotFound() throws Exception {
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenThrow(new ObjectNotFoundException("not found"));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 20, null, null);

    assertEquals(404, response.getStatusCodeValue());
  }

  @Test
  public void testGetContentListBadRequest() throws Exception {
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenThrow(new IllegalArgumentException("bad status"));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 20, null, null);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  public void testGetContentListForbidden() throws Exception {
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenThrow(new IllegalAccessException("denied"));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 20, null, null);

    assertEquals(403, response.getStatusCodeValue());
  }

  @Test
  public void testGetContentListInternalError() throws Exception {
    when(contentService.getContentList(any(ContentFilter.class), any(Identity.class))).thenThrow(new RuntimeException("boom"));

    ResponseEntity<ContentEntryList> response = contentRest.getContentList(null, null, null, null, null, 0, 20, null, null);

    assertEquals(500, response.getStatusCodeValue());
  }

  @Test
  public void testDeleteContentSuccess() throws Exception {
    ResponseEntity<Void> response = contentRest.deleteContent("1", "news", "published");

    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
  public void testDeleteContentNotFound() throws Exception {
    doThrow(new ObjectNotFoundException("not found")).when(contentService).deleteContent(eq("1"), eq("news"), eq("published"), any());

    ResponseEntity<Void> response = contentRest.deleteContent("1", "news", "published");

    assertEquals(404, response.getStatusCodeValue());
  }

  @Test
  public void testDeleteContentForbidden() throws Exception {
    doThrow(new IllegalAccessException("denied")).when(contentService).deleteContent(eq("1"), eq("news"), eq("published"), any());

    ResponseEntity<Void> response = contentRest.deleteContent("1", "news", "published");

    assertEquals(403, response.getStatusCodeValue());
  }

  @Test
  public void testDeleteContentInternalError() throws Exception {
    doThrow(new RuntimeException("boom")).when(contentService).deleteContent(eq("1"), eq("news"), eq("published"), any());

    ResponseEntity<Void> response = contentRest.deleteContent("1", "news", "published");

    assertEquals(500, response.getStatusCodeValue());
  }

}
