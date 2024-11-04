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
package io.meeds.news.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.meeds.news.model.News;
import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.wiki.model.DraftPage;

public class NewsUtils {

  private static final Log   LOG                             = ExoLogger.getLogger(NewsUtils.class);

  public static final String POST_NEWS                       = "exo.news.postArticle";

  public static final String POST_NEWS_ARTICLE               = "exo.news.gamification.postArticle";

  public static final String PUBLISH_NEWS                    = "exo.news.gamification.PublishArticle";

  public static final String VIEW_NEWS                       = "exo.news.viewArticle";

  public static final String SHARE_NEWS                      = "exo.news.shareArticle";

  public static final String COMMENT_NEWS                    = "exo.news.commentArticle";

  public static final String LIKE_NEWS                       = "exo.news.likeArticle";

  public static final String DELETE_NEWS                     = "exo.news.deleteArticle";

  public static final String UPDATE_NEWS                     = "exo.news.updateArticle";

  public static final String SCHEDULE_NEWS                   = "exo.news.scheduleArticle";

  public static final String UNSCHEDULE_NEWS                 = "exo.news.unscheduleArticle";

  public static final String NEWS_METADATA_OBJECT_TYPE       = "news";

  public static final String DISPLAYED_STATUS                = "displayed";

  public static final String TARGET_PERMISSIONS              = "permissions";

  public static final String SPACE_NEWS_AUDIENCE             = "space";

  public static final String ALL_NEWS_AUDIENCE               = "all";

  public static final String PUBLISHER_MEMBERSHIP_NAME       = "publisher";

  public static final String MANAGER_MEMBERSHIP_NAME         = "manager";

  public static final String PLATFORM_WEB_CONTRIBUTORS_GROUP = "/platform/web-contributors";

  public static final String ADD_ARTICLE_TRANSLATION         = "content.add.article.translation";

  public static final String REMOVE_ARTICLE_TRANSLATION      = "content.remove.article.translation";

  public static final String UPDATE_CONTENT_PERMISSIONS      = "content.update.permissions";

  public enum NewsObjectType {
    DRAFT, LATEST_DRAFT, ARTICLE;
  }

  public enum NewsUpdateType {
    CONTENT_AND_TITLE, SCHEDULE, POSTING_AND_PUBLISHING
  }

  public static void broadcastEvent(String eventName, Object source, Object data) {
    try {
      ListenerService listenerService = CommonsUtils.getService(ListenerService.class);
      listenerService.broadcast(eventName, source, data);
    } catch (Exception e) {
      LOG.warn("Error broadcasting event '" + eventName + "' using source '" + source + "' and data " + data, e);
    }
  }

  /**
   * Processes Mentioners who has been mentioned via the news body.
   *
   * @param content : the content in which update mention
   * @param space : the space of the news (for group mentioning)
   * @return set of mentioned users
   */
  public static Set<String> processMentions(String content, Space space) {
    Set<String> mentions = new HashSet<>();
    mentions.addAll(MentionUtils.getMentionedUsernames(content));

    if (space != null) {
      IdentityStorage identityStorage = CommonsUtils.getService(IdentityStorage.class);
      String spaceIdentityId = identityStorage.findIdentityId(SpaceIdentityProvider.NAME, space.getPrettyName());
      Set<String> mentionedRoles = MentionUtils.getMentionedRoles(content, spaceIdentityId);
      mentionedRoles.forEach(role -> {
        if (StringUtils.equals("member", role) && space.getMembers() != null) {
          mentions.addAll(Arrays.asList(space.getMembers()));
        } else if (StringUtils.equals("manager", role) && space.getManagers() != null) {
          mentions.addAll(Arrays.asList(space.getManagers()));
        } else if (StringUtils.equals("redactor", role) && space.getRedactors() != null) {
          mentions.addAll(Arrays.asList(space.getRedactors()));
        } else if (StringUtils.equals("publisher", role) && space.getPublishers() != null) {
          mentions.addAll(Arrays.asList(space.getPublishers()));
        }
      });
    }

    return mentions.stream().map(remoteId -> {
      IdentityStorage identityStorage = CommonsUtils.getService(IdentityStorage.class);

      Identity identity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, remoteId);
      return identity == null ? null : identity.getId();
    }).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  public static List<Long> getMyFilteredSpacesIds(org.exoplatform.services.security.Identity userIdentity,
                                                  List<String> filteredSpacesIds) throws Exception {
    if (!CollectionUtils.isEmpty(filteredSpacesIds)) {
      return filteredSpacesIds.stream().map(Long::parseLong).toList();
    }
    return getMySpaces(userIdentity).stream().map(space -> Long.valueOf(space.getId())).toList();
  }

  public static List<Long> getAllowedDraftArticleSpaceIds(org.exoplatform.services.security.Identity userIdentity,
                                                          List<String> filteredSpacesIds) throws Exception {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    return getMySpaces(userIdentity).stream().filter(space -> {
      boolean allowed = spaceService.canRedactOnSpace(space, userIdentity) || canPublishNews(space.getId(), userIdentity);
      if (!CollectionUtils.isEmpty(filteredSpacesIds)) {
        return allowed && filteredSpacesIds.contains(space.getId());
      }
      return allowed;
    }).map(space -> Long.valueOf(space.getId())).toList();
  }

