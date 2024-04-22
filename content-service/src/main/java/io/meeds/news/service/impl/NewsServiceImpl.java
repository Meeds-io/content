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
package io.meeds.news.service.impl;

import static io.meeds.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static io.meeds.news.utils.NewsUtils.NewsObjectType.LATEST_DRAFT;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

import io.meeds.news.filter.NewsFilter;
import io.meeds.news.model.News;
import io.meeds.news.model.NewsDraftObject;
import io.meeds.news.model.NewsLatestDraftObject;
import io.meeds.news.model.NewsPageObject;
import io.meeds.news.model.NewsPageVersionObject;
import io.meeds.news.notification.plugin.MentionInNewsNotificationPlugin;
import io.meeds.news.notification.plugin.PostNewsNotificationPlugin;
import io.meeds.news.notification.plugin.PublishNewsNotificationPlugin;
import io.meeds.news.notification.utils.NotificationConstants;
import io.meeds.news.notification.utils.NotificationUtils;
import io.meeds.news.search.NewsESSearchResult;
import io.meeds.news.search.NewsIndexingServiceConnector;
import io.meeds.news.service.NewsService;
import io.meeds.news.service.NewsTargetingService;
import io.meeds.news.utils.NewsUtils;
import io.meeds.news.utils.NewsUtils.NewsObjectType;

public class NewsServiceImpl implements NewsService {

  public static final String         NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME      = "Articles";

  public static final MetadataType   NEWS_METADATA_TYPE                     = new MetadataType(1000, "news");

  public static final String         NEWS_METADATA_NAME                     = "news";

  public static final String         NEWS_METADATA_DRAFT_OBJECT_TYPE        = "newsDraftPage";

  public static final String         NEWS_FILE_API_NAME_SPACE               = "news";

  public static final String         NEWS_SUMMARY                           = "summary";

  public static final String         NEWS_ILLUSTRATION_ID                   = "illustrationId";

  public static final String         NEWS_UPLOAD_ID                         = "uploadId";

  /** The Constant PUBLISHED. */
  public final static String         PUBLISHED                              = "published";

  /** The Constant DRAFT. */
  public final static String         DRAFT                                  = "draft";

  /** The Constant STAGED. */
  public final static String         STAGED                                 = "staged";

  /** The Constant AUDIENCE. */
  public static final String         NEWS_AUDIENCE                          = "audience";

  /** The Constant NEWS_ID. */
  public static final String         NEWS_ID                                = "newsId";

  /** The Constant SCHEDULE_POST_DATE. */
  public static final String         SCHEDULE_POST_DATE                     = "schedulePostDate";

  /** The Constant NEWS_ACTIVITIES. */
  public static final String         NEWS_ACTIVITIES                        = "activities";

  /** The Constant NEWS_PUBLICATION_STATE. */
  public static final String         NEWS_PUBLICATION_STATE                 = "publicationState";

  /** The Constant NEWS_ACTIVITY_POSTED. */
  public static final String         NEWS_ACTIVITY_POSTED                   = "activityPosted";

  /** The Constant NEWS_PUBLISH_DATE. */
  public static final String         NEWS_PUBLISH_DATE                      = "publishDate";

  /** The Constant NEWS_METADATA_PAGE_OBJECT_TYPE. */
  public static final String         NEWS_METADATA_PAGE_OBJECT_TYPE         = "newsPage";

  /** The Constant NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE. */
  public static final String         NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE = "newsPageVersion";

  /** The Constant NEWS_VIEWERS. */
  public static final String         NEWS_VIEWERS                           = "viewers";

  /** The Constant NEWS_VIEWS. */
  public static final String         NEWS_VIEWS                             = "viewsCount";

  /** The Constant NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE. */
  public static final String         NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE = "newsLatestDraftPage";

  public static final MetadataKey    NEWS_METADATA_KEY                      =
                                                       new MetadataKey(NEWS_METADATA_TYPE.getName(), NEWS_METADATA_NAME, 0);

  private static final Log           LOG                                    = ExoLogger.getLogger(NewsServiceImpl.class);

  private final SpaceService         spaceService;

  private final NoteService          noteService;

  private final MetadataService      metadataService;

  private final FileService          fileService;

  private final UploadService        uploadService;

  private final NewsTargetingService newsTargetingService;

  private final IndexingService      indexingService;

  private final IdentityManager      identityManager;

  private final UserACL              userACL;

  private final ActivityManager      activityManager;

  private final WikiService          wikiService;

