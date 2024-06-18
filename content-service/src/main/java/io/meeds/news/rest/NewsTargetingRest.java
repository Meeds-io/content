/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.news.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.metadata.model.Metadata;

import io.meeds.news.service.NewsTargetingService;
import io.meeds.news.utils.NewsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("targeting")
@Tag(name = "targeting", description = "Manage targeting operations")
public class NewsTargetingRest {

  private static final Log         LOG                     = ExoLogger.getLogger(NewsTargetingRest.class);

  @Autowired
  private NewsTargetingService     newsTargetingService;

  @Autowired
  private PortalContainer          container;

  private ScheduledExecutorService scheduledExecutor;

  private Map<String, String>      newsTargetToDeleteQueue = new HashMap<>();


  @PostConstruct
  public void init() {
    scheduledExecutor = Executors.newScheduledThreadPool(1);
  }

  @PreDestroy
  public void destroy() {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown();
    }
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON)
  @Secured("users")
  @Operation(summary = "Get all news targets", method = "GET", description = "Get all news targets")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error")})
  public ResponseEntity<List<NewsTargetingEntity>> getAllTargets() {
    try {
      List<NewsTargetingEntity> targets = newsTargetingService.getAllTargets();
      return ResponseEntity.ok(targets);
    } catch (Exception e) {
      LOG.error("Error when getting the news targets", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(path = "allowed", produces = MediaType.APPLICATION_JSON)
  @Secured("users")
  @Operation(summary = "Get all allowed news targets of the current user", method = "GET", description = "Get all allowed news targets of the current user")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error")})
  public ResponseEntity<List<NewsTargetingEntity>> getAllowedTargets() {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      List<NewsTargetingEntity> allowedTargets = newsTargetingService.getAllowedTargets(currentIdentity);
      return ResponseEntity.ok(allowedTargets);
    } catch (Exception e) {
      LOG.error("Error when getting allowed news targets for the user " + currentIdentity.getUserId(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping(path = "{targetName}", produces = MediaType.APPLICATION_JSON)
  @Secured("users")
  @Operation(summary = "Delete news target", method = "DELETE", description = "This deletes news target")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News target deleted"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to delete the news target"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteTarget(@PathVariable("targetName")
                               String targetName,
                               @RequestParam(name = "delay", required = false)
                               long delay) {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      if (StringUtils.isBlank(targetName)) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Target name ist mandatory").build();
      }
      if (delay > 0) {
        newsTargetToDeleteQueue.put(targetName, currentIdentity.getUserId());
        scheduledExecutor.schedule(() -> {
          if (newsTargetToDeleteQueue.containsKey(targetName)) {
            ExoContainerContext.setCurrentContainer(container);
            RequestLifeCycle.begin(container);
            try {
              newsTargetToDeleteQueue.remove(targetName);
              newsTargetingService.deleteTargetByName(targetName, currentIdentity);
            } catch (IllegalAccessException e) {
              LOG.warn("User '{}' is not authorized to delete the news target with name " + targetName,
                       currentIdentity.getUserId(),
                       e);
            } catch (Exception e) {
              LOG.error("Error when deleting the news target with name " + targetName, e);
            } finally {
              RequestLifeCycle.end();
            }
          }
        }, delay, TimeUnit.SECONDS);
      } else {
        newsTargetToDeleteQueue.remove(targetName);
        newsTargetingService.deleteTargetByName(targetName, currentIdentity);
      }
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Error when deleting the news target with name " + targetName, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @PostMapping(path = "{targetName}/undoDelete")
  @Secured("users")
  @Operation(summary = "Undo deleting news target if not yet effectively deleted", method = "POST", description = "Undo deleting news target if not yet effectively deleted")
  @ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Forbidden operation") })
  public Response undoDeleteTarget(@PathVariable("targetName")
                                   String targetName) {
    if (StringUtils.isBlank(targetName)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Target name ist mandatory").build();
    }
    if (newsTargetToDeleteQueue.containsKey(targetName)) {
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      String authenticatedUser = currentIdentity.getUserId();
      String originalModifierUser = newsTargetToDeleteQueue.get(targetName);
      if (!originalModifierUser.equals(authenticatedUser)) {
        LOG.warn("User {} attempts to cancel deletion of a news target deleted by user {}",
                 authenticatedUser,
                 originalModifierUser);
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      newsTargetToDeleteQueue.remove(targetName);
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("News target with name {} was already deleted or isn't planned to be deleted" + targetName)
                     .build();
    }
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
  @Secured("users")
  @Operation(summary = "Create news target", method = "POST", description = "Create news target")
  @ApiResponses(value = { @ApiResponse(responseCode = "401", description = "User not authorized to create news target"),
      @ApiResponse(responseCode = "403", description = "Forbidden operation"),
      @ApiResponse(responseCode = "409", description = "Conflict operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error")})
  public Response createNewsTarget(@RequestBody NewsTargetingEntity newsTargetingEntity) {
    if (newsTargetingEntity.getProperties() == null
            || newsTargetingEntity.getProperties().get(NewsUtils.TARGET_PERMISSIONS) == null
            || newsTargetingEntity.getProperties().get(NewsUtils.TARGET_PERMISSIONS).isEmpty()) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      Metadata addedNewsTarget = newsTargetingService.createNewsTarget(newsTargetingEntity, currentIdentity);
      return Response.ok(addedNewsTarget).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not authorized to create a news target with name " + newsTargetingEntity.getName(),
               currentIdentity.getUserId(),
               e);
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (IllegalArgumentException e) {
      LOG.warn("User '{}' can't create a news target with the same name " + newsTargetingEntity.getName(),
               currentIdentity.getUserId(),
               e);
      return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error when creating a news target with name " + newsTargetingEntity.getName(), e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @PutMapping(path = "{originalTargetName}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
  @Secured("users")
  @Operation(summary = "Update an existing news target", method = "PUT", description = "Update an existing news target")
  @ApiResponses(value = { @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "403", description = "Forbidden operation"),
      @ApiResponse(responseCode = "404", description = "Object not found"),
      @ApiResponse(responseCode = "409", description = "Conflict operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response updateNewsTarget(@RequestBody NewsTargetingEntity newsTargetingEntity,
                                   @PathVariable("originalTargetName")
                                   String originalTargetName) {
    if (newsTargetingEntity.getProperties() == null
        || newsTargetingEntity.getProperties().get(NewsUtils.TARGET_PERMISSIONS) == null
        || newsTargetingEntity.getProperties().get(NewsUtils.TARGET_PERMISSIONS).isEmpty()) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      Metadata metadata = newsTargetingService.updateNewsTargets(originalTargetName, newsTargetingEntity, currentIdentity);
      return Response.ok(metadata).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not authorized to update news target with name '{}'",
               currentIdentity.getUserId(),
               originalTargetName,
               e);
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (IllegalArgumentException e) {
      LOG.warn("User '{}' can't update news target with name '{}'", currentIdentity.getUserId(), originalTargetName, e);
      return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
    } catch (IllegalStateException e) {
      LOG.warn("The news target '{}' can't be found", newsTargetingEntity.getName(), e);
      return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error when updating the news target with name " + newsTargetingEntity.getName(), e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }
}