  public static List<Long> getAllowedScheduledNewsSpacesIds(org.exoplatform.services.security.Identity currentIdentity,
                                                            List<String> filteredSpacesIds) throws Exception {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    return getMySpaces(currentIdentity).stream().filter(space -> {
      boolean allowed = (spaceService.isManager(space, currentIdentity.getUserId())
          || spaceService.isRedactor(space, currentIdentity.getUserId()) || canPublishNews(space.getId(), currentIdentity));
      if (!CollectionUtils.isEmpty(filteredSpacesIds)) {
        return allowed && filteredSpacesIds.contains(space.getId());
      }
      return allowed;
    }).map(space -> Long.valueOf(space.getId())).toList();
  }

  public static boolean canPublishNews(String spaceId, org.exoplatform.services.security.Identity currentIdentity) {
    if (!StringUtils.isBlank(spaceId)) {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      Space space = spaceService.getSpaceById(spaceId);
      return currentIdentity != null && space != null
          && (currentIdentity.isMemberOf(PLATFORM_WEB_CONTRIBUTORS_GROUP, PUBLISHER_MEMBERSHIP_NAME)
              || spaceService.isPublisher(space, currentIdentity.getUserId())
              || spaceService.isManager(space, currentIdentity.getUserId())
              || spaceService.isSuperManager(currentIdentity.getUserId()));
    }
    return currentIdentity != null && currentIdentity.isMemberOf(PLATFORM_WEB_CONTRIBUTORS_GROUP, PUBLISHER_MEMBERSHIP_NAME);
  }

  public static boolean canManageNewsPublishTargets(org.exoplatform.services.security.Identity currentIdentity) {
    return currentIdentity != null && currentIdentity.isMemberOf(PLATFORM_WEB_CONTRIBUTORS_GROUP, MANAGER_MEMBERSHIP_NAME);
  }

  public static org.exoplatform.services.security.Identity getUserIdentity(String username) {
    IdentityRegistry identityRegistry = ExoContainerContext.getService(IdentityRegistry.class);
    org.exoplatform.services.security.Identity identity = identityRegistry.getIdentity(username);
    if (identity != null) {
      return identity;
    }
    Authenticator authenticator = ExoContainerContext.getService(Authenticator.class);
    try {
      identity = authenticator.createIdentity(username);
      if (identity != null) {
        // To cache identity for next times
        identityRegistry.register(identity);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error occurred while retrieving security identity of user " + username);
    }
    return identity;
  }

  public static String buildDraftUrl(DraftPage draftPage) {
    StringBuilder draftArticleUrl = new StringBuilder();
    draftArticleUrl.append("/")
                   .append(PortalContainer.getCurrentPortalContainerName())
                   .append("/")
                   .append(CommonsUtils.getCurrentPortalOwner())
                   .append("/news-detail?newsId=")
                   .append(draftPage.getId())
                   .append(draftPage.getTargetPageId() != null ? "&type=latest_draft" : "&type=draft");
    return draftArticleUrl.toString();
  }

  public static String buildNewsArticleUrl(News news, String currentUsername) throws SpaceException {
    StringBuilder newsArticleUrl = new StringBuilder();
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (currentUsername != null && spaceService.isMember(news.getSpaceId(), currentUsername) && news.getActivityId() != null) {
      newsArticleUrl.append("/")
                    .append(PortalContainer.getCurrentPortalContainerName())
                    .append("/")
                    .append(CommonsUtils.getCurrentPortalOwner())
                    .append("/activity?id=")
                    .append(news.getActivityId());
    } else {
      newsArticleUrl.append("/")
                    .append(PortalContainer.getCurrentPortalContainerName())
                    .append("/")
                    .append(CommonsUtils.getCurrentPortalOwner())
                    .append("/news-detail?newsId=")
                    .append(news.getId())
                    .append("&type=article");
    }
    if (news.getLang() != null) {
      newsArticleUrl.append("&lang=");
      newsArticleUrl.append(news.getLang());
    }
    return newsArticleUrl.toString();
  }

  public static String buildSpaceUrl(String spaceId) {
    return String.format("/portal/s/%s", spaceId);
  }

  public static String buildIllustrationUrl(NotePageProperties properties, String lang) {
    if (properties == null || properties.getFeaturedImage() == null || properties.getFeaturedImage().getId() == null
        || properties.getFeaturedImage().getId() == 0L) {
      return null;
    }
    NoteFeaturedImage featuredImage = properties.getFeaturedImage();
    StringBuilder illustrationUrl = new StringBuilder();
    illustrationUrl.append("/portal/rest/notes/illustration/");
    illustrationUrl.append(properties.getNoteId());
    illustrationUrl.append("?v=");
    illustrationUrl.append(featuredImage.getLastUpdated());
    illustrationUrl.append("&isDraft=");
    illustrationUrl.append(properties.isDraft());
    if (lang != null) {
      illustrationUrl.append("&lang=");
      illustrationUrl.append(lang);
    }
    return illustrationUrl.toString();
  }

  private static List<Space> getMySpaces(org.exoplatform.services.security.Identity userIdentity) throws Exception {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    ListAccess<Space> memberSpacesListAccess = spaceService.getMemberSpaces(userIdentity.getUserId());
    return Arrays.asList(memberSpacesListAccess.load(0, memberSpacesListAccess.getSize()));
  }

}
