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
package io.meeds.content.news.plugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.content.news.model.News;
import io.meeds.content.news.model.filter.NewsFilter;
import io.meeds.content.news.service.NewsService;
import io.meeds.social.cms.model.ContentLinkExtension;
import io.meeds.social.cms.model.ContentLinkSearchResult;
import io.meeds.social.cms.plugin.ContentLinkPlugin;
import io.meeds.social.cms.service.ContentLinkPluginService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
public class NewsContentLinkPlugin implements ContentLinkPlugin {

  public static final String                OBJECT_TYPE = NewsPermanentLinkPlugin.OBJECT_TYPE;

  private static final String               TITLE_KEY   = "contentLink.news";

  private static final String               ICON        = "fa fa-newspaper";

  private static final String               COMMAND     = "article";

  private static final ContentLinkExtension EXTENSION   = new ContentLinkExtension(OBJECT_TYPE,
                                                                                   TITLE_KEY,
                                                                                   ICON,
                                                                                   COMMAND);

  @Autowired
  private ContentLinkPluginService          contentLinkPluginService;

  @Autowired
  private NewsService                       newsService;

  @Autowired
  private UserACL                           userAcl;

  @Autowired
  private IdentityManager                   identityManager;

  @PostConstruct
  public void init() {
    contentLinkPluginService.addPlugin(this);
  }

  @Override
  public ContentLinkExtension getExtension() {
    return EXTENSION;
  }

  @Override
  @SneakyThrows
  public List<ContentLinkSearchResult> search(String keyword, Identity identity, Locale locale, int offset, int limit) {
    NewsFilter filter = new NewsFilter();
    filter.setLang(locale == null ? null : locale.toLanguageTag());
    filter.setSearchText(keyword);
    filter.setLimit(limit);
    filter.setOffset(offset);
    List<News> results = newsService.searchNews(filter,
                                                userAcl.isAnonymousUser(identity) ? null :
                                                                                  identityManager.getOrCreateUserIdentity(identity.getUserId()));
    return results.stream()
                  .map(searchResult -> toContentLink(searchResult, identity, locale))
                  .filter(Objects::nonNull)
                  .toList();
  }

  @Override
  @SneakyThrows
  public String getContentTitle(String objectId, Locale locale) {
    News news = locale == null ? newsService.getNewsArticleById(objectId) :
                               newsService.getNewsArticleByIdAndLang(objectId, locale.toLanguageTag());
    if (news == null && locale != null) {
      news = newsService.getNewsArticleById(objectId);
    }
    return news == null ? null : news.getTitle();
  }

  @SneakyThrows
  private ContentLinkSearchResult toContentLink(News news,
                                                Identity identity,
                                                Locale locale) {
    return new ContentLinkSearchResult(OBJECT_TYPE, news.getId(), news.getTitle(), EXTENSION.getIcon());
  }

}
