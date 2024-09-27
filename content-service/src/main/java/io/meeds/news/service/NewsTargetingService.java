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
package io.meeds.news.service;

import java.util.List;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataType;

import io.meeds.news.model.News;
import io.meeds.news.rest.NewsTargetingEntity;
import org.springframework.stereotype.Service;

@Service
public interface NewsTargetingService {

  public static final MetadataType METADATA_TYPE = new MetadataType(4, "newsTarget");

  /**
   * Gets the {@link List} of all targets
   *
   * @return {@link List} of all targets
   */
  List<NewsTargetingEntity> getAllTargets();

  /**
   * Gets the {@link List} of allowed targets for a given currentIdentity
   * 
   * @param userIdentity user {@link Identity} for which targets are allowed
   * @return {@link List} of allowed targets
   */
  List<NewsTargetingEntity> getAllowedTargets(org.exoplatform.services.security.Identity userIdentity);

  /**
   * Delete the {@link News} target by a given {@link News} target name
   * 
   * @param targetName {@link News} target name to be deleted
   * @param currentIdentity {@link Identity} technical identifier
   */
  void deleteTargetByName(String targetName,
                          org.exoplatform.services.security.Identity currentIdentity) throws IllegalAccessException;

  /**
   * Gets the {@link List} of {@link News} targets linked to a given
   * {@link News}
   * 
   * @param news {@link News} for which targets to be retrieved
   * @return {@link List} of {@link News} targets by {@link News} news object
   */
  List<String> getTargetsByNews(News news);

  /**
   * Gets the {@link List} of {@link News} target items by a given target name.
   *
   * @param targetName target name of metadata to be retrieved
   * @param offset limit
   * @param limit offset
   * @return {@link List} of {@link News} target items by a target name
   */
  List<MetadataItem> getNewsTargetItemsByTargetName(String targetName, long offset, long limit);

  /**
   * Save a {@link List} of {@link News} targets of a given {@link News} id by
   * the current user
   *
   * @param news {@link News} for which targets to be saved
   * @param displayed {@link News} is news displayed in news list portlet
   * @param targets {@link List} of {@link News} targets to be saved
   * @param currentUser current user attempting to save {@link News} targets
   * @throws IllegalAccessException when user doesn't have access to save
   *           {@link News} targets of a given {@link News} id
   */
  void saveNewsTarget(News news, boolean displayed, List<String> targets, String currentUser) throws IllegalAccessException;

  /**
   * Delete the {@link List} of {@link News} targets linked to a given
   * {@link News} id
   * 
   * @param news {@link News} for which targets to be deleted
   * @param currentUserId attempting to delete {@link News} target
   * @throws IllegalAccessException when user doesn't have access to delete
   *           {@link News} targets of a given {@link News} id
   */
  void deleteNewsTargets(News news, String currentUserId) throws IllegalAccessException;

  /**
   * Delete the {@link List} of {@link News} targets linked to a given
   * {@link News} id
   * 
   * @param news {@link News} for which targets to be deleted
   */
  void deleteNewsTargets(News news);

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
  Metadata createNewsTarget(NewsTargetingEntity newsTargetingEntity,
                            org.exoplatform.services.security.Identity currentIdentity) throws IllegalArgumentException,
                                                                                        IllegalAccessException;

  /**
   * Create news target
   * 
   * @param newsTargetingEntity {@link News} TargetingEntity
   * @param currentIdentity current {@link Identity} attempting to create
   *          {@link News} target
   * @param checkPermissions true if permissions are checked
   * @return created {@link News} target {@link Metadata}
   * @throws IllegalArgumentException when user creates a {@link News} target
   *           that already exists
   * @throws IllegalAccessException when user doesn't have access to create
   *           {@link News} target
   */
  Metadata createNewsTarget(NewsTargetingEntity newsTargetingEntity,
                            org.exoplatform.services.security.Identity currentIdentity,
                            boolean checkPermissions) throws IllegalArgumentException, IllegalAccessException;

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
  Metadata updateNewsTargets(String originalTargetName,
                             NewsTargetingEntity newsTargetingEntity,
                             org.exoplatform.services.security.Identity currentIdentity) throws IllegalAccessException,
                                                                                         IllegalStateException,
                                                                                         IllegalArgumentException;
}
