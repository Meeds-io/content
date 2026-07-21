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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.content.model.ContentEntry;
import io.meeds.content.model.ContentType;
import io.meeds.content.model.filter.ContentFilter;
import io.meeds.content.rest.model.ContentEntryList;
import io.meeds.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("contents/all")
@Tag(name = "content/rest/contents/all", description = "Listing content merged across content types (News, Notes)")
public class ContentRest {

  private static final Log LOG = ExoLogger.getLogger(ContentRest.class);

  @Autowired
  private ContentService   contentService;

  @GetMapping(path = "types", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Get registered content types", method = "GET", description = "This gets the list of content types registered by content-producing addons, for the Content List filter drawer.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Content types returned") })
  public ResponseEntity<List<ContentType>> getContentTypes() {
    return ResponseEntity.ok(contentService.getContentTypes());
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Get merged content list", method = "GET", description = "This gets the list of content (News articles and Notes) matching the given filters, that the authenticated user has access to.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Content list returned"),
    @ApiResponse(responseCode = "401", description = "User not authorized to get the content list"),
    @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<ContentEntryList> getContentList(@Parameter(description = "Content types to include (news, notes, event)")
  @RequestParam(name = "contentTypes", required = false)
  List<String> contentTypes,
                                                          @Parameter(description = "Content status (published, myContent, scheduled, draft)")
                                                          @RequestParam(name = "status", required = false)
                                                          String status,
                                                          @Parameter(description = "Spaces to restrict the content to")
                                                          @RequestParam(name = "spaces", required = false)
                                                          List<String> spaces,
                                                          @Parameter(description = "Category id to filter the content by")
                                                          @RequestParam(name = "categoryId", required = false)
                                                          Long categoryId,
                                                          @Parameter(description = "Text to search in the content name/summary/content")
                                                          @RequestParam(name = "text", required = false)
                                                          String text,
                                                          @Parameter(description = "Content pagination offset")
                                                          @RequestParam(name = "offset", defaultValue = "0", required = false)
                                                          int offset,
                                                          @Parameter(description = "Content pagination limit")
                                                          @RequestParam(name = "limit", defaultValue = "20", required = false)
                                                          int limit,
                                                          @Parameter(description = "Admin-configured category ids to restrict the list to")
                                                          @RequestParam(name = "includeCategoryIds", required = false)
                                                          List<Long> includeCategoryIds,
                                                          @Parameter(description = "Admin-configured category ids to always exclude from the list")
                                                          @RequestParam(name = "excludeCategoryIds", required = false)
                                                          List<Long> excludeCategoryIds) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      ContentFilter filter = new ContentFilter();
      filter.setContentTypes(contentTypes);
      filter.setStatus(status);
      filter.setSpaces(spaces);
      filter.setCategoryId(categoryId);
      filter.setSearchText(text);
      filter.setOffset(offset);
      filter.setLimit(limit);
      filter.setIncludeCategoryIds(includeCategoryIds);
      filter.setExcludeCategoryIds(excludeCategoryIds);

      List<ContentEntry> items = contentService.getContentList(filter, currentIdentity);

      ContentEntryList result = new ContentEntryList();
      result.setItems(items);
      result.setOffset(offset);
      result.setLimit(limit);
      result.setSize(items.size());
      result.setCategoryIds(items.stream()
                                 .map(ContentEntry::getCategoryIds)
                                 .filter(java.util.Objects::nonNull)
                                 .flatMap(List::stream)
                                 .distinct()
                                 .collect(java.util.stream.Collectors.toList()));
      return ResponseEntity.ok(result);
    } catch (IllegalAccessException e) {
      LOG.debug("User '{}' is not authorized to access the requested content", currentIdentity.getUserId(), e);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (Exception e) {
      LOG.warn("Error while retrieving the content list for user '{}'", currentIdentity.getUserId(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Delete a piece of content", method = "DELETE", description = "This deletes the News article or Note with the given id, if the authenticated user has edit permission on it.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Content deleted"),
    @ApiResponse(responseCode = "403", description = "User not authorized to delete this content"),
    @ApiResponse(responseCode = "404", description = "Content not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<Void> deleteContent(@PathVariable("id")
  String id,
                                            @Parameter(description = "Content type (news or notes)")
                                            @RequestParam("contentType")
                                            String contentType,
                                            @Parameter(description = "Content status, used to resolve which news object type to delete")
                                            @RequestParam(name = "status", required = false)
                                            String status) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      contentService.deleteContent(id, contentType, status, currentIdentity);
      return ResponseEntity.ok().build();
    } catch (ObjectNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalAccessException e) {
      LOG.debug("User '{}' is not authorized to delete content '{}'", currentIdentity.getUserId(), id, e);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (Exception e) {
      LOG.warn("Error while deleting content '{}' for user '{}'", id, currentIdentity.getUserId(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

}
