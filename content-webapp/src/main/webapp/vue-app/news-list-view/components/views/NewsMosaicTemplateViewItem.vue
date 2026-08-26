<!--

    This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2026 Meeds Association contact@meeds.io
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
  <v-hover v-slot="{ hover }">
    <div
      :class="{
        'background-grey-primary': hover
      }"
      :id="`articleItem-${index}`"
      class="article">
      <a
        class="articleLink d-block full-height full-width"
        target="_self"
        :href="articleUrl">
        <v-img
          :src="imgSrc"
          :alt="featuredImageAltText"
          v-bind="isSingleArticle ? {
            aspectRatio: 3/2
          } : {}"
          :class="isSingleArticle ? 'full-width' : 'full-width full-height position-absolute l-0 b-0 t-0 r-0'">
          <div
            v-if="!isSingleArticle"
            class="absolute-full-size linear-gradient-black-overlay-background"></div>
          <extension-registry-components
            v-if="isSingleArticle"
            :params="{
              parameters: item?.parameters,
            }"
            name="ContentList"
            type="content-card-event-date-chip"
            element="span"
            class="d-flex position-absolute b-0 line-height-normal" />
        </v-img>
        <component
          :is="isSingleArticle ? 'v-sheet' : 'div'"
          v-bind="isSingleArticle ? { height: 80 } : {}"
          :class="isSingleArticle ? 'background-transparent' : 'position-absolute b-0 pb-2 z-index-one'"
          class="article-item-content full-width d-flex flex-column align-stretch flex-grow-1 no-min-width ms-0 px-2">
          <extension-registry-components
            v-if="!isSingleArticle"
            :params="index ? {
              parameters: item?.parameters,
              chipSize: 32,
              chipArrowSize: 8,
              chipExtraClass: 'text-subtitle-font-size line-height-1 pa-1',
            } : {
              parameters: item?.parameters,
            }"
            name="ContentList"
            type="content-card-event-date-chip"
            element="span"
            class="d-flex line-height-normal" />
          <div
            v-if="showArticleDate && !(index && isSmallWidth)"
            :class="{
              'mt-2': isSingleArticle,
              'mt-1 white--text': !isSingleArticle}"
            class="d-flex align-center text-subtitle line-height-normal mb-1">
            <date-format
              :value="displayDate"
              :format="dateFormat" />
            <v-spacer v-if="firstCategory" />
            <div
              v-if="firstCategory"
              :class="{ 'mt-n6': isSingleArticle }"
              class="white rounded-pill">
              <category-chip
                :category="firstCategory"
                small
                tabindex="-1" />
            </div>
          </div>
          <span
            v-if="showArticleTitle"
            :class="{
              'mt-2': index && isSmallWidth,
              'white--text': !isSingleArticle
            }"
            class="text-body text-truncate">{{ item.title }}</span>
          <div
            v-if="!index"
            :class="{ 'white--text': !isSingleArticle }"
            class="d-flex text-subtitle mt-1 mt-auto">
            <v-img
              v-if="showArticleSpace"
              class="my-auto rounded flex-grow-0"
              :src="item.spaceAvatarUrl"
              width="20"
              height="20"
              alt="" />
            <span
              v-if="showArticleSpace"
              class="text-truncate flex-shrink-1 my-auto ms-2">
              {{ item.spaceDisplayName }}
            </span>
            <v-icon
              v-if="showArticleSpace && showArticleAuthor"
              :class="{ 'white--text': !isSingleArticle }"
              class="mx-1"
              small>
              mdi-chevron-right
            </v-icon>
            <div
              :class="{
                'flex-shrink-1': truncateAuthorName,
                'flex-shrink-0': !truncateAuthorName
              }"
              class="text-truncate flex-grow-1 flex-shrink-1 my-auto">
              <span v-if="showArticleAuthor">
                <v-avatar
                  size="20"
                  class="flex-shrink-0 my-auto me-2">
                  <img
                    :src="item.authorAvatarUrl"
                    :alt="item.authorDisplayName">
                </v-avatar>
                <span>{{ item.authorDisplayName }}</span>
              </span>
            </div>
            <news-template-view-item-reactions
              :item="item"
              :show-article-reactions="showArticleReactions"
              :text-color-class="isSingleArticle ? '' : 'white--text'"
              :icon-color-class="isSingleArticle ? 'icon-default-color' : 'white--text'"
              class="my-auto" />
          </div>
        </component>
      </a>
    </div>
  </v-hover>
</template>

<script>
export default {
  props: {
    item: {
      type: Object,
      default: () => ({})
    },
    index: {
      type: Number,
      default: 0
    },
    isSmallWidth: {
      type: Boolean,
      default: false
    },
    totalCount: {
      type: Number,
      default: 1
    },
    showArticleTitle: {
      type: Boolean,
      default: true
    },
    showArticleImage: {
      type: Boolean,
      default: true
    },
    showArticleDate: {
      type: Boolean,
      default: true
    },
    showArticleSummary: {
      type: Boolean,
      default: true
    },
    showArticleAuthor: {
      type: Boolean,
      default: true
    },
    showArticleSpace: {
      type: Boolean,
      default: true
    },
    showArticleReactions: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dateFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      },
      firstCategory: null,
    };
  },
  watch: {
    firstCategoryId: {
      immediate: true,
      handler() {
        if (this.firstCategoryId) {
          this.$categoryService.getCategory(this.firstCategoryId)
            .then(category => this.firstCategory = category)
            .catch(() => this.firstCategory = null);
        } else {
          this.firstCategory = null;
        }
      },
    },
  },
  computed: {
    isSingleArticle() {
      return this.totalCount === 1;
    },
    firstCategoryId() {
      return this.item?.categories?.[0];
    },
    articleUrl() {
      return eXo.env.portal.userName !== ''
        ? this.item.url
        : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${this.item.id}&type=article`;
    },
    imgSrc() {
      if (!this.showArticleImage || this.item.illustrationURL === null) {
        return '/content/images/news.png';
      }
      if (this.totalCount > 1) {
        return this.index === 0
          ? `${this.item.illustrationURL}&size=712x404`
          : `${this.item.illustrationURL}&size=712x201`;
      }
      return `${this.item.illustrationURL}&size=1426x404`;
    },
    featuredImageAltText() {
      return this.item?.properties?.featuredImage?.altText || '';
    },
    displayDate() {
      return this.item?.publicationDate && new Date(this.item.publicationDate);
    },
    hasSummary() {
      return !!this.item?.properties?.summary;
    },
    truncateAuthorName() {
      return this.item?.authorDisplayName?.length > 15;
    },
    hideAuthor() {
      return this.item?.properties?.hideAuthor;
    }
  },
};
</script>