  public NewsServiceImpl(SpaceService spaceService,
                         NoteService noteService,
                         MetadataService metadataService,
                         FileService fileService,
                         NewsTargetingService newsTargetingService,
                         IndexingService indexingService,
                         IdentityManager identityManager,
                         UserACL userACL,
                         ActivityManager activityManager,
                         WikiService wikiService,
                         UploadService uploadService) {
    this.spaceService = spaceService;
    this.noteService = noteService;
    this.metadataService = metadataService;
    this.fileService = fileService;
    this.uploadService = uploadService;
    this.newsTargetingService = newsTargetingService;
    this.indexingService = indexingService;
    this.identityManager = identityManager;
    this.userACL = userACL;
    this.activityManager = activityManager;
    this.wikiService = wikiService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News createNews(News news, Identity currentIdentity) throws Exception {
    Space space = spaceService.getSpaceById(news.getSpaceId());
    try {
      if (!canCreateNews(space, currentIdentity)) {
        throw new IllegalAccessException("User " + currentIdentity.getUserId() + " not authorized to create news");
      }
      News createdNews;
      if (PUBLISHED.equals(news.getPublicationState())) {
        createdNews = postNews(news, currentIdentity.getUserId());
      } else if (news.getSchedulePostDate() != null) {
        createdNews = unScheduleNews(news, currentIdentity);
      } else {
        createdNews = createDraftArticleForNewPage(news, space.getGroupId(), currentIdentity.getUserId());
      }
      return createdNews;
    } catch (Exception e) {
      LOG.error("Error when creating the news " + news.getTitle(), e);
      return null;
    }
  }

  @Override
  public News postNews(News news, String poster) throws Exception {
    news = createNewsArticlePage(news, poster);
    postNewsActivity(news);
    sendNotification(poster, news, NotificationConstants.NOTIFICATION_CONTEXT.POST_NEWS);
    if (news.isPublished()) {
      publishNews(news, poster);
    }
    NewsUtils.broadcastEvent(NewsUtils.POST_NEWS_ARTICLE, news.getId(), news);// Gamification
    NewsUtils.broadcastEvent(NewsUtils.POST_NEWS, news.getAuthor(), news);// Analytics
    return news;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canCreateNews(Space space, Identity currentIdentity) throws Exception {
    return space != null
        && (NewsUtils.canPublishNews(space.getId(), currentIdentity) || spaceService.canRedactOnSpace(space, currentIdentity));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News updateNews(News news, String updater, Boolean post, boolean publish) throws Exception {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News updateNews(News news, String updater, Boolean post, boolean publish, String newsObjectType) throws Exception {

    if (!canEditNews(news, updater)) {
      throw new IllegalAccessException("User " + updater + " is not authorized to update news");
    }
    Identity updaterIdentity = NewsUtils.getUserIdentity(updater);
    News originalNews = getNewsById(news.getId(), updaterIdentity, false, newsObjectType);
    List<String> oldTargets = newsTargetingService.getTargetsByNewsId(news.getId());
    boolean canPublish = NewsUtils.canPublishNews(news.getSpaceId(), updaterIdentity);
    Set<String> previousMentions = NewsUtils.processMentions(originalNews.getOriginalBody(),
                                                             spaceService.getSpaceById(news.getSpaceId()));
    if (NewsObjectType.DRAFT.name().toLowerCase().equals(newsObjectType)) {
      return updateDraftArticleForNewPage(news, updater);
    } else if (LATEST_DRAFT.name().toLowerCase().equals(newsObjectType)) {
      return createOrUpdateDraftForExistingPage(news, updater);
    } else if (ARTICLE.name().toLowerCase().equals(newsObjectType)) {
      news = updateNewsArticle(news, updaterIdentity);
    }
    if (publish != news.isPublished() && news.isCanPublish()) {
      news.setPublished(publish);
      if (news.isPublished()) {
        publishNews(news, updater);
      } else {
        unpublishNews(news.getId(), updater);
      }
    }
    boolean displayed = !(StringUtils.equals(news.getPublicationState(), STAGED) || news.isArchived());
    if (publish == news.isPublished() && news.isPublished() && canPublish) {
      if (news.getTargets() != null && (oldTargets == null || !oldTargets.equals(news.getTargets()))) {
        newsTargetingService.deleteNewsTargets(news, updater);
        newsTargetingService.saveNewsTarget(news, displayed, news.getTargets(), updater);
      }
      if (news.getAudience() != null && news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE)
          && originalNews.getAudience() != null && originalNews.getAudience().equals(NewsUtils.SPACE_NEWS_AUDIENCE)) {
        sendNotification(updater, news, NotificationConstants.NOTIFICATION_CONTEXT.PUBLISH_NEWS);
      }
    }

    if (PUBLISHED.equals(news.getPublicationState())) {
      // Send mention notifs
      if (StringUtils.isNotEmpty(news.getId()) && news.getCreationDate() != null) {
        News newMentionedNews = news;
        if (!previousMentions.isEmpty()) {
          // clear old mentions from news body before sending a custom object to
          // notification context.
          previousMentions.forEach(username -> newMentionedNews.setBody(newMentionedNews.getBody()
                                                                                        .replaceAll("@" + username, "")));
        }
        sendNotification(updater, newMentionedNews, NotificationConstants.NOTIFICATION_CONTEXT.MENTION_IN_NEWS);
      }
      indexingService.reindex(NewsIndexingServiceConnector.TYPE, String.valueOf(news.getId()));
    }
    if (!news.getPublicationState().isEmpty() && !DRAFT.equals(news.getPublicationState())) {
      if (post != null) {
        updateNewsActivity(news, post);
      }
      NewsUtils.broadcastEvent(NewsUtils.UPDATE_NEWS, updater, news);
    }
    return news;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteNews(String newsId, Identity currentIdentity, boolean isDraft) throws Exception {
    News news = getNewsById(newsId,
                            currentIdentity,
                            false,
                            isDraft ? NewsObjectType.DRAFT.name().toLowerCase() : NewsObjectType.ARTICLE.name().toLowerCase());
    if (!news.isCanDelete()) {
      throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to delete news");
    }
    if (isDraft) {
      deleteDraftArticle(newsId, currentIdentity.getUserId(), true);
    } else {
      deleteArticle(news, currentIdentity);
      MetadataObject newsMetadataObject = new MetadataObject(NewsUtils.NEWS_METADATA_OBJECT_TYPE, newsId);
      metadataService.deleteMetadataItemsByObject(newsMetadataObject);
      indexingService.unindex(NewsIndexingServiceConnector.TYPE, String.valueOf(news.getId()));
      NewsUtils.broadcastEvent(NewsUtils.DELETE_NEWS, currentIdentity.getUserId(), news);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void publishNews(News newsToPublish, String publisher) throws Exception {
    Identity publisherIdentity = NewsUtils.getUserIdentity(publisher);
    News news = getNewsArticleById(newsToPublish.getId());
    boolean displayed = !(StringUtils.equals(news.getPublicationState(), STAGED) || news.isArchived());

    // update page metadata
    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE, news.getId(), null);
    MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject).get(0);
    if (metadataItem != null) {
      Map<String, String> properties = metadataItem.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
      }
      properties.put(PUBLISHED, String.valueOf(true));
      Calendar updateCalendar = Calendar.getInstance();
      Date newsPublishDate = updateCalendar.getTime();
      properties.put(NEWS_PUBLISH_DATE, String.valueOf(newsPublishDate));
      metadataItem.setProperties(properties);
      String publisherId = identityManager.getOrCreateUserIdentity(publisherIdentity.getUserId()).getId();
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(publisherId));
    }
    if (newsToPublish.getTargets() != null) {
      newsTargetingService.deleteNewsTargets(news, publisher);
      newsTargetingService.saveNewsTarget(news, displayed, newsToPublish.getTargets(), publisher);
    }
    NewsUtils.broadcastEvent(NewsUtils.PUBLISH_NEWS, news.getId(), news);
    try {
      news.setAudience(newsToPublish.getAudience());
      sendNotification(publisher, news, NotificationConstants.NOTIFICATION_CONTEXT.PUBLISH_NEWS);
    } catch (Error | Exception e) {
      LOG.warn("Error sending notification when publishing news with Id " + news.getId(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unpublishNews(String newsId, String publisher) throws Exception {
    News news = getNewsArticleById(newsId);
    newsTargetingService.deleteNewsTargets(news, publisher);

    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE, news.getId(), null);
    MetadataItem newsMetadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                                   .stream()
                                                   .findFirst()
                                                   .orElse(null);

    if (newsMetadataItem != null) {
      Map<String, String> properties = newsMetadataItem.getProperties();
      if (properties != null) {
        properties.put(PUBLISHED, String.valueOf(false));
        properties.remove(NEWS_PUBLISH_DATE);
        properties.remove(NEWS_AUDIENCE);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News getNewsById(String newsId, boolean editMode) throws Exception {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News getNewsById(String newsId, Identity currentIdentity, boolean editMode) throws IllegalAccessException {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News getNewsById(String newsId,
                          Identity currentIdentity,
                          boolean editMode,
                          String newsObjectType) throws IllegalAccessException {
    News news = null;
    try {
      if (newsObjectType == null) {
        throw new IllegalArgumentException("Required argument news object type could not be null");
      }
      if (NewsObjectType.DRAFT.name().toLowerCase().equals(newsObjectType)) {
        news = buildDraftArticle(newsId, currentIdentity.getUserId());
      } else if (LATEST_DRAFT.name().toLowerCase().equals(newsObjectType)) {
        news = buildLatestDraftArticle(newsId, currentIdentity.getUserId());
      } else if (ARTICLE.name().toLowerCase().equals(newsObjectType)) {
        news = buildArticle(newsId);
      }
    } catch (Exception exception) {
      LOG.error("An error occurred while retrieving news with id {}", newsId, exception);
    }
    if (news != null) {
      if (editMode) {
        if (!canEditNews(news, currentIdentity.getUserId())) {
          throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to edit News");
        }
      } else if (!canViewNews(news, currentIdentity.getUserId())) {
        throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to view News");
      }
      news.setCanEdit(canEditNews(news, currentIdentity.getUserId()));
      news.setCanDelete(canDeleteNews(currentIdentity, news.getAuthor(), news.getSpaceId()));
      news.setCanPublish(NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity));
      news.setCanArchive(canArchiveNews(currentIdentity, news.getAuthor()));
      news.setTargets(newsTargetingService.getTargetsByNewsId(newsId));
      ExoSocialActivity activity = null;
      try {
        activity = activityManager.getActivity(news.getActivityId());
      } catch (Exception e) {
        LOG.debug("Error getting activity of News with id {}", news.getActivityId(), e);
      }
      if (activity != null) {
        RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getCommentsWithListAccess(activity, true);
        news.setCommentsCount(listAccess.getSize());
        news.setLikesCount(activity.getLikeIdentityIds() == null ? 0 : activity.getLikeIdentityIds().length);
      }
    }
    return news;
  }

  @Override
  public News getNewsArticleById(String newsId) {
    News news = null;
    try {
      news = buildArticle(newsId);
      news.setTargets(newsTargetingService.getTargetsByNewsId(newsId));
    } catch (Exception exception) {
      LOG.error("An error occurred while retrieving news with id {}", newsId, exception);
    }
    return news;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<News> getNews(NewsFilter filter, Identity currentIdentity) throws Exception {
    List<News> newsList = new ArrayList<>();
    if (filter != null) {
      if (filter.isArchivedNews()) {
        // TODO
      }
      if (filter.getSearchText() != null && !filter.getSearchText().equals("")) {
        // TODO
      } else {
        // TODO
      }
      if (filter.isPublishedNews()) {
        // TODO
      }

      List<String> spaces = filter.getSpaces();
      if (spaces != null && spaces.size() != 0) {
        // TODO
      }
      if (filter.isDraftNews()) {
        newsList = buildDraftArticles(filter, currentIdentity);
      } else if (filter.isScheduledNews()) {
        // TODO
      } else {
        // TODO
      }
    } else {
      throw new Exception("Unable to build query, filter is null");
    }
    newsList.stream().forEach(news -> {
      news.setCanEdit(canEditNews(news, currentIdentity.getUserId()));
      news.setCanDelete(canDeleteNews(currentIdentity, news.getAuthor(), news.getSpaceId()));
      news.setCanPublish(NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity));
      news.setCanArchive(canArchiveNews(currentIdentity, news.getAuthor()));
    });
    return newsList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<News> getNewsByTargetName(NewsFilter newsFilter, String targetName, Identity currentIdentity) throws Exception {
    List<MetadataItem> newsTargetItems =
                                       newsTargetingService.getNewsTargetItemsByTargetName(targetName, newsFilter.getOffset(), 0);
    return newsTargetItems.stream().filter(target -> {
      try {
        News news = getNewsById(target.getObjectId(), currentIdentity, false, ARTICLE.name().toLowerCase());
        return news != null
            && (news.getAudience().equals("") || news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE) || news.isSpaceMember());
      } catch (Exception e) {
        return false;
      }
    }).map(target -> {
      try {
        News news = getNewsById(target.getObjectId(), currentIdentity, false, ARTICLE.name().toLowerCase());
        news.setPublishDate(new Date(target.getCreatedDate()));
        news.setIllustration(null);
        return news;
      } catch (Exception e) {
        return null;
      }
    }).limit(newsFilter.getLimit()).toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNewsCount(NewsFilter filter) throws Exception {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void markAsRead(News news, String userId) throws Exception {
    try {
      MetadataItem metadataItem =
                                metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                    new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                                                                       news.getId(),
                                                                                                       null))
                                               .get(0);
      if (metadataItem != null) {
        Map<String, String> properties = metadataItem.getProperties();
        if (properties == null) {
          properties = new HashMap<>();
        }
        if (properties.containsKey(NEWS_VIEWERS) && StringUtils.isNotEmpty(properties.get(NEWS_VIEWERS))) {
          String newsViewers = properties.get(NEWS_VIEWERS);
          String[] newsViewersArray = newsViewers.split(",");
          boolean isUserInNewsViewers = Arrays.stream(newsViewersArray).anyMatch(userId::equals);
          if (isUserInNewsViewers) {
            return;
          }
          newsViewers.concat("," + userId);
          properties.put(NEWS_VIEWERS, newsViewers);
          if (properties.containsKey(NEWS_VIEWS) && StringUtils.isNotEmpty(properties.get(NEWS_VIEWS))) {
            Long newsViewsCount = Long.parseLong(properties.get(NEWS_VIEWS)) + 1L;
            properties.put(NEWS_VIEWS, String.valueOf(newsViewsCount));
          } else {
            properties.put(NEWS_VIEWS, "1");
          }
        } else {
          properties.put(NEWS_VIEWERS, userId);
          properties.put(NEWS_VIEWS, "1");
        }
        metadataItem.setProperties(properties);
        String userIdentityId = identityManager.getOrCreateUserIdentity(userId).getId();
        metadataService.updateMetadataItem(metadataItem, Long.parseLong(userIdentityId));
      }
    } catch (Exception exception) {
      LOG.error("Failed to mark news article " + news.getId() + " as read for current user", exception);
      return;
    }
    NewsUtils.broadcastEvent(NewsUtils.VIEW_NEWS, userId, news);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<News> searchNews(NewsFilter filter, String lang) throws Exception {
    return new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News getNewsByActivityId(String activityId, Identity currentIdentity) throws IllegalAccessException,
                                                                               ObjectNotFoundException {
    ExoSocialActivity activity = activityManager.getActivity(activityId);
    if (activity == null) {
      throw new ObjectNotFoundException("Activity with id " + activityId + " wasn't found");
    }
    Identity viewerIdentity = NewsUtils.getUserIdentity(currentIdentity.getUserId());
    if (!activityManager.isActivityViewable(activity, viewerIdentity)) {
      throw new IllegalAccessException("User " + currentIdentity.getUserId() + " isn't allowed to access activity with id "
          + activityId);
    }
    Map<String, String> templateParams = activity.getTemplateParams();
    if (templateParams == null) {
      throw new ObjectNotFoundException("Activity with id " + activityId + " isn't of type news nor a shared news");
    }
    String newsId = templateParams.get(NEWS_ID);
    if (StringUtils.isBlank(newsId)) {
      String originalActivityId = templateParams.get("originalActivityId");
      if (StringUtils.isNotBlank(originalActivityId)) {
        org.exoplatform.social.core.identity.model.Identity sharedActivityPosterIdentity =
                                                                                         identityManager.getIdentity(activity.getPosterId());
        if (sharedActivityPosterIdentity == null) {
          throw new IllegalAccessException("Shared Activity '" + activityId + "' Poster " + activity.getPosterId()
              + " isn't found");
        }
        return getNewsByActivityId(originalActivityId, NewsUtils.getUserIdentity(sharedActivityPosterIdentity.getRemoteId()));
      }
      throw new ObjectNotFoundException("Activity with id " + activityId + " isn't of type news nor a shared news");
    }
    return getNewsById(newsId, currentIdentity, false, ARTICLE.name().toLowerCase());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News scheduleNews(News news, Identity currentIdentity) throws Exception {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News unScheduleNews(News news, Identity currentIdentity) throws Exception {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<NewsESSearchResult> search(org.exoplatform.social.core.identity.model.Identity currentIdentity, NewsFilter filter) {
    return new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canScheduleNews(Space space, Identity currentIdentity) {
    return spaceService.isManager(space, currentIdentity.getUserId())
        || spaceService.isRedactor(space, currentIdentity.getUserId())
        || NewsUtils.canPublishNews(space.getId(), currentIdentity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canViewNews(News news, String authenticatedUser) {
    try {
      String spaceId = news.getSpaceId();
      Space space = spaceId == null ? null : spaceService.getSpaceById(spaceId);
      if (space == null) {
        LOG.warn("Can't find space with id {} when checking access on news with id {}", spaceId, news.getId());
        return false;
      }
      if (!news.isPublished() && StringUtils.equals(news.getPublicationState(), PUBLISHED)
          && !(spaceService.isSuperManager(authenticatedUser) || spaceService.isMember(space, authenticatedUser)
              || isMemberOfsharedInSpaces(news, authenticatedUser))) {
        return false;
      }
      if (news.isPublished() && news.getAudience().equals(NewsUtils.SPACE_NEWS_AUDIENCE)
          && !spaceService.isMember(space, authenticatedUser)) {
        return false;
      }
      if (StringUtils.equals(news.getPublicationState(), STAGED)
          && !canScheduleNews(space, NewsUtils.getUserIdentity(authenticatedUser))) {
        return false;
      }
    } catch (Exception e) {
      LOG.warn("Error retrieving access permission for user {} on news with id {}", authenticatedUser, news.getId());
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void shareNews(News news,
                        Space space,
                        org.exoplatform.social.core.identity.model.Identity userIdentity,
                        String sharedActivityId) throws Exception {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void archiveNews(String newsId, String currentUserName) throws Exception {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unarchiveNews(String newsId, String currentUserName) throws Exception {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canArchiveNews(Identity currentIdentity, String newsAuthor) {
    return false;
  }

  private News createDraftArticleForNewPage(News draftArticle, String pageOwnerId, String draftArticleCreator) throws Exception {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(WikiType.GROUP.name().toLowerCase(), pageOwnerId);
    Page newsArticlesRootNotePage = null;
    if (wiki != null) {
      newsArticlesRootNotePage = noteService.getNoteOfNoteBookByName(WikiType.GROUP.name().toLowerCase(),
                                                                     pageOwnerId,
                                                                     NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
      // create the news root page if the wiki exist
      if (newsArticlesRootNotePage == null) {
        newsArticlesRootNotePage = createNewsArticlesNoteRootPage(wiki);
      }
    } else {
      // create the wiki
      pageOwnerId = formatWikiOwnerToGroupId(pageOwnerId);
      wiki = wikiService.createWiki(WikiType.GROUP.name().toLowerCase(), pageOwnerId);
      // create the news root page
      newsArticlesRootNotePage = createNewsArticlesNoteRootPage(wiki);
    }
    if (newsArticlesRootNotePage != null) {
      DraftPage draftArticlePage = new DraftPage();
      draftArticlePage.setNewPage(true);
      draftArticlePage.setTargetPageId(null);
      draftArticlePage.setTitle(draftArticle.getTitle());
      draftArticlePage.setContent(draftArticle.getBody());
      draftArticlePage.setParentPageId(newsArticlesRootNotePage.getId());
      draftArticlePage.setAuthor(draftArticle.getAuthor());
      draftArticlePage = noteService.createDraftForNewPage(draftArticlePage, System.currentTimeMillis());

      draftArticle.setId(draftArticlePage.getId());
      draftArticle.setCreationDate(draftArticlePage.getCreatedDate());
      draftArticle.setUpdateDate(draftArticlePage.getUpdatedDate());
      Space draftArticleSpace = spaceService.getSpaceByGroupId(pageOwnerId);
      NewsDraftObject draftArticleMetaDataObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                       draftArticlePage.getId(),
                                                                       null,
                                                                       Long.parseLong(draftArticleSpace.getId()));
      String draftArticleMetadataItemCreatorIdentityId = identityManager.getOrCreateUserIdentity(draftArticleCreator).getId();
      Map<String, String> draftArticleMetadataItemProperties = new HashMap<>();
      // save illustration
      if (StringUtils.isNotEmpty(draftArticle.getUploadId())) {
        Long draftArticleIllustrationId = saveArticleIllustration(draftArticle.getUploadId(), null);
        setArticleIllustration(draftArticle, draftArticleIllustrationId, NewsObjectType.DRAFT.name());
        draftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(draftArticleIllustrationId));
        draftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, draftArticle.getUploadId());
      }
      if (StringUtils.isNotEmpty(draftArticle.getSummary())) {
        draftArticleMetadataItemProperties.put(NEWS_SUMMARY, draftArticle.getSummary());
      }
      metadataService.createMetadataItem(draftArticleMetaDataObject,
                                         NEWS_METADATA_KEY,
                                         draftArticleMetadataItemProperties,
                                         Long.parseLong(draftArticleMetadataItemCreatorIdentityId));

      return draftArticle;
    }
    return null;
  }

  private News updateDraftArticleForNewPage(News draftArticle, String draftArticleUpdater) throws WikiException,
                                                                                           IllegalAccessException,
                                                                                           FileStorageException {
    DraftPage draftArticlePage = noteService.getDraftNoteById(draftArticle.getId(), draftArticleUpdater);
    if (draftArticlePage != null) {
      draftArticlePage.setTitle(draftArticle.getTitle());
      draftArticlePage.setContent(draftArticle.getBody());
      // created and updated date set by default during the draft creation
      // process
      draftArticlePage = noteService.updateDraftForNewPage(draftArticlePage, System.currentTimeMillis());
      Space draftArticleSpace = spaceService.getSpaceByGroupId(draftArticlePage.getWikiOwner());
      NewsDraftObject draftArticleMetaDataObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                       draftArticlePage.getId(),
                                                                       null,
                                                                       Long.parseLong(draftArticleSpace.getId()));
      List<MetadataItem> draftArticleMetadataItems =
                                                   metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                       draftArticleMetaDataObject);
      if (draftArticleMetadataItems != null && !draftArticleMetadataItems.isEmpty()) {
        MetadataItem draftArticleMetadataItem = draftArticleMetadataItems.get(0);
        Map<String, String> draftArticleMetadataItemProperties = draftArticleMetadataItem.getProperties();
        if (draftArticleMetadataItemProperties == null) {
          draftArticleMetadataItemProperties = new HashMap<>();
        }
        // create or update the illustration
        if (StringUtils.isNotEmpty(draftArticle.getUploadId())) {
          if (draftArticleMetadataItemProperties.containsKey(NEWS_UPLOAD_ID)
              && draftArticleMetadataItemProperties.get(NEWS_UPLOAD_ID) != null
              && draftArticleMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
              && draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null) {
            if (!draftArticleMetadataItemProperties.get(NEWS_UPLOAD_ID).equals(draftArticle.getUploadId())) {
              FileItem draftArticleIllustrationFileItem =
                                                        fileService.getFile(Long.parseLong(draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)));
              Long draftArticleIllustrationId = saveArticleIllustration(draftArticle.getUploadId(),
                                                                        draftArticleIllustrationFileItem.getFileInfo().getId());
              draftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(draftArticleIllustrationId));
              setArticleIllustration(draftArticle, draftArticleIllustrationId, NewsObjectType.DRAFT.name());
            }
          } else {
            Long draftArticleIllustrationId = saveArticleIllustration(draftArticle.getUploadId(), null);
            draftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(draftArticleIllustrationId));
            draftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, draftArticle.getUploadId());
            setArticleIllustration(draftArticle, draftArticleIllustrationId, NewsObjectType.DRAFT.name());
          }
          draftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, draftArticle.getUploadId());
        } else {
          if (draftArticleMetadataItemProperties.containsKey(NEWS_UPLOAD_ID)
              && draftArticleMetadataItemProperties.get(NEWS_UPLOAD_ID) != null
              && draftArticleMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
              && draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null && draftArticle.getUploadId() != null) {
            draftArticleMetadataItemProperties.remove(NEWS_UPLOAD_ID);
            FileItem draftArticleIllustrationFileItem =
                                                      fileService.getFile(Long.parseLong(draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)));
            draftArticleMetadataItemProperties.remove(NEWS_ILLUSTRATION_ID);

            fileService.deleteFile(draftArticleIllustrationFileItem.getFileInfo().getId());
          }
        }
        if (StringUtils.isNotEmpty(draftArticle.getSummary())) {
          draftArticleMetadataItemProperties.put(NEWS_SUMMARY, draftArticle.getSummary());
        }
        draftArticleMetadataItem.setProperties(draftArticleMetadataItemProperties);
        String draftArticleMetadataItemUpdaterIdentityId = identityManager.getOrCreateUserIdentity(draftArticleUpdater).getId();
        metadataService.updateMetadataItem(draftArticleMetadataItem, Long.parseLong(draftArticleMetadataItemUpdaterIdentityId));
      }
      return draftArticle;
    }
    return null;
  }

  private News buildDraftArticle(String draftArticleId, String currentUserId) throws WikiException, IllegalAccessException {
    DraftPage draftArticlePage = noteService.getDraftNoteById(draftArticleId, currentUserId);
    if (draftArticlePage != null) {
      News draftArticle = new News();
      draftArticle.setId(draftArticlePage.getId());
      draftArticle.setTitle(draftArticlePage.getTitle());
      draftArticle.setAuthor(draftArticlePage.getAuthor());
      draftArticle.setCreationDate(draftArticlePage.getCreatedDate());
      draftArticle.setUpdateDate(draftArticlePage.getUpdatedDate());
      draftArticle.setDraftUpdateDate(draftArticlePage.getUpdatedDate());
      draftArticle.setDraftUpdaterUserName(draftArticlePage.getAuthor());
      org.exoplatform.social.core.identity.model.Identity draftUpdaterIdentity =
                                                                               identityManager.getOrCreateUserIdentity(currentUserId);
      if (draftUpdaterIdentity != null && draftUpdaterIdentity.getProfile() != null) {
        draftArticle.setDraftUpdaterDisplayName(draftUpdaterIdentity.getProfile().getFullName());
      }
      draftArticle.setBody(draftArticlePage.getContent());
      draftArticle.setPublicationState(DRAFT);
      Space draftArticleSpace = spaceService.getSpaceByGroupId(draftArticlePage.getWikiOwner());
      draftArticle.setSpaceId(draftArticleSpace.getId());
      draftArticle.setSpaceAvatarUrl(draftArticleSpace.getAvatarUrl());
      draftArticle.setSpaceDisplayName(draftArticleSpace.getDisplayName());
      boolean hiddenSpace = draftArticleSpace.getVisibility().equals(Space.HIDDEN)
          && !spaceService.isMember(draftArticleSpace, currentUserId) && !spaceService.isSuperManager(currentUserId);
      draftArticle.setHiddenSpace(hiddenSpace);
      boolean isSpaceMember =
                            spaceService.isSuperManager(currentUserId) || spaceService.isMember(draftArticleSpace, currentUserId);
      draftArticle.setSpaceMember(isSpaceMember);
      if (StringUtils.isNotEmpty(draftArticleSpace.getGroupId())) {
        String spaceGroupId = draftArticleSpace.getGroupId().split("/")[2];
        String spaceUrl = "/portal/g/:spaces:" + spaceGroupId + "/" + draftArticleSpace.getPrettyName();
        draftArticle.setSpaceUrl(spaceUrl);
      }
      StringBuilder draftArticleUrl = new StringBuilder("");
      draftArticleUrl.append("/")
                     .append(PortalContainer.getCurrentPortalContainerName())
                     .append("/")
                     .append(CommonsUtils.getCurrentPortalOwner())
                     .append("/news/detail?newsId=")
                     .append(draftArticle.getId())
                     .append(draftArticlePage.getTargetPageId() != null ? "&type=latest_draft" : "&type=draft");
      draftArticle.setUrl(draftArticleUrl.toString());

      MetadataObject draftArticleMetaDataObject = null;
      if (draftArticlePage.getTargetPageId() == null) {
        draftArticleMetaDataObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                         draftArticle.getId(),
                                                         null,
                                                         Long.parseLong(draftArticleSpace.getId()));
      } else {
        draftArticleMetaDataObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                               draftArticle.getId(),
                                                               draftArticlePage.getTargetPageId());
      }
      List<MetadataItem> draftArticleMetadataItems =
                                                   metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                       draftArticleMetaDataObject);
      if (draftArticleMetadataItems != null && !draftArticleMetadataItems.isEmpty()) {
        Map<String, String> draftArticleMetadataItemProperties = draftArticleMetadataItems.get(0).getProperties();
        if (draftArticleMetadataItemProperties != null && !draftArticleMetadataItemProperties.isEmpty()) {
          if (draftArticleMetadataItemProperties.containsKey(NEWS_SUMMARY)) {
            draftArticle.setSummary(draftArticleMetadataItemProperties.get(NEWS_SUMMARY));
          }
          if (draftArticleMetadataItemProperties.containsKey(NEWS_ACTIVITY_POSTED)) {
            draftArticle.setActivityPosted(Boolean.parseBoolean(draftArticleMetadataItemProperties.get(NEWS_ACTIVITY_POSTED)));
          } else {
            draftArticle.setActivityPosted(false);
          }
          if (draftArticleMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
              && draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null) {
            setArticleIllustration(draftArticle,
                                   Long.valueOf(draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)),
                                   NewsObjectType.DRAFT.name().toLowerCase());
          }
        }
      }
      return draftArticle;
    }
    return null;
  }

  private List<News> buildDraftArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    List<Long> allowedDraftNewsSpacesIds = NewsUtils.getAllowedDraftNewsSpaces(currentIdentity)
                                                    .stream()
                                                    .map(Space::getId)
                                                    .map(spaceId -> Long.parseLong(spaceId))
                                                    .toList();
    List<News> draftArticles =
                             metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndSpaceIds(NEWS_METADATA_NAME,
                                                                                                       NEWS_METADATA_TYPE.getName(),
                                                                                                       NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                                                       allowedDraftNewsSpacesIds,
                                                                                                       filter.getOffset(),
                                                                                                       filter.getLimit())
                                            .stream()
                                            .map(draftArticle -> {
                                              try {
                                                return buildDraftArticle(draftArticle.getObjectId(), currentIdentity.getUserId());
                                              } catch (IllegalAccessException | WikiException e) {
                                                // TODO Auto-generated catch
                                                // block
                                                e.printStackTrace();
                                                return null;
                                              }
                                            })
                                            .toList();
    return draftArticles;
  }

  private void deleteDraftArticle(String draftArticleId,
                                  String draftArticleCreator,
                                  boolean deleteIllustration) throws Exception {
    DraftPage draftArticlePage = noteService.getDraftNoteById(draftArticleId, draftArticleCreator);
    if (draftArticlePage != null) {
      noteService.removeDraftById(draftArticlePage.getId());
      Space draftArticleSpace = spaceService.getSpaceByGroupId(draftArticlePage.getWikiOwner());
      NewsDraftObject draftArticleMetaDataObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                       draftArticlePage.getId(),
                                                                       null,
                                                                       Long.parseLong(draftArticleSpace.getId()));
      List<MetadataItem> draftArticleMetadataItems =
                                                   metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                       draftArticleMetaDataObject);
      if (draftArticleMetadataItems != null && !draftArticleMetadataItems.isEmpty()) {
        Map<String, String> draftArticleMetadataItemProperties = draftArticleMetadataItems.get(0).getProperties();
        if (deleteIllustration) {
          if (draftArticleMetadataItemProperties != null && draftArticleMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
              && draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null) {
            FileItem draftArticleIllustrationFileItem =
                                                      fileService.getFile(Long.parseLong(draftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)));
            fileService.deleteFile(draftArticleIllustrationFileItem.getFileInfo().getId());
          }
        }
        metadataService.deleteMetadataItem(draftArticleMetadataItems.get(0).getId(), false);
      }
    }
  }

  private boolean canEditNews(News news, String authenticatedUser) {
    String spaceId = news.getSpaceId();
    Space space = spaceId == null ? null : spaceService.getSpaceById(spaceId);
    if (space == null) {
      return false;
    }
    Identity authenticatedUserIdentity = NewsUtils.getUserIdentity(authenticatedUser);
    if (authenticatedUserIdentity == null) {
      LOG.warn("Can't find user with id {} when checking access on news with id {}", authenticatedUser, news.getId());
      return false;
    }
    return NewsUtils.canPublishNews(news.getSpaceId(), authenticatedUserIdentity)
        || spaceService.canRedactOnSpace(space, authenticatedUserIdentity);
  }

  private void setArticleIllustration(News article, Long articleIllustrationId, String newsObjectType) {
    try {
      FileItem articleIllustrationFileItem = fileService.getFile(articleIllustrationId);
      if (articleIllustrationFileItem != null) {
        article.setIllustration(articleIllustrationFileItem.getAsByte());
        article.setIllustrationMimeType(articleIllustrationFileItem.getFileInfo().getMimetype());
        article.setIllustrationUpdateDate(articleIllustrationFileItem.getFileInfo().getUpdatedDate());
        long lastModified = article.getIllustrationUpdateDate().getTime();
        article.setIllustrationURL("/portal/rest/v1/news/" + article.getId() + "/illustration?v=" + lastModified + "&type="
            + newsObjectType);
      }
    } catch (Exception exception) {
      LOG.info("Failed to set article illustration");
    }
  }

  private Long saveArticleIllustration(String articleUploadId, Long oldArticleIllustrationFileId) {
    if (StringUtils.isEmpty(articleUploadId)) {
      throw new IllegalArgumentException("Article uploadId is mandatory");
    }
    if (oldArticleIllustrationFileId != null && oldArticleIllustrationFileId != 0) {
      fileService.deleteFile(oldArticleIllustrationFileId);
    }
    UploadResource articleUploadResource = uploadService.getUploadResource(articleUploadId);
    if (articleUploadResource == null) {
      throw new IllegalStateException("Can't find article uploaded resource with id : " + articleUploadId);
    }
    try {
      InputStream articleIllustrationFileInputStream = new FileInputStream(articleUploadResource.getStoreLocation());
      FileItem articleIllustrationFileItem = new FileItem(null,
                                                          articleUploadResource.getFileName(),
                                                          articleUploadResource.getMimeType(),
                                                          NEWS_FILE_API_NAME_SPACE,
                                                          (long) articleUploadResource.getUploadedSize(),
                                                          new Date(),
                                                          IdentityConstants.SYSTEM,
                                                          false,
                                                          articleIllustrationFileInputStream);
      articleIllustrationFileItem = fileService.writeFile(articleIllustrationFileItem);
      return articleIllustrationFileItem != null
          && articleIllustrationFileItem.getFileInfo() != null ? articleIllustrationFileItem.getFileInfo().getId() : null;
    } catch (Exception e) {
      throw new IllegalStateException("Error while saving article illustration file", e);
    } finally {
      uploadService.removeUploadResource(articleUploadResource.getUploadId());
    }
  }

  private boolean canDeleteNews(Identity currentIdentity, String posterId, String spaceId) {
    if (currentIdentity == null) {
      return false;
    }
    String authenticatedUser = currentIdentity.getUserId();
    Space currentSpace = spaceService.getSpaceById(spaceId);
    return authenticatedUser.equals(posterId) || userACL.isSuperUser() || spaceService.isSuperManager(authenticatedUser)
        || spaceService.isManager(currentSpace, authenticatedUser);
  }

  private boolean isMemberOfsharedInSpaces(News news, String username) {
    for (String sharedInSpaceId : news.getSharedInSpacesList()) {
      Space sharedInSpace = spaceService.getSpaceById(sharedInSpaceId);
      if (sharedInSpace != null && spaceService.isMember(sharedInSpace, username)) {
        return true;
      }
    }
    return false;
  }

  private Page createNewsArticlesNoteRootPage(Wiki wiki) throws WikiException {
    if (wiki != null) {
      Page newsArticlesRootNotePage = new Page();
      newsArticlesRootNotePage.setWikiType(wiki.getType());
      newsArticlesRootNotePage.setWikiOwner(wiki.getOwner());
      newsArticlesRootNotePage.setName(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
      newsArticlesRootNotePage.setTitle(NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);
      Date now = Calendar.getInstance().getTime();
      newsArticlesRootNotePage.setCreatedDate(now);
      newsArticlesRootNotePage.setUpdatedDate(now);
      newsArticlesRootNotePage.setContent("");
      // inherit syntax from wiki
      newsArticlesRootNotePage.setSyntax(wiki.getPreferences().getWikiPreferencesSyntax().getDefaultSyntax());
      return noteService.createNote(wiki, null, newsArticlesRootNotePage);
    }
    return null;
  }

  private String formatWikiOwnerToGroupId(String wikiOwner) {
    if (wikiOwner == null || wikiOwner.length() == 0) {
      return null;
    }
    if (!wikiOwner.startsWith("/")) {
      wikiOwner = "/" + wikiOwner;
    }
    if (wikiOwner.endsWith("/")) {
      wikiOwner = wikiOwner.substring(0, wikiOwner.length() - 1);
    }
    return wikiOwner;
  }

  private void sendNotification(String currentUserId,
                                News news,
                                NotificationConstants.NOTIFICATION_CONTEXT context) throws Exception {
    String newsId = news.getId();
    String contentAuthor = news.getAuthor();
    String currentUser = currentUserId != null ? currentUserId : contentAuthor;
    String activities = news.getActivities();
    String contentTitle = news.getTitle();
    String contentBody = news.getBody();
    String lastSpaceIdActivityId = activities.split(";")[activities.split(";").length - 1];
    String contentSpaceId = lastSpaceIdActivityId.split(":")[0];
    String contentActivityId = lastSpaceIdActivityId.split(":")[1];
    Space contentSpace = spaceService.getSpaceById(contentSpaceId);
    boolean isMember = spaceService.isMember(contentSpace, contentAuthor);
    if (contentSpace == null) {
      throw new NullPointerException("Cannot find a space with id " + contentSpaceId + ", it may not exist");
    }
    org.exoplatform.social.core.identity.model.Identity identity = identityManager.getOrCreateUserIdentity(contentAuthor);
    String authorAvatarUrl = LinkProviderUtils.getUserAvatarUrl(identity.getProfile());
    String activityLink = NotificationUtils.getNotificationActivityLink(contentSpace, contentActivityId, isMember);
    String contentSpaceName = contentSpace.getDisplayName();

    // Send Notification
    NotificationContext ctx = NotificationContextImpl.cloneInstance()
                                                     .append(PostNewsNotificationPlugin.CONTEXT, context)
                                                     .append(PostNewsNotificationPlugin.CONTENT_TITLE, contentTitle)
                                                     .append(PostNewsNotificationPlugin.CONTENT_AUTHOR, contentAuthor)
                                                     .append(PostNewsNotificationPlugin.CURRENT_USER, currentUser)
                                                     .append(PostNewsNotificationPlugin.CONTENT_SPACE_ID, contentSpaceId)
                                                     .append(PostNewsNotificationPlugin.CONTENT_SPACE, contentSpaceName)
                                                     .append(PostNewsNotificationPlugin.AUTHOR_AVATAR_URL, authorAvatarUrl)
                                                     .append(PostNewsNotificationPlugin.ACTIVITY_LINK, activityLink)
                                                     .append(PostNewsNotificationPlugin.NEWS_ID, newsId);

    if (context.equals(NotificationConstants.NOTIFICATION_CONTEXT.POST_NEWS)) {
      ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(PostNewsNotificationPlugin.ID))).execute(ctx);
      Matcher matcher = MentionInNewsNotificationPlugin.MENTION_PATTERN.matcher(contentBody);
      if (matcher.find()) {
        sendMentionInNewsNotification(newsId,
                                      contentAuthor,
                                      currentUser,
                                      contentTitle,
                                      contentBody,
                                      contentSpaceId,
                                      authorAvatarUrl,
                                      activityLink,
                                      contentSpaceName);
      }
    } else if (context.equals(NotificationConstants.NOTIFICATION_CONTEXT.MENTION_IN_NEWS)) {
      sendMentionInNewsNotification(newsId,
                                    contentAuthor,
                                    currentUser,
                                    contentTitle,
                                    contentBody,
                                    contentSpaceId,
                                    authorAvatarUrl,
                                    activityLink,
                                    contentSpaceName);
    } else if (context.equals(NotificationConstants.NOTIFICATION_CONTEXT.PUBLISH_NEWS)) {
      if (news.getAudience() != null) {
        News originalNews = getNewsArticleById(news.getId());
        if (news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE) && originalNews.getAudience() != null
            && originalNews.getAudience().equals(NewsUtils.SPACE_NEWS_AUDIENCE)) {
          ctx.append(PostNewsNotificationPlugin.AUDIENCE, "excludeSpaceMembers"); // Notification
                                                                                  // will
                                                                                  // not
                                                                                  // be
                                                                                  // sent
                                                                                  // to
                                                                                  // news
                                                                                  // space
                                                                                  // members
                                                                                  // when
                                                                                  // news
                                                                                  // audience
                                                                                  // is
                                                                                  // changed
                                                                                  // from
                                                                                  // "space"
                                                                                  // to
                                                                                  // "all"
        } else {
          ctx.append(PostNewsNotificationPlugin.AUDIENCE, news.getAudience());
        }
      }
      ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(PublishNewsNotificationPlugin.ID))).execute(ctx);
    }
  }

