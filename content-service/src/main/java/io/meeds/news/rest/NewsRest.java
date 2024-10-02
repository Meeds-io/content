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

import static io.meeds.news.utils.NewsUtils.NewsObjectType.ARTICLE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import javax.ws.rs.core.Response;


import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;


import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.application.localization.LocalizationFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.metadata.tag.model.TagFilter;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.news.filter.NewsFilter;
import io.meeds.news.model.News;
import io.meeds.news.search.NewsESSearchResult;
import io.meeds.news.service.NewsService;
import io.meeds.news.utils.NewsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("contents")
@Tag(name = "content/rest/contents", description = "Managing contents")
public class NewsRest {

  private static final Log          LOG                             = ExoLogger.getLogger(NewsRest.class);
  
  @Autowired
  private NewsService               newsService;

  @Autowired
  private SpaceService              spaceService;

  @Autowired
  private IdentityManager           identityManager;

  @Autowired
  private PortalContainer           container;

  @Autowired
  private FavoriteService           favoriteService;

  private Map<String, String>       newsToDeleteQueue               = new HashMap<>();

  private ScheduledExecutorService  scheduledExecutor;

  private static final int          CACHE_DURATION_SECONDS          = 31536000;

  private enum FilterType {
    PINNED, MYPOSTED, DRAFTS, SCHEDULED, ALL
  }

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

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Create a news", method = "POST", description = "This creates the news if the authenticated user is a member of the space or a spaces super manager. The news is created in draft status, unless the publicationState property is set to 'posted'.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News created"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to create the news"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<News> createNews(@RequestBody News news) {
    if (news == null || StringUtils.isEmpty(news.getSpaceId())) {
      return ResponseEntity.badRequest().build();
    }

    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      News createdNews = newsService.createNews(news, currentIdentity);

      return ResponseEntity.ok(createdNews);
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not authorized to create news", currentIdentity.getUserId(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error when creating the news " + news.getTitle(), e);
      return ResponseEntity.badRequest().build();
    }
  }


  @GetMapping(path = "canCreateNews/{spaceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "check if the current user can create a news in the given space", method = "GET", description = "This checks if the current user can create a news in the given space")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "User ability to create a news is returned"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to create a news"),
      @ApiResponse(responseCode = "404", description = "Space not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<Boolean> canCreateNews(@PathVariable("spaceId") String spaceId) {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      if (StringUtils.isBlank(spaceId)) {
        return ResponseEntity.badRequest().build();
      }
      Space space = spaceService.getSpaceById(spaceId);
      if (space == null) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok(newsService.canCreateNews(space, currentIdentity));
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not autorized to check if we can create news", currentIdentity.getUserId(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error when checking if the authenticated user can create a news", e);
      return ResponseEntity.internalServerError().build();
    }
  }


  @PutMapping(path = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Create a news", method = "PUT", description = "This updates the news if the authenticated user is a member of the space or a spaces super manager.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News updated"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to update the news"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<News> updateNews(@PathVariable("id")
                                         String id,
                                         @Parameter(description = "Post news")
                                         @RequestParam(name = "post", required = false)
                                         boolean post,
                                         @Parameter(description = "News object type to be updated")
                                         @RequestParam("type")
                                         String newsObjectType,
                                         @Parameter(description = "News update action type to be done")
                                         @RequestParam(name = "newsUpdateType", defaultValue = "content", required = false)
                                         String newsUpdateType,
                                         @RequestBody
                                         News updatedNews) {

    if (updatedNews == null) {
      return ResponseEntity.badRequest().build();
    }
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      News news = newsService.getNewsById(id, currentIdentity, false, newsObjectType);
      if (news == null) {
        return ResponseEntity.notFound().build();
      }
      news.setTitle(updatedNews.getTitle());
      news.setBody(updatedNews.getBody());
      news.setUploadId(updatedNews.getUploadId());
      news.setPublicationState(updatedNews.getPublicationState());
      news.setUpdaterFullName(updatedNews.getUpdaterFullName());
      news.setActivityPosted(updatedNews.isActivityPosted());
      news.setTargets(updatedNews.getTargets());
      news.setAudience(updatedNews.getAudience());
      news.setProperties(updatedNews.getProperties());
      news.setLang(updatedNews.getLang());
      news = newsService.updateNews(news, currentIdentity.getUserId(), post, updatedNews.isPublished(), newsObjectType, newsUpdateType);

      return ResponseEntity.ok(news);
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not authorized to update news", currentIdentity.getUserId(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error when updating the news " + id, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Delete news", method = "DELETE", description = "This deletes the news")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News deleted"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to delete the news"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteNews(@PathVariable("id")
                             String id,
                             @Parameter(description = "news object to be deleted")
                             @RequestParam("type")
                             String newsObjectType,
                             @Parameter(description = "Time to effectively delete news")
                             @RequestParam(name = "delay", required = false)
                             long delay) {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      if (StringUtils.isBlank(id)) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      News news = newsService.getNewsById(id,
                                          currentIdentity,
                                          false,
                                          newsObjectType);
      if (news == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      if (delay > 0) {// TODO Move to service layer
        newsToDeleteQueue.put(id, currentIdentity.getUserId());
        scheduledExecutor.schedule(() -> {
          if (newsToDeleteQueue.containsKey(id)) {
            ExoContainerContext.setCurrentContainer(container);
            RequestLifeCycle.begin(container);
            try {
              newsToDeleteQueue.remove(id);
              newsService.deleteNews(id, currentIdentity, newsObjectType);
            } catch (IllegalAccessException e) {
              LOG.error("User '{}' attempts to delete a non authorized news", currentIdentity.getUserId(), e);
            } catch (Exception e) {
              LOG.warn("Error when deleting the news with id " + id, e);
            } finally {
              RequestLifeCycle.end();
            }
          }
        }, delay, TimeUnit.SECONDS);
      } else {
        newsToDeleteQueue.remove(id);
        newsService.deleteNews(id, currentIdentity, newsObjectType);
      }
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not autorized to delete news", currentIdentity.getUserId(), e);
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error when deleting the news with id " + id, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @PostMapping("{id}/undoDelete")
  @Secured("users")
  @Operation(summary = "Undo deleting news if not yet effectively deleted", method = "POST", description = "Undo deleting news if not yet effectively deleted")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Forbidden operation"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response undoDeleteNews(@PathVariable("id")
                                 String id) {
    if (StringUtils.isBlank(id)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("News identifier must not be null or empty").build();
    }
    if (newsToDeleteQueue.containsKey(id)) {// TODO Move to service layer
      String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
      String originalModifierUser = newsToDeleteQueue.get(id);
      if (!originalModifierUser.equals(authenticatedUser)) {
        LOG.warn("User {} attempts to cancel deletion of a news deleted by user {}", authenticatedUser, originalModifierUser);
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      newsToDeleteQueue.remove(id);
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("News with id {} was already deleted or isn't planned to be deleted" + id)
                     .build();
    }
  }

  @GetMapping(path = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get a news", method = "GET", description = "This gets the news with the given id if the authenticated user is a member of the space or a spaces super manager.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News returned"),
      @ApiResponse(responseCode = "401", description = "User not authorized to get the news"),
      @ApiResponse(responseCode = "404", description = "News not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<News> getNewsById(@PathVariable("id")
                                          String id,
                                          @Parameter(description = "fields")
                                          @RequestParam(name = "fields", required = false)
                                          String fields,
                                          @Parameter(description = "News object type to be fetched")
                                          @RequestParam("type")
                                          String newsObjectType,
                                          @Parameter(description = "Is edit mode")
                                          @RequestParam(name = "editMode", defaultValue = "false", required = false)
                                          boolean editMode,
                                          @Parameter(description = "article translation")
                                          @RequestParam(name = "lang", required = false)
                                          String lang) {
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    try {
      if (StringUtils.isBlank(id)) {
        return ResponseEntity.badRequest().build();
      }
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      News news = newsService.getNewsByIdAndLang(id, currentIdentity, editMode, newsObjectType, StringUtils.isBlank(lang) ? null : lang);
      if (news == null || news.isDeleted()) {
        return ResponseEntity.notFound().build();
      }
      Locale userLocale = LocalizationFilter.getCurrentLocale();
      news.setBody(MentionUtils.substituteRoleWithLocale(news.getBody(), userLocale));
      // check favorite
      Identity userIdentity = identityManager.getOrCreateUserIdentity(currentIdentity.getUserId());
      if (userIdentity != null) {
        news.setFavorite(favoriteService.isFavorite(new Favorite("news",
                                                                 news.getLang() != null ? news.getId().concat("-").concat(news.getLang()) : news.getId(),
                                                                 "",
                                                                 Long.parseLong(userIdentity.getId()))));
      }

      if (StringUtils.isNotEmpty(fields) && fields.equals("spaces")) {// TODO
                                                                      // Move to
                                                                      // service
                                                                      // layer
        News filteredNews = new News();
        List<String> spacesList = new ArrayList<>();
        String newsActivities = news.getActivities();
        for (String newsActivity : newsActivities.split(";")) {
          String spaceId = newsActivity.split(":")[0];
          spacesList.add(spaceId);
        }
        filteredNews.setSharedInSpacesList(spacesList);
        return ResponseEntity.ok(filteredNews);
      } else {
        return ResponseEntity.ok(news);
      }
    } catch (IllegalAccessException e) {
      LOG.warn("User {} attempt to access unauthorized news with id {}", authenticatedUser, id);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error when getting the news " + id, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping(path = "markAsRead/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "mark a news article as read", method = "POST", description = "This marks a news article as read by the user who accessed its details.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "User not authorized to get the news"),
      @ApiResponse(responseCode = "404", description = "News not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })

  public Response markNewsAsRead(@PathVariable("id")
                                 String id) {
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    try {
      if (StringUtils.isBlank(id)) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      News news = newsService.getNewsById(id, currentIdentity, false, ARTICLE.name().toLowerCase());
      if (news == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      newsService.markAsRead(news, authenticatedUser);
      return Response.ok("ok").type(MediaType.APPLICATION_JSON_VALUE).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} has no access rights on news with id {}", authenticatedUser, id);
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error while marking news with id: {} as read", id, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Get news list", method = "GET", description = "This gets the list of news with the given search text, of the given author, in the given space or spaces, with the given publication state, with the given pinned state if the authenticated user is a member of the spaces or a super manager.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News list returned"),
      @ApiResponse(responseCode = "401", description = "User not authorized to get the news list"),
      @ApiResponse(responseCode = "404", description = "News list not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<NewsEntity> getNews(@Parameter(description = "News author")
                                            @RequestParam("author")
                                            String author,
                                            @Parameter(description = "News spaces")
                                            @RequestParam(name = "spaces", required = false)
                                            String spaces,
                                            @Parameter(description = "News filter")
                                            @RequestParam("filter")
                                            String filter,
                                            @Parameter(description = "search text")
                                            @RequestParam(name = "text", required = false)
                                            String text,
                                            @Parameter(description = "News pagination offset")
                                            @RequestParam(name = "offset", defaultValue = "0", required = false)
                                            int offset,
                                            @Parameter(description = "News pagination limit")
                                            @RequestParam(name = "limit", defaultValue = "10")
                                            int limit,
                                            @Parameter(description = "News total size")
                                            @RequestParam(name = "returnSize", defaultValue = "false", required = false)
                                            boolean returnSize,
                                            HttpServletRequest request) {
    try {// TODO Move to service layer
      String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
      if (StringUtils.isBlank(author) || !authenticatedUser.equals(author)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      NewsEntity newsEntity = new NewsEntity();
      // Get news drafts by space
      List<String> spacesList = new ArrayList<>();
      // Set spaces to search news in
      if (StringUtils.isNotEmpty(spaces)) {
        for (String spaceId : spaces.split(",")) {
          Space space = spaceService.getSpaceById(spaceId);
          if (space == null
              || (!spaceService.isSuperManager(authenticatedUser) && !spaceService.isMember(space, authenticatedUser))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
          }
          spacesList.add(spaceId);
        }
      }
      NewsFilter newsFilter = buildFilter(spacesList, filter, text, author, limit, offset);
      String lang = request.getLocale().getLanguage();
      newsFilter.setLang(lang);
      List<News> news;
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      // Set text to search news with
      if (StringUtils.isNotEmpty(text)) {
        TagService tagService = CommonsUtils.getService(TagService.class);
        long userIdentityId = RestUtils.getCurrentUserIdentityId();
        if (text.indexOf("#") == 0) {
          String tagName = text.replace("#", "");
          List<TagName> tagNames = tagService.findTags(new TagFilter(tagName, 0), userIdentityId);
          if (tagNames != null && !tagNames.isEmpty())
            newsFilter.setTagNames(tagNames.stream().map(e -> e.getName()).toList());
        }

        Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentIdentity.getUserId());
        news = newsService.searchNews(newsFilter, identity);
      } else {
        news = newsService.getNews(newsFilter, currentIdentity);
      }

      if (news != null) {
        Locale userLocale = LocalizationFilter.getCurrentLocale();
        news.stream()
            .filter(Objects::nonNull)
            .forEach(news1 -> news1.setBody(MentionUtils.substituteRoleWithLocale(news1.getBody(), userLocale)));
      }
      newsEntity.setNews(news);
      newsEntity.setOffset(offset);
      newsEntity.setLimit(limit);
      if (returnSize) {
        newsEntity.setSize(newsService.getNewsCount(newsFilter));
      }
      return ResponseEntity.ok(newsEntity);
    } catch (Exception e) {
      LOG.error("Error when getting the news with params author=" + author + ", spaces=" + spaces, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(path = "byTarget/{targetName}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get news list", method = "GET", description = "This gets the list of news by the given target.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News list returned"),
      @ApiResponse(responseCode = "401", description = "User not authorized to get the news list"),
      @ApiResponse(responseCode = "404", description = "News list not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<NewsEntity> getNewsByTarget(@PathVariable("targetName")
                                                    String targetName,
                                                    @Parameter(description = "News pagination offset")
                                                    @RequestParam(name = "offset", defaultValue = "0", required = false)
                                                    int offset,
                                                    @Parameter(description = "News pagination limit")
                                                    @RequestParam(name = "limit", defaultValue = "10")
                                                    int limit,
                                                    @Parameter(description = "News total size")
                                                    @RequestParam(name = "returnSize", required = false)
                                                    boolean returnSize,
                                                    HttpServletRequest request) {
    try {
      String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
      if (StringUtils.isBlank(targetName)) {
        return ResponseEntity.badRequest().build();
      }
      if (offset < 0) {
        ResponseEntity.badRequest().build();
      }
      if (limit < 0) {
        return ResponseEntity.badRequest().build();
      }
      NewsFilter newsFilter = buildFilter(null, "", "", authenticatedUser, limit, offset);
      newsFilter.setLang(request.getLocale().getLanguage());
      NewsEntity newsEntity = new NewsEntity();
      org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      List<News> news = newsService.getNewsByTargetName(newsFilter, targetName, currentIdentity);
      Locale userLocale = LocalizationFilter.getCurrentLocale();
      news.forEach(newsArticle -> {
        if (newsArticle != null) {
          newsArticle.setBody(MentionUtils.substituteRoleWithLocale(newsArticle.getBody(), userLocale));
        }
      });
      newsEntity.setNews(news);
      newsEntity.setOffset(offset);
      newsEntity.setLimit(limit);
      if (returnSize) {
        newsEntity.setSize(news.size());
      }
      return ResponseEntity.ok(newsEntity);
    } catch (Exception e) {
      LOG.error("Error when getting the news with target name=" + targetName, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(path = "byActivity/{activityId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Get a news identified by its activity or shared activity identifier", method = "GET", description = "This gets the news with the given id if the authenticated user is a member of the space or a spaces super manager.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News returned"),
      @ApiResponse(responseCode = "401", description = "User not authorized to get the news"),
      @ApiResponse(responseCode = "404", description = "News not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<News> getNewsByActivityId(@PathVariable("activityId")
                                                  String activityId,
                                                  @RequestParam(name = "lang", defaultValue = "null", required = false)
                                                  String lang) {
    if (StringUtils.isBlank(activityId)) {
      return ResponseEntity.badRequest().build();
    }
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      News news = newsService.getNewsByActivityIdAndLang(activityId, currentIdentity, lang);
      if (news == null) {
        return ResponseEntity.notFound().build();
      }
      Locale userLocale = LocalizationFilter.getCurrentLocale();
      news.setBody(MentionUtils.substituteRoleWithLocale(news.getBody(), userLocale));

      Identity userIdentity = identityManager.getOrCreateUserIdentity(currentIdentity.getUserId());
      if (userIdentity != null) {
        news.setFavorite(favoriteService.isFavorite(new Favorite("news",
                                                                 news.getLang() != null ? news.getId().concat("-").concat(news.getLang()) : news.getId(),
                                                                 "",
                                                                 Long.parseLong(userIdentity.getId()))));
      }
      return ResponseEntity.ok(news);
    } catch (IllegalAccessException e) {
      LOG.warn("User {} attempt to access unauthorized news with id {}", currentIdentity.getUserId(), activityId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (ObjectNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      LOG.error("Error when getting the news " + activityId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PatchMapping(path = "schedule", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Schedule a news", method = "POST", description = "This schedules the news if the authenticated user is a member of the space or a spaces super manager. The news is created in staged status, after reaching a date of publication startPublishedDate, the publicationState property is set to 'posted'.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "News scheduled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to schedule the news"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<News> scheduleNews(@Parameter(description = "News object type to be fetched")
                                           @RequestParam("type")
                                           String newsObjectType,
                                           @RequestBody
                                           News scheduledNews) {
    if (scheduledNews == null || StringUtils.isEmpty(scheduledNews.getId())) {
      return ResponseEntity.badRequest().build();
    }
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      News news = newsService.getNewsById(scheduledNews.getId(), currentIdentity, false, newsObjectType);
      if (news == null) {
        return ResponseEntity.notFound().build();
      }
      news = newsService.scheduleNews(scheduledNews, currentIdentity, newsObjectType);
      return ResponseEntity.ok(news);
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not autorized to schedule news", currentIdentity.getUserId(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error when scheduling the news " + scheduledNews.getTitle(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(path = "search", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Search the list of news available with query", method = "GET", description = "Search the list of news available with query")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public ResponseEntity<List<NewsSearchResultEntity>> search(@Parameter(description = "Term to search")
                                                             @RequestParam(name = "query", required = false)
                                                             String query,
                                                             @Parameter(description = "Properties to expand")
                                                             @RequestParam(name = "expand", required = false)
                                                             String expand,
                                                             @Parameter(description = "Offset")
                                                             @RequestParam(name = "offset", defaultValue = "0", required = false)
                                                             int offset,
                                                             @Parameter(description = "Tag names used to search news")
                                                             @RequestParam(name = "tags", required = false)
                                                             List<String> tagNames,
                                                             @Parameter(description = "Limit")
                                                             @RequestParam(name = "limit", defaultValue = "10")
                                                             int limit,
                                                             @Parameter(description = "Favorites")
                                                             @RequestParam(name = "favorites", defaultValue = "false", required = false)
                                                             boolean favorites) {

    if (StringUtils.isBlank(query) && !favorites && CollectionUtils.isEmpty(tagNames)) {
      return ResponseEntity.badRequest().build();
    }

    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser);

    if (offset < 0) {
      return ResponseEntity.badRequest().build();
    }
    if (limit < 0) {
      return ResponseEntity.badRequest().build();
    }
    NewsFilter filter = new NewsFilter();
    filter.setSearchText(query);
    filter.setFavorites(favorites);
    filter.setLimit(limit);
    filter.setOffset(offset);
    filter.setTagNames(tagNames);
    List<NewsESSearchResult> searchResults = newsService.search(currentIdentity, filter);
    List<NewsSearchResultEntity> results =
                                         searchResults.stream()
                                                      .map(searchResult -> io.meeds.news.utils.EntityBuilder.fromNewsSearchResult(favoriteService,
                                                                                                                                  searchResult,
                                                                                                                                  currentIdentity))
                                                      .collect(Collectors.toList());

    return ResponseEntity.ok(results);
  }

  @GetMapping(path = "canScheduleNews/{spaceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "check if the current user can schedule a news in the given space", method = "GET", description = "This checks if the current user can schedule a news in the given space")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "User ability to schedule a news"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to schedule a news"),
      @ApiResponse(responseCode = "404", description = "Space not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<Boolean> canScheduleNews(@PathVariable("spaceId") String spaceId) {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      if (StringUtils.isBlank(spaceId)) {
        return ResponseEntity.badRequest().build();
      }
      Space space = spaceService.getSpaceById(spaceId);
      if (space == null) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok(newsService.canScheduleNews(space, currentIdentity));
    } catch (Exception e) {
      LOG.error("Error when checking if the authenticated user can schedule a news", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(path = "canPublishNews", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "check if the current user can publish a news to all users", method = "GET", description = "This checks if the current user can publish a news to all users")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "User ability to publish a news is returned"), @ApiResponse(responseCode = "401", description = "User not authorized to publish a news") })
  public ResponseEntity<Boolean> canPublishNews(@RequestParam(name = "spaceId", required = false) String spaceId) {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      if (!StringUtils.isBlank(spaceId)) {
        Space space = spaceService.getSpaceById(spaceId);
        if (space == null) {
          return ResponseEntity.notFound().build();
        }
      }
      return ResponseEntity.ok(NewsUtils.canPublishNews(spaceId, currentIdentity));
    } catch (Exception e) {
      LOG.error("Error when checking if the authenticated user can publish a news to all users", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  private NewsFilter buildFilter(List<String> spaces, String filter, String text, String author, int limit, int offset) {
    NewsFilter newsFilter = new NewsFilter();

    newsFilter.setSpaces(spaces);
    if (StringUtils.isNotEmpty(filter)) {
      FilterType filterType = FilterType.valueOf(filter.toUpperCase());
      switch (filterType) {
      case PINNED: {
        newsFilter.setPublishedNews(true);
        break;
      }

      case MYPOSTED: {
        if (StringUtils.isNotEmpty(author)) {
          newsFilter.setAuthor(author);
        }
        break;
      }
      case DRAFTS: {
        if (StringUtils.isNotEmpty(author)) {
          newsFilter.setAuthor(author);
        }
        newsFilter.setDraftNews(true);
        break;
      }
      case SCHEDULED: {
        if (StringUtils.isNotEmpty(author)) {
          newsFilter.setAuthor(author);
        }
        newsFilter.setScheduledNews(true);
        break;
      }
      }
      newsFilter.setOrder("UPDATED_DATE");
    }
    // Set text to search news with
    if (StringUtils.isNotEmpty(text) && text.indexOf("#") != 0) {
      newsFilter.setSearchText(text);
    }
    newsFilter.setLimit(limit);
    newsFilter.setOffset(offset);

    return newsFilter;
  }
  
  @DeleteMapping(path = "translation/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Delete article version with language", method = "DELETE", description = "This deletes the article version ")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "article version deleted"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "User not authorized to delete the article"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteArticleTranslation(@PathVariable("id")
                                           String id,
                                           @Parameter(description = "article version language")
                                           @RequestParam(name = "lang")
                                           String lang) {
    org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    try {
      if (StringUtils.isBlank(id)) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      // fetch always the original news
      News news = newsService.getNewsById(id, currentIdentity, false, ARTICLE.name());
      if (news == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (!news.isCanDelete()) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
      newsService.deleteVersionsByArticleIdAndLang(id, lang);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not authorized to delete article translation", currentIdentity.getUserId(), e);
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (Exception e) {
      LOG.error("Error when deleting the article translation with id " + id, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  /**
   * Get available translation languages for an article
   *
   * @param articleId the ID of the article
   * @param withDrafts boolean flag to include drafts translation languages
   * @return a list of available translation languages
   */
  @GetMapping(path = "translation/{articleId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(summary = "Get article available translation languages", method = "GET", description = "Get article available translation languages")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Article available translation languages returned"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "User not authorized to get the article available translation languages "),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<List<String>> getAvailableTranslationLanguages(@PathVariable("articleId")
                                                                       String articleId,
                                                                       @RequestParam(name = "withDrafts")
                                                                       boolean withDrafts) {
    try {
      if (StringUtils.isBlank(articleId)) {
        return ResponseEntity.badRequest().build();
      }
      return ResponseEntity.ok(newsService.getArticleLanguages(articleId, withDrafts));
    } catch (Exception e) {
      LOG.error("Error when getting the article available translation languages with id " + articleId, e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
