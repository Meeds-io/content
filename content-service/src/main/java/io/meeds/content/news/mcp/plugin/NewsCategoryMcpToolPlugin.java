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
package io.meeds.content.news.mcp.plugin;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;

import io.meeds.content.news.model.News;
import io.meeds.content.news.plugin.NewsPermanentLinkPlugin;
import io.meeds.content.news.service.NewsService;
import io.meeds.mcp.server.tool.plugin.CategoryMcpToolPlugin;
import io.meeds.social.activity.plugin.ActivityCategoryPlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

@Service
@Profile("mcp-server")
public class NewsCategoryMcpToolPlugin implements CategoryMcpToolPlugin {

  @Autowired
  private ActivityManager     activityManager;

  @Autowired
  private NewsService         newsService;

  @Autowired
  private CategoryLinkService categoryLinkService;

  @Override
  public boolean match(String contentType, String contentId) {
    return NewsPermanentLinkPlugin.OBJECT_TYPE.equals(contentType);
  }

  @Override
  public void addCategory(long categoryId,
                          String contentType,
                          String contentId,
                          String username) throws IllegalAccessException,
                                           ObjectNotFoundException {
    News news = newsService.getNewsArticleById(contentId);
    if (news != null && StringUtils.isNotBlank(news.getActivityId())) {
      try {
        ExoSocialActivity activity = activityManager.getActivity(news.getActivityId());
        if (activity != null) {
          CategoryObject categoryObject = new CategoryObject(activity.getMetadataObject());
          if (activity.hasSpecificMetadataObject()) {
            categoryObject.setType(ActivityCategoryPlugin.OBJECT_TYPE);
            categoryObject.setId(activity.getId());
          }
          categoryLinkService.link(categoryId,
                                   categoryObject,
                                   username);
        }
      } catch (ObjectAlreadyExistsException e) {
        // No error to catch, since the object is already linked
      }
    }
  }

}
