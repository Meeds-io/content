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

import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static io.meeds.content.news.utils.NewsUtils.NewsObjectType.LATEST_DRAFT;
import static io.meeds.content.news.utils.NewsUtils.NewsUpdateType.CONTENT_AND_TITLE;
import static io.meeds.content.news.utils.NewsUtils.NewsUpdateType.PAGE_REFERENCE;
import static io.meeds.content.news.utils.NewsUtils.NewsUpdateType.POSTING_AND_PUBLISHING;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.localization.LocalizationFilter;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
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
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.plugin.WikiDraftPageAttachmentPlugin;

import io.meeds.content.news.model.ArticleTarget;
import io.meeds.content.news.model.ContentPublishEvent;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.NewsDraftObject;
import io.meeds.content.news.model.NewsLatestDraftObject;
import io.meeds.content.news.model.NewsPageObject;
import io.meeds.content.news.model.NewsPageVersionObject;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.notification.plugin.MentionInNewsNotificationPlugin;
import io.meeds.content.news.notification.plugin.PostNewsNotificationPlugin;
import io.meeds.content.news.notification.plugin.PublishNewsNotificationPlugin;
import io.meeds.content.news.notification.utils.NotificationConstants;
import io.meeds.content.news.notification.utils.NotificationUtils;
import io.meeds.content.news.plugin.NewsPageAttachmentPlugin;
import io.meeds.content.news.rest.model.NewsSearchResultEntity;
import io.meeds.content.news.search.NewsESSearchResult;
import io.meeds.content.news.search.NewsIndexingServiceConnector;
import io.meeds.content.news.search.NewsSearchConnector;
import io.meeds.content.news.utils.EntityBuilder;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.news.utils.NewsUtils.NewsObjectType;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryService;
import lombok.SneakyThrows;

@Service
public class NewsService {

  public static final String       NEWS_ARTICLES_ROOT_NOTE_PAGE_NAME      = "Articles";

  private static final String      HTML_AT_SYMBOL_PATTERN                 = "@";

  private static final String      HTML_AT_SYMBOL_ESCAPED_PATTERN         = "&#64;";

  public static final MetadataType NEWS_METADATA_TYPE                     = new MetadataType(1000, "news");

  public static final String       NEWS_METADATA_NAME                     = "news";

  public static final String       NEWS_METADATA_DRAFT_OBJECT_TYPE        = "newsDraftPage";

  /** The Constant PUBLISHED. */
  public static final String       PUBLISHED                              = "published";

  /** The Constant PUBLISHER. */
  public static final String       PUBLISHER                              = "publisher";

  /** The Constant POSTED. */
  public static final String       POSTED                                 = "posted";

  /** The Constant DRAFT. */
  public static final String       DRAFT                                  = "draft";

  /** The Constant STAGED. */
  public static final String       STAGED                                 = "staged";

  /** The Constant AUDIENCE. */
  public static final String       NEWS_AUDIENCE                          = "audience";

  /** The Constant DELETED. */
  public static final String       NEWS_DELETED                           = "deleted";

  /** The Constant NEWS_ID. */
  public static final String       NEWS_ID                                = "newsId";

  /** The Constant SCHEDULE_POST_DATE. */
  public static final String       SCHEDULE_POST_DATE                     = "schedulePostDate";

  /** The Constant NEWS_ACTIVITIES. */
  public static final String       NEWS_ACTIVITIES                        = "activities";

  /** The Constant NEWS_PUBLICATION_STATE. */
  public static final String       NEWS_PUBLICATION_STATE                 = "publicationState";

  /** The Constant NEWS_ACTIVITY_POSTED. */
  public static final String       NEWS_ACTIVITY_POSTED                   = "activityPosted";

  /** The Constant NEWS_ACTIVITY_CATEGORIES. */
  public static final String       NEWS_ACTIVITY_CATEGORIES               = "activityCategories";

  /** The Constant NEWS_METADATA_PAGE_OBJECT_TYPE. */
  public static final String       NEWS_METADATA_PAGE_OBJECT_TYPE         = "newsPage";

  /** The Constant NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE. */
  public static final String       NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE = "newsPageVersion";

  /** The Constant NEWS_VIEWERS. */
  public static final String       NEWS_VIEWERS                           = "viewers";

  /** The Constant NEWS_VIEWS. */
  public static final String       NEWS_VIEWS                             = "viewsCount";

  /** The Constant NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE. */
  public static final String       NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE = "newsLatestDraftPage";

  public static final String       EXTERNAL_PAGE                          = "externalPage";

  public static final String       PAGE_REFERRED                          = "pageReferred";

  public static final String       DE_REFER_PAGE_ID                       = "deReferPageId";

  public static final String       ARTICLE_CONTENT                        = "content";

  public static final String       UNPUBLISH_SCHEDULED                    = "unpublishScheduled";

  public static final String       UNPUBLISH_SCHEDULED_DATE               = "unpublishScheduledDate";

  public static final MetadataKey  NEWS_METADATA_KEY                      =
                                                     new MetadataKey(NEWS_METADATA_TYPE.getName(), NEWS_METADATA_NAME, 0);

  private static final Log         LOG                                    = ExoLogger.getLogger(NewsService.class);

  @Autowired
  private SpaceService             spaceService;

  @Autowired
  private NoteService              noteService;

  @Autowired
  private MetadataService          metadataService;

  @Autowired
  private NewsTargetingService     newsTargetingService;

  @Autowired
  private IndexingService          indexingService;

  @Autowired
  private IdentityManager          identityManager;

  @Autowired
  private ActivityManager          activityManager;

  @Autowired
  private WikiService              wikiService;

  @Autowired
  private NewsSearchConnector      newsSearchConnector;

  @Autowired
  private UserACL                  userAcl;

  /**
   * Create and publish a News containing the data. If the given News has an id
   * and that a draft already exists with this id, the draft is updated and
   * published.
   * 
   * @param news The news to create
   * @param currentIdentity
   * @return created News object
   * @throws Exception when error
   */
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
        createdNews = unScheduleNews(news, space, currentIdentity.getUserId());
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

  /**
   * Create and publish a News A news containing the data. If the given News has
   * an id and that a draft already exists with this id, the draft is updated
   * and published.
   * 
   * @param news The news to post
   * @param poster the Poster of the News
   * @return The posted News
   * @throws Exception when error
   */
  public News postNews(News news, String poster) throws Exception {
    if (news == null || poster == null || poster.isBlank()) {
      throw new IllegalArgumentException("News and poster cannot be null or empty.");
    }

    if (STAGED.equals(news.getPublicationState()) || news.getSchedulePostDate() != null) {
      news = postScheduledArticle(news);
    } else if (!news.isFromDraft() && noteService.getNoteById(news.getId()) != null) {
      news = createArticleFromExistingPage(news, poster);
    } else {
      news = createNewsArticlePage(news, poster);
    }

    if (news != null) {
      postProcessing(news, poster);
    }
    return news;
  }

  /**
   * Checks if the user can create a News
   *
   * @param space
   * @param currentIdentity
   * @return boolean true if the user can create the news
   */
  public boolean canCreateNews(Space space, Identity currentIdentity) {
    return space != null && (spaceService.canRedactOnSpace(space, currentIdentity));
  }

  /**
   * Update a news If the uploadId of the news is null, the illustration is not
   * updated. If the uploadId of the news is empty, the illustration is removed
   * (if any).
   *
   * @param news
   * @param updater user attempting to update news
   * @param post
   * @param publish
   * @param newsObjectType
   * @param newsUpdateType
   * @return updated News
   * @throws Exception
   */
  public News updateNews(News news,
                         String updater,
                         Boolean post,
                         boolean publish,
                         String newsObjectType,
                         String newsUpdateType) throws Exception {

    if (CONTENT_AND_TITLE.name().equalsIgnoreCase(newsUpdateType) && !canEditNews(news, updater)) {
      throw new IllegalAccessException("User " + updater + " is not authorized to update news");
    }
    Identity updaterIdentity = NewsUtils.getUserIdentity(updater);
    if (POSTING_AND_PUBLISHING.name().equalsIgnoreCase(newsUpdateType)
        && (!canEditNews(news, updater) && !NewsUtils.canPublishNews(news.getSpaceId(), updaterIdentity))) {
      throw new IllegalAccessException("User " + updater + " is not authorized to update news");
    }
    if (PAGE_REFERENCE.name().equalsIgnoreCase(newsUpdateType) && !canReferToNote(news, updaterIdentity)) {
      throw new IllegalAccessException("User " + updater + " is not authorized to refer or derefer news");
    }

    String newsId = news.getTargetPageId() != null ? news.getTargetPageId() : news.getId();
    News originalNews = getNewsById(newsId, updaterIdentity, false, newsObjectType);
    List<ArticleTarget> oldTargets = newsTargetingService.getTargetsByNews(news);
    boolean canPublish = NewsUtils.canPublishNews(news.getSpaceId(), updaterIdentity);
    Space space = spaceService.getSpaceById(news.getSpaceId());
    Set<String> previousMentions = NewsUtils.processMentions(originalNews.getOriginalBody(), space);
    if (NewsObjectType.DRAFT.name().toLowerCase().equals(newsObjectType)) {
      return updateDraftArticleForNewPage(news, updater, space);
    } else if (LATEST_DRAFT.name().toLowerCase().equals(newsObjectType)) {
      return createOrUpdateDraftArticleForExistingPage(news, updater, space);
    } else if (ARTICLE.name().equalsIgnoreCase(newsObjectType) && CONTENT_AND_TITLE.name().equalsIgnoreCase(newsUpdateType)
        && StringUtils.isNotEmpty(news.getLang())) {
      return addNewArticleVersionWithLang(news, updaterIdentity, space);
    } else if (ARTICLE.name().equalsIgnoreCase(newsObjectType) && PAGE_REFERENCE.name().equalsIgnoreCase(newsUpdateType)) {
      return updateArticle(news, updaterIdentity, newsUpdateType);
    }
    if (publish != news.isPublished() && news.isCanPublish()) {
      news.setPublished(publish);
      if (news.isPublished()) {
        publishNews(news, updater);
      } else {
        unpublishNews(newsId, updater, false);
      }
    }
    if (publish == news.isPublished() && news.isPublished() && canPublish) {
      if (news.getTargets() != null && (oldTargets == null || !oldTargets.equals(news.getTargets()))) {
        updateArticleTargets(news, oldTargets, updater);
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
      news = updateArticle(news, updaterIdentity, newsUpdateType);
    }

    if (POSTED.equals(news.getPublicationState())) {
      // Send mention notifs
      if (StringUtils.isNotEmpty(newsId) && news.getCreationDate() != null) {
        News newMentionedNews = news;
        if (!previousMentions.isEmpty()) {
          // clear old mentions from news body before sending a custom object to
          // notification context.
          previousMentions.forEach(username -> newMentionedNews.setBody(newMentionedNews.getBody()
                                                                                        .replaceAll("@" + username, "")));
        }
        sendNotification(updater, newMentionedNews, NotificationConstants.NOTIFICATION_CONTEXT.MENTION_IN_NEWS);
      }
      indexingService.reindex(NewsIndexingServiceConnector.TYPE, String.valueOf(newsId));
    }
    if (!news.getPublicationState().isEmpty() && !DRAFT.equals(news.getPublicationState())) {
      if (post != null) {
        updateNewsActivity(news, post, originalNews.isActivityPosted(), true);
      }
      NewsUtils.broadcastEvent(NewsUtils.UPDATE_NEWS, updater, news);
      NewsUtils.broadcastEvent(NewsUtils.UPDATE_PUBLISH_CONTENT, updater, new ContentPublishEvent(originalNews, news));
    }
    return news;
  }

  /**
   * Delete news
   * 
   * @param newsId the news id to delete
   * @param currentIdentity user attempting to delete news
   * @param newsObjectType the News object type to be deleted
   * @throws Exception when error
   */
  public void deleteNews(String newsId, Identity currentIdentity, String newsObjectType) throws Exception {
    News news = getNewsById(newsId, currentIdentity, false, newsObjectType);
    if (!news.isCanDelete()) {
      throw new IllegalAccessException("User " + currentIdentity.getUserId() + " is not authorized to delete news");
    }
    if (NewsObjectType.DRAFT.name().toLowerCase().equals(newsObjectType)) {
      deleteDraftArticle(newsId, currentIdentity.getUserId());
    } else {
      deleteArticle(news, currentIdentity.getUserId());
      if (news.getActivities() != null) {
        String newsActivities = news.getActivities();
        Stream.of(newsActivities.split(";")).map(activity -> activity.split(":")[1]).forEach(activityManager::deleteActivity);
      }
      MetadataObject newsMetadataObject = new MetadataObject(NEWS_METADATA_PAGE_OBJECT_TYPE, newsId);
      metadataService.deleteMetadataItemsByObject(newsMetadataObject);
      indexingService.unindex(NewsIndexingServiceConnector.TYPE, String.valueOf(news.getId()));
      List<String> articleLanguages = getArticleLanguages(newsId, false);
      if (CollectionUtils.isNotEmpty(articleLanguages)) {
        articleLanguages.forEach(lang -> indexingService.unindex(NewsIndexingServiceConnector.TYPE,
                                                                 news.getId().concat("-").concat(lang)));
      }
      NewsUtils.broadcastEvent(NewsUtils.DELETE_NEWS, currentIdentity.getUserId(), news);
    }
  }

  /**
   * Publish a news
   *
   * @param newsToPublish to be published
   * @param publisher of the News
   * @throws Exception when error
   */
  public void publishNews(News newsToPublish, String publisher) throws Exception {
    Identity publisherIdentity = NewsUtils.getUserIdentity(publisher);
    News news = getNewsArticleById(newsToPublish.getId());

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
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(publisherId), false);
    }
    if (newsToPublish.getTargets() != null) {
      updateArticleTargets(newsToPublish, news.getTargets(), publisher);
    }
    news.setAudience(newsToPublish.getAudience());
    NewsUtils.broadcastEvent(NewsUtils.PUBLISH_NEWS, news.getId(), news);