  private void updateNewsActivity(News news, boolean post) {
    ExoSocialActivity activity = activityManager.getActivity(news.getActivityId());
    if (activity != null) {
      if (post) {
        activity.setUpdated(System.currentTimeMillis());
      }
      activity.isHidden(!news.isActivityPosted());
      Map<String, String> templateParams = activity.getTemplateParams() == null ? new HashMap<>() : activity.getTemplateParams();
      templateParams.put(NEWS_ID, news.getId());
      activity.setTemplateParams(templateParams);
      activity.setMetadataObjectId(news.getId());
      activity.setMetadataObjectType(NewsUtils.NEWS_METADATA_OBJECT_TYPE);
      activityManager.updateActivity(activity, true);
    }
  }

  private void sendMentionInNewsNotification(String newsId,
                                             String contentAuthor,
                                             String currentUser,
                                             String contentTitle,
                                             String contentBody,
                                             String contentSpaceId,
                                             String authorAvatarUrl,
                                             String activityLink,
                                             String contentSpaceName) {
    Space space = spaceService.getSpaceById(contentSpaceId);
    Set<String> mentionedIds = NewsUtils.processMentions(contentBody, space);
    NotificationContext mentionNotificationCtx =
                                               NotificationContextImpl.cloneInstance()
                                                                      .append(MentionInNewsNotificationPlugin.CONTEXT,
                                                                              NotificationConstants.NOTIFICATION_CONTEXT.MENTION_IN_NEWS)
                                                                      .append(PostNewsNotificationPlugin.CURRENT_USER,
                                                                              currentUser)
                                                                      .append(PostNewsNotificationPlugin.CONTENT_AUTHOR,
                                                                              contentAuthor)
                                                                      .append(PostNewsNotificationPlugin.CONTENT_SPACE_ID,
                                                                              contentSpaceId)
                                                                      .append(PostNewsNotificationPlugin.CONTENT_TITLE,
                                                                              contentTitle)
                                                                      .append(PostNewsNotificationPlugin.CONTENT_SPACE,
                                                                              contentSpaceName)
                                                                      .append(PostNewsNotificationPlugin.AUTHOR_AVATAR_URL,
                                                                              authorAvatarUrl)
                                                                      .append(PostNewsNotificationPlugin.ACTIVITY_LINK,
                                                                              activityLink)
                                                                      .append(MentionInNewsNotificationPlugin.MENTIONED_IDS,
                                                                              mentionedIds)
                                                                      .append(PostNewsNotificationPlugin.NEWS_ID, newsId);
    mentionNotificationCtx.getNotificationExecutor()
                          .with(mentionNotificationCtx.makeCommand(PluginKey.key(MentionInNewsNotificationPlugin.ID)))
                          .execute(mentionNotificationCtx);
  }

