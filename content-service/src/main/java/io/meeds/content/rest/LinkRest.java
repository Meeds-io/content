/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.content.model.Link;
import io.meeds.content.model.LinkSetting;
import io.meeds.content.rest.model.LinkSettingRestEntity;
import io.meeds.content.rest.util.LinkEntityBuilder;
import io.meeds.content.service.LinkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("links")
@Tag(name = "/content/rest/links", description = "Managing links for Links Application")
public class LinkRest {

  @Autowired
  private LinkService linkService;

  @GetMapping(path = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Retrieves a link application settings with associated links", description = "Retrieves a link application settings with associated links", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public ResponseEntity<LinkSettingRestEntity> getLinkSetting(
                                                              WebRequest request,
                                                              @Parameter(description = "Link name", required = true)
                                                              @PathVariable("name")
                                                              String name,
                                                              @Parameter(description = "User language", required = false)
                                                              @RequestParam("lang")
                                                              String lang) {
    try {
      LinkSetting linkSetting = linkService.getLinkSetting(name, lang, RestUtils.getCurrentUserAclIdentity());
      if (linkSetting == null) {
        return ResponseEntity.notFound().build();
      }
      LinkSettingRestEntity linkSettingEntity = getLinkSettingEntity(linkSetting, lang);
      String eTag = String.valueOf(linkSettingEntity.hashCode());
      if (request.checkNotModified(eTag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
      } else {
        return ResponseEntity.ok()
                             .eTag(eTag)
                             .body(linkSettingEntity);
      }
    } catch (IllegalAccessException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PutMapping(path = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Saves a link application settings with associated links", description = "Saves a link application settings with associated links", method = "GET")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
  })
  public LinkSettingRestEntity saveLinkSetting(
                                               WebRequest request,
                                               @Parameter(description = "Link name", required = true)
                                               @PathVariable("name")
                                               String name,
                                               LinkSettingRestEntity linkSettingEntity) {
    try {
      LinkSetting linkSetting = LinkEntityBuilder.toLinkSetting(linkSettingEntity);
      List<Link> linksToSave = LinkEntityBuilder.toLinks(linkSettingEntity);
      linkSetting = linkService.saveLinkSetting(linkSetting, linksToSave, RestUtils.getCurrentUserAclIdentity());
      return getLinkSettingEntity(linkSetting, null);
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping(path = "{name}/{id}/icon", produces = MediaType.IMAGE_PNG_VALUE)
  @Operation(summary = "Gets a link icon specified by setting name and link id", method = "GET", description = "Gets a link icon specified by setting name and link id")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public ResponseEntity<InputStreamResource> getLinkIcon(
                                                         WebRequest request,
                                                         @Parameter(description = "Link name", required = true)
                                                         @PathVariable("name")
                                                         String name,
                                                         @Parameter(description = "Link id", required = true)
                                                         @PathVariable("id")
                                                         long id) {
    try {
      LinkSetting linkSetting = linkService.getLinkSetting(name, null, RestUtils.getCurrentUserAclIdentity());
      if (linkSetting == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      String eTag = String.valueOf(linkSetting.hashCode());
      if (request.checkNotModified(eTag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
      } else {
        InputStream stream = linkService.getLinkIconStream(linkSetting.getName(), id);
        if (stream == null) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
          BodyBuilder builder = ResponseEntity.ok();
          return builder.contentType(MediaType.IMAGE_PNG)
                        .lastModified(linkSetting.getLastModified())
                        .eTag(eTag)
                        .body(new InputStreamResource(stream));
        }
      }
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  private LinkSettingRestEntity getLinkSettingEntity(LinkSetting linkSetting, String lang) {
    List<Link> links = linkService.getLinks(linkSetting.getName(), lang, true);
    return LinkEntityBuilder.build(linkSetting, links);
  }

}
