/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.news.mcp;

import static io.meeds.mcp.server.tool.util.McpToolPluginUtils.getInteger;
import static io.meeds.mcp.server.util.McpToolUtils.formatDate;
import static io.meeds.mcp.server.util.McpToolUtils.markdownToHtml;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.news.mcp.model.NewsModel;
import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.service.NewsService;
import io.meeds.content.news.utils.NewsUtils;
import io.meeds.content.news.utils.NewsUtils.NewsObjectType;
import io.meeds.content.news.utils.NewsUtils.NewsUpdateType;
import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.model.ActivityModel;
import io.meeds.mcp.server.tool.model.SpaceModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.ActivityToolUtils;
import io.meeds.mcp.server.tool.util.SpaceToolUtils;
import io.meeds.mcp.server.tool.util.UploadToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@Service
@Profile("mcp-server")
public class NewsMcpTool implements McpToolPlugin {

  private static final String     EDIT_NEWS_URL = "/portal/%s/news-editor?spaceId=%s&newsId=%s&type=latest_draft";

  private ActivityManager         activityManager;

  private PermanentLinkService    permanentLinkService;

  private I18NActivityProcessor   i18NActivityProcessor;

  private SpaceService            spaceService;

  private IdentityManager         identityManager;

  private NewsService             newsService;

  private UserACL                 userAcl;

  private ProfilePropertyService  profilePropertyService;

  private TranslationService      translationService;

  private UserPortalConfigService portalConfigService;

  private NoteService             noteService;

  private UploadService           uploadService;

  private AttachmentService       attachmentService;

  private FileService             fileService;

  public NewsMcpTool(PortalContainer container) {
    this.spaceService = container.getComponentInstanceOfType(SpaceService.class);
    this.activityManager = container.getComponentInstanceOfType(ActivityManager.class);
    this.permanentLinkService = container.getComponentInstanceOfType(PermanentLinkService.class);
    this.i18NActivityProcessor = container.getComponentInstanceOfType(I18NActivityProcessor.class);
    this.identityManager = container.getComponentInstanceOfType(IdentityManager.class);
    this.newsService = container.getComponentInstanceOfType(NewsService.class);
    this.userAcl = container.getComponentInstanceOfType(UserACL.class);
    this.profilePropertyService = container.getComponentInstanceOfType(ProfilePropertyService.class);
    this.translationService = container.getComponentInstanceOfType(TranslationService.class);
    this.portalConfigService = container.getComponentInstanceOfType(UserPortalConfigService.class);
    this.noteService = container.getComponentInstanceOfType(NoteService.class);
    this.uploadService = container.getComponentInstanceOfType(UploadService.class);
    this.attachmentService = container.getComponentInstanceOfType(AttachmentService.class);
    this.fileService = container.getComponentInstanceOfType(FileService.class);
  }

