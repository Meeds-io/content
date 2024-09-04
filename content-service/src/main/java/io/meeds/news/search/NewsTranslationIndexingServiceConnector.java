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
package io.meeds.news.search;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.search.DocumentWithMetadata;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.wiki.service.NoteService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NewsTranslationIndexingServiceConnector extends NewsIndexingServiceConnector {

  public static final String TYPE = "news-translation";

  private static final Log   LOG  = ExoLogger.getLogger(NewsTranslationIndexingServiceConnector.class);

  private final NoteService  noteService;

  public NewsTranslationIndexingServiceConnector(IdentityManager identityManager,
                                                 InitParams initParams,
                                                 NewsService newsService,
                                                 ActivityManager activityManager,
                                                 MetadataService metadataService,
                                                 NoteService noteService) {
    super(identityManager, initParams, newsService, activityManager, metadataService);
    this.noteService = noteService;

  }

  @Override
  public String getConnectorName() {
    return TYPE;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id is mandatory");
    }
    String lang = null;
    String newsTranslationVersionId = null;
    if (id.contains("-")) {
      id = StringUtils.substringBefore(id, "-");
      lang = StringUtils.substringAfter(id, "-");
    }
    LOG.debug("Index document for news translation with id={} and lang={}", id, lang);
    News news = null;
    try {
      news = newsService.getNewsArticleByIdAndLang(id, lang);
      newsTranslationVersionId = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(id), lang).getId();
    } catch (Exception e) {
      LOG.error("Error when getting the news translation with id={} and lang={} " + id, lang, e);
    }
    if (news == null) {
      throw new IllegalStateException("news translation with id '" + id + "' is mandatory");
    }
    Map<String, String> fields = new HashMap<>();
    fields.put("id", newsTranslationVersionId);

    fields.put("title", news.getTitle());

    String body = news.getBody();
    String summary = "";
    if (news.getProperties() != null) {
      summary = news.getProperties().getSummary();
    }
    if (StringUtils.isBlank(body)) {
      body = news.getTitle();
    }
    // Ensure to index text only without html tags
    if (StringUtils.isNotBlank(body)) {
      body = StringEscapeUtils.unescapeHtml4(body);
      try {
        body = HTMLSanitizer.sanitize(body);
      } catch (Exception e) {
        LOG.warn("Error sanitizing news '{}' body", news.getId());
      }
      body = htmlToText(body);
      fields.put("body", body);
    }

    if (StringUtils.isNotBlank(summary)) {
      summary = StringEscapeUtils.unescapeHtml4(summary);
      try {
        summary = HTMLSanitizer.sanitize(summary);
      } catch (Exception e) {
        LOG.warn("Error sanitizing news '{}' summary", news.getId());
      }
      summary = htmlToText(summary);
      fields.put("summary", summary);
    }

    if (StringUtils.isNotBlank(news.getAuthor())) {
      fields.put("posterId", news.getAuthor());
      Identity posterIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, news.getAuthor());
      if (posterIdentity != null && posterIdentity.getProfile() != null
          && StringUtils.isNotBlank(posterIdentity.getProfile().getFullName())) {
        fields.put("posterName", posterIdentity.getProfile().getFullName());
      }
    }
    if (news.getSpaceDisplayName() != null) {
      fields.put("spaceDisplayName", news.getSpaceDisplayName());
    }

    String newsActivities = news.getActivities();
    String ownerIdentityId = null;

    if (newsActivities != null) {
      String newsActivityId = newsActivities.split(";")[0].split(":")[1];
      fields.put("newsActivityId", newsActivityId);
      ExoSocialActivity newsActivity = activityManager.getActivity(newsActivityId);
      ActivityStream activityStream = newsActivity.getActivityStream();

      if (newsActivity.getParentId() != null
          && (activityStream == null || activityStream.getType() == null || StringUtils.isBlank(activityStream.getPrettyId()))) {
        ExoSocialActivity parentActivity = activityManager.getActivity(newsActivity.getParentId());
        activityStream = parentActivity.getActivityStream();
      }

      if (activityStream != null && activityStream.getType() != null && StringUtils.isNotBlank(activityStream.getPrettyId())) {
        String prettyId = activityStream.getPrettyId();
        String providerId = activityStream.getType().getProviderId();
        Identity streamOwner = identityManager.getOrCreateIdentity(providerId, prettyId);
        ownerIdentityId = streamOwner.getId();
      }
    } else {
      return null;
    }
    if (news.getCreationDate() != null) {
      fields.put("postedTime", String.valueOf(news.getCreationDate().getTime()));
    }
    if (news.getUpdateDate() != null) {
      fields.put("lastUpdatedTime", String.valueOf(news.getUpdateDate().getTime()));
    }
    fields.put("language", lang);
    fields.put("originalNewsId", news.getId());

    DocumentWithMetadata document = new DocumentWithMetadata();
    document.setId(id);
    document.setLastUpdatedDate(news.getUpdateDate());
    document.setPermissions(Collections.singleton(ownerIdentityId));
    document.setFields(fields);
    addDocumentMetadata(document, id);

    return document;
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

}
