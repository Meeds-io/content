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
package io.meeds.news.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import io.meeds.notes.model.NotePageProperties;
import org.exoplatform.social.metadata.model.MetadataItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class News {

  private String                          id;

  private String                          targetPageId;

  private String                          title;

  private String                          name;

  /* sanitizedBody with usernames */
  private String                          body;

  /* originalBody with user mentions */
  private String                          originalBody;

  private String                          author;

  private String                          authorDisplayName;

  private String                          authorAvatarUrl;

  private String                          updater;

  private String                          updaterFullName;

  private String                          draftUpdater;

  private String                          draftUpdaterDisplayName;

  private String                          draftUpdaterUserName;

  private Date                            draftUpdateDate;

  private String                          uploadId;

  private Date                            creationDate;

  private Date                            publicationDate;

  private String                          publicationState;

  private Date                            updateDate;

  private Date                            publishDate;

  private boolean                         published;

  private String                          audience;

  private String                          spaceId;

  private String                          spaceDisplayName;

  private String                          spaceUrl;

  private boolean                         isSpaceMember;

  private String                          path;

  private Long                            viewsCount;

  private int                             commentsCount;

  private int                             likesCount;

  private String                          activities;

  private String                          activityId;

  private List<String>                    attachmentsIds;

  private String                          spaceAvatarUrl;

  private boolean                         canEdit;

  private boolean                         canDelete;

  private boolean                         canPublish;

  private List<String>                    sharedInSpacesList;

  private String                          url;

  private boolean                         hiddenSpace;

  private String                          schedulePostDate;

  private String                          timeZoneId;

  private boolean                         activityPosted;

  private Map<String, List<MetadataItem>> metadatas;

  private List<String>                    targets;

  private boolean                         favorite;

  private boolean                         deleted;

  private String                          lang;

  private String                          illustrationURL;

  private String                          latestVersionId;

  private NotePageProperties              properties;
}
