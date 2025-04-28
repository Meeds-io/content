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
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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

  private static final CacheControl CACHE_CONTROL          = new CacheControl();

  private static final CacheControl IMG_CACHE_CONTROL      = new CacheControl();

  private static final int          CACHE_IN_SECONDS       = 365 * 86400;

  private static final long         CACHE_IN_MILLI_SECONDS = CACHE_IN_SECONDS * 1000l;

  private static final Log          LOG                    = ExoLogger.getLogger(LinkRest.class);

  static {
    CACHE_CONTROL.setNoCache(true);
    IMG_CACHE_CONTROL.setMaxAge(CACHE_IN_SECONDS);
  }

  @Autowired
  private LinkService linkService;

  @GetMapping(path = "{name}", produces = MediaType.APPLICATION_JSON)
  @Operation(summary = "Retrieves a link application settings with associated links", description = "Retrieves a link application settings with associated links", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public Response getLinkSetting(
                                 @Context
                                 Request request,
                                 @Parameter(description = "Link name", required = true)
                                 @PathVariable("name")
                                 String name,
                                 @Parameter(description = "User language", required = false)
                                 @QueryParam("lang")
                                 String lang) {
    try {
      LinkSetting linkSetting = linkService.getLinkSetting(name, lang, RestUtils.getCurrentUserAclIdentity());
      if (linkSetting == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      EntityTag eTag = new EntityTag(String.valueOf(linkSetting.hashCode()));
      Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
      if (builder == null) {
        builder = Response.ok(getLinkSettingEntity(linkSetting, lang));
      }
      builder.lastModified(new Date(linkSetting.getLastModified()));
      builder.tag(eTag);
      builder.cacheControl(CACHE_CONTROL);
      return builder.build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error accessing setting {} for user {}", name, RestUtils.getCurrentUser(), e);
      return Response.status(Status.UNAUTHORIZED).build();
    }
  }

  @PUT
  @Path("{name}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Saves a link application settings with associated links", description = "Saves a link application settings with associated links", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"), })
  public Response saveLinkSetting(LinkSettingRestEntity linkSettingEntity) {
    try {
      LinkSetting linkSetting = LinkEntityBuilder.toLinkSetting(linkSettingEntity);
      List<Link> linksToSave = LinkEntityBuilder.toLinks(linkSettingEntity);
      linkSetting = linkService.saveLinkSetting(linkSetting, linksToSave, RestUtils.getCurrentUserAclIdentity());
      return Response.ok(getLinkSettingEntity(linkSetting, null)).build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error saving setting '{}' by user '{}'", linkSettingEntity, RestUtils.getCurrentUser(), e);
      return Response.status(Status.UNAUTHORIZED).build();
    } catch (ObjectNotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @GetMapping(path = "{name}/{id}/icon")
  @Operation(summary = "Gets a link icon specified by setting name and link id", method = "GET", description = "Gets a link icon specified by setting name and link id")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "304", description = "Not modified"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public Response getLinkIcon(
                              @Context
                              Request request,
                              @Parameter(description = "Link setting name", required = true)
                              @PathVariable("name")
                              String name,
                              @Parameter(description = "Link id", required = true)
                              @PathVariable("id")
                              long id) {
    try {
      LinkSetting linkSetting = linkService.getLinkSetting(name, null, RestUtils.getCurrentUserAclIdentity());
      if (linkSetting == null) {
        return Response.status(Status.NOT_FOUND).build();
      }

      EntityTag eTag = new EntityTag(String.valueOf(linkSetting.getLastModified()));
      Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
      if (builder == null) {
        InputStream stream = linkService.getLinkIconStream(linkSetting.getName(), id);
        if (stream == null) {
          return Response.status(Status.NOT_FOUND).build();
        } else {
          builder = Response.ok(stream, "image/png");
          builder.lastModified(new Date(linkSetting.getLastModified()));
          builder.tag(eTag);
          builder.cacheControl(IMG_CACHE_CONTROL);
          builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
        }
      }
      return builder.build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error getting icon for link '{}/{}' by user '{}'", name, id, RestUtils.getCurrentUser(), e);
      return Response.status(Status.UNAUTHORIZED).build();
    } catch (IOException e) {
      LOG.warn("Error getting icon for link '{}/{}' by user '{}'", name, id, RestUtils.getCurrentUser(), e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private LinkSettingRestEntity getLinkSettingEntity(LinkSetting linkSetting, String lang) {
    List<Link> links = linkService.getLinks(linkSetting.getName(), lang, true);
    return LinkEntityBuilder.build(linkSetting, links);
  }

}
