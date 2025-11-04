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
    class="card card-border-radius"
    :href="articleUrl"
    target="_self"
    :class="{ 'keyboard-slide': slideActive }"
    tabindex="0">
    <div class="imgContainer">
      <img
        :src="articleImage"
        :alt="featuredImageAltText"
        class="article-illustration-img">
    </div>
    <div
      class="text-area"
      tabindex="0"
      role="link"
      :aria-label="$t('news.illustration.link.title', {0: item.title})"
      @keydown.enter.prevent="openArticle">
      <div
        class="upper-row">
        <div
          v-if="!isHiddenSpace && showArticleSpace"
          :id="`space-link-${item.activityId}`"
          class="space-link"
          :aria-label="$t('news.space.icon.title', { 0: item.spaceDisplayName })">
          <div class="article-space">
            <img
              class="space-icon"
              :src="item.spaceAvatarUrl"
              alt="">
            <div class="space-name text-subtitle">
              {{ item.spaceDisplayName }}
            </div>
          </div>
        </div>
        <div class="articleLink">
          <div v-if="showArticleTitle" class="article-title text-body">
            {{ item.title }}
          </div>
          <div class="d-flex text-no-wrap text-truncate text-subtitle">
            <div v-if="showArticleAuthor" class="author-name">
              {{ item.authorDisplayName }}
            </div>
            <span v-if="showArticleAuthor && showArticleDate">, </span>
            <div v-if="showArticleDate">
              <date-format
                :value="displayDate"
                :format="dateFormat" />
            </div>
          </div>
          <div
            v-if="showArticleSummary"
            class="d-flex mt-4 text-subtitle">
            {{ item?.properties?.summary }}
          </div>
          <div
            class="mt-2 align-self-center text-subtitle font-weight-bold primary--text">
            {{ $t('news.cards.readMore') }}
          </div>
        </div>
      </div>
    </div>
    <div
      v-if="showArticleReactions"
      class="bottom-row border-top-color pa-2 width-full b-0 position-absolute white-background text-subtitle">
      <div class="d-flex text-truncate text-no-wrap">
        <div class="width-fit-content">
          <div class="d-flex text-truncate text-subtitle">
            <v-icon size="14">mdi-thumb-up</v-icon>
            <span class="screen-reader-only">
              {{ $t('news.app.number.likes') }}
            </span>
            <div class="likes-count ms-1">
              {{ item.likesCount }}
            </div>
            <v-icon class="counters-icons mt-1 ml-2" size="14">
              mdi-comment
            </v-icon>
            <span class="screen-reader-only">
              {{ $t('news.app.number.comments') }}
            </span>
            <div class="comments-count ms-1">
              {{ item.commentsCount }}
            </div>
          </div>
        </div>
        <div class="articleLink">
          <div class="views">
            <v-icon class="counters-icons" size="16">mdi-eye</v-icon>
            <span class="screen-reader-only">
              {{ $t('news.app.number.views') }}
            </span>
            <div class="views-count ms-1">
              {{ item.viewsCount }}
            </div>
          </div>
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
    slideActive: false,
  }),
  computed: {
    displayDate() {
      return this.item.publishDate && new Date(this.item.publishDate);
    },
    showArticleAuthor() {
      return this.selectedOption && this.selectedOption.showArticleAuthor && this.item && !this.item.properties.hideAuthor;
    },
    showArticleDate() {
      return this.selectedOption && this.selectedOption.showArticleDate;
    },
    showArticleTitle() {
      return this.selectedOption && this.selectedOption.showArticleTitle;
    },
    showArticleReactions() {
      return this.selectedOption && this.selectedOption.showArticleReactions;
    },
    showArticleImage() {
      return this.selectedOption && this.selectedOption.showArticleImage;
    },
    showArticleSummary() {
      return this.selectedOption && this.selectedOption.showArticleSummary;
    },
    showArticleSpace() {
      return this.selectedOption && this.selectedOption.showArticleSpace;
    },
    featuredImageAltText() {
      return this.item?.properties?.featuredImage?.altText || '';
    },
    articleImage() {
      return this.showArticleImage && this.item
                                   && this.item.illustrationURL
                                   && this.item.illustrationURL.concat('&size=235x140').toString()
                                   || '/content/images/news.png';
    },
    isHiddenSpace() {
      return this.item && !this.item.spaceMember && this.item.hiddenSpace;
    },
    articleUrl() {
      return eXo.env.portal.userName !== '' ? this.item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${this.item.id}&type=article`;
    }
  },
  methods: {
    openArticle() {
      window.open(this.articleUrl, '_self');
    }
  }
};
</script>
