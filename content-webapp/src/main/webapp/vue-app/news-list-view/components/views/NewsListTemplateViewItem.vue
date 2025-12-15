<!--

    This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software Foundation,
    Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-->
<template>
  <a
    class="article-item-link"
    target="_self"
    :href="articleUrl">
    <div v-if="showArticleImage" class="article-item-image">
      <img
        :src="articleImage"
        :alt="featuredImageAltText"
        class="card-border-radius">
    </div>
    <div class="article-item-content">
      <span
        v-if="showArticleTitle"
        :class="extraClass"
        class="text-body">{{ item.title }}</span>
      <span
        v-if="showArticleSummary"
        :class="extraClass"
        class="text-subtitle">
        {{ item?.properties?.summary }}
      </span>
      <div class="d-flex text-subtitle mb-1 width-full">
        <span
          v-if="showArticleAuthor"
          :class="{
            'flex-shrink-1': truncateAuthorName,
            'flex-shrink-0' : !truncateAuthorName
          }"
          class="text-truncate flex-grow-0">
          {{ item.authorDisplayName }}
        </span>
        <v-icon
          v-if="showArticleSpace && showArticleAuthor"
          small>
          mdi-chevron-right
        </v-icon>
        <span 
          v-if="showArticleSpace"
          class="text-truncate flex-grow-1 flex-shrink-1">
          {{ item.spaceDisplayName }}
        </span>
      </div>
      <div class="text-no-wrap text-truncate d-flex text-subtitle">
        <span class="article-date me-2">
          <div v-if="showArticleDate" class="flex-column">
            <date-format
              :value="displayDate"
              :format="dateFormat" />
          </div>
        </span>
        <div v-if="showArticleReactions" class="reactions">
          <v-icon class="me-1" size="12">
            mdi-thumb-up
          </v-icon>
          <div class="likes-count me-2">{{ item.likesCount }}</div>
          <v-icon class="me-1" size="12">
            mdi-comment
          </v-icon>
          <div class="comments-count me-2">{{ item.commentsCount }}</div>
          <v-icon class="me-1" size="12">
            mdi-eye
          </v-icon>
          <div class="viewCount">{{ item.viewsCount }}</div>           
        </div>
      </div>
    </div>
  </a>
</template>

<script>
export default {
  props: {
    item: {
      type: Object,
      required: false,
      default: null
    },
    selectedOption: {
      type: Object,
      required: false,
      default: null
    },
  },
  data: ()=> ({
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
  }),
  computed: {
    truncateAuthorName() {
      return this.item?.authorDisplayName?.length > 15;
    },
    displayDate() {
      return this.item?.publishDate && new Date(this.item.publishDate);
    },
    showArticleImage() {
      return this.selectedOption?.showArticleImage;
    },
    hideAuthor() {
      return this.item?.properties?.hideAuthor;
    },
    showArticleAuthor() {
      return this.selectedOption?.showArticleAuthor && !this.hideAuthor ;
    },
    showArticleSpace() {
      return this.selectedOption?.showArticleSpace;
    },
    showArticleDate() {
      return this.selectedOption?.showArticleDate;
    },
    showArticleTitle() {
      return this.selectedOption?.showArticleTitle;
    },
    showArticleSummary() {
      return this.selectedOption?.showArticleSummary;
    },
    showArticleReactions() {
      return this.selectedOption?.showArticleReactions;
    },
    featuredImageAltText() {
      return this.item?.properties?.featuredImage?.altText || '';
    },
    articleImage() {
      return this.item?.illustrationURL?.concat('&size=70x70').toString() || '/content/images/news.png';
    },
    extraClass() {
      return (!this.showArticleSummary || !this.showArticleTitle) && 'text-truncate-2' || 'article-title' ;
    },
    articleUrl() {
      return eXo.env.portal.userName !== '' ? this.item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${this.item.id}&type=article`;
    }
  },
};
</script>