  @SneakyThrows
  public ActivityModel publishNewsInActivityStream(long newsId) {
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    News news = newsService.getNewsById(String.valueOf(newsId),
                                        currentIdentity,
                                        false,
                                        NewsObjectType.ARTICLE.name().toLowerCase());
    if (news == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't exist. Use 'search_news' to search for a news bi title, summary or content.
          """.formatted(newsId));
    }
    Space space = spaceService.getSpaceById(news.getSpaceId());
    if (space == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't have an associated space to post into.
          """.formatted(newsId));
    }
    if (!spaceService.canPublishOnSpace(space, currentUsername)) {
      throw new IllegalAccessException("""
          The current user can't publish a news on the designated space '%s' with id '%s'.
          """.formatted(space.getDisplayName(), space.getSpaceId()));
    }
    news.setPublicationState(NewsService.POSTED);
    news.setPublisher(currentUsername);
    News draft = newsService.getNewsById(String.valueOf(newsId),
                                         userAcl.getUserIdentity(getCurrentUserName()),
                                         false,
                                         NewsObjectType.LATEST_DRAFT.name().toLowerCase());
    if (draft != null && !StringUtils.equals(draft.getId(), news.getId())) {
      news.setTitle(draft.getTitle());
      news.setBody(draft.getBody());
      news.setUpdater(currentUsername);
      news = newsService.updateNews(news,
                                    currentIdentity.getUserId(),
                                    true,
                                    news.isPublished(),
                                    NewsObjectType.ARTICLE.name().toLowerCase(),
                                    NewsUpdateType.CONTENT_AND_TITLE.name());
    } else {
      news = newsService.updateNews(news,
                                    currentIdentity.getUserId(),
                                    true,
                                    news.isPublished(),
                                    NewsObjectType.ARTICLE.name().toLowerCase(),
                                    NewsUpdateType.POSTING_AND_PUBLISHING.name());
    }
    newsService.postNews(news, currentUsername);
    news = newsService.getNewsById(String.valueOf(newsId),
                                   currentIdentity,
                                   false,
                                   NewsObjectType.ARTICLE.name().toLowerCase());
    return toActivityModel(news.getActivityId());
  }

  @SneakyThrows
  public NewsModel createNews(String title,
                              String summary,
                              String htmlContent,
                              long targetSpaceId) {
    if (targetSpaceId <= 0) {
      throw new IllegalArgumentException("""
          The news 'targetSpaceId' is mandatory.
          Use the tool get_my_spaces to search the destination space by name.
          Use the designated 'spaceId' field as input for 'targetSpaceId' parameter.
          """);
    }
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("The news 'title' is mandatory");
    }
    if (StringUtils.isBlank(htmlContent)) {
      throw new IllegalArgumentException("The news 'content' is mandatory");
    }
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    Space space = spaceService.getSpaceById(targetSpaceId);
    if (space == null) {
      throw new ObjectNotFoundException("""
          Space with id '%s' doesn't exist.
          Use the tool get_my_spaces to search the destination space by name.
          Use the designated 'spaceId' field as input for 'targetSpaceId' parameter.
          """.formatted(targetSpaceId));
    }
    if (!newsService.canCreateNews(space, currentIdentity)) {
      throw new IllegalAccessException("""
          The current user can't post a news on the designated space '%s' with id '%s'.
          """.formatted(space.getDisplayName(), space.getSpaceId()));
    }
    NotePageProperties properties = new NotePageProperties();
    properties.setSummary(summary);
    News news = new News();
    news.setProperties(properties);
    news.setSpaceId(String.valueOf(targetSpaceId));
    news.setTitle(title);
    news.setBody(markdownToHtml(htmlContent));
    news.setAudience(NewsUtils.SPACE_NEWS_AUDIENCE);
    news.setActivityPosted(true);
    news.getProperties().setDraft(false);
    news.setPublicationState(NewsService.POSTED);
    news.setAuthor(currentUsername);
    news.setUpdater(currentUsername);
    news.setPublisher(currentUsername);
    news = newsService.createNewsArticlePage(news, currentUsername);
    news = newsService.getNewsArticleById(news.getId());
    return toNewsModel(news, space);
  }

  // Updates a news (article) title, body and/or summary. Only the provided
  // fields are changed. Title/body go through NewsService#updateNews
  // (CONTENT_AND_TITLE) as before; the summary lives in the note metadata, so it
  // is persisted the same way as the cover image (NoteService#saveNoteMetadata,
  // language-aware), based on the loaded properties so the existing featured
  // image (cover) is preserved. The article is then refreshed (re-index +
  // recompute) so the new summary lands on the live article.
  @SneakyThrows
  public NewsModel updateNews(long newsId,
                              String title,
                              String summary,
                              String htmlContent,
                              String language) {
    checkNewsId(newsId);
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    News news = newsService.getNewsById(String.valueOf(newsId),
                                        currentIdentity,
                                        false,
                                        NewsObjectType.ARTICLE.name().toLowerCase());
    if (news == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't exist. Use 'search_news' to search for a news bi title, summary or content.
          """.formatted(newsId));
    }
    Space space = spaceService.getSpaceById(news.getSpaceId());
    if (space == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't have an associated space.
          """.formatted(newsId));
    }
    if (!newsService.canEditNews(news, currentUsername)) {
      throw new IllegalAccessException("""
          The current user doesn't have priviledges to update the news with id '%s'.
          """.formatted(space.getDisplayName(), space.getSpaceId()));
    }
    boolean titleOrContentChanged = false;
    if (StringUtils.isNotBlank(title)) {
      news.setTitle(title);
      titleOrContentChanged = true;
    }
    if (StringUtils.isNotBlank(htmlContent)) {
      news.setBody(markdownToHtml(htmlContent));
      titleOrContentChanged = true;
    }
    // Persist title/body first (CONTENT_AND_TITLE ignores metadata, so it never
    // touches the summary).
    if (titleOrContentChanged) {
      newsService.updateNews(news,
                             currentIdentity.getUserId(),
                             false,
                             news.isPublished(),
                             NewsObjectType.ARTICLE.name().toLowerCase(),
                             NewsUpdateType.CONTENT_AND_TITLE.name());
    }
    // The summary is stored in the note metadata, which NewsUpdateType.* never
    // writes; persist it the same way the cover image is persisted, basing the
    // write on the loaded properties so the current featured image is preserved.
    if (StringUtils.isNotBlank(summary)) {
      // for a published article news_id == the note page id; a draft/staged
      // article points at its target page id
      long pageId = news.getTargetPageId() != null ? Long.parseLong(news.getTargetPageId()) : newsId;
      boolean isDraft = news.getTargetPageId() != null;
      // base the write on the target language's own metadata so the current
      // featured image (cover) of that translation is preserved, not clobbered
      // with the default's
      NotePageProperties properties = resolveBaseProperties(news, pageId, language, currentIdentity);
      properties.setSummary(summary);
      properties.setNoteId(pageId);
      properties.setDraft(isDraft);
      String lang = StringUtils.isBlank(language) ? news.getLang() : language;
      noteService.saveNoteMetadata(properties, lang, getUserIdentityId(currentUsername));
    }
    // Refresh the news lifecycle (re-index + recompute) so the metadata summary
    // lands on the live article; CONTENT_AND_TITLE ignores metadata, so the
    // just-saved summary is not reverted.
    return refreshAndModel(newsId, currentIdentity, space);
  }

  @SneakyThrows
  public void deleteNews(long newsId) {
    checkNewsId(newsId);
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    News news = newsService.getNewsById(String.valueOf(newsId),
                                        currentIdentity,
                                        false,
                                        NewsObjectType.ARTICLE.name().toLowerCase());
    if (news == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' wasn't found.
          Use 'search_news' Tool to find News details by a search term.
          """);
    }
    Space space = spaceService.getSpaceById(news.getSpaceId());
    if (space == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't have an associated space to post into.
          """.formatted(newsId));
    }
    if (!newsService.canEditNews(news, currentUsername)) {
      throw new IllegalAccessException("""
          The current user can't post a news on the designated space '%s' with id '%s'.
          """.formatted(space.getDisplayName(), space.getSpaceId()));
    }
    newsService.deleteNews(news.getId(), currentIdentity, NewsObjectType.ARTICLE.name());
  }

  @SneakyThrows
  public List<NewsModel> getNewsList(Long targetSpaceId,
                                     Integer offset,
                                     Integer limit) {
    NewsFilter filter = new NewsFilter();
    filter.setOffset(getInteger(offset, DEFAULT_OFFSET));
    filter.setLimit(getInteger(limit, DEFAULT_LIMIT));
    if (targetSpaceId != null && targetSpaceId.longValue() > 0) {
      filter.setSpaces(List.of(String.valueOf(targetSpaceId)));
    }
    filter.setSortField("UPDATED_DATE");
    filter.setSortDirection("desc");
    String username = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(username);
    List<News> newsList = newsService.getNews(filter, currentIdentity);
    Space space = targetSpaceId == null ? null : spaceService.getSpaceById(targetSpaceId);
    return newsList.stream().map(news -> toNewsModel(news, space)).toList();
  }

  @SneakyThrows
  public List<NewsModel> searchNews(String query,
                                    Long targetSpaceId,
                                    Integer offset,
                                    Integer limit) {
    NewsFilter filter = new NewsFilter();
    filter.setSearchText(query);
    filter.setOffset(getInteger(offset, DEFAULT_OFFSET));
    filter.setLimit(getInteger(limit, DEFAULT_LIMIT));
    if (targetSpaceId != null && targetSpaceId.longValue() > 0) {
      filter.setSpaces(List.of(String.valueOf(targetSpaceId)));
    }
    filter.setSortField("UPDATED_DATE");
    filter.setSortDirection("desc");
    String currentUsername = getCurrentUserName();
    List<News> newsList = newsService.searchNews(filter, identityManager.getOrCreateUserIdentity(currentUsername));
    Space space = targetSpaceId == null ? null : spaceService.getSpaceById(targetSpaceId);
    return newsList.stream()
                   .map(news -> toNewsModel(news,
                                            space == null ? spaceService.getSpaceById(news.getSpaceId()) : space))
                   .toList();
  }

  @SneakyThrows
  public NewsModel getNewsById(long newsId) {
    checkNewsId(newsId);
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    News news = newsService.getNewsById(String.valueOf(newsId),
                                        currentIdentity,
                                        false,
                                        NewsObjectType.ARTICLE.name().toLowerCase());
    if (news == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' wasn't found.
          Use 'search_news' Tool to find News details by a search term.
          """);
    } else if (!newsService.canViewNews(news, currentUsername)) {
      throw new IllegalAccessException("""
          The current user can't access the news with id '%s'.
          """.formatted(newsId));
    }
    Space space = spaceService.getSpaceById(news.getSpaceId());
    return toNewsModel(news, space);
  }

  // Sets a news (article) cover / illustration from exactly one of a public
  // http(s) URL, base64 bytes, or an ACL-checked reference to an existing
  // platform attachment. The cover is shared with the backing note's featured
  // image, so it is written onto the article note's metadata and then the news
  // lifecycle is refreshed (search re-index + linked activity thumbnail).
  @SneakyThrows
  public NewsModel setNewsIllustration(long newsId,
                                       String imageUrl,
                                       String imageBase64,
                                       String attachmentObjectType,
                                       String attachmentObjectId,
                                       String altText,
                                       String language) {
    checkNewsId(newsId);
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    News news = newsService.getNewsById(String.valueOf(newsId),
                                        currentIdentity,
                                        false,
                                        NewsObjectType.ARTICLE.name().toLowerCase());
    if (news == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' wasn't found.
          Use 'search_news' Tool to find News details by a search term.
          """.formatted(newsId));
    }
    Space space = spaceService.getSpaceById(news.getSpaceId());
    if (space == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't have an associated space.
          """.formatted(newsId));
    }
    if (!newsService.canEditNews(news, currentUsername)) {
      throw new IllegalAccessException("""
          The current user doesn't have priviledges to update the news with id '%s'.
          """.formatted(newsId));
    }
    UploadToolUtils.FetchedContent image = UploadToolUtils.resolveImage(attachmentService,
                                                                        fileService,
                                                                        getCurrentUserAclIdentity(),
                                                                        new UploadToolUtils.ImageSource(imageUrl,
                                                                                                        imageBase64,
                                                                                                        attachmentObjectType,
                                                                                                        attachmentObjectId),
                                                                        UploadToolUtils.DEFAULT_MAX_BYTES);
    String uploadId = UploadToolUtils.materialize(uploadService, image.bytes(), image.fileName(), image.mimeType());
    // for a published article news_id == the note page id; a draft/staged article
    // points at its target page id
    long pageId = news.getTargetPageId() != null ? Long.parseLong(news.getTargetPageId()) : newsId;
    boolean isDraft = news.getTargetPageId() != null;
    try {
      NotePageProperties properties = resolveBaseProperties(news, pageId, language, currentIdentity);
      NoteFeaturedImage featuredImage = new NoteFeaturedImage();
      NoteFeaturedImage existing = properties.getFeaturedImage();
      if (existing != null && existing.getId() != null && existing.getId() > 0) {
        featuredImage.setId(existing.getId()); // update the existing cover file in place
      }
      featuredImage.setUploadId(uploadId);
      featuredImage.setMimeType(image.mimeType());
      featuredImage.setFileName(image.fileName());
      featuredImage.setAltText(altText);
      properties.setNoteId(pageId);
      properties.setDraft(isDraft);
      properties.setFeaturedImage(featuredImage);
      String lang = StringUtils.isBlank(language) ? news.getLang() : language;
      noteService.saveNoteMetadata(properties, lang, getUserIdentityId(currentUsername));
    } catch (Exception e) {
      UploadToolUtils.release(uploadService, uploadId);
      throw new IllegalStateException("Could not set the news cover image: " + e.getMessage());
    }
    return refreshAndModel(newsId, currentIdentity, space);
  }

  // Removes a news (article) cover / illustration. Mirrors the note featured
  // image removal, then refreshes the news lifecycle.
  @SneakyThrows
  public NewsModel removeNewsIllustration(long newsId, String language) {
    checkNewsId(newsId);
    String currentUsername = getCurrentUserName();
    org.exoplatform.services.security.Identity currentIdentity = userAcl.getUserIdentity(currentUsername);
    News news = newsService.getNewsById(String.valueOf(newsId),
                                        currentIdentity,
                                        false,
                                        NewsObjectType.ARTICLE.name().toLowerCase());
    if (news == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' wasn't found.
          Use 'search_news' Tool to find News details by a search term.
          """.formatted(newsId));
    }
    Space space = spaceService.getSpaceById(news.getSpaceId());
    if (space == null) {
      throw new ObjectNotFoundException("""
          News with id '%s' doesn't have an associated space.
          """.formatted(newsId));
    }
    if (!newsService.canEditNews(news, currentUsername)) {
      throw new IllegalAccessException("""
          The current user doesn't have priviledges to update the news with id '%s'.
          """.formatted(newsId));
    }
    long pageId = news.getTargetPageId() != null ? Long.parseLong(news.getTargetPageId()) : newsId;
    boolean isDraft = news.getTargetPageId() != null;
    // resolve the existing cover from the target language's own metadata, so a
    // translation's own cover is removed rather than the default's
    NotePageProperties properties = resolveBaseProperties(news, pageId, language, currentIdentity);
    NoteFeaturedImage existing = properties.getFeaturedImage();
    if (existing == null || existing.getId() == null || existing.getId() <= 0) {
      throw new ObjectNotFoundException("News with id '%s' has no cover image to remove.".formatted(newsId));
    }
    try {
      String lang = StringUtils.isBlank(language) ? news.getLang() : language;
      noteService.removeNoteFeaturedImage(pageId, existing.getId(), lang, isDraft, getUserIdentityId(currentUsername));
    } catch (Exception e) {
      throw new IllegalStateException("Could not remove the news cover image: " + e.getMessage());
    }
    return refreshAndModel(newsId, currentIdentity, space);
  }

  // Refreshes the news lifecycle after a cover metadata change (re-index +
  // linked activity thumbnail + illustrationURL recompute) by going through the
  // same NewsService#updateNews path the native editor uses, then returns the
  // up-to-date model.
  private NewsModel refreshAndModel(long newsId,
                                    org.exoplatform.services.security.Identity currentIdentity,
                                    Space space) throws Exception {
    News refreshed = newsService.getNewsById(String.valueOf(newsId),
                                             currentIdentity,
                                             false,
                                             NewsObjectType.ARTICLE.name().toLowerCase());
    News updated = newsService.updateNews(refreshed,
                                          currentIdentity.getUserId(),
                                          false,
                                          refreshed.isPublished(),
                                          NewsObjectType.ARTICLE.name().toLowerCase(),
                                          NewsUpdateType.CONTENT_AND_TITLE.name());
    return toNewsModel(updated, space);
  }

  // When a language is provided, the base metadata (summary + cover) must come
  // from THAT language's backing note, not the default article's: covers and
  // summaries are stored per language, so carrying the default's properties into
  // an EXISTING translation would clobber the translation's own distinct
  // cover/summary with the default's. A brand-new translation (no per-language
  // metadata yet) still inherits the default article's properties. The
  // default-language path (blank language) keeps using the loaded article's
  // properties.
  private NotePageProperties resolveBaseProperties(News news,
                                                   long pageId,
                                                   String language,
                                                   org.exoplatform.services.security.Identity currentIdentity) {
    NotePageProperties defaultProperties = news.getProperties() != null ? news.getProperties() : new NotePageProperties();
    if (StringUtils.isBlank(language)) {
      return defaultProperties;
    }
    try {
      Page langNote = noteService.getNoteByIdAndLang(pageId, currentIdentity, null, language);
      if (langNote != null && langNote.getProperties() != null) {
        return langNote.getProperties();
      }
    } catch (Exception e) {
      // fall back to the default article's properties (new-translation inheritance)
    }
    return defaultProperties;
  }

  private long getUserIdentityId(String username) {
    return Long.parseLong(identityManager.getOrCreateUserIdentity(username).getId());
  }

  private void checkNewsId(long newsId) {
    if (newsId <= 0) {
      throw new IllegalArgumentException("""
          The field 'newsId' is mandatory.
          Use 'search_news' Tool to find News details by a search term.
          """);
    }
  }

  private NewsModel toNewsModel(News news, Space space) {
    String currentUserName = getCurrentUserName();
    UserModel publisherUser = null;
    if (StringUtils.isNotBlank(news.getPublisher())) {
      publisherUser = UserToolUtils.toUserModel(identityManager,
                                                profilePropertyService,
                                                userAcl,
                                                translationService,
                                                portalConfigService,
                                                news.getPublisher(),
                                                currentUserName,
                                                getCurrentUserLocale(currentUserName),
                                                true);

    }
    if (space == null) {
      space = spaceService.getSpaceById(news.getSpaceId());
    }
    SpaceModel targetSpace = SpaceToolUtils.toSpaceModel(spaceService,
                                                         space,
                                                         currentUserName);
    String baseUrl = CommonsUtils.getCurrentDomain();
    return new NewsModel(Long.parseLong(news.getId()),
                         news.getTitle(),
                         news.getProperties().getSummary(),
                         news.getBody(),
                         baseUrl + news.getUrl(),
                         baseUrl + getNewsEditUrl(news, currentUserName),
                         news.isPublished(),
                         !news.isPublished(),
                         getPublicationDate(news),
                         news.getCommentsCount(),
                         news.getLikesCount(),
                         news.getViewsCount() == null ? 0 : news.getViewsCount().intValue(),
                         news.isActivityPosted(),
                         getActivityId(news),
                         publisherUser,
                         targetSpace,
                         news.getIllustrationURL());
  }

  private long getActivityId(News news) {
    return StringUtils.isBlank(news.getActivityId()) ? 0 : Long.parseLong(news.getActivityId());
  }

  private String getPublicationDate(News news) {
    return news.getPublicationDate() == null ? null : formatDate(news.getPublicationDate());
  }

  private String getNewsEditUrl(News news, String currentUserName) {
    return EDIT_NEWS_URL.formatted(portalConfigService.getDefaultSite(currentUserName).getName(),
                                   news.getSpaceId(),
                                   news.getId());
  }

  private ActivityModel toActivityModel(String activityId) {
    return ActivityToolUtils.toActivityModel(activityManager,
                                             spaceService,
                                             identityManager,
                                             userAcl,
                                             permanentLinkService,
                                             profilePropertyService,
                                             translationService,
                                             i18NActivityProcessor,
                                             portalConfigService,
                                             activityId,
                                             getCurrentUserAclIdentity(),
                                             getCurrentUserLocale());
  }

}
