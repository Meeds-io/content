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
package io.meeds.news.rest;

import java.util.List;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.rest.entity.BaseEntity;
import org.exoplatform.social.rest.entity.IdentityEntity;

import io.meeds.news.search.NewsESSearchResult;

public class NewsSearchResultEntity extends BaseEntity {

  private static final long serialVersionUID = 1L;

  private String            title;

  private String            body;

  private String            spaceDisplayName;

  private String            newsUrl;

  private List<String>      excerpts;

  private long              postedTime;

  private long              lastUpdatedTime;

  private boolean           favorite;

  private String            activityId;

  private String            posterFullName;

  private String            posterUserName;

  public NewsSearchResultEntity() {
  }

  public NewsSearchResultEntity(NewsESSearchResult newsESSearchResult) {
    this.setId(String.valueOf(newsESSearchResult.getId()));
    this.body = newsESSearchResult.getBody();
    this.title = newsESSearchResult.getTitle();
    this.newsUrl = newsESSearchResult.getNewsUrl();
    this.spaceDisplayName = newsESSearchResult.getSpaceDisplayName();
    this.excerpts = newsESSearchResult.getExcerpts();
    this.postedTime = newsESSearchResult.getPostedTime();
    this.lastUpdatedTime = newsESSearchResult.getLastUpdatedTime();
    this.activityId = newsESSearchResult.getActivityId();
    this.posterFullName = newsESSearchResult.getPoster().getProfile().getFullName();
    this.posterUserName = newsESSearchResult.getPoster().getRemoteId();
  }
  public String getPosterFullName() {
    return posterFullName;
  }

  public void setPosterFullName(String posterFullName) {
    this.posterFullName = posterFullName;
  }

  public String getPosterUserName() {
    return posterUserName;
  }

  public void setPosterUserName(String posterUserName) {
    this.posterUserName = posterUserName;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public List<String> getExcerpts() {
    return excerpts;
  }

  public void setExcerpts(List<String> excerpts) {
    this.excerpts = excerpts;
  }

  public long getPostedTime() {
    return postedTime;
  }

  public void setPostedTime(long postedTime) {
    this.postedTime = postedTime;
  }

  public long getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime(long lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSpaceDisplayName() {
    return spaceDisplayName;
  }

  public void setSpaceDisplayName(String spaceDisplayName) {
    this.spaceDisplayName = spaceDisplayName;
  }

  public String getNewsUrl() {
    return newsUrl;
  }

  public void setNewsUrl(String newsUrl) {
    this.newsUrl = newsUrl;
  }

  public boolean isFavorite() {
    return favorite;
  }

  public void setFavorite(boolean favorite) {
    this.favorite = favorite;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

}
