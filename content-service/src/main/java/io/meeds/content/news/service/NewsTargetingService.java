/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.content.news.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.common.ObjectAlreadyExistsException;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.content.news.model.ArticleTarget;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.NewsTargetObject;
import io.meeds.content.news.rest.model.NewsTargetingEntity;
import io.meeds.content.news.rest.model.NewsTargetingPermissionsEntity;
import io.meeds.content.news.utils.EntityBuilder;
import io.meeds.content.news.utils.NewsUtils;

import static io.meeds.content.news.utils.NewsUtils.TARGET_PERMISSIONS;

@SuppressWarnings("removal")
@Service
public class NewsTargetingService {

  public static final MetadataType METADATA_TYPE                  = new MetadataType(4, "newsTarget");

  private static final Log         LOG                            = ExoLogger.getLogger(NewsTargetingService.class);

  private static final String      SPACE_TARGET_PERMISSION_PREFIX = "space:";

  private static final String      PUBLISHER_MEMBERSHIP_NAME      = "publisher";

  @Autowired
  private MetadataService          metadataService;

  @Autowired
  private IdentityManager          identityManager;

  @Autowired
  private SpaceService             spaceService;

  @Autowired
  private OrganizationService      organizationService;

  /**
   * @return {@link List} of all news targets
   */
  public List<NewsTargetingEntity> getAllTargets() {
    List<Metadata> targets = metadataService.getMetadatas(METADATA_TYPE.getName(), 0);
    return targets.stream().filter(targetMetadata -> targetMetadata.getProperties() != null).map(this::toEntity).toList();
  }

  /**
   * Gets the {@link List} of allowed targets for a given currentIdentity
   * 
   * @param userIdentity user {@link Identity} for which targets are allowed
   * @return {@link List} of allowed targets
   */
  public List<NewsTargetingEntity> getAllowedTargets(org.exoplatform.services.security.Identity userIdentity) {
    List<Metadata> allTargetsMetadatas = metadataService.getMetadatas(METADATA_TYPE.getName(), 0);
    return allTargetsMetadatas.stream()
                              .filter(targetMetadata -> targetMetadata.getProperties() != null
                                  && targetMetadata.getProperties().get(TARGET_PERMISSIONS) != null
                                  && List.of(targetMetadata.getProperties().get(TARGET_PERMISSIONS).split(","))
                                         .stream()
                                         .anyMatch(targetMetadataPermission -> {
                                           if (targetMetadataPermission.contains(SPACE_TARGET_PERMISSION_PREFIX)) {
                                             if (targetMetadataPermission.split(SPACE_TARGET_PERMISSION_PREFIX).length > 1) {
                                               Space targetPermissionSpace =
                                                                           spaceService.getSpaceById(targetMetadataPermission.split(SPACE_TARGET_PERMISSION_PREFIX)[1]);
                                               return targetPermissionSpace != null
                                                   && NewsUtils.canPublishNews(targetPermissionSpace.getId(), userIdentity);
                                             }
                                             return false;
                                           }
                                           try {
                                             Group targetPermissionGroup =
                                                                         organizationService.getGroupHandler()
                                                                                            .findGroupById(targetMetadataPermission);
                                             return targetPermissionGroup != null
                                                 && userIdentity.isMemberOf(targetMetadataPermission, PUBLISHER_MEMBERSHIP_NAME);
                                           } catch (Exception e) {
                                             LOG.error("Could not find group from permission " + targetMetadataPermission);
                                             return false;
                                           }
                                         }))
                              .map(this::toEntity)
                              .toList();
  }

  /**
   * Delete the {@link News} target by a given {@link News} target name
   * 
   * @param targetName {@link News} target name to be deleted
   * @param currentIdentity {@link Identity} technical identifier
   * @throws IllegalAccessException when not authorized to delete news targets
   */
  public void deleteTargetByName(String targetName, org.exoplatform.services.security.Identity currentIdentity) throws IllegalAccessException {
    if (!NewsUtils.canManageNewsPublishTargets(currentIdentity)) {
      throw new IllegalAccessException(String.format("User %s not authorized to delete news target with name %s",
                                                       currentIdentity.getUserId(),
                                                       targetName));
    }
    MetadataKey targetMetadataKey = new MetadataKey(METADATA_TYPE.getName(), targetName, 0);
    Metadata targetMetadata = metadataService.getMetadataByKey(targetMetadataKey);
    metadataService.deleteMetadataById(targetMetadata.getId());
  }

