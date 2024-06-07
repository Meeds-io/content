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
import static io.meeds.news.utils.NewsUtils.NewsUpdateType.CONTENT;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.social.metadata.MetadataFilter;
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
import io.meeds.news.search.NewsSearchConnector;
import io.meeds.news.service.NewsService;
import io.meeds.news.service.NewsTargetingService;
import io.meeds.news.utils.NewsUtils;
import io.meeds.news.utils.NewsUtils.NewsObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class NewsServiceImpl implements NewsService {

  public static final String         NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME      = "Articles";

  private static final String        HTML_AT_SYMBOL_PATTERN                 = "@";

  private static final String        HTML_AT_SYMBOL_ESCAPED_PATTERN         = "&#64;";

  public static final MetadataType   NEWS_METADATA_TYPE                     = new MetadataType(1000, "news");

  public static final String         NEWS_METADATA_NAME                     = "news";

  public static final String         NEWS_METADATA_DRAFT_OBJECT_TYPE        = "newsDraftPage";

  public static final String         NEWS_FILE_API_NAME_SPACE               = "news";

  public static final String         NEWS_SUMMARY                           = "summary";

  public static final String         NEWS_ILLUSTRATION_ID                   = "illustrationId";

  public static final String         NEWS_UPLOAD_ID                         = "uploadId";

  /** The Constant PUBLISHED. */
  public final static String         PUBLISHED                              = "published";

  /** The Constant POSTED. */
  public final static String         POSTED                                 = "posted";

  /** The Constant DRAFT. */
  public final static String         DRAFT                                  = "draft";

  /** The Constant STAGED. */
  public final static String         STAGED                                 = "staged";

  /** The Constant AUDIENCE. */
  public static final String         NEWS_AUDIENCE                          = "audience";

  /** The Constant DELETED. */
  public static final String         NEWS_DELETED                           = "deleted";

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

  public static final String         NEWS_ATTACHMENTS_IDS                   = "attachmentsIds";

  public static final String         ARTICLE_CONTENT                        = "content";

  public static final MetadataKey    NEWS_METADATA_KEY                      =
                                                       new MetadataKey(NEWS_METADATA_TYPE.getName(), NEWS_METADATA_NAME, 0);

  private static final Log           LOG                                    = ExoLogger.getLogger(NewsServiceImpl.class);

  @Autowired
  private SpaceService          spaceService;

  @Autowired
  private NoteService           noteService;

  @Autowired
  private MetadataService       metadataService;

  @Autowired
  private FileService           fileService;

  @Autowired
  private UploadService         uploadService;

  @Autowired
  private NewsTargetingService  newsTargetingService;

  @Autowired
  private IndexingService       indexingService;

  @Autowired
  private IdentityManager       identityManager;

  @Autowired
  private ActivityManager       activityManager;

  @Autowired
  private WikiService           wikiService;

  @Autowired
  private NewsSearchConnector newsSearchConnector;

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
      if (POSTED.equals(news.getPublicationState())) {
        createdNews = postNews(news, currentIdentity.getUserId());
      } else if (news.getSchedulePostDate() != null) {
        createdNews = unScheduleNews(news, space.getGroupId(), currentIdentity.getUserId());
      } else {
        createdNews = createDraftArticleForNewPage(news,
                                                   space.getGroupId(),
                                                   currentIdentity.getUserId(),
                                                   System.currentTimeMillis());
      }
      return createdNews;
    } catch (Exception e) {
      LOG.error("Error when creating the news " + news.getTitle(), e);
      return null;
    }
  }

  @Override
  public News postNews(News news, String poster) throws Exception {
    if (news.getPublicationState().equals(STAGED) || news.getSchedulePostDate() != null) {
      news = postScheduledArticle(news);
    } else {
      news = createNewsArticlePage(news, poster);
    }
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
  public News updateNews(News news,
                         String updater,
                         Boolean post,
                         boolean publish,
                         String newsObjectType,
                         String newsUpdateType) throws Exception {

    if (!canEditNews(news, updater)) {
      throw new IllegalAccessException("User " + updater + " is not authorized to update news");
    }
    Identity updaterIdentity = NewsUtils.getUserIdentity(updater);
    News originalNews = getNewsById(news.getId(), updaterIdentity, false, newsObjectType);
    List<String> oldTargets = newsTargetingService.getTargetsByNews(news);
    boolean canPublish = NewsUtils.canPublishNews(news.getSpaceId(), updaterIdentity);
    Set<String> previousMentions = NewsUtils.processMentions(originalNews.getOriginalBody(),
                                                             spaceService.getSpaceById(news.getSpaceId()));
    if (NewsObjectType.DRAFT.name().toLowerCase().equals(newsObjectType)) {
      return updateDraftArticleForNewPage(news, updater);
    } else if (LATEST_DRAFT.name().toLowerCase().equals(newsObjectType)) {
      return createOrUpdateDraftForExistingPage(news, updater);
    }
    if (publish != news.isPublished() && news.isCanPublish()) {
      news.setPublished(publish);
      if (news.isPublished()) {
        publishNews(news, updater);
      } else {
        unpublishNews(news.getId(), updater);
      }
    }
    boolean displayed = !(StringUtils.equals(news.getPublicationState(), STAGED));
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
    // update the news article after executing publish and send notification
    // methods
    // They need the original news to treat the news audience and exclude space
    // members from notification.
    if (ARTICLE.name().toLowerCase().equals(newsObjectType)) {
      news = updateNewsArticle(news, updaterIdentity, newsUpdateType);
    }

    if (POSTED.equals(news.getPublicationState())) {
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
  public void deleteNews(String newsId, Identity currentIdentity, String newsObjectType) throws Exception {
    News news = getNewsById(newsId, currentIdentity, false, newsObjectType);
    if (!news.isCanDelete()) {
      throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to delete news");
    }
    if (NewsObjectType.DRAFT.name().toLowerCase().equals(newsObjectType)) {
      deleteDraftArticle(newsId, currentIdentity.getUserId(), true);
    } else if (LATEST_DRAFT.name().toLowerCase().equals(newsObjectType)) {
      Page newsArticlePage = noteService.getNoteById(newsId);
      if (newsArticlePage != null) {
        DraftPage draft = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(newsArticlePage.getId()),
                                                                                   currentIdentity.getUserId(),
                                                                                   null);
        if (draft != null) {
          // check if the latest draft has the same illustration
          // with the news article to do not remove it.
          deleteDraftArticle(draft.getId(),
                             currentIdentity.getUserId(),
                             !isSameIllustration(draft.getId(), newsArticlePage.getId()));
        }
      }
    } else {
      deleteArticle(news, currentIdentity.getUserId());
      if (news.getActivities() != null) {
        String newsActivities = news.getActivities();
        Stream.of(newsActivities.split(";")).map(activity -> activity.split(":")[1]).forEach(activityManager::deleteActivity);
      }
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
    boolean displayed = !(StringUtils.equals(news.getPublicationState(), STAGED));

    // update page metadata
    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                       news.getId(),
                                                       null,
                                                       Long.parseLong(news.getSpaceId()));
    MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject).get(0);
    if (metadataItem != null) {
      Map<String, String> properties = metadataItem.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
      }
      properties.put(PUBLISHED, String.valueOf(true));
      if (StringUtils.isNotEmpty(newsToPublish.getAudience())) {
        properties.put(NEWS_AUDIENCE, news.getAudience());
      }
      metadataItem.setProperties(properties);
      Date updatedDate = Calendar.getInstance().getTime();
      metadataItem.setUpdatedDate(updatedDate.getTime());
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

    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                       news.getId(),
                                                       null,
                                                       Long.parseLong(news.getSpaceId()));
    MetadataItem newsMetadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                                   .stream()
                                                   .findFirst()
                                                   .orElse(null);

    if (newsMetadataItem != null) {
      Map<String, String> properties = newsMetadataItem.getProperties();
      if (properties != null) {
        properties.put(PUBLISHED, String.valueOf(false));
        properties.remove(NEWS_AUDIENCE);
      }
      newsMetadataItem.setProperties(properties);
      Date updatedDate = Calendar.getInstance().getTime();
      newsMetadataItem.setUpdatedDate(updatedDate.getTime());
      String publisherId = identityManager.getOrCreateUserIdentity(publisher).getId();
      metadataService.updateMetadataItem(newsMetadataItem, Long.parseLong(publisherId));
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
      news.setTargets(newsTargetingService.getTargetsByNews(news));
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
      news.setTargets(newsTargetingService.getTargetsByNews(news));
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
      if (StringUtils.isNotBlank(filter.getSearchText())) {
        newsList =
                 searchNews(filter,
                            identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentIdentity.getUserId()));
      } else if (filter.isPublishedNews()) {
        newsList = getPublishedArticles(filter, currentIdentity);
      } else if (filter.isDraftNews()) {
        newsList = buildDraftArticles(filter, currentIdentity);
      } else if (filter.isScheduledNews()) {
        newsList = getScheduledArticles(filter, currentIdentity);
      } else if (filter.getAuthor() != null) {
        newsList = getMyPostedArticles(filter, currentIdentity);
      } else {
        newsList = getPostedArticles(filter, currentIdentity);
      }
    } else {
      throw new Exception("Unable to build query, filter is null");
    }
    newsList.stream().filter(Objects::nonNull).forEach(news -> {
      news.setCanEdit(canEditNews(news, currentIdentity.getUserId()));
      news.setCanDelete(canDeleteNews(currentIdentity, news.getAuthor(), news.getSpaceId()));
      news.setCanPublish(NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity));
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
            && (StringUtils.isEmpty(news.getAudience()) || news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE) || news.isSpaceMember());
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
                                                                                                       null,
                                                                                                       Long.parseLong(news.getSpaceId())))
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
  public List<News> searchNews(NewsFilter filter,
                               org.exoplatform.social.core.identity.model.Identity currentIdentity) throws Exception {
    return newsSearchConnector.search(currentIdentity, filter).stream().map(articleSearchResult -> {
      try {
        return buildArticle(articleSearchResult.getId());
      } catch (Exception e) {
        LOG.error("Error while building news article", e);
        return null;
      }
    }).toList();
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
  public News scheduleNews(News news, Identity currentIdentity, String newsObjectType) throws Exception {
    Space space = news.getSpaceId() == null ? null : spaceService.getSpaceById(news.getSpaceId());
    if (!canScheduleNews(space, currentIdentity)) {
      throw new IllegalArgumentException("User " + currentIdentity.getUserId() + " is not authorized to schedule news");
    }
    if (newsObjectType.equalsIgnoreCase(NewsObjectType.DRAFT.name())) {
      // Create news article with the publication state STAGED without posting
      // or publishing it ( displayed false news target)
      // it will be posted and published by the news schedule job or the edit
      // scheduling.
      news = createNewsArticlePage(news, currentIdentity.getUserId());
    } else if (newsObjectType.equalsIgnoreCase(ARTICLE.name())) {
      updateNewsArticle(news, currentIdentity, NewsUtils.NewsUpdateType.SCHEDULE.name().toLowerCase());
    }
    if (news != null) {
      if (news.isPublished()) {
        publishNews(news, currentIdentity.getUserId());
      } else {
        unpublishNews(news.getId(), currentIdentity.getUserId());
      }
      // set the url and the space url to the scheduled news
      news.setUrl(NewsUtils.buildNewsArticleUrl(news, currentIdentity.getUserId()));
      news.setSpaceUrl(NewsUtils.buildSpaceUrl(news.getSpaceId()));
      return news;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News unScheduleNews(News news, String pageOwnerId, String articleCreator) throws Exception {
    News existingNews = getNewsArticleById(news.getId());
    if (existingNews != null) {
      Map<String, String> illustrationInfo = getArticleIllustrationInfo(news);

      news.setId(null);
      news.setUploadId(null);
      news = createDraftArticleForNewPage(news, pageOwnerId, articleCreator, System.currentTimeMillis());

      if (!MapUtils.isEmpty(illustrationInfo)) {
        MetadataItem draftMetadataItem =
                                       metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                           new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                                                               news.getId(),
                                                                                                               null,
                                                                                                               Long.parseLong(news.getSpaceId())))
                                                      .stream()
                                                      .findFirst()
                                                      .orElse(null);
        Map<String, String> draftProperties = draftMetadataItem.getProperties();
        if (draftProperties == null) {
          draftProperties = new HashMap<>();
        }
        if (illustrationInfo.get(NEWS_UPLOAD_ID) != null) {
          draftProperties.put(NEWS_UPLOAD_ID, illustrationInfo.get(NEWS_UPLOAD_ID));
        }
        if (illustrationInfo.get(NEWS_ILLUSTRATION_ID) != null) {
          draftProperties.put(NEWS_ILLUSTRATION_ID, illustrationInfo.get(NEWS_ILLUSTRATION_ID));
        }
        draftMetadataItem.setProperties(draftProperties);
        String draftArticleMetadataItemUpdaterIdentityId = identityManager.getOrCreateUserIdentity(articleCreator).getId();
        metadataService.updateMetadataItem(draftMetadataItem, Long.parseLong(draftArticleMetadataItemUpdaterIdentityId));
      }
      deleteArticle(existingNews, articleCreator);
      return buildDraftArticle(news.getId(), articleCreator);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<NewsESSearchResult> search(org.exoplatform.social.core.identity.model.Identity currentIdentity, NewsFilter filter) {
    return newsSearchConnector.search(currentIdentity, filter);
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
      if (!news.isPublished() && StringUtils.equals(news.getPublicationState(), POSTED)
          && !(spaceService.isSuperManager(authenticatedUser) || spaceService.isMember(space, authenticatedUser)
              || isMemberOfsharedInSpaces(news, authenticatedUser))) {
        return false;
      }
      if (news.isPublished() && StringUtils.equals(news.getPublicationState(), POSTED)
          && NewsUtils.SPACE_NEWS_AUDIENCE.equals(news.getAudience())
          && !(spaceService.isMember(space, authenticatedUser) || isMemberOfsharedInSpaces(news, authenticatedUser))) {
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

    if (!canViewNews(news, userIdentity.getRemoteId())) {
      throw new IllegalAccessException("User with id " + userIdentity.getRemoteId() + "doesn't have access to news");
    }
    if (sharedActivityId != null) {
      // update article metadata activities
      NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                         news.getId(),
                                                         null,
                                                         Long.parseLong(news.getSpaceId()));
      MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                                 .stream()
                                                 .findFirst()
                                                 .orElse(null);
      if (metadataItem == null) {
        throw new ObjectNotFoundException("News metadata object with id " + news.getId() + " wasn't found");
      }

      Map<String, String> properties = metadataItem.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
      }
      if (properties.containsKey(NEWS_ACTIVITIES)) {
        String newsActivities = properties.get(NEWS_ACTIVITIES);
        newsActivities = newsActivities.concat(";").concat(space.getId()).concat(":").concat(sharedActivityId);
        properties.put(NEWS_ACTIVITIES, newsActivities);
      } else {
        properties.put(NEWS_ACTIVITIES, space.getId().concat(":").concat(sharedActivityId));
      }

      metadataItem.setProperties(properties);
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(userIdentity.getId()));

      NewsUtils.broadcastEvent(NewsUtils.SHARE_CONTENT_ATTACHMENTS,
                               Map.of(NEWS_ATTACHMENTS_IDS,
                                      getArticleAttachmentIdsToShare(news,
                                                                     Long.parseLong(space.getId()),
                                                                     Long.parseLong(newsPageObject.getId())),
                                      ARTICLE_CONTENT,
                                      news.getBody()),
                               space);
      NewsUtils.broadcastEvent(NewsUtils.SHARE_NEWS, userIdentity.getRemoteId(), news);
    }

  }

  private List<String> getArticleAttachmentIdsToShare(News article, long spaceId, long articlePageId) {
    List<String> attachmentIds = new ArrayList<>();
    PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(articlePageId, null);
    NewsPageVersionObject newsPageVersionObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                            pageVersion.getId(),
                                                                            null,
                                                                            spaceId);
    List<MetadataItem> newsPageVersionMetadataItems = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                          newsPageVersionObject);
    Map<String, String> newsPageVersionMetadataItemProperties = newsPageVersionMetadataItems.get(0).getProperties();
    if (!MapUtils.isEmpty(newsPageVersionMetadataItems.get(0).getProperties())) {

      if (newsPageVersionMetadataItemProperties.containsKey(NEWS_ATTACHMENTS_IDS)
          && newsPageVersionMetadataItemProperties.get(NEWS_ATTACHMENTS_IDS) != null) {
        attachmentIds.addAll(List.of(newsPageVersionMetadataItemProperties.get(NEWS_ATTACHMENTS_IDS).split(";")));
      }
    }
    return attachmentIds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News createDraftArticleForNewPage(News draftArticle,
                                           String pageOwnerId,
                                           String draftArticleCreator,
                                           long creationDate) throws Exception {
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
      draftArticlePage = noteService.createDraftForNewPage(draftArticlePage, creationDate);

      draftArticle.setId(draftArticlePage.getId());
      draftArticle.setCreationDate(draftArticlePage.getCreatedDate());
      draftArticle.setUpdateDate(draftArticlePage.getUpdatedDate());
      Space draftArticleSpace = spaceService.getSpaceByGroupId(pageOwnerId);
      draftArticle.setSpaceId(draftArticleSpace.getId());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public News createNewsArticlePage(News newsArticle, String newsArticleCreator) throws Exception {
    // get the news draft article from the news model before setting the news
    // article id to the news model
    String draftNewsId = newsArticle.getId();

    Identity poster = NewsUtils.getUserIdentity(newsArticleCreator);
    Space space = spaceService.getSpaceById(newsArticle.getSpaceId());
    String pageOwnerId = space.getGroupId();

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
      Page newsArticlePage = new Page();
      newsArticlePage.setName(newsArticle.getName());
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
                                                                                           null,
                                                                                           Long.parseLong(space.getId()));
        String newsArticleMetadataItemCreatorIdentityId = identityManager.getOrCreateUserIdentity(newsArticleCreator).getId();
        Map<String, String> newsArticleVersionMetadataItemProperties = new HashMap<>();

        NewsDraftObject newsDraftObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                              draftNewsId,
                                                              null,
                                                              Long.parseLong(space.getId()));
        MetadataItem draftMetadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsDraftObject)
                                                        .stream()
                                                        .findFirst()
                                                        .orElse(null);

        if (draftMetadataItem != null && draftMetadataItem.getProperties() != null
            && !draftMetadataItem.getProperties().isEmpty()) {
          if (draftMetadataItem.getProperties().containsKey(NEWS_UPLOAD_ID)) {
            newsArticleVersionMetadataItemProperties.put(NEWS_UPLOAD_ID, draftMetadataItem.getProperties().get(NEWS_UPLOAD_ID));
          }
          if (draftMetadataItem.getProperties().containsKey(NEWS_ILLUSTRATION_ID)) {
            newsArticleVersionMetadataItemProperties.put(NEWS_ILLUSTRATION_ID,
                                                         draftMetadataItem.getProperties().get(NEWS_ILLUSTRATION_ID));
            setArticleIllustration(newsArticle,
                                   Long.parseLong(newsArticleVersionMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)),
                                   ARTICLE.name().toLowerCase());
          }
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
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                           newsArticlePage.getId(),
                                                           null,
                                                           Long.parseLong(space.getId()));
        Map<String, String> newsPageProperties = new HashMap<>();
        if (StringUtils.isNotEmpty(newsArticle.getAudience())) {
          newsPageProperties.put(NEWS_AUDIENCE, newsArticle.getAudience());
        }
        if (StringUtils.isNotEmpty(newsArticle.getSchedulePostDate())) {
          setSchedulePostDate(newsArticle, newsPageProperties);
        }
        if (StringUtils.isNotEmpty(newsArticle.getPublicationState())) {
          newsPageProperties.put(NEWS_PUBLICATION_STATE, newsArticle.getPublicationState());
        }
        newsPageProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(newsArticle.isActivityPosted()));
        newsPageProperties.put(PUBLISHED, String.valueOf(newsArticle.isPublished()));
        newsPageProperties.put(NEWS_DELETED, String.valueOf(newsArticlePage.isDeleted()));
        metadataService.createMetadataItem(newsPageObject,
                                           NEWS_METADATA_KEY,
                                           newsPageProperties,
                                           Long.parseLong(newsArticleMetadataItemCreatorIdentityId));
        // delete the draft
        deleteDraftArticle(draftNewsId, poster.getUserId(), newsArticle.getIllustration() == null);
        return newsArticle;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public News createDraftForExistingPage(News draftArticle,
                                         String updater,
                                         Page targetArticlePage,
                                         long creationDate) throws Exception {
    DraftPage draftArticlePage = new DraftPage();
    draftArticlePage.setNewPage(false);
    draftArticlePage.setTargetPageId(targetArticlePage.getId());
    draftArticlePage.setTitle(draftArticle.getTitle());
    draftArticlePage.setContent(draftArticle.getBody());
    draftArticlePage.setParentPageId(targetArticlePage.getParentPageId());
    draftArticlePage.setAuthor(draftArticle.getAuthor());
    draftArticlePage.setLang(null);

    draftArticlePage = noteService.createDraftForExistPage(draftArticlePage, targetArticlePage, null, creationDate, updater);

    draftArticle.setDraftUpdateDate(draftArticlePage.getCreatedDate());
    draftArticle.setDraftUpdater(draftArticlePage.getAuthor());
    draftArticle.setId(draftArticlePage.getId());
    NewsLatestDraftObject latestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                        draftArticlePage.getId(),
                                                                        targetArticlePage.getId(),
                                                                        Long.parseLong(draftArticle.getSpaceId()));

    Map<String, String> draftArticleMetadataItemProperties = new HashMap<>();
    if (StringUtils.isNotEmpty(draftArticle.getSummary())) {
      draftArticleMetadataItemProperties.put(NEWS_SUMMARY, draftArticle.getSummary());
    }
    draftArticleMetadataItemProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(draftArticle.isActivityPosted()));
    // check if the article has an illustration to link it to the created draft
    PageVersion latestPageVersion =
                                  noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(targetArticlePage.getId()), null);
    if (latestPageVersion != null) {
      // fetch the version related metadata item
      MetadataItem pageVersionMetadataItem =
                                           metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                               new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                                                         latestPageVersion.getId(),
                                                                                                                         null,
                                                                                                                         Long.parseLong(draftArticle.getSpaceId())))
                                                          .stream()
                                                          .findFirst()
                                                          .orElse(null);
      Map<String, String> pageVersionMetadataItemProperties = pageVersionMetadataItem.getProperties();
      if (pageVersionMetadataItemProperties != null && !pageVersionMetadataItemProperties.isEmpty()) {
        if (pageVersionMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)) {
          draftArticleMetadataItemProperties.put(NEWS_ILLUSTRATION_ID,
                                                 pageVersionMetadataItemProperties.get(NEWS_ILLUSTRATION_ID));
        }
        if (pageVersionMetadataItemProperties.containsKey(NEWS_UPLOAD_ID)) {
          draftArticleMetadataItemProperties.put(NEWS_UPLOAD_ID, pageVersionMetadataItemProperties.get(NEWS_UPLOAD_ID));
        }
      }
    }
    String draftArticleMetadataItemCreatorIdentityId = identityManager.getOrCreateUserIdentity(updater).getId();
    metadataService.createMetadataItem(latestDraftObject,
                                       NEWS_METADATA_KEY,
                                       draftArticleMetadataItemProperties,
                                       Long.parseLong(draftArticleMetadataItemCreatorIdentityId));
    return draftArticle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteArticle(News news, String articleCreator) throws Exception {
    Page articlePage = noteService.getNoteById(news.getId());
    if (articlePage != null && !articlePage.isDeleted()) {
      boolean hasDraft = true;
      while (hasDraft) {
        try {
          DraftPage latestDraftPage = noteService.getLatestDraftOfPage(articlePage, articleCreator);
          if (latestDraftPage != null) {
            deleteDraftArticle(latestDraftPage.getId(), articleCreator, true);
          } else {
            hasDraft = false;
          }
        } catch (Exception exception) {
          hasDraft = false;
        }
      }
      boolean isDeleted = noteService.deleteNote(articlePage.getWikiType(), articlePage.getWikiOwner(), articlePage.getName());
      if (isDeleted) {
        NewsPageObject newsPageMetadataObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                                   news.getId(),
                                                                   null,
                                                                   Long.parseLong(news.getSpaceId()));
        MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageMetadataObject)
                                                   .stream()
                                                   .findFirst()
                                                   .orElse(null);
        if (metadataItem != null) {
          Map<String, String> properties = metadataItem.getProperties();
          properties.put(NEWS_DELETED, String.valueOf(true));
          metadataItem.setProperties(properties);
          String currentIdentityId = identityManager.getOrCreateUserIdentity(articleCreator).getId();
          metadataService.updateMetadataItem(metadataItem, Long.parseLong(currentIdentityId));
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteDraftArticle(String draftArticleId, String draftArticleCreator, boolean deleteIllustration) throws Exception {
    DraftPage draftArticlePage = noteService.getDraftNoteById(draftArticleId, draftArticleCreator);
    if (draftArticlePage != null) {
      noteService.removeDraftById(draftArticlePage.getId());
      Space draftArticleSpace = spaceService.getSpaceByGroupId(draftArticlePage.getWikiOwner());
      MetadataObject draftArticleMetaDataObject =
                                                new MetadataObject(draftArticlePage.getTargetPageId() != null ? NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE
                                                                                                              : NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                   draftArticlePage.getId(),
                                                                   draftArticlePage.getTargetPageId(),
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

  private News buildDraftArticle(String draftArticleId, String currentUserId) throws Exception {
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
      processPageContent(draftArticlePage, draftArticle);
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
        draftArticle.setSpaceUrl(NewsUtils.buildSpaceUrl(draftArticleSpace.getId()));
      }

      draftArticle.setUrl(NewsUtils.buildDraftUrl(draftArticlePage));
      MetadataObject draftArticleMetaDataObject;
      if (draftArticlePage.getTargetPageId() == null) {
        draftArticleMetaDataObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                         draftArticle.getId(),
                                                         null,
                                                         Long.parseLong(draftArticleSpace.getId()));
      } else {
        draftArticleMetaDataObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                               draftArticle.getId(),
                                                               draftArticlePage.getTargetPageId(),
                                                               Long.parseLong(draftArticleSpace.getId()));
      }
      MetadataItem draftArticleMetadataItem = metadataService
                                                             .getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                  draftArticleMetaDataObject)
                                                             .stream()
                                                             .findFirst()
                                                             .orElse(null);
      buildDraftArticleProperties(draftArticle, draftArticleMetadataItem);
      if (draftArticlePage.getTargetPageId() != null) {
        draftArticle.setPublicationDate(noteService.getNoteById(draftArticlePage.getTargetPageId()).getCreatedDate());
      }
      return draftArticle;
    }
    return null;
  }

  private void buildArticleVersionProperties(News article, List<MetadataItem> newsPageVersionMetadataItems) {
    if (!CollectionUtils.isEmpty(newsPageVersionMetadataItems)) {
      Map<String, String> newsPageVersionMetadataItemProperties = newsPageVersionMetadataItems.get(0).getProperties();
      if (!MapUtils.isEmpty(newsPageVersionMetadataItemProperties)) {
        if (newsPageVersionMetadataItemProperties.containsKey(NEWS_SUMMARY)) {
          article.setSummary(newsPageVersionMetadataItemProperties.get(NEWS_SUMMARY));
        }
        if (newsPageVersionMetadataItemProperties.containsKey(NEWS_ILLUSTRATION_ID)
            && newsPageVersionMetadataItemProperties.get(NEWS_ILLUSTRATION_ID) != null) {
          setArticleIllustration(article,
                                 Long.valueOf(newsPageVersionMetadataItemProperties.get(NEWS_ILLUSTRATION_ID)),
                                 ARTICLE.name().toLowerCase());
        }
        if (newsPageVersionMetadataItemProperties.containsKey(NEWS_UPLOAD_ID)
            && newsPageVersionMetadataItemProperties.get(NEWS_UPLOAD_ID) != null) {
          article.setUploadId(newsPageVersionMetadataItemProperties.get(NEWS_UPLOAD_ID));
        }
        if (newsPageVersionMetadataItemProperties.containsKey(NEWS_ATTACHMENTS_IDS)
            && newsPageVersionMetadataItemProperties.get(NEWS_ATTACHMENTS_IDS) != null) {
          List<String> attachmentsIds = List.of(newsPageVersionMetadataItemProperties.get(NEWS_ATTACHMENTS_IDS).split(";"));
          article.setAttachmentsIds(attachmentsIds);
        }
      }
    }
  }

  private void buildArticleProperties(News article,
                                      Page articlePage,
                                      String currentUsername,
                                      MetadataItem metadataItem) throws Exception {
    if (metadataItem != null && !MapUtils.isEmpty(metadataItem.getProperties())) {
      Map<String, String> properties = metadataItem.getProperties();
      if (properties.containsKey(NEWS_ACTIVITIES) && properties.get(NEWS_ACTIVITIES) != null) {
        String[] activities = properties.get(NEWS_ACTIVITIES).split(";");
        StringBuilder memberSpaceActivities = new StringBuilder();
        String newsActivityId = activities[0].split(":")[1];
        article.setActivityId(newsActivityId);
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
        article.setActivities(memberSpaceActivities.toString());
        article.setSharedInSpacesList(sharedInSpacesList);
      }
      if (properties.containsKey(NEWS_AUDIENCE) && StringUtils.isNotEmpty(properties.get(NEWS_AUDIENCE))) {
        article.setAudience(properties.get(NEWS_AUDIENCE));
      }
      if (properties.containsKey(SCHEDULE_POST_DATE) && StringUtils.isNotEmpty(properties.get(SCHEDULE_POST_DATE))) {
        article.setSchedulePostDate(properties.get(SCHEDULE_POST_DATE));
      }
      if (properties.containsKey(NEWS_PUBLICATION_STATE) && StringUtils.isNotEmpty(properties.get(NEWS_PUBLICATION_STATE))) {
        article.setPublicationState(properties.get(NEWS_PUBLICATION_STATE));
      }
      if (properties.containsKey(PUBLISHED) && StringUtils.isNotEmpty(properties.get(PUBLISHED))) {
        article.setPublished(Boolean.parseBoolean(properties.get(PUBLISHED)));
      }
      if (properties.containsKey(NEWS_VIEWS) && StringUtils.isNotEmpty(properties.get(NEWS_VIEWS))) {
        article.setViewsCount(Long.parseLong(properties.get(NEWS_VIEWS)));
      }
      if (properties.containsKey(NEWS_ACTIVITY_POSTED)) {
        article.setActivityPosted(Boolean.parseBoolean(properties.get(NEWS_ACTIVITY_POSTED)));
      } else {
        article.setActivityPosted(false);
      }
    }
  }

  private void buildDraftArticleProperties(News draftArticle, MetadataItem metadataItem) {
    if (metadataItem != null) {
      Map<String, String> draftArticleMetadataItemProperties = metadataItem.getProperties();
      if (!MapUtils.isEmpty(draftArticleMetadataItemProperties)) {
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
      if (metadataItem.getParentObjectId() != null) {
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                           metadataItem.getParentObjectId(),
                                                           null,
                                                           metadataItem.getSpaceId());
        MetadataItem parentMetadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                                         .get(0);
        Map<String, String> properties = parentMetadataItem.getProperties();
        if (properties.containsKey(NEWS_ACTIVITIES) && properties.get(NEWS_ACTIVITIES) != null) {
          String[] activities = properties.get(NEWS_ACTIVITIES).split(";");
          String newsActivityId = activities[0].split(":")[1];
          draftArticle.setActivityId(newsActivityId);
        }
        if (properties.containsKey(NEWS_VIEWS) && StringUtils.isNotEmpty(properties.get(NEWS_VIEWS))) {
          draftArticle.setViewsCount(Long.parseLong(properties.get(NEWS_VIEWS)));
        }
      }
    }
  }

  private List<News> getPublishedArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setSortField(filter.getOrder());
    metadataFilter.setMetadataProperties(Map.of(PUBLISHED,
                                                "true",
                                                NEWS_AUDIENCE,
                                                NewsUtils.SPACE_NEWS_AUDIENCE,
                                                NEWS_DELETED,
                                                "false",
                                                NEWS_PUBLICATION_STATE,
                                                POSTED));
    metadataFilter.setMetadataSpaceIds(NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces()));
    metadataFilter.setCombinedMetadataProperties(Map.of(PUBLISHED,
                                                        "true",
                                                        NEWS_AUDIENCE,
                                                        NewsUtils.ALL_NEWS_AUDIENCE,
                                                        NEWS_DELETED,
                                                        "false",
                                                        NEWS_PUBLICATION_STATE,
                                                        POSTED));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(article -> {
                            try {
                              return buildArticle(article.getObjectId());
                            } catch (Exception e) {
                              LOG.error("Error while building published news article", e);
                              return null;
                            }
                          })
                          .toList();
  }

  private List<News> getPostedArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setMetadataProperties(Map.of(NEWS_PUBLICATION_STATE, POSTED, NEWS_DELETED, "false"));
    metadataFilter.setMetadataSpaceIds(NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces()));
    metadataFilter.setSortField(filter.getOrder());
    metadataFilter.setCombinedMetadataProperties(Map.of(PUBLISHED,
                                                        "true",
                                                        NEWS_AUDIENCE,
                                                        NewsUtils.ALL_NEWS_AUDIENCE,
                                                        NEWS_DELETED,
                                                        "false",
                                                        NEWS_PUBLICATION_STATE,
                                                        POSTED));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(article -> {
                            try {
                              return buildArticle(article.getObjectId());
                            } catch (Exception e) {
                              LOG.error("Error while building news article", e);
                              return null;
                            }
                          })
                          .toList();
  }

  private List<News> getScheduledArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setMetadataProperties(Map.of(NEWS_PUBLICATION_STATE, STAGED, NEWS_DELETED, "false"));
    metadataFilter.setSortField(filter.getOrder());
    metadataFilter.setMetadataSpaceIds(NewsUtils.getAllowedScheduledNewsSpacesIds(currentIdentity, filter.getSpaces()));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(article -> {
                            try {
                              return buildArticle(article.getObjectId());
                            } catch (Exception e) {
                              LOG.error("Error while building news article", e);
                              return null;
                            }
                          })
                          .toList();
  }

  private List<News> getMyPostedArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setCreatorId(Long.parseLong(identityManager.getOrCreateUserIdentity(filter.getAuthor()).getId()));
    metadataFilter.setMetadataProperties(Map.of(NEWS_PUBLICATION_STATE, POSTED, NEWS_DELETED, "false"));
    metadataFilter.setMetadataSpaceIds(NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces()));
    metadataFilter.setSortField(filter.getOrder());
    metadataFilter.setCombinedMetadataProperties(Map.of(PUBLISHED,
                                                        "true",
                                                        NEWS_AUDIENCE,
                                                        NewsUtils.ALL_NEWS_AUDIENCE,
                                                        NEWS_DELETED,
                                                        "false",
                                                        NEWS_PUBLICATION_STATE,
                                                        POSTED));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(article -> {
                            try {
                              return buildArticle(article.getObjectId());
                            } catch (Exception e) {
                              LOG.error("Error while building news article", e);
                              return null;
                            }
                          })
                          .toList();
  }

  private List<News> buildDraftArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setSortField(filter.getOrder());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_DRAFT_OBJECT_TYPE, NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE));
    metadataFilter.setMetadataSpaceIds(NewsUtils.getAllowedDraftArticleSpaceIds(currentIdentity, filter.getSpaces()));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(draftArticle -> {
                            try {
                              News draft = buildDraftArticle(draftArticle.getObjectId(), currentIdentity.getUserId());
                              if (draft != null && draftArticle.getParentObjectId() != null) {
                                draft.setId(draftArticle.getParentObjectId());
                              }
                              return draft;
                            } catch (IllegalAccessException e) {
                              LOG.error("User with id " + currentIdentity.getUserId() + " not authorized to view news", e);
                              return null;
                            } catch (Exception e) {
                              LOG.error("Error while building new draft article", e);
                              return null;
                            }
                          })
                          .toList();
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
      if (articleIllustrationFileItem != null && !articleIllustrationFileItem.getFileInfo().isDeleted()) {
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
    Space space = spaceId == null ? null : spaceService.getSpaceById(spaceId);
    if (space == null) {
      return false;
    }
    return spaceService.canRedactOnSpace(space, currentIdentity);
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
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                           newsPage.getId(),
                                                           null,
                                                           Long.parseLong(news.getSpaceId()));
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
          Date updateDate = Calendar.getInstance().getTime();
          metadataItem.setUpdatedDate(updateDate.getTime());
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

  private News updateNewsArticle(News news, Identity updater, String newsUpdateType) throws Exception {
    Page existingPage = noteService.getNoteById(news.getId());
    if (existingPage != null) {
      if (newsUpdateType.equals(CONTENT.name().toLowerCase())) {
        existingPage.setTitle(news.getTitle());
        existingPage.setContent(news.getBody());
      }
      existingPage.setUpdatedDate(Calendar.getInstance().getTime());
      existingPage = noteService.updateNote(existingPage);
      news.setUpdateDate(existingPage.getUpdatedDate());
      news.setUpdater(existingPage.getAuthor());
      news.setUpdaterFullName(existingPage.getAuthorFullName());

      String newsArticleUpdaterIdentityId = identityManager.getOrCreateUserIdentity(updater.getUserId()).getId();

      // update the metadata item page
      NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                         news.getId(),
                                                         null,
                                                         Long.parseLong(news.getSpaceId()));
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
          String existingSchedulePostDate = newsPageProperties.getOrDefault(SCHEDULE_POST_DATE, null);
          if (existingSchedulePostDate == null || !existingSchedulePostDate.equals(news.getSchedulePostDate())) {
            setSchedulePostDate(news, newsPageProperties);
          }
        }
        if (StringUtils.isNotEmpty(news.getPublicationState())) {
          newsPageProperties.put(NEWS_PUBLICATION_STATE, news.getPublicationState());
        }
        newsPageProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(news.isActivityPosted()));
        existingPageMetadataItem.setProperties(newsPageProperties);
        Date updateDate = Calendar.getInstance().getTime();
        existingPageMetadataItem.setUpdatedDate(updateDate.getTime());
        metadataService.updateMetadataItem(existingPageMetadataItem, Long.parseLong(newsArticleUpdaterIdentityId));
      } else {
        throw new ObjectNotFoundException("No such news article metadata item exists with id " + news.getId());
      }

      // create the version
      if (newsUpdateType.equals(CONTENT.name().toLowerCase())) {
        noteService.createVersionOfNote(existingPage, updater.getUserId());
        PageVersion createdVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(existingPage.getId()), null);
        // create the version metadata item
        NewsPageVersionObject newsArticleVersionMetaDataObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                           createdVersion.getId(),
                                                                                           null,
                                                                                           Long.parseLong(news.getSpaceId()));
        Map<String, String> newsArticleVersionMetadataItemProperties = new HashMap<>();

        String draftNewsId = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(existingPage.getId()),
                                                                                      updater.getUserId(),
                                                                                      null)
                                        .getId();

        NewsLatestDraftObject newsLatestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                                draftNewsId,
                                                                                existingPage.getId(),
                                                                                Long.parseLong(news.getSpaceId()));
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

      }
      // remove the draft
      if (newsUpdateType.equalsIgnoreCase(CONTENT.name())) {
        DraftPage draftPage = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(existingPage.getId()),
                                                                                       updater.getUserId(),
                                                                                       null);
        deleteDraftArticle(draftPage.getId(), updater.getUserId(), news.getIllustration() == null);
      }
      return news;
    }
    return null;
  }

  private News buildArticle(String newsId) throws Exception {
    if (StringUtils.isNumeric(newsId)) {
      Page articlePage = noteService.getNoteById(newsId);
      Identity userIdentity = getCurrentIdentity();
      String currentUsername = userIdentity == null ? null : userIdentity.getUserId();
      if (articlePage != null) {
        Space space = spaceService.getSpaceByGroupId(articlePage.getWikiOwner());
        News news = new News();
        news.setId(articlePage.getId());
        news.setCreationDate(articlePage.getCreatedDate());
        news.setAuthor(articlePage.getAuthor());
        news.setUpdater(articlePage.getAuthor());

        // fetch related metadata item properties
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                           articlePage.getId(),
                                                           null,
                                                           Long.parseLong(space.getId()));
        MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject).get(0);
        news.setSpaceId(space.getId());
        news.setSpaceAvatarUrl(space.getAvatarUrl());
        news.setSpaceDisplayName(space.getDisplayName());
        boolean hiddenSpace = space.getVisibility().equals(Space.HIDDEN) && !spaceService.isMember(space, currentUsername)
            && !spaceService.isSuperManager(currentUsername);
        news.setHiddenSpace(hiddenSpace);
        boolean isSpaceMember = spaceService.isSuperManager(currentUsername) || spaceService.isMember(space, currentUsername);
        news.setSpaceMember(isSpaceMember);
        if (StringUtils.isNotEmpty(space.getGroupId())) {
          news.setSpaceUrl(NewsUtils.buildSpaceUrl(space.getId()));
        }

        org.exoplatform.social.core.identity.model.Identity identity =
                                                                     identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                         news.getAuthor());
        if (identity != null && identity.getProfile() != null) {
          news.setAuthorDisplayName(identity.getProfile().getFullName());
          news.setAuthorAvatarUrl(identity.getProfile().getAvatarUrl());
        }

        buildArticleProperties(news, articlePage, currentUsername, metadataItem);
        news.setDeleted(articlePage.isDeleted());
        news.setUrl(NewsUtils.buildNewsArticleUrl(news, currentUsername));
        news.setPublicationDate(articlePage.getCreatedDate());
        news.setUpdateDate(new Date(metadataItem.getUpdatedDate()));
        // fetch the last version of the given lang
        PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(articlePage.getId()), null);
        news.setTitle(pageVersion.getTitle());
        processPageContent(pageVersion, news);
        news.setUpdaterFullName(pageVersion.getAuthorFullName());

        NewsPageVersionObject newsPageVersionObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                pageVersion.getId(),
                                                                                null,
                                                                                Long.parseLong(space.getId()));
        List<MetadataItem> newsPageVersionMetadataItems =
                                                        metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                            newsPageVersionObject);
        buildArticleVersionProperties(news, newsPageVersionMetadataItems);
        return news;
      }
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
      news = createDraftForExistingPage(news, updater, existingPage, System.currentTimeMillis());
    } else {
      news = updateDraftForExistingPage(news, updater, existingPage, draftPage);
    }
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
                                                                          page.getId(),
                                                                          Long.parseLong(news.getSpaceId()));

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

  private void processPageContent(Page page, News news) throws Exception {
    String portalOwner = CommonsUtils.getCurrentPortalOwner();
    String body = page.getContent();
    String sanitizedBody = HTMLSanitizer.sanitize(body);
    sanitizedBody = sanitizedBody.replaceAll(HTML_AT_SYMBOL_ESCAPED_PATTERN, HTML_AT_SYMBOL_PATTERN);
    news.setBody(MentionUtils.substituteUsernames(portalOwner, sanitizedBody));
    news.setOriginalBody(sanitizedBody);
  }

  private void setSchedulePostDate(News news, Map<String, String> newsProperties) throws ParseException {
    String schedulePostDate = news.getSchedulePostDate();
    ZoneId userTimeZone = StringUtils.isBlank(news.getTimeZoneId()) ? ZoneOffset.UTC : ZoneId.of(news.getTimeZoneId());
    String offsetTimeZone = String.valueOf(OffsetTime.now(userTimeZone).getOffset()).replace(":", "");
    schedulePostDate = schedulePostDate.concat(" ").concat(offsetTimeZone);

    // Create a SimpleDateFormat object to parse the scheduled post date given
    // by the front
    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss" + "Z");
    Calendar startPublishedDate = Calendar.getInstance();
    startPublishedDate.setTime(format.parse(schedulePostDate));

    // create a SimpleDateFormat to format the parsed date and then save it as
    // string
    SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    defaultFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    String startPublishedDateString = defaultFormat.format(startPublishedDate.getTime());

    newsProperties.put(SCHEDULE_POST_DATE, startPublishedDateString);
  }

  private boolean isSameIllustration(String newsDraftId, String newsArticleId) throws ObjectNotFoundException,
                                                                               FileStorageException {

    News news = getNewsArticleById(newsArticleId);
    if (news == null || news.getIllustration() == null) {
      return false;
    }
    NewsLatestDraftObject draftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                  newsDraftId,
                                                                  newsArticleId,
                                                                  Long.parseLong(news.getSpaceId()));
    MetadataItem draftMetadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, draftObject)
                                                    .stream()
                                                    .findFirst()
                                                    .orElse(null);
    if (draftMetadataItem == null) {
      throw new ObjectNotFoundException("Metadata items not found for news draft " + newsDraftId);
    }

    // Retrieve illustration ID from metadata
    Map<String, String> draftProperties = draftMetadataItem.getProperties();
    if (draftProperties != null) {
      String draftIllustrationId = draftProperties.getOrDefault(NEWS_ILLUSTRATION_ID, null);
      FileItem fileItem = fileService.getFile(Long.parseLong(draftIllustrationId));
      if (fileItem != null) {
        return Arrays.equals(fileItem.getAsByte(), news.getIllustration());
      }
    }
    return false;
  }

  private News postScheduledArticle(News news) throws ObjectNotFoundException {
    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                       news.getId(),
                                                       null,
                                                       Long.parseLong(news.getSpaceId()));
    MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                               .stream()
                                               .findFirst()
                                               .orElse(null);
    if (metadataItem == null) {
      throw new ObjectNotFoundException("Metadata items not found for news " + news.getId());
    }
    Map<String, String> properties = metadataItem.getProperties();
    if (properties != null) {
      properties.put(NEWS_PUBLICATION_STATE, POSTED);
      properties.remove(SCHEDULE_POST_DATE);
      String poster = identityManager.getOrCreateUserIdentity(news.getAuthor()).getId();
      Date updateDate = Calendar.getInstance().getTime();
      metadataItem.setUpdatedDate(updateDate.getTime());
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(poster));
      news.setSchedulePostDate(null);
      return news;
    }
    if (!news.isPublished()) {
      newsTargetingService.deleteNewsTargets(news);
    }
    return null;
  }

  private Map<String, String> getArticleIllustrationInfo(News news) {
    Map<String, String> illustrationInfo = new HashMap<>();
    if (news.getIllustration() != null) {
      PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(news.getId()), null);
      if (pageVersion != null) {
        MetadataItem metadataItem =
                                  metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                      new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                                                pageVersion.getId(),
                                                                                                                null,
                                                                                                                Long.parseLong(news.getSpaceId())))
                                                 .stream()
                                                 .findFirst()
                                                 .orElse(null);
        if (metadataItem != null && metadataItem.getProperties() != null && !metadataItem.getProperties().isEmpty()) {
          illustrationInfo.put(NEWS_ILLUSTRATION_ID, metadataItem.getProperties().get(NEWS_ILLUSTRATION_ID));
          illustrationInfo.put(NEWS_UPLOAD_ID, metadataItem.getProperties().get(NEWS_UPLOAD_ID));
        }
      }
    }
    return illustrationInfo;
  }
}