  private Identity getCurrentIdentity() {
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null ? null : conversationState.getIdentity();
  }

  private void updateNewsActivities(String activityId, News news) throws Exception {
    if (activityId != null && !StringUtils.isEmpty(news.getId())) {
      Page newsPage = noteService.getNoteById(news.getId());
      if (newsPage != null) {
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE, newsPage.getId(), null);
        MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject).get(0);
        if (metadataItem != null) {
          Map<String, String> properties = metadataItem.getProperties();
          if (properties == null) {
            properties = new HashMap<>();
          }
          String newsActivity = news.getSpaceId().concat(":").concat(activityId);
          if (properties.containsKey(NEWS_ACTIVITIES)) {
            properties.put(NEWS_ACTIVITIES, properties.get(NEWS_ACTIVITIES).concat("; ").concat(newsActivity));
          } else {
            properties.put(NEWS_ACTIVITIES, newsActivity);
          }
          metadataItem.setProperties(properties);
          String updaterId = identityManager.getOrCreateUserIdentity(news.getAuthor()).getId();
          metadataService.updateMetadataItem(metadataItem, Long.parseLong(updaterId));
          news.setActivities(properties.get(NEWS_ACTIVITIES));
          news.setActivityId(activityId);
        }
      } else {
        throw new ObjectNotFoundException("No metadata item found for the news article page " + news.getId());
      }
    }
  }

  private void postNewsActivity(News news) throws Exception {
    org.exoplatform.social.core.identity.model.Identity poster = identityManager.getOrCreateUserIdentity(news.getAuthor());

    Space space = spaceService.getSpaceById(news.getSpaceId());
    org.exoplatform.social.core.identity.model.Identity spaceIdentity =
                                                                      identityManager.getOrCreateSpaceIdentity(space.getPrettyName());

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(news.getTitle());
    activity.setType("news");
    activity.setUserId(poster.getId());
    activity.isHidden(!news.isActivityPosted());
    Map<String, String> templateParams = new HashMap<>();
    templateParams.put(NEWS_ID, news.getId());
    activity.setTemplateParams(templateParams);
    activity.setMetadataObjectId(news.getId());
    activity.setMetadataObjectType(NewsUtils.NEWS_METADATA_OBJECT_TYPE);

    activityManager.saveActivityNoReturn(spaceIdentity, activity);
    updateNewsActivities(activity.getId(), news);
  }

  private News createNewsArticlePage(News newsArticle, String newsArticleCreator) throws Exception {
    // get the news draft article from the news model before setting the news
    // article id to the news model
    String draftNewsId = newsArticle.getId();

    Identity poster = NewsUtils.getUserIdentity(newsArticleCreator);
    Space space = spaceService.getSpaceById(newsArticle.getSpaceId());
    Wiki wiki = wikiService.getWikiByTypeAndOwner(WikiType.GROUP.name().toLowerCase(), space.getGroupId());
    Page newsArticlesRootNotePage = noteService.getNoteOfNoteBookByName(WikiType.GROUP.name().toLowerCase(),
                                                                        space.getGroupId(),
                                                                        NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME);

    if (newsArticlesRootNotePage != null) {
      Page newsArticlePage = new Page();
      newsArticlePage.setTitle(newsArticle.getTitle());
      newsArticlePage.setContent(newsArticle.getBody());
      newsArticlePage.setParentPageId(newsArticlesRootNotePage.getId());
      newsArticlePage.setAuthor(newsArticle.getAuthor());
      newsArticlePage.setLang(null);
      newsArticlePage = noteService.createNote(wiki, newsArticlesRootNotePage.getName(), newsArticlePage, poster);
      // create the version
      noteService.createVersionOfNote(newsArticlePage, poster.getUserId());

      if (newsArticlePage != null) {
        PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(newsArticlePage.getId()), null);
        // set properties
        newsArticle.setId(newsArticlePage.getId());
        newsArticle.setCreationDate(pageVersion.getCreatedDate());

        NewsPageVersionObject newsArticleVersionMetaDataObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                           pageVersion.getId(),
                                                                                           null);
        String newsArticleMetadataItemCreatorIdentityId = identityManager.getOrCreateUserIdentity(newsArticleCreator).getId();
        Map<String, String> newsArticleVersionMetadataItemProperties = new HashMap<>();

        // save illustration
        boolean hasIllustration = false;
        Long oldIllustrationId = null;
        String oldUploadId = null;

        NewsDraftObject newsDraftObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                              draftNewsId,
                                                              null,
                                                              Long.parseLong(space.getId()));
        List<MetadataItem> metadataItems =
                                         metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsDraftObject);
        MetadataItem metadataItem = metadataItems.isEmpty() ? null : metadataItems.get(0);
        if (metadataItem != null && metadataItem.getProperties() != null && !metadataItem.getProperties().isEmpty()) {
          hasIllustration = metadataItem.getProperties().containsKey(NEWS_ILLUSTRATION_ID)
              && StringUtils.isNotEmpty(metadataItem.getProperties().get(NEWS_ILLUSTRATION_ID));
          if (hasIllustration) {
            oldIllustrationId = Long.parseLong(metadataItem.getProperties().get(NEWS_ILLUSTRATION_ID));
            oldUploadId = metadataItem.getProperties().get(NEWS_UPLOAD_ID);
          }
        }
        if (StringUtils.isNotEmpty(newsArticle.getUploadId())) {
          if (hasIllustration && oldUploadId.equals(newsArticle.getUploadId())) {
            newsArticleVersionMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(oldIllustrationId));
            newsArticleVersionMetadataItemProperties.put(NEWS_UPLOAD_ID, oldUploadId);
            setArticleIllustration(newsArticle, oldIllustrationId, ARTICLE.name().toLowerCase());
          } else {
            Long newIllustrationId = saveArticleIllustration(newsArticle.getUploadId(), oldIllustrationId);
            if (newIllustrationId != null) {
              newsArticleVersionMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(newIllustrationId));
              newsArticleVersionMetadataItemProperties.put(NEWS_UPLOAD_ID, newsArticle.getUploadId());
              setArticleIllustration(newsArticle, newIllustrationId, ARTICLE.name().toLowerCase());
            }
          }
        } else if (newsArticle.getUploadId() == null && hasIllustration) {
          // link the illustration to the newly created version
          newsArticleVersionMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(oldIllustrationId));
          newsArticleVersionMetadataItemProperties.put(NEWS_UPLOAD_ID, oldUploadId);
          setArticleIllustration(newsArticle, oldIllustrationId, ARTICLE.name().toLowerCase());
        }
        // create the page version metadata item
        if (StringUtils.isNotEmpty(newsArticle.getSummary())) {
          newsArticleVersionMetadataItemProperties.put(NEWS_SUMMARY, newsArticle.getSummary());
        }
        metadataService.createMetadataItem(newsArticleVersionMetaDataObject,
                                           NEWS_METADATA_KEY,
                                           newsArticleVersionMetadataItemProperties,
                                           Long.parseLong(newsArticleMetadataItemCreatorIdentityId));

        // create metadata item page
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE, newsArticlePage.getId(), null);
        Map<String, String> newsPageProperties = new HashMap<>();
        if (StringUtils.isNotEmpty(newsArticle.getAudience())) {
          newsPageProperties.put(NEWS_AUDIENCE, newsArticle.getAudience());
        }
        if (StringUtils.isNotEmpty(newsArticle.getSchedulePostDate())) {
          newsPageProperties.put(SCHEDULE_POST_DATE, newsArticle.getSchedulePostDate());
        }
        if (StringUtils.isNotEmpty(newsArticle.getPublicationState())) {
          newsPageProperties.put(NEWS_PUBLICATION_STATE, newsArticle.getPublicationState());
        }
        newsPageProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(newsArticle.isActivityPosted()));
        metadataService.createMetadataItem(newsPageObject, NEWS_METADATA_KEY, newsPageProperties);

        // delete the draft
        deleteDraftArticle(draftNewsId, poster.getUserId(), false);
        return newsArticle;
      }
    }
    return null;
  }

  private News updateNewsArticle(News news, Identity updater) throws Exception {
    Page existingPage = noteService.getNoteById(news.getId());
    if (existingPage != null) {
      existingPage.setTitle(news.getTitle());
      existingPage.setContent(news.getBody());
      existingPage.setUpdatedDate(Calendar.getInstance().getTime());
      existingPage = noteService.updateNote(existingPage);

      // create the version
      noteService.createVersionOfNote(existingPage, updater.getUserId());
      PageVersion createdVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(existingPage.getId()), null);
      news.setUpdateDate(createdVersion.getCreatedDate());
      news.setUpdater(createdVersion.getAuthor());
      news.setUpdaterFullName(createdVersion.getAuthorFullName());

      String newsArticleUpdaterIdentityId = identityManager.getOrCreateUserIdentity(updater.getUserId()).getId();

      // update the metadata item page
      NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE, news.getId(), null);
      MetadataItem existingPageMetadataItem =
                                            metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                                           .stream()
                                                           .findFirst()
                                                           .orElse(null);
      if (existingPageMetadataItem != null) {
        Map<String, String> newsPageProperties = existingPageMetadataItem.getProperties();
        if (StringUtils.isNotEmpty(news.getAudience())) {
          newsPageProperties.put(NEWS_AUDIENCE, news.getAudience());
        }
        if (StringUtils.isNotEmpty(news.getSchedulePostDate())) {
          newsPageProperties.put(SCHEDULE_POST_DATE, news.getSchedulePostDate());
        }
        if (StringUtils.isNotEmpty(news.getPublicationState())) {
          newsPageProperties.put(NEWS_PUBLICATION_STATE, news.getPublicationState());
        }
        newsPageProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(news.isActivityPosted()));
        existingPageMetadataItem.setProperties(newsPageProperties);
        metadataService.updateMetadataItem(existingPageMetadataItem, Long.parseLong(newsArticleUpdaterIdentityId));
      } else {
        throw new ObjectNotFoundException("No such news article metadata item exists with id " + news.getId());
      }

      // create the version metadata item
      NewsPageVersionObject newsArticleVersionMetaDataObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                         createdVersion.getId(),
                                                                                         null);
      Map<String, String> newsArticleVersionMetadataItemProperties = new HashMap<>();

      String draftNewsId = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(existingPage.getId()),
                                                                                    updater.getUserId(),
                                                                                    null)
                                      .getId();

      NewsLatestDraftObject newsLatestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                              draftNewsId,
                                                                              existingPage.getId());
      MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsLatestDraftObject)
                                                 .stream()
                                                 .findFirst()
                                                 .orElse(null);
      if (metadataItem != null && metadataItem.getProperties() != null && !metadataItem.getProperties().isEmpty()) {
        Map<String, String> properties = metadataItem.getProperties();
        if (properties.containsKey(NEWS_UPLOAD_ID)) {
          newsArticleVersionMetadataItemProperties.put(NEWS_UPLOAD_ID, properties.get(NEWS_UPLOAD_ID));
        }
        if (properties.containsKey(NEWS_ILLUSTRATION_ID)) {
          newsArticleVersionMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, properties.get(NEWS_ILLUSTRATION_ID));
          setArticleIllustration(news, Long.parseLong(properties.get(NEWS_ILLUSTRATION_ID)), ARTICLE.name().toLowerCase());
        }
      } else {
        throw new ObjectNotFoundException("No such news draft article metadata item exists with id " + draftNewsId);
      }
      if (StringUtils.isNotEmpty(news.getSummary())) {
        newsArticleVersionMetadataItemProperties.put(NEWS_SUMMARY, news.getSummary());
      }
      metadataService.createMetadataItem(newsArticleVersionMetaDataObject,
                                         NEWS_METADATA_KEY,
                                         newsArticleVersionMetadataItemProperties,
                                         Long.parseLong(newsArticleUpdaterIdentityId));

      // remove the draft
      noteService.removeDraftOfNote(existingPage, updater.getUserId());
      return news;
    }
    return null;
  }

  private News buildArticle(String newsId) throws WikiException {
    Page articlePage = noteService.getNoteById(newsId);
    Identity userIdentity = getCurrentIdentity();
    String currentUsername = userIdentity == null ? null : userIdentity.getUserId();
    if (articlePage != null) {
      News news = new News();
      news.setCreationDate(articlePage.getCreatedDate());

      // fetch related metadata item properties
      NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE, articlePage.getId(), null);
      MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject).get(0);
      if (metadataItem != null && metadataItem.getProperties() != null && !metadataItem.getProperties().isEmpty()) {
        Map<String, String> properties = metadataItem.getProperties();
        if (properties.containsKey(NEWS_ACTIVITIES) && properties.get(NEWS_ACTIVITIES) != null) {
          String[] activities = properties.get(NEWS_ACTIVITIES).split(";");
          StringBuilder memberSpaceActivities = new StringBuilder();
          String newsActivityId = activities[0].split(":")[1];
          news.setActivityId(newsActivityId);
          StringBuilder newsUrl = new StringBuilder();
          Space newsPostedInSpace = spaceService.getSpaceById(activities[0].split(":")[0]);
          if (currentUsername != null && spaceService.isMember(newsPostedInSpace, currentUsername)) {
            newsUrl.append("/")
                   .append(PortalContainer.getCurrentPortalContainerName())
                   .append("/")
                   .append(CommonsUtils.getCurrentPortalOwner())
                   .append("/activity?id=")
                   .append(newsActivityId);
            news.setUrl(newsUrl.toString());
          } else {
            newsUrl.append("/")
                   .append(PortalContainer.getCurrentPortalContainerName())
                   .append("/")
                   .append(CommonsUtils.getCurrentPortalOwner())
                   .append("/news/detail?newsId=")
                   .append(newsId)
                   .append("&type=article");
            news.setUrl(newsUrl.toString());
          }
          memberSpaceActivities.append(activities[0]).append(";");
          List<String> sharedInSpacesList = new ArrayList<>();
          for (int i = 1; i < activities.length; i++) {
            String sharedInSpaceId = activities[i].split(":")[0];
            sharedInSpacesList.add(sharedInSpaceId);
            Space sharedInSpace = spaceService.getSpaceById(sharedInSpaceId);
            String activityId = activities[i].split(":")[1];
            if (sharedInSpace != null && currentUsername != null && spaceService.isMember(sharedInSpace, currentUsername)
                && activityManager.isActivityExists(activityId)) {
              memberSpaceActivities.append(activities[i]).append(";");
            }
          }
          news.setActivities(memberSpaceActivities.toString());
          news.setSharedInSpacesList(sharedInSpacesList);
        }
        if (properties.containsKey(NEWS_AUDIENCE) && StringUtils.isNotEmpty(properties.get(NEWS_AUDIENCE))) {
          news.setAudience(properties.get(NEWS_AUDIENCE));
        }
        if (properties.containsKey(SCHEDULE_POST_DATE) && StringUtils.isNotEmpty(properties.get(SCHEDULE_POST_DATE))) {
          news.setSchedulePostDate(properties.get(SCHEDULE_POST_DATE));
        }
        if (properties.containsKey(NEWS_PUBLICATION_STATE) && StringUtils.isNotEmpty(properties.get(NEWS_PUBLICATION_STATE))) {
          news.setPublicationState(properties.get(NEWS_PUBLICATION_STATE));
        }
        if (properties.containsKey(PUBLISHED) && StringUtils.isNotEmpty(properties.get(PUBLISHED))) {
          news.setPublished(Boolean.valueOf(properties.get(PUBLISHED)));
        }
        if (properties.containsKey(NEWS_PUBLISH_DATE) && StringUtils.isNotEmpty(properties.get(NEWS_PUBLISH_DATE))) {
          try {
            SimpleDateFormat format = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
            Date date = format.parse(properties.get(NEWS_PUBLISH_DATE));
            news.setPublishDate(date);
          } catch (Exception exception) {
            LOG.warn("failed to parse news published date for article with id " + news.getId());
          }
        }
        if (properties.containsKey(NEWS_VIEWS) && StringUtils.isNotEmpty(properties.get(NEWS_VIEWS))) {
          news.setViewsCount(Long.parseLong(properties.get(NEWS_VIEWS)));
        }
        if (properties.containsKey(NEWS_ACTIVITY_POSTED)) {
          news.setActivityPosted(Boolean.valueOf(properties.get(NEWS_ACTIVITY_POSTED)));
        } else {
          news.setActivityPosted(false);
        }
        news.setDeleted(articlePage.isDeleted());
      }

      // fetch the last version of the given lang
      PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(articlePage.getId()), null);
      news.setId(articlePage.getId());
      news.setTitle(pageVersion.getTitle());
      news.setAuthor(articlePage.getAuthor());
      news.setUpdateDate(pageVersion.getUpdatedDate());
      news.setBody(pageVersion.getContent());
      news.setUpdaterFullName(pageVersion.getAuthorFullName());
      if (articlePage.getWikiOwner() != null) {
        Space space = spaceService.getSpaceByGroupId(articlePage.getWikiOwner());
        if (space != null) {
          news.setSpaceId(space.getId());
          news.setSpaceAvatarUrl(space.getAvatarUrl());
          news.setSpaceDisplayName(space.getDisplayName());
          boolean hiddenSpace = space.getVisibility().equals(Space.HIDDEN) && !spaceService.isMember(space, currentUsername)
              && !spaceService.isSuperManager(currentUsername);
          news.setHiddenSpace(hiddenSpace);
          boolean isSpaceMember = spaceService.isSuperManager(currentUsername) || spaceService.isMember(space, currentUsername);
          news.setSpaceMember(isSpaceMember);
          if (StringUtils.isNotEmpty(space.getGroupId())) {
            String spaceGroupId = space.getGroupId().split("/")[2];
            String spaceUrl = "/portal/g/:spaces:" + spaceGroupId + "/" + space.getPrettyName();
            news.setSpaceUrl(spaceUrl);
          }
        }
      }
      NewsPageVersionObject newsArticleObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                          pageVersion.getId(),
                                                                          null);
      List<MetadataItem> metadataItems =
                                       metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsArticleObject);
      if (metadataItems != null && !metadataItems.isEmpty()) {
        Map<String, String> properties = metadataItems.get(0).getProperties();
        if (properties != null && !properties.isEmpty()) {
          if (properties.containsKey(NEWS_SUMMARY)) {
            news.setSummary(properties.get(NEWS_SUMMARY));
          }
          if (properties.containsKey(NEWS_ILLUSTRATION_ID) && properties.get(NEWS_ILLUSTRATION_ID) != null) {
            setArticleIllustration(news, Long.valueOf(properties.get(NEWS_ILLUSTRATION_ID)), ARTICLE.name().toLowerCase());
          }
          if (properties.containsKey(NEWS_UPLOAD_ID) && properties.get(NEWS_UPLOAD_ID) != null) {
            news.setUploadId(properties.get(NEWS_UPLOAD_ID));
          }
        }
      }
      return news;
    }
    return null;
  }

  private News createOrUpdateDraftForExistingPage(News news, String updater) throws Exception {
    String pageId = news.getId();
    Page existingPage = noteService.getNoteById(pageId);
    if (existingPage == null) {
      return null;
    }
    DraftPage draftPage = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(pageId), updater, null);
    if (draftPage == null) {
      news = createDraftForExistingPage(news, updater, existingPage);
    } else {
      news = updateDraftForExistingPage(news, updater, existingPage, draftPage);
    }
    return news;
  }

  private News createDraftForExistingPage(News news, String updater, Page page) throws Exception {
    DraftPage draftArticlePage = new DraftPage();
    draftArticlePage.setNewPage(false);
    draftArticlePage.setTargetPageId(page.getId());
    draftArticlePage.setTitle(news.getTitle());
    draftArticlePage.setContent(news.getBody());
    draftArticlePage.setParentPageId(page.getParentPageId());
    draftArticlePage.setAuthor(news.getAuthor());
    draftArticlePage.setLang(null);

    draftArticlePage = noteService.createDraftForExistPage(draftArticlePage, page, null, System.currentTimeMillis(), updater);

    news.setDraftUpdateDate(draftArticlePage.getCreatedDate());
    news.setDraftUpdater(draftArticlePage.getAuthor());

    NewsLatestDraftObject latestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                        draftArticlePage.getId(),
                                                                        page.getId());

    Map<String, String> draftArticleMetadataItemProperties = new HashMap<>();
    if (StringUtils.isNotEmpty(news.getSummary())) {
      draftArticleMetadataItemProperties.put(NEWS_SUMMARY, news.getSummary());
    }
    draftArticleMetadataItemProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(news.isActivityPosted()));
    // check if the article has an illustration to lik it to the created draft
    PageVersion latestPageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()), null);
    if (latestPageVersion != null) {
      // fetch the version related metadata item
      MetadataItem metadataItem =
                                metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                    new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                                              latestPageVersion.getId(),
                                                                                                              null))
                                               .get(0);
      boolean hasIllustration = false;
      Long oldIllustrationId = null;
      String oldIllustrationUploadId = null;
      if (metadataItem != null && metadataItem.getProperties() != null && !metadataItem.getProperties().isEmpty()) {
        hasIllustration = metadataItem.getProperties().containsKey(NEWS_ILLUSTRATION_ID)
            && StringUtils.isNotEmpty(metadataItem.getProperties().get(NEWS_ILLUSTRATION_ID));
        if (hasIllustration) {
          oldIllustrationId = Long.parseLong(metadataItem.getProperties().get(NEWS_ILLUSTRATION_ID));
          oldIllustrationUploadId = metadataItem.getProperties().get(NEWS_UPLOAD_ID);
        }
      }
      if (StringUtils.isNotEmpty(news.getUploadId())) {
        // save the illustration
        Long newIllustrationId = saveArticleIllustration(news.getUploadId(), null);
        if (newIllustrationId != null) {
          draftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(newIllustrationId));
          draftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, news.getUploadId());
          setArticleIllustration(news, newIllustrationId, NewsObjectType.DRAFT.name().toLowerCase());
        }
      } else if (news.getUploadId() == null && hasIllustration) {
        // link the illustration to the newly created draft
        draftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(oldIllustrationId));
        draftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, oldIllustrationUploadId);
        setArticleIllustration(news, oldIllustrationId, NewsObjectType.DRAFT.name().toLowerCase());
      }
    }
    metadataService.createMetadataItem(latestDraftObject, NEWS_METADATA_KEY, draftArticleMetadataItemProperties);
    return news;
  }

  private News updateDraftForExistingPage(News news, String updater, Page page, DraftPage draftPage) {
    try {
      draftPage.setTitle(news.getTitle());
      draftPage.setContent(news.getBody());
      draftPage.setAuthor(news.getAuthor());
      draftPage.setTargetPageId(page.getId());
      draftPage.setLang(null);

      draftPage = noteService.updateDraftForExistPage(draftPage, page, null, System.currentTimeMillis(), updater);

      news.setDraftUpdateDate(draftPage.getUpdatedDate());
      news.setDraftUpdater(draftPage.getAuthor());

      NewsLatestDraftObject latestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                          draftPage.getId(),
                                                                          page.getId());

      List<MetadataItem> latestDraftArticleMetadataItems = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                               latestDraftObject);
      if (latestDraftArticleMetadataItems != null && !latestDraftArticleMetadataItems.isEmpty()) {
        MetadataItem latestDraftArticleMetadataItem = latestDraftArticleMetadataItems.get(0);
        Map<String, String> latestDraftArticleMetadataItemProperties = latestDraftArticleMetadataItem.getProperties();
        if (latestDraftArticleMetadataItemProperties == null) {
          latestDraftArticleMetadataItemProperties = new HashMap<>();
        }
        // create or update the illustration
        if (StringUtils.isNotEmpty(news.getUploadId())) {
          // update the illustration if exist
          if (latestDraftArticleMetadataItemProperties.containsKey(NEWS_UPLOAD_ID)
              && latestDraftArticleMetadataItemProperties.get(NEWS_UPLOAD_ID) != null
              && latestDraftArticleMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
              && latestDraftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null) {
            if (!latestDraftArticleMetadataItemProperties.get(NEWS_UPLOAD_ID).equals(news.getUploadId())) {
              FileItem draftArticleIllustrationFileItem =
                                                        fileService.getFile(Long.parseLong(latestDraftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)));
              Long draftArticleIllustrationId = saveArticleIllustration(news.getUploadId(),
                                                                        draftArticleIllustrationFileItem.getFileInfo().getId());
              latestDraftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(draftArticleIllustrationId));
              setArticleIllustration(news, draftArticleIllustrationId, NewsObjectType.DRAFT.name());
            }
          } else {
            // create the illustration if not exist
            Long draftArticleIllustrationId = saveArticleIllustration(news.getUploadId(), null);
            latestDraftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID, String.valueOf(draftArticleIllustrationId));
            latestDraftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, news.getUploadId());
            setArticleIllustration(news, draftArticleIllustrationId, NewsObjectType.DRAFT.name());
          }
          latestDraftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, news.getUploadId());
        } else {
          // if the upload id is empty we should remove the existing
          // illustration
          if (latestDraftArticleMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
              && latestDraftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null && news.getUploadId() != null) {
            latestDraftArticleMetadataItemProperties.remove(NEWS_UPLOAD_ID);
            FileItem draftArticleIllustrationFileItem =
                                                      fileService.getFile(Long.parseLong(latestDraftArticleMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)));
            latestDraftArticleMetadataItemProperties.remove(NEWS_ILLUSTRATION_ID);

            fileService.deleteFile(draftArticleIllustrationFileItem.getFileInfo().getId());
          }
        }
        if (StringUtils.isNotEmpty(news.getSummary())) {
          latestDraftArticleMetadataItemProperties.put(NEWS_SUMMARY, news.getSummary());
        }
        latestDraftArticleMetadataItemProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(news.isActivityPosted()));
        latestDraftArticleMetadataItem.setProperties(latestDraftArticleMetadataItemProperties);
        String draftArticleMetadataItemUpdaterIdentityId = identityManager.getOrCreateUserIdentity(updater).getId();
        metadataService.updateMetadataItem(latestDraftArticleMetadataItem,
                                           Long.parseLong(draftArticleMetadataItemUpdaterIdentityId));
      } else {
        throw new ObjectNotFoundException("No metadata item found for the draft " + news.getId());
      }
    } catch (Exception exception) {
      return null;
    }
    return news;
  }

  private News buildLatestDraftArticle(String parentPageId, String currentIdentityId) throws Exception {
    Page parentPage = noteService.getNoteById(parentPageId);
    if (parentPage == null) {
      return null;
    }
    // if the latest draft exist return it , else return the article
    DraftPage latestDraft = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(parentPageId),
                                                                                     currentIdentityId,
                                                                                     null);
    if (latestDraft == null) {
      return buildArticle(parentPageId);
    }
    News draftArticle = buildDraftArticle(latestDraft.getId(), currentIdentityId);
    // set always the article id to use it to fetch the article if the draft not
    // exist
    draftArticle.setId(parentPageId);
    return draftArticle;
  }

  private void deleteArticle(News news, Identity currentIdentity) throws Exception {
    Page articlePage = noteService.getNoteById(news.getId());
    if (articlePage != null && !articlePage.isDeleted()) {
      boolean hasDraft = true;
      while (hasDraft) {
        try {
          DraftPage latestDraftPage = noteService.getLatestDraftOfPage(articlePage, currentIdentity.getUserId());
          if (latestDraftPage != null) {
            deleteDraftArticle(latestDraftPage.getId(), currentIdentity.getUserId(), true);
          } else {
            hasDraft = false;
          }
        } catch (Exception exception) {
          hasDraft = false;
        }
      }
      boolean isDeleted = noteService.deleteNote(articlePage.getWikiType(), articlePage.getWikiOwner(), articlePage.getName());
      if (isDeleted) {
        if (news.getActivities() != null) {
          String newsActivities = news.getActivities();
          Stream.of(newsActivities.split(";")).map(activity -> activity.split(":")[1]).forEach(activityManager::deleteActivity);
        }
      }
    }
  }
}