  /**
   * Gets the {@link List} of {@link News} targets linked to a given
   * {@link News}
   * 
   * @param news {@link News} for which targets to be retrieved
   * @return {@link List} of {@link News} targets by {@link News} news object
   */
  public List<ArticleTarget> getTargetsByNews(News news) {
    NewsTargetObject newsTargetObject = new NewsTargetObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE,
                                                             news.getId(),
                                                             null,
                                                             Long.parseLong(news.getSpaceId()));
    List<MetadataItem> newsTargets = metadataService.getMetadataItemsByMetadataTypeAndObject(METADATA_TYPE.getName(),
                                                                                             newsTargetObject);
    return EntityBuilder.toArticleTargets(newsTargets);
  }

  /**
   * Delete list of associated article targets
   *
   * @param article target article
   * @param targets list of associated targets
   */
  public void deleteNewsTargets(News article, final Set<String> targets) {
    final NewsTargetObject newsTargetObject = new NewsTargetObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE,
                                                                   article.getId(),
                                                                   null,
                                                                   Long.parseLong(article.getSpaceId()));

    final List<MetadataItem> newsTargets = metadataService.getMetadataItemsByMetadataTypeAndObject(METADATA_TYPE.getName(),
                                                                                                   newsTargetObject);
    newsTargets.stream()
               .filter(target -> targets.contains(target.getMetadata().getName()))
               .forEach(target -> {
                 try {
                   metadataService.deleteMetadataItem(target.getId(), false);
                 } catch (ObjectNotFoundException e) {
                   LOG.error("Failed to delete news target. ID: " + target.getId() + ", " +
                       "Name: " + target.getMetadata().getName(), e);
                 }
               });
  }

  /**
   * Save a {@link List} of {@link News} targets of a given {@link News} id by
   * the current user
   *
   * @param news {@link News} for which targets to be saved
   * @param displayed {@link News} is news displayed in news list portlet
   * @param targets {@link List} of {@link News} targets to be saved
   * @param currentUserId current user attempting to save {@link News} targets
   * @throws IllegalAccessException when user doesn't have access to save
   *           {@link News} targets of a given {@link News} id
   */
  public void saveNewsTarget(News news,
                             boolean displayed,
                             List<String> targets,
                             String currentUserId) throws IllegalAccessException {
    org.exoplatform.services.security.Identity currentIdentity = NewsUtils.getUserIdentity(currentUserId);
    if (!NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity)) {
      throw new IllegalAccessException(String.format("User %s not authorized to save news targets",
                                                     currentUserId));
    }
    NewsTargetObject newsTargetObject = new NewsTargetObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE,
                                                             news.getId(),
                                                             null,
                                                             Long.parseLong(news.getSpaceId()));
    Identity currentSocIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUserId);
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put(NewsUtils.DISPLAYED_STATUS, String.valueOf(displayed));
    properties.put(NewsUtils.PUBLISHED_DATE, String.valueOf(new Date().getTime()));
    targets.forEach(targetName -> {
      try {
        MetadataKey metadataKey = new MetadataKey(NewsTargetingService.METADATA_TYPE.getName(), targetName, 0);
        metadataService.createMetadataItem(newsTargetObject,
                                           metadataKey,
                                           properties,
                                           Long.parseLong(currentSocIdentity.getId()),
                                           false);
      } catch (ObjectAlreadyExistsException e) {
        LOG.warn("Targets with name {} is already associated to object {}. Ignore error since it will not affect result.",
                 targetName,
                 newsTargetObject,
                 e);
      }
    });
  }

  /**
   * Gets the {@link List} of {@link News} target items by a given target name.
   *
   * @param targetName target name of metadata to be retrieved
   * @param offset limit
   * @param limit offset
   * @return {@link List} of {@link News} target items by a target name
   */
  public List<MetadataItem> getNewsTargetItemsByTargetName(String targetName, long offset, long limit) {
    return metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndMetadataItemProperty(targetName,
                                                                                                 METADATA_TYPE.getName(),
                                                                                                 NewsUtils.NEWS_METADATA_OBJECT_TYPE,
                                                                                                 NewsUtils.DISPLAYED_STATUS,
                                                                                                 String.valueOf(true),
                                                                                                 offset,
                                                                                                 limit);
  }

  /**
   * Delete the {@link List} of {@link News} targets linked to a given
   * {@link News} id
   * 
   * @param news {@link News} for which targets to be deleted
   */
  public void deleteNewsTargets(News news) {
    NewsTargetObject newsTargetObject = new NewsTargetObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE,
                                                             news.getId(),
                                                             null,
                                                             Long.parseLong(news.getSpaceId()));
    metadataService.deleteMetadataItemsByMetadataTypeAndObject(METADATA_TYPE.getName(), newsTargetObject);
  }

  /**
   * Delete the {@link List} of {@link News} targets linked to a given
   * {@link News} id
   * 
   * @param news {@link News} for which targets to be deleted
   * @param currentUserId attempting to delete {@link News} target
   * @throws IllegalAccessException when user doesn't have access to delete
   *           {@link News} targets of a given {@link News} id
   */
  public void deleteNewsTargets(News news, String currentUserId) throws IllegalAccessException {
    org.exoplatform.services.security.Identity currentIdentity = NewsUtils.getUserIdentity(currentUserId);
    if (!NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity)) {
      throw new IllegalAccessException(String.format("User %s not authorized to delete news targets",
                                                     currentUserId));
    }
    deleteNewsTargets(news);
  }

  /**
   * Create news target
   * 
   * @param newsTargetingEntity {@link News} TargetingEntity
   * @param currentIdentity current {@link Identity} attempting to create
   *          {@link News} target
   * @return created {@link News} target {@link Metadata}
   * @throws IllegalArgumentException when user creates a {@link News} target
   *           that already exists
   * @throws IllegalAccessException when user doesn't have access to create
   *           {@link News} target
   */
  public Metadata createNewsTarget(NewsTargetingEntity newsTargetingEntity,
                                   org.exoplatform.services.security.Identity currentIdentity) throws IllegalArgumentException,
                                                                                               IllegalAccessException {
    return createNewsTarget(newsTargetingEntity, currentIdentity, null);
  }

  /**
   * Create news target
   * 
   * @param newsTargetingEntity {@link News} TargetingEntity
   * @param currentIdentity current {@link Identity} attempting to create
   *          {@link News} target
   * @param spaceId the space identifier used to scope the target creation (optional)
   * @return created {@link News} target {@link Metadata}
   * @throws IllegalArgumentException when user creates a {@link News} target
   *           that already exists
   * @throws IllegalAccessException when user doesn't have access to create
   *           {@link News} target
   */
  public Metadata createNewsTarget(NewsTargetingEntity newsTargetingEntity,
                                   org.exoplatform.services.security.Identity currentIdentity,
                                   Long spaceId) throws IllegalArgumentException, IllegalAccessException {
    if (!NewsUtils.canCreateNewsPublishTargets(currentIdentity, spaceId)) {
      throw new IllegalAccessException(String.format("User %s not authorized to create news targets",
                                                     currentIdentity.getUserId()));
    }
    // Verify that the permission corresponds to the given space
    // when the target is created by a space admin or publisher
    if (spaceId != null && spaceId > 0
        && !matchesSpacePermission(newsTargetingEntity.getProperties().get(TARGET_PERMISSIONS), spaceId)) {
      throw new IllegalStateException(String.format("Invalid target permission for the given space with Id: %s", spaceId));
    }
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentIdentity.getUserId());
    long userIdentityId = identity == null ? 0 : Long.parseLong(identity.getId());
    Metadata metadata = fromEntity(newsTargetingEntity);
    metadata.setCreatorId(userIdentityId);

    MetadataKey targetMetadataKey = new MetadataKey(METADATA_TYPE.getName(), metadata.getName(), 0);
    Metadata storedMetadata = metadataService.getMetadataByKey(targetMetadataKey);
    if (storedMetadata != null) {
      throw new IllegalArgumentException(String.format("User %s not authorized to create news target with same name %s",
                                                       currentIdentity.getUserId(),
                                                       metadata.getName()));
    }
    return metadataService.createMetadata(metadata, userIdentityId);
  }

  /**
   * Update news target
   * 
   * @param originalTargetName identifier of the {@link News} target
   * @param newsTargetingEntity {@link News} TargetingEntity to be updated
   * @param currentIdentity current {@link Identity} attempting to update
   *          {@link News} target
   * @return updated {@link News} target {@link Metadata}
   * @throws IllegalAccessException when user doesn't have access to update
   *           {@link News} target
   * @throws IllegalStateException when user tries to update a not existing
   *           {@link News} target
   * @throws IllegalArgumentException when user tries to update a not changed
   *           {@link News} target
   */
  public Metadata updateNewsTargets(String originalTargetName,
                                    NewsTargetingEntity newsTargetingEntity,
                                    org.exoplatform.services.security.Identity currentIdentity) throws IllegalAccessException,
                                                                                                IllegalStateException,
                                                                                                IllegalArgumentException {
    if (!NewsUtils.canManageNewsPublishTargets(currentIdentity)) {
      throw new IllegalAccessException(String.format("User %s not authorized to get news targets",
                                                     currentIdentity.getUserId()));
    }
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentIdentity.getUserId());
    long userIdentityId = identity == null ? 0 : Long.parseLong(identity.getId());
    MetadataKey targetMetadataKey = new MetadataKey(METADATA_TYPE.getName(), originalTargetName, 0);
    Metadata storedMetadata = metadataService.getMetadataByKey(targetMetadataKey);
    if (storedMetadata == null) {
      throw new IllegalStateException(String.format("User %s can not get news target with this name %s",
                                                    currentIdentity.getUserId(),
                                                    originalTargetName));
    }
    boolean isSameName = StringUtils.equals(newsTargetingEntity.getName(), originalTargetName);
    if (!isSameName) {
      storedMetadata.setName(newsTargetingEntity.getName());
    }
    boolean isSameDescription = newsTargetingEntity.getProperties()
                                                   .entrySet()
                                                   .stream()
                                                   .allMatch(e -> e.getValue()
                                                                   .equals(storedMetadata.getProperties().get(e.getKey())));
    if (!isSameDescription) {
      storedMetadata.setProperties(newsTargetingEntity.getProperties());
    }
    if (isSameName && isSameDescription) {
      throw new IllegalArgumentException(String.format("User %s don't make any changes",
                                                       currentIdentity.getUserId()));
    }
    return metadataService.updateMetadata(storedMetadata, userIdentityId);
  }

  /**
   * Get target by its name
   * 
   * @param targetName target name
   * @return {@link NewsTargetingEntity}
   */
  public NewsTargetingEntity getTargetByName(String targetName) {
    MetadataKey targetMetadataKey = new MetadataKey(METADATA_TYPE.getName(), targetName, 0);
    Metadata targetMetadata = metadataService.getMetadataByKey(targetMetadataKey);
    return toEntity(targetMetadata);
  }

  private NewsTargetingEntity toEntity(Metadata metadata) { // NOSONAR
    NewsTargetingEntity newsTargetingEntity = new NewsTargetingEntity();
    newsTargetingEntity.setName(metadata.getName());
    newsTargetingEntity.setProperties(metadata.getProperties());
    if (newsTargetingEntity.getProperties().get(TARGET_PERMISSIONS) != null) {
      org.exoplatform.services.security.Identity currentIdentity = NewsUtils.getUserIdentity(RestUtils.getCurrentUser());
      boolean isSpacePublisher = false;
      boolean isGroupPublisher = false;
      String permissions = newsTargetingEntity.getProperties().get(TARGET_PERMISSIONS);
      List<String> permissionsList = List.of(permissions.split(","));
      List<NewsTargetingPermissionsEntity> permissionsEntities = new ArrayList<>();
      for (String permission : permissionsList) {
        NewsTargetingPermissionsEntity permissionEntity = new NewsTargetingPermissionsEntity();
        if (permission.contains(SPACE_TARGET_PERMISSION_PREFIX)) {
          if (permission.split(SPACE_TARGET_PERMISSION_PREFIX).length > 1) {
            Space space = spaceService.getSpaceById(permission.split(SPACE_TARGET_PERMISSION_PREFIX)[1]);
            if (space != null) {
              permissionEntity.setId(SPACE_TARGET_PERMISSION_PREFIX + space.getId());
              permissionEntity.setName(space.getDisplayName());
              permissionEntity.setProviderId("space");
              permissionEntity.setRemoteId(space.getPrettyName());
              permissionEntity.setAvatar(space.getAvatarUrl());
              if (!isSpacePublisher) {
                isSpacePublisher = NewsUtils.canPublishNews(space.getId(), currentIdentity);
              }
            }
          }
        } else {
          try {
            Group group = organizationService.getGroupHandler().findGroupById(permission);
            if (group != null) {
              permissionEntity.setId(group.getId());
              permissionEntity.setName(group.getLabel());
              permissionEntity.setProviderId("group");
              permissionEntity.setRemoteId(group.getGroupName());
              if (!isGroupPublisher) {
                isGroupPublisher = currentIdentity.isMemberOf(group.getId(), PUBLISHER_MEMBERSHIP_NAME);
              }
            }
          } catch (Exception e) {
            LOG.error("Could not find group from permission" + permission);
          }
        }
        if (permissionEntity.getId() != null) {
          permissionsEntities.add(permissionEntity);
        }
      }
      newsTargetingEntity.setPermissions(permissionsEntities);
      newsTargetingEntity.setRestrictedAudience(isSpacePublisher && !isGroupPublisher);
    }
    return newsTargetingEntity;
  }

  private Metadata fromEntity(NewsTargetingEntity newsTargetingEntity) {
    Metadata metadata = new Metadata();
    metadata.setName(newsTargetingEntity.getName());
    metadata.setAudienceId(0);
    metadata.setType(METADATA_TYPE);
    metadata.setProperties(newsTargetingEntity.getProperties());
    metadata.setCreatorId(0);
    return metadata;
  }

  private boolean matchesSpacePermission(String permission, Long spaceId) {
    return String.format("%s%s", SPACE_TARGET_PERMISSION_PREFIX, spaceId).equals(permission);
  }

}