    Space space = spaceService.getSpaceById(news.getSpaceId());
    // Update content permissions
    updateArticlePermissions(List.of(space), news);
    try {
      sendNotification(publisher, news, NotificationConstants.NOTIFICATION_CONTEXT.PUBLISH_NEWS);
    } catch (Error | Exception e) {
      LOG.warn("Error sending notification when publishing news with Id " + news.getId(), e);
    }
  }

  /**
   * Unpublish a News
   * 
   * @param newsId the ID of the News
   * @param publisher the publisher of the News
   * @param unpublishScheduled
   * @throws Exception when an error occurs
   */
  public void unpublishNews(String newsId, String publisher, boolean unpublishScheduled) throws Exception {
    News news = getNewsArticleById(newsId);
    if (news != null) {
      Space space = spaceService.getSpaceById(news.getSpaceId());
      if (unpublishScheduled) {
        newsTargetingService.deleteNewsTargets(news);
      } else {
        newsTargetingService.deleteNewsTargets(news, publisher);
      }

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
          if (unpublishScheduled) {
            properties.put(UNPUBLISH_SCHEDULED, "false");
            properties.remove(UNPUBLISH_SCHEDULED_DATE);
          }
        }
        newsMetadataItem.setProperties(properties);
        Date updatedDate = Calendar.getInstance().getTime();
        newsMetadataItem.setUpdatedDate(updatedDate.getTime());
        String publisherId = identityManager.getOrCreateUserIdentity(publisher).getId();
        metadataService.updateMetadataItem(newsMetadataItem, Long.parseLong(publisherId), false);
        // Update content permissions
        updateArticlePermissions(List.of(space), news);
      }
    }
  }

  /**
   * Retrieves a news identified by its technical identifier
   *
   * @param newsId {@link News} identifier
   * @param currentIdentity user attempting to access news
   * @param editMode access mode to news: whether to edit news to to view it.
   * @param newsObjectType news object type to be retrieved.
   * @return {@link News} if found else null
   * @throws IllegalAccessException when user doesn't have access to
   *           {@link News}
   */
  public News getNewsById(String newsId,
                          Identity currentIdentity,
                          boolean editMode,
                          String newsObjectType) throws IllegalAccessException {
    return getNewsByIdAndLang(newsId, currentIdentity, editMode, newsObjectType, null);
  }

  /**
   * Retrieves a news identified by its technical identifier and corresponding
   * translation
   *
   * @param newsId {@link News} identifier
   * @param currentIdentity user attempting to access news
   * @param editMode access mode to news: whether to edit news to to view it.
   * @param newsObjectType news object type to be retrieved.
   * @param lang news translate version
   * @return {@link News} if found else null
   * @throws IllegalAccessException when user doesn't have access to
   *           {@link News}
   */
  public News getNewsByIdAndLang(String newsId,
                                 Identity currentIdentity,
                                 boolean editMode,
                                 String newsObjectType,
                                 String lang) throws IllegalAccessException {
    News news = null;
    try {
      if (newsObjectType == null) {
        throw new IllegalArgumentException("Required argument news object type could not be null");
      }
      if (NewsObjectType.DRAFT.name().equalsIgnoreCase(newsObjectType)) {
        news = buildDraftArticle(newsId, currentIdentity);
      } else if (LATEST_DRAFT.name().equalsIgnoreCase(newsObjectType)) {
        news = buildLatestDraftArticle(newsId, currentIdentity, lang);
      } else if (ARTICLE.name().equalsIgnoreCase(newsObjectType)) {
        news = buildArticle(newsId, currentIdentity, lang, true);
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
      news.setCanDelete(canEditNews(news, currentIdentity.getUserId()));
      news.setCanPublish(NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity));
      news.setCanRefer(canReferToNote(news, currentIdentity));
      news.setCanSchedule(canScheduleNews(news.getSpaceId(), currentIdentity, news));
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
        news.setCategories(activity.getCategoryIds());
      }
    }
    return news;
  }

  /**
   * Retrieves a news identified by its technical identifier without identity
   * and lang
   *
   * @param newsId {@link News} identifier
   * @return {@link News} if found else null
   */
  public News buildArticle(String newsId) {
    return buildArticle(newsId, null, null, false);
  }

  /**
   * Retrieves a news identified by its technical identifier
   * 
   * @param newsId {@link News} identifier
   * @return {@link News} if found else null
   */
  public News getNewsArticleById(String newsId) {
    return getNewsArticleByIdAndLang(newsId, null);
  }

  /**
   * Retrieves a news identified by its technical identifier
   *
   * @param newsId {@link News} identifier
   * @param lang {@link News} news translation language
   * @return {@link News} if found else null
   */
  public News getNewsArticleByIdAndLang(String newsId, String lang) {
    News news = null;
    try {
      news = buildArticle(newsId, null, lang, true);
      if (news != null) {
        news.setTargets(newsTargetingService.getTargetsByNews(news));

      }
    } catch (Exception exception) {
      LOG.error("An error occurred while retrieving news with id {}", newsId, exception);
    }
    return news;
  }

  /**
   * Get all news
   * 
   * @param filter
   * @param currentIdentity
   * @return all news
   * @throws Exception when error
   */
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
      news.setCanDelete(canEditNews(news, currentIdentity.getUserId()));
      news.setCanPublish(NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity));
      news.setCanRefer(canReferToNote(news, currentIdentity));
      news.setCanSchedule(canScheduleNews(news.getSpaceId(), currentIdentity, news));
    });
    return newsList;
  }

  /**
   * Get list of news by a given target name
   * 
   * @param newsFilter
   * @param targetName
   * @param currentIdentity user attempting to access news
   * @return {@link News} list by target name.
   * @throws Exception when error
   */
  public List<News> getNewsByTargetName(NewsFilter newsFilter, String targetName, Identity currentIdentity) throws Exception {
    List<MetadataItem> newsTargetItems =
                                       newsTargetingService.getNewsTargetItemsByTargetName(targetName, newsFilter.getOffset(), 0);
    return newsTargetItems.stream().map(target -> {
      try {
        News news = getNewsByIdAndLang(target.getObjectId(),
                                       currentIdentity,
                                       false,
                                       ARTICLE.name().toLowerCase(),
                                       newsFilter.getLang());
        news.setPublishDate(new Date(target.getCreatedDate()));
        return news;
      } catch (Exception e) {
        return null;
      }
    })
                          .filter(news -> news != null && (StringUtils.isEmpty(news.getAudience())
                              || news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE) || news.isSpaceMember()))
                          .limit(newsFilter.getLimit())
                          .toList();
  }

  /**
   * get the count of News after applying a filter
   * 
   * @param filter
   * @return int the number of News
   * @throws Exception
   */
  public int getNewsCount(NewsFilter filter) throws Exception {
    return 0;
  }

  /**
   * Increment the number of views for a news
   * 
   * @param news The news to be updated
   * @param userId The current user id
   * @throws Exception when error
   */
  public void markAsRead(News news, String userId) throws Exception {
    try {
      MetadataItem metadataItem =
                                metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                    new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                                                                       news.getId(),
                                                                                                       null,
                                                                                                       Long.parseLong(news.getSpaceId())))
                                               .getFirst();
      if (metadataItem != null) {
        NewsUtils.broadcastEvent(NewsUtils.VIEW_NEWS, userId, news);
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
          newsViewers = newsViewers.concat("," + userId);
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
        metadataService.updateMetadataItem(metadataItem, Long.parseLong(userIdentityId), false);
      }
    } catch (Exception exception) {
      LOG.error("Failed to mark news article " + news.getId() + " as read for current user", exception);
      return;
    }
  }

  /**
   * Search news with the given text
   * 
   * @param filter news filter
   * @param currentIdentity current user identity
   * @throws Exception when error
   * @return List of News returned by the search
   */
  public List<News> searchNews(NewsFilter filter,
                               org.exoplatform.social.core.identity.model.Identity currentIdentity) throws Exception {
    return newsSearchConnector.search(currentIdentity, filter).stream().map(articleSearchResult -> {
      try {
        return buildArticle(articleSearchResult.getId());
      } catch (Exception e) {
        LOG.error("Error while building news article", e);
        return null;
      }
    }).filter(Objects::nonNull).toList();
  }

  /**
   * Retrieves a news item identified by originating Activity identifier or a
   * shared activity identifier
   * 
   * @param activityId {@link ExoSocialActivity} identifier
   * @param currentIdentity user attempting to access news
   * @return {@link News} if found else null
   * @throws IllegalAccessException when user doesn't have access to
   *           {@link News} or {@link ExoSocialActivity}
   * @throws ObjectNotFoundException when a {@link News} wasn't found for this
   *           activity identifier
   */
  public News getNewsByActivityId(String activityId, Identity currentIdentity) throws IllegalAccessException,
                                                                               ObjectNotFoundException {
    return getNewsByActivityIdAndLang(activityId, currentIdentity, null);
  }

  /**
   * Retrieves a {@link News} by its related activity identifier or its shared
   * activity identifier
   * 
   * @param activityId {@link ExoSocialActivity} identifier
   * @param currentIdentity user attempting to access news
   * @param lang {@link News} translation language
   * @return {@link News} if found else null
   * @throws IllegalAccessException when user doesn't have access to
   *           {@link News} or {@link ExoSocialActivity}
   * @throws ObjectNotFoundException when a {@link News} wasn't found for the
   *           given activity identifier
   */
  public News getNewsByActivityIdAndLang(String activityId, Identity currentIdentity, String lang) throws IllegalAccessException,
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
        return getNewsByActivityIdAndLang(originalActivityId,
                                          NewsUtils.getUserIdentity(sharedActivityPosterIdentity.getRemoteId()),
                                          lang);
      }
      throw new ObjectNotFoundException("Activity with id " + activityId + " isn't of type news nor a shared news");
    }
    return getNewsByIdAndLang(newsId, currentIdentity, false, ARTICLE.name().toLowerCase(), lang);
  }

  /**
   * Schedule publishing a News
   * 
   * @param news
   * @param currentIdentity
   * @param newsObjectType
   * @return the published news
   * @throws Exception when error occurs
   */
  public News scheduleNews(News news, Identity currentIdentity, String newsObjectType) throws Exception {
    if (!canScheduleNews(news.getSpaceId(), currentIdentity, news)) {
      throw new IllegalArgumentException("User " + currentIdentity.getUserId() + " is not authorized to schedule news");
    }
    News originalArticle = null;
    if (newsObjectType.equalsIgnoreCase(NewsObjectType.DRAFT.name())) {
      // Create news article with the publication state STAGED without posting
      // or publishing it ( displayed false news target)
      // it will be posted and published by the news schedule job or the edit
      // scheduling.
      news = createNewsArticlePage(news, currentIdentity.getUserId());
    } else if (newsObjectType.equalsIgnoreCase(NewsObjectType.EXISTING_PAGE.name())) {
      news = createArticleFromExistingPage(news, currentIdentity.getUserId());
    } else if (newsObjectType.equalsIgnoreCase(ARTICLE.name())) {
      originalArticle = getNewsArticleById(news.getId());
      news = updateArticle(news, currentIdentity, NewsUtils.NewsUpdateType.SCHEDULE.name().toLowerCase());
    }
    if (news != null) {
      if (NewsUtils.canPublishNews(news.getSpaceId(), currentIdentity)) {
        if (news.isPublished()) {
          publishNews(news, currentIdentity.getUserId());
        } else {
          unpublishNews(news.getId(), currentIdentity.getUserId(), false);
        }
      }
      // set the url and the space url to the scheduled news
      news.setUrl(NewsUtils.buildNewsArticleUrl(news, currentIdentity.getUserId()));
      news.setSpaceUrl(NewsUtils.buildSpaceUrl(news.getSpaceId()));
      if (originalArticle != null && !originalArticle.getPublicationState().equalsIgnoreCase(STAGED)) {
        NewsUtils.broadcastEvent(NewsUtils.UPDATE_PUBLISH_CONTENT,
                                 currentIdentity.getUserId(),
                                 new ContentPublishEvent(originalArticle, news));
      }
      return news;
    }
    return null;
  }

  /**
   * Un-schedule publishing a News
   *
   * @param news news article
   * @param space owner space
   * @param articleCreator article creator
   * @return unscheduled News
   * @throws Exception when error occurs
   */
  public News unScheduleNews(News news, Space space, String articleCreator) throws Exception {
    News existingNews = getNewsArticleById(news.getId());
    if (existingNews != null && !existingNews.isFromExternalPage()) {
      if (news.getProperties() != null) {
        news.getProperties().setDraft(true);
      }
      news = createDraftArticleForNewPage(news, space.getGroupId(), articleCreator, System.currentTimeMillis());
      broadcastUnScheduleArticleEvent(existingNews, news.getId());
      deleteArticle(existingNews, articleCreator);
      return buildDraftArticle(news.getId(), userAcl.getUserIdentity(articleCreator));
    } else if (existingNews != null) {
      PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(existingNews.getId()), null);
      NewsPageVersionObject articleVersionMetaDataObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                     pageVersion.getId(),
                                                                                     null,
                                                                                     Long.parseLong(space.getId()));
      NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                         existingNews.getId(),
                                                         null,
                                                         Long.parseLong(space.getId()));
      metadataService.deleteMetadataItemsByObject(articleVersionMetaDataObject);
      metadataService.deleteMetadataItemsByObject(newsPageObject);
      return existingNews;
    }
    return null;
  }

  /**
   * Search news by term
   *
   * @param currentIdentity
   * @param filter
   * @return News Search Result
   */
  public List<NewsSearchResultEntity> search(org.exoplatform.social.core.identity.model.Identity currentIdentity,
                                             NewsFilter filter) {
    if (CollectionUtils.isNotEmpty(filter.getSpaces())) {
      filter.setSpaces(SpaceUtils.getSpaceIdentityIds(currentIdentity.getRemoteId(), filter.getSpaces()));
    }
    List<NewsESSearchResult> searchResults = newsSearchConnector.search(currentIdentity, filter);
    return searchResults.stream().map(result -> {
      try {
        News news = getNewsArticleByIdAndLang(result.getId(), result.getLang());
        return news != null ? EntityBuilder.toSearchResultEntity(result, news, getCurrentIdentity()) : null;
      } catch (Exception e) {
        LOG.error("Error when searching the news with id {}", result.getId(), e);
        return null;
      }
    }).filter(Objects::nonNull).toList();
  }

  /**
   * Checks if the user can view the News
   *
   * @param news {@link News} to check
   * @param authenticatedUser authenticated username
   * @return true if user has access to news, else false
   */
  public boolean canViewNews(News news, String authenticatedUser) {
    try {
      String spaceId = news.getSpaceId();
      Space space = spaceId == null ? null : spaceService.getSpaceById(spaceId);
      if (space == null) {
        LOG.warn("Can't find space with id {} when checking access on news with id {}", spaceId, news.getId());
        return false;
      } else if (spaceService.isSuperManager(space, authenticatedUser)) {
        return true;
      }
      if (!news.isPublished() && StringUtils.equals(news.getPublicationState(), POSTED)
          && !(spaceService.canViewSpace(space, authenticatedUser) || canViewSharedInSpaces(news, authenticatedUser))) {
        return false;
      }
      if (news.isPublished() && StringUtils.equals(news.getPublicationState(), POSTED)
          && NewsUtils.SPACE_NEWS_AUDIENCE.equals(news.getAudience())
          && !(spaceService.canViewSpace(space, authenticatedUser) || canViewSharedInSpaces(news, authenticatedUser))) {
        return false;
      }
      if (StringUtils.equals(news.getPublicationState(), STAGED)
          && !canScheduleNews(spaceId, NewsUtils.getUserIdentity(authenticatedUser), news)) {
        return false;
      }
    } catch (Exception e) {
      LOG.warn("Error retrieving access permission for user {} on news with id {}", authenticatedUser, news.getId());
      return false;
    }
    return true;
  }

  /**
   * Checks if the user can schedule publishing a News
   *
   * @param spaceId target space id
   * @param currentIdentity current user identity id
   * @param article target article
   * @return boolean : true if the user can schedule publishing a News
   */
  public boolean canScheduleNews(String spaceId, Identity currentIdentity, News article) {
    return canEditNews(article, currentIdentity.getUserId()) || NewsUtils.canPublishNews(spaceId, currentIdentity);
  }

  /**
   * Checks if the user can schedule publishing a News
   * 
   * @param articleId external article to be scheduled identifier
   * @param currentIdentity current user identity
   * @return boolean : true if the user can schedule publishing a News
   */
  public boolean canScheduleExternalPageAsNews(String articleId, Identity currentIdentity) {
    try {
      Page pageToBeScheduled = noteService.getNoteById(articleId, currentIdentity);
      return pageToBeScheduled != null && pageToBeScheduled.isCanManage();
    } catch (Exception exception) {
      return false;
    }
  }

  /**
   * Shares a news item into a dedicated space
   *
   * @param news {@link News} to share
   * @param space {@link Space} to share with, the news
   * @param userIdentity {@link Identity} of user making the modification
   * @param sharedActivityId newly generated activity identifier
   * @throws Exception when user doesn't have access to {@link News}
   */
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
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(userIdentity.getId()), false);
      // Update content permissions
      updateArticlePermissions(List.of(space), news);
      NewsUtils.broadcastEvent(NewsUtils.SHARE_NEWS, userIdentity.getRemoteId(), news);
    }

  }

  /**
   * Deletes an article version by its given id and lang
   *
   * @param articleId article id
   * @param lang article version language
   */
  @SneakyThrows
  public void deleteVersionsByArticleIdAndLang(String articleId, String lang) {
    News article = getNewsArticleByIdAndLang(articleId, lang);
    noteService.deleteVersionsByNoteIdAndLang(Long.parseLong(articleId), lang);
    NewsUtils.broadcastEvent(NewsUtils.REMOVE_ARTICLE_TRANSLATION, article.getAuthor(), article);
    String newsTranslationId = articleId.concat("-").concat(lang);
    indexingService.unindex(NewsIndexingServiceConnector.TYPE, newsTranslationId);
  }

  /**
   * @param draftArticle {@link News} news draft article to be created
   * @param pageOwnerId
   * @param draftArticleCreator
   * @param creationDate
   * @return the created draft news article
   * @throws Exception when error occurs
   */
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
      draftArticlePage.setProperties(draftArticle.getProperties());
      draftArticlePage.setAttachmentObjectType(WikiDraftPageAttachmentPlugin.OBJECT_TYPE);
      draftArticlePage =
                       noteService.createDraftForNewPage(draftArticlePage,
                                                         creationDate,
                                                         Long.parseLong(identityManager.getOrCreateUserIdentity(draftArticleCreator)
                                                                                       .getId()));

      draftArticle.setProperties(draftArticlePage.getProperties());
      draftArticle.setIllustrationURL(NewsUtils.buildIllustrationUrl(draftArticle.getProperties(), draftArticlePage.getLang()));
      draftArticle.setId(draftArticlePage.getId());
      draftArticle.setOwner(draftArticlePage.getOwner());
      draftArticle.setCreationDate(draftArticlePage.getCreatedDate());
      draftArticle.setUpdateDate(draftArticlePage.getUpdatedDate());
      draftArticle.setBody(draftArticlePage.getContent());
      Space draftArticleSpace = spaceService.getSpaceByGroupId(pageOwnerId);
      draftArticle.setSpaceId(draftArticleSpace.getId());
      NewsDraftObject draftArticleMetaDataObject = new NewsDraftObject(NEWS_METADATA_DRAFT_OBJECT_TYPE,
                                                                       draftArticlePage.getId(),
                                                                       null,
                                                                       Long.parseLong(draftArticleSpace.getId()));
      String draftArticleMetadataItemCreatorIdentityId = identityManager.getOrCreateUserIdentity(draftArticleCreator).getId();
      Map<String, String> draftArticleMetadataItemProperties = new HashMap<>();
      metadataService.createMetadataItem(draftArticleMetaDataObject,
                                         NEWS_METADATA_KEY,
                                         draftArticleMetadataItemProperties,
                                         Long.parseLong(draftArticleMetadataItemCreatorIdentityId),
                                         false);
      // Update content permissions
      updateArticlePermissions(List.of(draftArticleSpace), draftArticle);
      return draftArticle;
    }
    return null;
  }

  /**
   * Create an article from exiting page
   *
   * @param article article object
   * @param creator article creator
   * @return {@link News}
   * @throws Exception
   */
  public News createArticleFromExistingPage(News article, String creator) throws Exception {
    Page articlePage = noteService.getNoteById(article.getId());
    Space space = spaceService.getSpaceById(article.getSpaceId());
    if (articlePage != null && space != null) {
      PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(articlePage.getId()), null);
      article.setAuthor(pageVersion != null ? pageVersion.getAuthor() : articlePage.getAuthor());
      article.setIllustrationURL(NewsUtils.buildIllustrationUrl(articlePage.getProperties(), articlePage.getLang()));
      buildNewArticleProperties(article, articlePage, creator, space.getId(), pageVersion.getId(), true);
    }
    return article;
  }

  /**
   * @param newsArticle {@link News} news article to be created
   * @param newsArticleCreator
   * @return the created news article
   * @throws Exception when error occurs
   */
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
      newsArticlePage.setAttachmentObjectType(NewsPageAttachmentPlugin.OBJECT_TYPE);
      newsArticlePage.setName(newsArticle.getName());
      newsArticlePage.setTitle(newsArticle.getTitle());
      newsArticlePage.setContent(newsArticle.getBody());
      newsArticlePage.setParentPageId(newsArticlesRootNotePage.getId());
      newsArticlePage.setAuthor(newsArticle.getAuthor());
      newsArticlePage.setLang(null);
      newsArticlePage.setProperties(newsArticle.getProperties());
      if (newsArticlePage.getProperties() == null) {
        newsArticlePage.setProperties(new NotePageProperties(Long.parseLong(draftNewsId), null, null, false, false, true));
      }
      newsArticlePage = noteService.createNote(wiki, newsArticlesRootNotePage.getName(), newsArticlePage, poster, false);
      if (newsArticlePage != null) {
        PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(newsArticlePage.getId()), null);
        // set properties
        newsArticle.setId(newsArticlePage.getId());
        newsArticle.setBody(pageVersion.getContent());
        newsArticle.setLang(newsArticlePage.getLang());
        newsArticle.setCreationDate(pageVersion.getCreatedDate());
        newsArticle.setProperties(newsArticlePage.getProperties());
        newsArticle.setLatestVersionId(pageVersion.getId());
        newsArticle.setIllustrationURL(NewsUtils.buildIllustrationUrl(newsArticlePage.getProperties(), newsArticle.getLang()));

        buildNewArticleProperties(newsArticle, newsArticlePage, newsArticleCreator, space.getId(), pageVersion.getId(), false);
        // delete the draft
        deleteDraftArticle(draftNewsId, poster.getUserId());
        return newsArticle;
      }
    }
    return null;
  }

  /**
   * @param draftArticle {@link News} news draft article to be created
   * @param updater
   * @param targetArticlePage
   * @param creationDate
   * @param space
   * @return the created news draft for an existing news article
   */
  @SneakyThrows
  public News createDraftForExistingPage(News draftArticle,
                                         String updater,
                                         Page targetArticlePage,
                                         long creationDate,
                                         Space space) {
    DraftPage draftArticlePage = new DraftPage();
    draftArticlePage.setAttachmentObjectType(NewsPageAttachmentPlugin.OBJECT_TYPE);
    draftArticlePage.setNewPage(false);
    draftArticlePage.setTargetPageId(targetArticlePage.getId());
    draftArticlePage.setTitle(draftArticle.getTitle());
    draftArticlePage.setContent(draftArticle.getBody());
    draftArticlePage.setParentPageId(targetArticlePage.getParentPageId());
    draftArticlePage.setAuthor(draftArticle.getAuthor());
    draftArticlePage.setLang(draftArticle.getLang());
    draftArticlePage.setProperties(draftArticle.getProperties());

    draftArticlePage = noteService.createDraftForExistPage(draftArticlePage, targetArticlePage, null, creationDate, updater);

    draftArticle.setDraftUpdateDate(draftArticlePage.getCreatedDate());
    draftArticle.setDraftUpdater(draftArticlePage.getAuthor());
    draftArticle.setTargetPageId(draftArticlePage.getTargetPageId());
    draftArticle.setProperties(draftArticlePage.getProperties());
    draftArticle.setId(draftArticlePage.getId());
    draftArticle.setBody(draftArticlePage.getContent());
    NewsLatestDraftObject latestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                        draftArticlePage.getId(),
                                                                        targetArticlePage.getId(),
                                                                        Long.parseLong(draftArticle.getSpaceId()));

    Map<String, String> draftArticleMetadataItemProperties = new HashMap<>();
    draftArticleMetadataItemProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(draftArticle.isActivityPosted()));
    draftArticleMetadataItemProperties.put(PUBLISHED, String.valueOf(draftArticle.isPublished()));

    setScheduleProperties(draftArticle, draftArticleMetadataItemProperties);
    String draftArticleMetadataItemCreatorIdentityId = identityManager.getOrCreateUserIdentity(updater).getId();
    metadataService.createMetadataItem(latestDraftObject,
                                       NEWS_METADATA_KEY,
                                       draftArticleMetadataItemProperties,
                                       Long.parseLong(draftArticleMetadataItemCreatorIdentityId),
                                       false);

    // Update content permissions
    updateArticlePermissions(List.of(space), draftArticle);
    return draftArticle;
  }

  /**
   * @param news {@link News} news article to be deleted
   * @param articleCreator
   */
  public void deleteArticle(News news, String articleCreator) {
    Page articlePage = noteService.getNoteById(news.getId());

    if (articlePage != null) {
      if (!articlePage.isDeleted()) {
        deleteAllDrafts(articlePage, articleCreator);
        if (noteService.deleteNote(articlePage.getWikiType(), articlePage.getWikiOwner(), articlePage.getName())) {
          updateDeletedArticleMetadata(news, articleCreator);
        }
      } else {
        // If the article is already deleted,
        // case of external publish, still update metadata
        updateDeletedArticleMetadata(news, articleCreator);
      }
    }
  }

  /**
   * Deletes a draft article by its given id
   *
   * @param draftArticleId draft article id
   * @param draftArticleCreator creator
   * @throws Exception when error occurs
   */
  public void deleteDraftArticle(String draftArticleId, String draftArticleCreator) throws Exception {
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
      metadataService.deleteMetadataItem(draftArticleMetadataItems.getFirst().getId(), false);
    }
  }

  /**
   * Get all article available languages by its given id
   *
   * @param articleId article id
   */
  @SneakyThrows
  public List<String> getArticleLanguages(String articleId, boolean withDrafts) {
    return noteService.getPageAvailableTranslationLanguages(Long.parseLong(articleId), withDrafts);
  }
  
  public List<News> getNewsByIds(List<Long> ids, Identity currentUser, String lang, String expand) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    return ids.stream()
              .map(id -> buildArticleWithExpand(String.valueOf(id), currentUser, lang, true, expand))
              .filter(news -> news != null && canViewNews(news, currentUser.getUserId()))
              .toList();
  }

  public void removeArticleMetadataProperty(String articleId, String propertyKey, long updater) {
    News article = getNewsArticleById(articleId);
    if (article == null) {
      return;
    }
    MetadataItem metadataItem = getArticlePageMetadataItem(article);
    if (metadataItem == null) {
      return;
    }
    Map<String, String> properties = metadataItem.getProperties();
    if (!properties.containsKey(propertyKey)) {
      return;
    }
    properties.remove(propertyKey);
    metadataItem.setProperties(properties);
    metadataService.updateMetadataItem(metadataItem, updater, false);
  }

  public Map<String, String> updateMetadataProperties(String articleId, Map<String, String> properties, Long updater)
      throws ObjectNotFoundException {

    News article = getNewsArticleById(articleId);
    if (article == null) {
      throw new ObjectNotFoundException("Article not found");
    }

    MetadataItem existingItem = getArticlePageMetadataItem(article);

    if (existingItem != null) {
      Map<String, String> mergedProperties = new HashMap<>(existingItem.getProperties());
      mergedProperties.putAll(properties);
      existingItem.setProperties(mergedProperties);
      metadataService.updateMetadataItem(existingItem, updater, false);
      updateNewsActivity(article, false, article.isActivityPosted(), false);
      article.setParameters(mergedProperties);
      NewsUtils.broadcastEvent(NewsUtils.UPDATE_NEWS, updater, article);
      return mergedProperties;
    } else {
      return properties;
    }
  }

  private MetadataItem getArticlePageMetadataItem(News article) {
    NewsPageObject newsPageObject = new NewsPageObject(
        NEWS_METADATA_PAGE_OBJECT_TYPE, article.getId(), null, Long.parseLong(article.getSpaceId())
    );

    return metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                          .stream()
                          .findFirst()
                          .orElse(null);
  }

  @SneakyThrows
  private void updateArticleTargets(News article, List<ArticleTarget> oldTargets, String updater) {
    Set<String> oldTargetNames = new HashSet<>(NewsUtils.toTargetNames(oldTargets));
    Set<String> newTargetNames = new HashSet<>(NewsUtils.toTargetNames(article.getTargets()));
    if (!oldTargetNames.isEmpty()) {
      newsTargetingService.deleteNewsTargets(article, oldTargetNames);
    }
    if (!newTargetNames.isEmpty()) {
      newsTargetingService.saveNewsTarget(article,
                                          !(StringUtils.equals(article.getPublicationState(), STAGED)),
                                          List.copyOf(newTargetNames),
                                          updater);
    }
  }

  public boolean canEditNews(News news, String authenticatedUser) {
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
    return (spaceService.canRedactOnSpace(space, authenticatedUserIdentity)
        && (news.isReferred() || news.isFromExternalPage() || isArticleOwner(news, authenticatedUser)))
        || spaceService.isManager(space, authenticatedUser) || spaceService.isSuperManager(space, authenticatedUser)
        || spaceService.isRedactor(space, authenticatedUser) || spaceService.canPublishOnSpace(space, authenticatedUser);
  }

  private boolean canReferToNote(News article, org.exoplatform.services.security.Identity currentIdentity) {
    Space space = spaceService.getSpaceById(article.getSpaceId());
    if (space == null || currentIdentity == null) {
      return false;
    }
    if (article.isFromExternalPage()) {
      return false;
    }
    return canEditNews(article, currentIdentity.getUserId())
        || spaceService.canPublishOnSpace(space, currentIdentity.getUserId());
  }

  private void deleteAllDrafts(Page articlePage, String articleCreator) {
    boolean hasDraft = true;
    while (hasDraft) {
      try {
        DraftPage latestDraft = noteService.getLatestDraftOfPage(articlePage);

        if (latestDraft != null) {
          deleteDraftArticle(latestDraft.getId(), articleCreator);
        } else {
          hasDraft = false;
        }
      } catch (Exception e) {
        LOG.error("Error while deleting draft", e.getMessage());
        hasDraft = false;
      }
    }
  }

  private void updateDeletedArticleMetadata(News news, String articleCreator) {
    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                       news.getId(),
                                                       null,
                                                       Long.parseLong(news.getSpaceId()));

    MetadataItem metadataItem = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject)
                                               .stream()
                                               .findFirst()
                                               .orElse(null);

    if (metadataItem != null) {
      Map<String, String> properties = metadataItem.getProperties();
      properties.put(NEWS_DELETED, String.valueOf(true));
      metadataItem.setProperties(properties);
      String currentIdentityId = identityManager.getOrCreateUserIdentity(articleCreator).getId();
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(currentIdentityId), false);
    }
  }

  private void postProcessing(News news, String poster) throws Exception {
    postNewsActivity(news);
    sendNotification(poster, news, NotificationConstants.NOTIFICATION_CONTEXT.POST_NEWS);

    if (news.isPublished()) {
      publishNews(news, poster);
    }
    news.setUrl(NewsUtils.buildNewsArticleUrl(news, poster));
    // Broadcast events for gamification and analytics
    NewsUtils.broadcastEvent(NewsUtils.POST_NEWS_ARTICLE, news.getId(), news);
    NewsUtils.broadcastEvent(NewsUtils.POST_NEWS, news.getAuthor(), news);
    NewsUtils.broadcastEvent(NewsUtils.CREATE_PUBLISH_CONTENT, poster, new ContentPublishEvent(null, news));
  }

  private void buildNewArticleProperties(News article,
                                         Page articlePage,
                                         String creator,
                                         String spaceId,
                                         String versionId,
                                         boolean externalPage) throws Exception {
    NewsPageVersionObject articleVersionMetaDataObject = new NewsPageVersionObject(NEWS_METADATA_PAGE_VERSION_OBJECT_TYPE,
                                                                                   versionId,
                                                                                   null,
                                                                                   Long.parseLong(spaceId));
    org.exoplatform.social.core.identity.model.Identity creatorIdentity = identityManager.getOrCreateUserIdentity(creator);
    Map<String, String> newsArticleVersionMetadataItemProperties = new HashMap<>();
    // create the page version metadata item
    metadataService.createMetadataItem(articleVersionMetaDataObject,
                                       NEWS_METADATA_KEY,
                                       newsArticleVersionMetadataItemProperties,
                                       Long.parseLong(creatorIdentity.getId()),
                                       false);

    // create metadata item page
    NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                       articlePage.getId(),
                                                       null,
                                                       Long.parseLong(spaceId));
    Map<String, String> newsPageProperties = new HashMap<>();
    if (StringUtils.isNotEmpty(article.getAudience())) {
      newsPageProperties.put(NEWS_AUDIENCE, article.getAudience());
    }
    setScheduleProperties(article, newsPageProperties);

    if (StringUtils.isNotEmpty(article.getPublicationState())) {
      newsPageProperties.put(NEWS_PUBLICATION_STATE, article.getPublicationState());
    }
    newsPageProperties.put(EXTERNAL_PAGE, String.valueOf(externalPage));
    newsPageProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(article.isActivityPosted()));
    if (article.getSchedulePostDate() != null && CollectionUtils.isNotEmpty(article.getCategories())) {
      String categories = article.getCategories().stream().map(String::valueOf).collect(Collectors.joining(";"));
      newsPageProperties.put(NEWS_ACTIVITY_CATEGORIES, categories);
    }
    newsPageProperties.put(PUBLISHED, String.valueOf(article.isPublished()));
    newsPageProperties.put(NEWS_DELETED, String.valueOf(articlePage.isDeleted()));
    newsPageProperties.put(PUBLISHER, creatorIdentity.getProfile().getFullName());

    if (MapUtils.isNotEmpty(article.getParameters())) {
      newsPageProperties.putAll(article.getParameters());
    }

    metadataService.createMetadataItem(newsPageObject,
                                       NEWS_METADATA_KEY,
                                       newsPageProperties,
                                       Long.parseLong(creatorIdentity.getId()),
                                       false);
  }

  private void referOrDeReferArticlePage(News article, Page articlePage, Map<String, String> properties) {
    if (article.isReferred()) {
      properties.put(PAGE_REFERRED, Boolean.TRUE.toString());
      properties.put(DE_REFER_PAGE_ID, articlePage.getParentPageId());
    } else {
      properties.remove(PAGE_REFERRED);
      properties.remove(DE_REFER_PAGE_ID);
      indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, articlePage.getId());
    }
  }

  private News updateDraftArticleForNewPage(News draftArticle, String draftArticleUpdater, Space space) throws WikiException,
                                                                                                        IllegalAccessException {
    DraftPage draftArticlePage = noteService.getDraftNoteById(draftArticle.getId(), draftArticleUpdater);
    if (draftArticlePage != null) {
      draftArticlePage.setTitle(draftArticle.getTitle());
      draftArticlePage.setContent(draftArticle.getBody());
      draftArticlePage.setProperties(draftArticle.getProperties());
      draftArticlePage.setAttachmentObjectType(WikiDraftPageAttachmentPlugin.OBJECT_TYPE);
      // created and updated date set by default during the draft creation
      DraftPage draftPage =
                          noteService.updateDraftForNewPage(draftArticlePage,
                                                            System.currentTimeMillis(),
                                                            Long.parseLong(identityManager.getOrCreateUserIdentity(draftArticleUpdater)
                                                                                          .getId()));
      draftArticle.setProperties(draftPage.getProperties());
      draftArticle.setBody(draftPage.getContent());
      draftArticle.setIllustrationURL(NewsUtils.buildIllustrationUrl(draftPage.getProperties(), draftArticle.getLang()));

      // Update content permissions
      updateArticlePermissions(List.of(space), draftArticle);
      return draftArticle;
    }
    return null;
  }

  private News buildDraftArticle(String draftArticleId, Identity currentIdentity) throws Exception {
    String currentUserId = currentIdentity == null ? null : currentIdentity.getUserId();
    DraftPage draftArticlePage = noteService.getDraftNoteById(draftArticleId, currentUserId);
    if (draftArticlePage != null) {
      News draftArticle = new News();
      draftArticle.setId(draftArticlePage.getId());
      draftArticle.setTargetPageId(draftArticlePage.getTargetPageId());
      draftArticle.setTitle(draftArticlePage.getTitle());
      draftArticle.setAuthor(draftArticlePage.getAuthor());
      draftArticle.setOwner(draftArticlePage.getOwner());
      draftArticle.setCreationDate(draftArticlePage.getCreatedDate());
      draftArticle.setUpdateDate(draftArticlePage.getUpdatedDate());
      draftArticle.setDraftUpdateDate(draftArticlePage.getUpdatedDate());
      draftArticle.setDraftUpdaterUserName(draftArticlePage.getAuthor());
      draftArticle.setLang(draftArticlePage.getLang());
      draftArticle.setProperties(draftArticlePage.getProperties());
      if (draftArticlePage.getProperties() != null && draftArticlePage.getProperties().getFeaturedImage() != null
          && draftArticlePage.getProperties().getFeaturedImage().getId() != 0) {
        draftArticle.setIllustrationURL(NewsUtils.buildIllustrationUrl(draftArticlePage.getProperties(),
                                                                       draftArticlePage.getLang()));
      }
      org.exoplatform.social.core.identity.model.Identity draftUpdaterIdentity =
                                                                               currentIdentity == null ? null
                                                                                                       : identityManager.getOrCreateUserIdentity(currentIdentity.getUserId());
      if (draftUpdaterIdentity != null && draftUpdaterIdentity.getProfile() != null) {
        draftArticle.setDraftUpdaterDisplayName(draftUpdaterIdentity.getProfile().getFullName());
      }
      processPageContent(draftArticlePage, draftArticle, draftArticle.getLang());
      draftArticle.setPublicationState(DRAFT);
      Space draftArticleSpace = spaceService.getSpaceByGroupId(draftArticlePage.getWikiOwner());
      draftArticle.setSpaceId(draftArticleSpace.getId());
      draftArticle.setSpaceAvatarUrl(draftArticleSpace.getAvatarUrl());
      draftArticle.setSpaceDisplayName(draftArticleSpace.getDisplayName());
      boolean hiddenSpace = draftArticleSpace.getVisibility().equals(Space.HIDDEN)
          && !spaceService.canViewSpace(draftArticleSpace, currentUserId);
      draftArticle.setHiddenSpace(hiddenSpace);
      boolean isSpaceMember = spaceService.canViewSpace(draftArticleSpace, currentUserId);
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

  private void buildArticleProperties(News article, String currentUsername, MetadataItem metadataItem) {
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
          if (sharedInSpace != null && currentUsername != null && spaceService.canViewSpace(sharedInSpace, currentUsername)
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
      if (properties.containsKey(UNPUBLISH_SCHEDULED_DATE) && StringUtils.isNotEmpty(properties.get(UNPUBLISH_SCHEDULED_DATE))) {
        article.setScheduleUnpublishDate(properties.get(UNPUBLISH_SCHEDULED_DATE));
      }
      if (properties.containsKey(NEWS_PUBLICATION_STATE) && StringUtils.isNotEmpty(properties.get(NEWS_PUBLICATION_STATE))) {
        article.setPublicationState(properties.get(NEWS_PUBLICATION_STATE));
      }
      if (properties.containsKey(PUBLISHED) && StringUtils.isNotEmpty(properties.get(PUBLISHED))) {
        article.setPublished(Boolean.parseBoolean(properties.get(PUBLISHED)));
      }
      if (properties.containsKey(PUBLISHER) && StringUtils.isNotEmpty(properties.get(PUBLISHER))) {
        article.setPublisher(properties.get(PUBLISHER));
      }
      if (properties.containsKey(EXTERNAL_PAGE) && StringUtils.isNotEmpty(properties.get(EXTERNAL_PAGE))) {
        article.setFromExternalPage(Boolean.parseBoolean(properties.get(EXTERNAL_PAGE)));
      }
      if (properties.containsKey(PAGE_REFERRED) && StringUtils.isNotEmpty(properties.get(PAGE_REFERRED))) {
        article.setReferred(Boolean.parseBoolean(properties.get(PAGE_REFERRED)));
      }
      if (properties.containsKey(DE_REFER_PAGE_ID) && StringUtils.isNotEmpty(properties.get(DE_REFER_PAGE_ID))) {
        article.setDeReferPageId(Long.parseLong(properties.get(DE_REFER_PAGE_ID)));
      }
      if (properties.containsKey(NEWS_VIEWS) && StringUtils.isNotEmpty(properties.get(NEWS_VIEWS))) {
        article.setViewsCount(Long.parseLong(properties.get(NEWS_VIEWS)));
      }
      if (properties.containsKey(NEWS_ACTIVITY_POSTED)) {
        article.setActivityPosted(Boolean.parseBoolean(properties.get(NEWS_ACTIVITY_POSTED)));
      } else {
        article.setActivityPosted(false);
      }
      if (properties.containsKey(NEWS_ACTIVITY_CATEGORIES) && StringUtils.isNotEmpty(properties.get(NEWS_ACTIVITY_CATEGORIES))) {
        List<Long> categories = Arrays.stream(properties.get(NEWS_ACTIVITY_CATEGORIES).split(";"))
                                      .map(Long::valueOf)
                                      .collect(Collectors.toList());
        article.setCategories(categories);
      }
      article.setParameters(properties);
    }
  }

  private void buildDraftArticleProperties(News draftArticle, MetadataItem metadataItem) {
    if (metadataItem != null) {
      Map<String, String> draftArticleMetadataItemProperties = metadataItem.getProperties();
      if (!MapUtils.isEmpty(draftArticleMetadataItemProperties)) {
        if (draftArticleMetadataItemProperties.containsKey(NEWS_ACTIVITY_POSTED)) {
          draftArticle.setActivityPosted(Boolean.parseBoolean(draftArticleMetadataItemProperties.get(NEWS_ACTIVITY_POSTED)));
        } else {
          draftArticle.setActivityPosted(false);
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
        if (properties.containsKey(SCHEDULE_POST_DATE) && StringUtils.isNotEmpty(properties.get(SCHEDULE_POST_DATE))) {
          draftArticle.setSchedulePostDate(properties.get(SCHEDULE_POST_DATE));
        }
        if (properties.containsKey(UNPUBLISH_SCHEDULED_DATE)
            && StringUtils.isNotEmpty(properties.get(UNPUBLISH_SCHEDULED_DATE))) {
          draftArticle.setScheduleUnpublishDate(properties.get(UNPUBLISH_SCHEDULED_DATE));
        }
        if (properties.containsKey(PUBLISHED) && StringUtils.isNotEmpty(properties.get(PUBLISHED))) {
          draftArticle.setPublished(Boolean.valueOf(properties.get(PUBLISHED)));
        }
      }
    }
  }

  private List<News> getPublishedArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setSortField(filter.getSortField());
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
                              return buildArticle(article.getObjectId(), currentIdentity, filter.getLang(), true);
                            } catch (Exception e) {
                              LOG.error("Error while building published news article", e);
                              return null;
                            }
                          })
                          .filter(Objects::nonNull)
                          .toList();
  }

  private List<News> getPostedArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
                MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setMetadataProperties(Map.of(NEWS_PUBLICATION_STATE, POSTED, NEWS_DELETED, "false"));
    metadataFilter.setMetadataSpaceIds(NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces()));
    metadataFilter.setSortField(filter.getSortField());
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
                              return buildArticleWithExpand(article.getObjectId(),
                                                            currentIdentity,
                                                            filter.getLang(),
                                                            true,
                                                            filter.getExpand());
                            } catch (Exception e) {
                              LOG.error("Error while building news article", e);
                              return null;
                            }
                          })
                          .filter(Objects::nonNull)
                          .toList();
  }

  private List<News> getScheduledArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_PAGE_OBJECT_TYPE));
    metadataFilter.setMetadataProperties(Map.of(NEWS_PUBLICATION_STATE, STAGED, NEWS_DELETED, "false"));
    metadataFilter.setCombinedMetadataProperties(Map.of(UNPUBLISH_SCHEDULED, "true", NEWS_DELETED, "false"));
    metadataFilter.setSortField(filter.getSortField());
    metadataFilter.setMetadataSpaceIds(NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces()));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(article -> {
                            try {
                              return buildArticle(article.getObjectId(), currentIdentity, filter.getLang(), true);
                            } catch (Exception e) {
                              LOG.error("Error while building news article", e);
                              return null;
                            }
                          })
                          .filter(article -> {
                            if (article != null) {
                              return canScheduleNews(article.getSpaceId(), currentIdentity, article);
                            }
                            return false;
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
    metadataFilter.setSortField(filter.getSortField());
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
                              return buildArticle(article.getObjectId(), currentIdentity, filter.getLang(), true);
                            } catch (Exception e) {
                              LOG.error("Error while building news article", e);
                              return null;
                            }
                          })
                          .filter(Objects::nonNull)
                          .toList();
  }

  private List<News> buildDraftArticles(NewsFilter filter, Identity currentIdentity) throws Exception {
    MetadataFilter metadataFilter = new MetadataFilter();
    metadataFilter.setMetadataName(NEWS_METADATA_NAME);
    metadataFilter.setMetadataTypeName(NEWS_METADATA_TYPE.getName());
    metadataFilter.setSortField(filter.getSortField());
    metadataFilter.setMetadataObjectTypes(List.of(NEWS_METADATA_DRAFT_OBJECT_TYPE, NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE));
    metadataFilter.setMetadataSpaceIds(NewsUtils.getMyFilteredSpacesIds(currentIdentity, filter.getSpaces()));
    return metadataService.getMetadataItemsByFilter(metadataFilter, filter.getOffset(), filter.getLimit())
                          .stream()
                          .map(draftArticle -> {
                            try {
                              News draft = buildDraftArticle(draftArticle.getObjectId(), currentIdentity);
                              if (draft != null && draftArticle.getParentObjectId() != null) {
                                News parentArticle = buildArticle(draftArticle.getParentObjectId(),
                                                                  currentIdentity,
                                                                  draft.getLang(),
                                                                  true);
                                draft.setReferred(parentArticle.isReferred());
                                draft.setFromExternalPage(parentArticle.isFromExternalPage());
                                draft.setOwner(parentArticle.getOwner());
                              }
                              return draft;
                            } catch (IllegalAccessException e) {
                              return null;
                            } catch (Exception e) {
                              LOG.error("Error while building new draft article", e);
                              return null;
                            }
                          })
                          .filter(article -> {
                            if (article != null) {
                              return canEditNews(article, currentIdentity.getUserId());
                            }
                            return false;
                          })
                          .toList();
  }

  private boolean isArticleOwner(News article, String userName) {
    return StringUtils.isNotEmpty(article.getOwner()) && article.getOwner().equals(userName);
  }

  private boolean canViewSharedInSpaces(News news, String username) {
    for (String sharedInSpaceId : news.getSharedInSpacesList()) {
      Space sharedInSpace = spaceService.getSpaceById(sharedInSpaceId);
      if (sharedInSpace != null && spaceService.canViewSpace(sharedInSpace, username)) {
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
      return noteService.createNote(wiki, null, newsArticlesRootNotePage, false);
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
    if (news.getActivities() == null || news.getActivities().isEmpty()) {
      return;
    }
    String newsId = news.getTargetPageId() != null ? news.getTargetPageId() : news.getId();
    String contentAuthor = news.getAuthor();
    String currentUser = currentUserId != null ? currentUserId : contentAuthor;
    String activities = news.getActivities();
    String contentTitle = news.getTitle();
    String contentBody = news.getBody();
    String lastSpaceIdActivityId = StringUtils.deleteWhitespace(activities.split(";")[activities.split(";").length - 1]);
    String contentSpaceId = lastSpaceIdActivityId.split(":")[0];
    String contentActivityId = lastSpaceIdActivityId.split(":")[1];
    Space contentSpace = spaceService.getSpaceById(contentSpaceId);
    boolean canView = spaceService.canViewSpace(contentSpace, contentAuthor);
    if (contentSpace == null) {
      throw new NullPointerException("Cannot find a space with id " + contentSpaceId + ", it may not exist");
    }
    org.exoplatform.social.core.identity.model.Identity identity = identityManager.getOrCreateUserIdentity(contentAuthor);
    String authorAvatarUrl = LinkProviderUtils.getUserAvatarUrl(identity.getProfile());
    String activityLink = NotificationUtils.getNotificationActivityLink(contentSpace, contentActivityId, canView);
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

  private void updateNewsActivity(News news, boolean post, boolean isPosted, boolean linkCategories) {
    ExoSocialActivity activity = activityManager.getActivity(news.getActivityId());
    if (activity != null) {
      if (post && !isPosted) {
        activity.setUpdated(System.currentTimeMillis());
      }
      activity.isHidden(!news.isActivityPosted());
      Map<String, String> templateParams = activity.getTemplateParams() == null ? new HashMap<>() : activity.getTemplateParams();
      templateParams.put(NEWS_ID, news.getId());
      activity.setTemplateParams(templateParams);
      activity.setMetadataObjectId(news.getId());
      activity.setMetadataObjectType(NewsUtils.NEWS_METADATA_OBJECT_TYPE);
      activityManager.updateActivity(activity, false);
      if (linkCategories) {
        linkActivityCategories(activity, news.getCategories());
      }
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
          if (properties.containsKey(NEWS_ACTIVITY_CATEGORIES)) {
            properties.remove(NEWS_ACTIVITY_CATEGORIES);
          }
          metadataItem.setProperties(properties);
          String updaterId = identityManager.getOrCreateUserIdentity(news.getAuthor()).getId();
          Date updateDate = Calendar.getInstance().getTime();
          metadataItem.setUpdatedDate(updateDate.getTime());
          metadataService.updateMetadataItem(metadataItem, Long.parseLong(updaterId), false);
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
    linkActivityCategories(activity, news.getCategories());
    updateNewsActivities(activity.getId(), news);
  }

  private News updateArticle(News news, Identity updater, String newsUpdateType) throws Exception {
    String newsId = news.getTargetPageId() != null ? news.getTargetPageId() : news.getId();
    Page existingPage = noteService.getNoteById(newsId);
    if (existingPage != null) {
      if (newsUpdateType.equals(CONTENT_AND_TITLE.name())) {
        existingPage.setTitle(news.getTitle());
        existingPage.setContent(news.getBody());
      }
      existingPage.setProperties(news.getProperties());
      existingPage.setAttachmentObjectType(NewsPageAttachmentPlugin.OBJECT_TYPE);
      existingPage = noteService.updateNote(existingPage, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, updater, false);
      news.setUpdateDate(existingPage.getUpdatedDate());
      news.setUpdater(existingPage.getAuthor());
      news.setLang(existingPage.getLang());
      news.setUpdaterFullName(existingPage.getAuthorFullName());
      news.setProperties(existingPage.getProperties());
      news.setUrl(NewsUtils.buildNewsArticleUrl(news, updater.getUserId()));
      news.setIllustrationURL(NewsUtils.buildIllustrationUrl(existingPage.getProperties(), news.getLang()));

      String newsArticleUpdaterIdentityId = identityManager.getOrCreateUserIdentity(updater.getUserId()).getId();

      // update the metadata item page
      NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                         newsId,
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

        setScheduleProperties(news, newsPageProperties);

        if (StringUtils.isNotEmpty(news.getPublicationState())) {
          newsPageProperties.put(NEWS_PUBLICATION_STATE, news.getPublicationState());
        }
        referOrDeReferArticlePage(news, existingPage, newsPageProperties);
        newsPageProperties.put(NEWS_ACTIVITY_POSTED, String.valueOf(news.isActivityPosted()));
        if (newsUpdateType.equalsIgnoreCase(NewsUtils.NewsUpdateType.SCHEDULE.name())
            && CollectionUtils.isNotEmpty(news.getCategories())) {
          String categories = news.getCategories().stream().map(String::valueOf).collect(Collectors.joining(";"));
          newsPageProperties.put(NEWS_ACTIVITY_CATEGORIES, categories);
        }
        if (newsUpdateType.equalsIgnoreCase(POSTING_AND_PUBLISHING.name())) {
          org.exoplatform.social.core.identity.model.Identity publisherIdentity =
                                                                                identityManager.getOrCreateUserIdentity(updater.getUserId());
          newsPageProperties.put(PUBLISHER, publisherIdentity.getProfile().getFullName());
        }
        if (MapUtils.isNotEmpty(news.getParameters())) {
          newsPageProperties.putAll(news.getParameters());
        }
        existingPageMetadataItem.setProperties(newsPageProperties);
        Date updateDate = Calendar.getInstance().getTime();
        existingPageMetadataItem.setUpdatedDate(updateDate.getTime());
        metadataService.updateMetadataItem(existingPageMetadataItem, Long.parseLong(newsArticleUpdaterIdentityId), false);
      } else {
        throw new ObjectNotFoundException("No such news article metadata item exists with id " + newsId);
      }

      // create the version
      if (newsUpdateType.equalsIgnoreCase(CONTENT_AND_TITLE.name())) {
        noteService.createVersionOfNote(existingPage, updater.getUserId());
        PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.valueOf(news.getId()), news.getLang());
        news.setLatestVersionId(pageVersion.getId());
        news.setBody(pageVersion.getContent());
        // remove the draft
        DraftPage draftPage = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(existingPage.getId()),
                                                                                       updater.getUserId(),
                                                                                       null);
        if (draftPage != null) {
          deleteDraftArticle(draftPage.getId(), updater.getUserId());
        }
      }
      Map<String, String> metadataItemProperties = existingPageMetadataItem.getProperties();
      if (metadataItemProperties.containsKey(NEWS_ACTIVITIES) && metadataItemProperties.get(NEWS_ACTIVITIES) != null) {
        String[] articleActivities = metadataItemProperties.get(NEWS_ACTIVITIES).split(";");
        List<Space> articleSpaces = new ArrayList<>();
        for (int i = 0; i < articleActivities.length; i++) {
          String sharedInSpaceId = articleActivities[i].split(":")[0];
          Space space = spaceService.getSpaceById(sharedInSpaceId);
          if (space != null) {
            articleSpaces.add(space);
          }
        }
        updateArticlePermissions(articleSpaces, news);
      }
      return news;
    }
    return null;
  }

  private News buildArticleWithExpand(String newsId,
                                      Identity currentIdentity,
                                      String lang,
                                      boolean fetchOriginal,
                                      String expand) {
    News article = buildArticle(newsId, currentIdentity, lang, fetchOriginal);
    if (article == null) {
      return null;
    }
    if (expand != null && StringUtils.isNotBlank(article.getActivityId())) {
      List<String> expandFields = Arrays.asList(expand.split(","));
      if (expandFields.contains("activityReactions")) {
        ExoSocialActivity activity = null;
        try {
          activity = activityManager.getActivity(article.getActivityId());
        } catch (Exception e) {
          LOG.debug("Error getting activity of News with id {}", article.getActivityId(), e);
        }
        if (activity != null) {
          RealtimeListAccess<ExoSocialActivity> listAccess =
              activityManager.getCommentsWithListAccess(activity, true);
          article.setCommentsCount(listAccess.getSize());
          article.setLikesCount(activity.getLikeIdentityIds() == null ? 0 : activity.getLikeIdentityIds().length);
          article.setCategories(activity.getCategoryIds());
        }
      }
    }
    return article;
  }

  private News buildArticle(String newsId, Identity currentIdentity, String lang, boolean fetchOriginal) {
    if (StringUtils.isNumeric(newsId)) {
      Page articlePage = noteService.getNoteById(newsId);
      Identity userIdentity = currentIdentity != null ? currentIdentity : getCurrentIdentity();
      String currentUsername = userIdentity == null ? null : userIdentity.getUserId();
      if (articlePage != null) {
        Space space = spaceService.getSpaceByGroupId(articlePage.getWikiOwner());
        // fetch the last version of the given lang
        PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(articlePage.getId()), lang);
        if (pageVersion == null && fetchOriginal) {
          pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(articlePage.getId()), null);
        }
        News news = new News();
        news.setId(articlePage.getId());
        news.setCreationDate(articlePage.getCreatedDate());
        news.setAuthor(pageVersion != null ? pageVersion.getAuthor() : articlePage.getAuthor());
        news.setUpdater(pageVersion != null ? pageVersion.getAuthor() : articlePage.getAuthor());
        news.setOwner(articlePage.getOwner());
        news.setSpaceId(space.getId());
        news.setSpaceAvatarUrl(space.getAvatarUrl());
        news.setSpaceDisplayName(space.getDisplayName());
        boolean hiddenSpace = space.getVisibility().equals(Space.HIDDEN) && !spaceService.canViewSpace(space, currentUsername);
        news.setHiddenSpace(hiddenSpace);
        news.setSpaceMember(spaceService.isMember(space, currentUsername));
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

        // fetch related metadata item properties
        NewsPageObject newsPageObject = new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                           articlePage.getId(),
                                                           null,
                                                           Long.parseLong(space.getId()));
        List<MetadataItem> metadataItems = metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY, newsPageObject);

        if (metadataItems.isEmpty()) {
          return null;
        }

        MetadataItem metadataItem = metadataItems.getFirst();
        buildArticleProperties(news, currentUsername, metadataItem);
        news.setDeleted(articlePage.isDeleted());
        news.setPublicationDate(articlePage.getCreatedDate());
        news.setTitle(pageVersion != null ? pageVersion.getTitle() : articlePage.getTitle());
        processPageContent(pageVersion, news, lang);
        news.setUpdaterFullName(pageVersion.getAuthorFullName());
        news.setLang(pageVersion.getLang());
        news.setUpdateDate(metadataItem != null ? new Date(metadataItem.getUpdatedDate()) : articlePage.getUpdatedDate());
        news.setProperties(pageVersion.getProperties());
        news.setUrl(NewsUtils.buildNewsArticleUrl(news, currentUsername));
        news.setLatestVersionId(pageVersion.getId());
        if (news.getProperties() != null && news.getProperties().getFeaturedImage() != null
            && news.getProperties().getFeaturedImage().getId() != 0) {
          news.setIllustrationURL(NewsUtils.buildIllustrationUrl(news.getProperties(), pageVersion.getLang()));

        }
        return news;
      }
    }
    return null;
  }

  private News createOrUpdateDraftArticleForExistingPage(News news, String updater, Space space) throws Exception {
    String pageId = news.getTargetPageId() != null ? news.getTargetPageId() : news.getId();
    Page existingPage = noteService.getNoteById(pageId);
    if (existingPage == null) {
      return null;
    }
    DraftPage draftPage =
                        noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(pageId), updater, news.getLang());
    if (draftPage == null) {
      news = createDraftForExistingPage(news, updater, existingPage, System.currentTimeMillis(), space);
    } else {
      news = updateDraftArticleForExistingPage(news, updater, existingPage, draftPage, space);
    }
    return news;
  }

  private News updateDraftArticleForExistingPage(News news, String updater, Page page, DraftPage draftPage, Space space) {
    try {
      draftPage.setTitle(news.getTitle());
      draftPage.setContent(news.getBody());
      draftPage.setAuthor(news.getAuthor());
      draftPage.setTargetPageId(page.getId());
      draftPage.setLang(news.getLang());
      draftPage.setProperties(news.getProperties());
      draftPage.setAttachmentObjectType(NewsPageAttachmentPlugin.OBJECT_TYPE);

      draftPage = noteService.updateDraftForExistPage(draftPage, page, null, System.currentTimeMillis(), updater);
      news.setId(draftPage.getId());
      news.setDraftUpdateDate(draftPage.getUpdatedDate());
      news.setDraftUpdater(draftPage.getAuthor());
      news.setTargetPageId(draftPage.getTargetPageId());
      news.setProperties(draftPage.getProperties());
      news.setBody(draftPage.getContent());
      news.setIllustrationURL(NewsUtils.buildIllustrationUrl(draftPage.getProperties(), news.getLang()));

      NewsLatestDraftObject latestDraftObject = new NewsLatestDraftObject(NEWS_METADATA_LATEST_DRAFT_OBJECT_TYPE,
                                                                          draftPage.getId(),
                                                                          page.getId(),
                                                                          Long.parseLong(news.getSpaceId()));

      MetadataItem latestDraftArticleMetadataItem = metadataService
                                                                   .getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                                        latestDraftObject)
                                                                   .stream()
                                                                   .findFirst()
                                                                   .orElse(null);
      if (latestDraftArticleMetadataItem != null) {
        Map<String, String> latestDraftArticleMetadataItemProperties = latestDraftArticleMetadataItem.getProperties();
        if (latestDraftArticleMetadataItemProperties == null) {
          latestDraftArticleMetadataItemProperties = new HashMap<>();
        }
        setLatestDraftProperties(latestDraftArticleMetadataItemProperties, news);
        latestDraftArticleMetadataItem.setProperties(latestDraftArticleMetadataItemProperties);
        String draftArticleMetadataItemUpdaterIdentityId = identityManager.getOrCreateUserIdentity(updater).getId();
        metadataService.updateMetadataItem(latestDraftArticleMetadataItem,
                                           Long.parseLong(draftArticleMetadataItemUpdaterIdentityId),
                                           false);
      } else {
        Map<String, String> latestDraftArticleMetadataItemProperties = new HashMap<>();
        setLatestDraftProperties(latestDraftArticleMetadataItemProperties, news);
        metadataService.createMetadataItem(latestDraftObject, NEWS_METADATA_KEY, latestDraftArticleMetadataItemProperties, false);

      }
      // Update content permissions
      updateArticlePermissions(List.of(space), news);
    } catch (Exception exception) {
      return null;
    }
    return news;
  }

  private void setLatestDraftProperties(Map<String, String> properties, News news) {
    properties.put(NEWS_ACTIVITY_POSTED, String.valueOf(news.isActivityPosted()));
    setScheduleProperties(news, properties);
  }

  private News buildLatestDraftArticle(String parentPageId, Identity currentIdentity, String lang) throws Exception {
    Page parentPage = noteService.getNoteById(parentPageId);
    if (parentPage == null) {
      return null;
    }
    // if the latest draft exist return it , else return the article
    DraftPage latestDraft = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.parseLong(parentPageId),
                                                                                     currentIdentity.getUserId(),
                                                                                     lang);
    News parentArticle = buildArticle(parentPageId, currentIdentity, lang, true);
    if (latestDraft == null) {
      return parentArticle;
    }
    News draftArticle = buildDraftArticle(latestDraft.getId(), currentIdentity);

    draftArticle.setReferred(parentArticle.isReferred());
    draftArticle.setFromExternalPage(parentArticle.isFromExternalPage());
    draftArticle.setOwner(parentArticle.getOwner());
    return draftArticle;
  }

  private void processPageContent(Page page, News news, String lang) {
    String portalOwner = CommonsUtils.getCurrentPortalOwner();
    Locale locale = lang == null ? LocalizationFilter.getCurrentLocale() : LocaleUtils.toLocale(news.getLang());
    String body = page.getContent();
    String sanitizedBody = HTMLSanitizer.sanitize(body);
    sanitizedBody = sanitizedBody.replaceAll(HTML_AT_SYMBOL_ESCAPED_PATTERN, HTML_AT_SYMBOL_PATTERN);
    news.setBody(MentionUtils.substituteUsernames(portalOwner, sanitizedBody));
    news.setBody(MentionUtils.substituteRoleWithLocale(news.getBody(), locale));
    news.setOriginalBody(sanitizedBody);
  }

  private String parseAndNormalizeScheduleDate(String date, String timeZoneId) {
    if (StringUtils.isBlank(date) || date.equals("0")) {
      return null;
    }
    ZoneId userTimeZone = StringUtils.isBlank(timeZoneId) ? ZoneId.of("UTC") : ZoneId.of(timeZoneId);
    ZonedDateTime zonedDateTime = ZonedDateTime.parse(date);
    ZonedDateTime userZonedDateTime = zonedDateTime.withZoneSameInstant(userTimeZone);
    ZonedDateTime utcDateTime = userZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    return utcDateTime.format(outputFormatter);
  }

  private void setScheduleProperties(News news, Map<String, String> newsProperties) throws DateTimeParseException {
    String scheduledPostDate = parseAndNormalizeScheduleDate(news.getSchedulePostDate(), news.getTimeZoneId());
    String scheduledUnpublishDate = parseAndNormalizeScheduleDate(news.getScheduleUnpublishDate(), news.getTimeZoneId());
    if (scheduledPostDate != null) {
      newsProperties.put(SCHEDULE_POST_DATE, scheduledPostDate);
    } else {
      newsProperties.remove(SCHEDULE_POST_DATE);
    }
    if (scheduledUnpublishDate != null) {
      newsProperties.put(UNPUBLISH_SCHEDULED_DATE, scheduledUnpublishDate);
      newsProperties.put(UNPUBLISH_SCHEDULED, "true");
    } else {
      newsProperties.remove(UNPUBLISH_SCHEDULED_DATE);
      newsProperties.remove(UNPUBLISH_SCHEDULED);
    }
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
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(poster), false);
      news.setSchedulePostDate(null);
      news.setPublicationState(POSTED);
      return news;
    }
    return null;
  }

  private News addNewArticleVersionWithLang(News news, Identity versionCreator, Space space) throws Exception {
    News existingNews = getNewsArticleById(news.getId());
    String newsId = news.getTargetPageId() != null ? news.getTargetPageId() : news.getId();
    Page existingPage = noteService.getNoteById(newsId);
    if (existingPage != null) {
      existingPage.setAttachmentObjectType(NewsPageAttachmentPlugin.OBJECT_TYPE);
      existingPage = noteService.updateNote(existingPage, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, versionCreator, false);
      news.setPublicationState(POSTED);
      // update the metadata item
      MetadataItem metadataItem =
                                metadataService.getMetadataItemsByMetadataAndObject(NEWS_METADATA_KEY,
                                                                                    new NewsPageObject(NEWS_METADATA_PAGE_OBJECT_TYPE,
                                                                                                       newsId,
                                                                                                       null,
                                                                                                       Long.parseLong(existingNews.getSpaceId())))
                                               .stream()
                                               .findFirst()
                                               .orElse(null);
      if (metadataItem != null) {
        Calendar calendar = Calendar.getInstance();
        metadataItem.setUpdatedDate(calendar.getTime().getTime());
        metadataService.updateMetadataItem(metadataItem,
                                           Long.parseLong(identityManager.getOrCreateUserIdentity(versionCreator.getUserId())
                                                                         .getId()),
                                           false);
      }
      existingPage.setTitle(news.getTitle());
      existingPage.setContent(news.getBody());
      existingPage.setLang(news.getLang());
      NotePageProperties properties = news.getProperties();
      if (properties != null) {
        properties.setDraft(false);
      }
      existingPage.setProperties(properties);
      noteService.createVersionOfNote(existingPage, versionCreator.getUserId());
      PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.valueOf(newsId), news.getLang());
      news.setLatestVersionId(pageVersion.getId());
      news.setBody(pageVersion.getContent());
      news.setIllustrationURL(NewsUtils.buildIllustrationUrl(news.getProperties(), news.getLang()));
      DraftPage draftPage = noteService.getLatestDraftPageByTargetPageAndLang(Long.parseLong(newsId), news.getLang());
      if (draftPage != null) {
        deleteDraftArticle(draftPage.getId(), draftPage.getAuthor());
      }
      NewsUtils.broadcastEvent(NewsUtils.ADD_ARTICLE_TRANSLATION, versionCreator.getUserId(), news);
      if (StringUtils.isNotEmpty(news.getLang())) {
        String newsTranslationId = news.getId().concat("-").concat(news.getLang());
        indexingService.index(NewsIndexingServiceConnector.TYPE, newsTranslationId);
      }
      updateArticlePermissions(List.of(space), news);
      return news;
    }
    return null;
  }

  private void updateArticlePermissions(List<Space> spaces, News article) {
    Map<String, Object> updateContentPermissionEventListenerData = new HashMap<>();
    if (CollectionUtils.isNotEmpty(article.getSharedInSpacesList())) {
      List<Space> spaceList = article.getSharedInSpacesList().stream().map(spaceId -> {
        try {
          return spaceService.getSpaceById(spaceId);
        } catch (Exception e) {
          return null;
        }
      }).filter(Objects::nonNull).toList();
      // create new ref to avoid ImmutableCollections exception
      spaces = new ArrayList<>(spaces);
      spaces.addAll(spaceList);
    }
    updateContentPermissionEventListenerData.putAll(Map.of("spaces", spaces, ARTICLE_CONTENT, article.getBody()));
    String entityType = article.getPublicationState().equals(DRAFT) ? "WIKI_DRAFT_PAGES" : "WIKI_PAGE_VERSIONS";
    String entityId = entityType.equals("WIKI_DRAFT_PAGES") ? article.getId() : article.getLatestVersionId();
    updateContentPermissionEventListenerData.put("entityId", entityId);
    updateContentPermissionEventListenerData.put("entityType", entityType);
    if (article.getAudience() != null) {
      updateContentPermissionEventListenerData.put(NEWS_AUDIENCE, article.getAudience());
    }
    NewsUtils.broadcastEvent(NewsUtils.UPDATE_CONTENT_PERMISSIONS, this, updateContentPermissionEventListenerData);
  }

  private void broadcastUnScheduleArticleEvent(News unscheduledArticle, String createdDraftId) {
    String unscheduledPageVersionId = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(unscheduledArticle.getId()),
                                                                                     unscheduledArticle.getLang())
                                                 .getId();
    if (unscheduledPageVersionId != null) {
      Map<String, String> eventData = new HashMap();
      eventData.put("draftPageId", createdDraftId);
      eventData.put("unscheduledPageVersionId", unscheduledPageVersionId);
      NewsUtils.broadcastEvent("note.draft.for.new.page.created", this, eventData);
    }
  }

  private void linkActivityCategories(ExoSocialActivity activity, List<Long> categories) {
    if (activity.getSpaceId() == null || (activity.getCategoryIds() == null && categories == null)) {
      return;
    }
    CategoryObject activityCategoryObject =
                                          new CategoryObject("activity", activity.getId(), Long.parseLong(activity.getSpaceId()));
    Set<Long> currentCategories = activity.getCategoryIds() != null ? new HashSet<>(activity.getCategoryIds())
                                                                    : Collections.emptySet();
    Set<Long> newCategories = categories != null ? new HashSet<>(categories) : Collections.emptySet();
    CategoryLinkService categoryLinkService = CommonsUtils.getService(CategoryLinkService.class);
    for (Long categoryId : currentCategories) {
      if (!newCategories.contains(categoryId)) {
        categoryLinkService.unlink(categoryId, activityCategoryObject);
      }
    }
    for (Long categoryId : newCategories) {
      if (!currentCategories.contains(categoryId)) {
        categoryLinkService.link(categoryId, activityCategoryObject);
      }
    }
  }
}
