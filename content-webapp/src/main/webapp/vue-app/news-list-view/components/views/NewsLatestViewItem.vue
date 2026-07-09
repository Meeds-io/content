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
  <v-hover v-slot="{ hover }">
    <a
      :class="{
        'flex-column': !isMobile && index === 0,
        'background-grey-primary': hover
      }"
      class="articleLink"
      target="_self"
      :href="articleUrl"
      :aria-label="$t('news.space.icon.title',{ 0:item.spaceDisplayName })">
      <v-sheet
        v-if="showImage"
        v-bind="isMobile ? { minWidth: 80, height: 80 }
          : !isMobile && !index ? {height: 'calc(100% - 80px)'}: {}"
        class="articleImage line-height-normal d-flex background-transparent">
        <v-img
          v-if="isMobile"
          :src="articleImg"
          :alt="featuredImageAltText"
          height="80"
          width="80"
          class="application-border-radius" />
        <v-img
          v-else
          :src="articleImg"
          :alt="featuredImageAltText"
          v-bind="!index ? {
            height: 'calc(100% - 80px)',
            aspectRatio: 16/9
          }: {}"
          class="application-border-radius full-width position-absolute l-0 b-0 t-0 r-0" />
        <extension-registry-components
          v-if="!index && !isMobile"
          :params="{
            parameters: item?.parameters
          }"
          name="ContentList"
          type="content-card-event-date-chip"
          element="span"
          class="mt-auto" />
        <extension-registry-components
          v-else  
          :params="{
            parameters: item?.parameters,
            chipSize: 32,
            chipArrowSize: 8,
            chipExtraClass: 'text-subtitle-font-size line-height-1 pa-1',
            parentExtraClass: 'application-border-radius no-border-top-left-radius no-border-top-right-radius no-border-bottom-right-radius overflow-hidden'
          }"
          name="ContentList"
          type="content-card-event-date-chip"
          element="span"
          class="position-absolute b-0" />
      </v-sheet>
      <v-sheet
        height="80"
        :class="{
          'mb-n2 d-flex flex-column pb-2 position-absolute b-0 r-0 l-0': !isMobile && index === 0,
          'px-2': index === 0 && !isMobile,
          'ps-3 d-flex flex-column': index === 0 && isMobile
        }"
        class="articleInfos no-min-width full-width background-transparent">
        <div
          v-if="firstCategory && index === 0"
          class="position-absolute t-0 r-0 mt-n5 me-2 mb-2 white rounded-pill">
          <category-chip
            :category="firstCategory"
            small />
        </div>
        <div class="d-flex align-center mb-1">
          <div v-if="showArticleDate" class="postDate text-subtitle line-height-1 flex-column mt-0 my-auto">
            <date-format
              :value="displayDate"
              :format="dateFormat" />
          </div>
          <v-spacer v-if="firstCategory && index > 0" />
          <category-chip
            v-if="firstCategory && index > 0"
            :category="firstCategory"
            small />
        </div>
        <span
          v-if="showArticleTitle"
          :class="{
            'text-truncate': (index === 0 && !isMobile) || (index > 0 && hasSummary) || isMobile,
            'text-truncate-1': index > 0 && !hasSummary
          }"
          class="articleTitle text-color text-body line-height-normal">
          {{ item.title }}
        </span>
        <span
          v-if="showSummary"
          class="text-subtitle text-truncate">
          {{ item?.properties?.summary }}
        </span>
        <div
          class="mt-auto articlePostTitle">
          <div
            v-if="!isHiddenSpace && showArticleSpace"
            class="articleSpace align-stretch text-subtitle d-flex flex-grow-1 no-min-width my-auto rounded flex-grow-0">
            <v-img
              v-if="showArticleSpace"
              class="my-auto rounded flex-grow-0"
              :src="item.spaceAvatarUrl"
              width="20"
              height="20"
              alt="" />
            <v-icon
              v-if="showArticleSpace && showArticleAuthor"
              class="mx-1"
              small>
              mdi-chevron-right
            </v-icon>
            <span
              v-if="showArticleAuthor"
              :class="{
                'flex-shrink-1': truncateAuthorName,
                'flex-shrink-0' : !truncateAuthorName
              }"
              class="text-truncate flex-grow-1 flex-shrink-1 my-auto">
              <v-avatar 
                size="20" 
                class="flex-shrink-0 my-auto me-2">
                <img 
                  :src="item.authorAvatarUrl" 
                  :alt="item.authorDisplayName">
              </v-avatar>
              <span>
                {{ item.authorDisplayName }}
              </span>
            </span>
          </div>
          <news-template-view-item-reactions
            :item="item"
            :show-article-reactions="showArticleReactions" />
        </div>
      </v-sheet>
    </a>
  </v-hover>
</template>
<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: null
    },
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
    index: {
      type: Number,
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
    showArticleTitle: true,
    showArticleSummary: true,
    showArticleImage: true,
    showArticleAuthor: true,
    showArticleSpace: true,
    showArticleDate: true,
    showArticleReactions: true,
    firstCategory: null,
  }),
  computed: {
    firstCategoryId() {
      return this.item?.categories?.[0];
    },
    hasSummary() {
      return !!this.item?.properties?.summary;
    },
    showSummary() {
      return this.showArticleSummary && this.hasSummary && (this.index > 0 || this.isMobile);
    },
    isMobile() {
      return this.$vuetify.breakpoint.name === 'sm';
    },
    truncateAuthorName() {
      return this.item?.authorDisplayName?.length > 15;
    },
    articleImg(){
      return this.showImage && this.img ;
    },
    showImage(){
      return  this.showArticleImage || (!this.showArticleImage && !this.index );
    },
    featuredImageAltText() {
      return this.item?.properties?.featuredImage?.altText || '';
    },
    img() {
      return this.illustrationURL() || '/content/images/news.png';
    },
    displayDate() {
      return this.item?.publicationDate && new Date(this.item.publicationDate);
    },
    isHiddenSpace() {
      return this.item && !this.item.spaceMember && this.item.hiddenSpace;
    },
    articleUrl() {
      return eXo.env.portal.userName !== '' ? this.item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${this.item.id}&type=article`;
    }
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
  created() {
    this.reset();
  },
  methods: {
    reset() {
      this.viewTemplate = this.$root.viewTemplate;
      this.viewExtensions = this.$root.viewExtensions;
      this.newsTarget = this.$root.newsTarget;
      this.newsHeader = this.$root.header;
      this.limit = this.$root.limit;
      this.showHeader = this.$root.showHeader;
      this.showSeeAll = this.$root.showSeeAll;
      this.showArticleTitle = this.$root.showArticleTitle;
      this.showArticleImage = this.$root.showArticleImage;
      this.showArticleSummary = this.$root.showArticleSummary;
      this.showArticleAuthor = this.$root.showArticleAuthor;
      this.showArticleSpace = this.$root.showArticleSpace;
      this.showArticleDate = this.$root.showArticleDate;
      this.showArticleReactions = this.$root.showArticleReactions;
      this.seeAllUrl = this.$root.seeAllUrl;
    },
    illustrationURL(){
      if (this.news.length > 1) {
        if (this.index === 0){
          return this.item.illustrationURL?.concat('&size=700x344').toString();
        } else {
          return this.item.illustrationURL?.concat('&size=80x80').toString();
        }
      } else {
        return this.item.illustrationURL?.concat('&size=1410x344').toString();
      }
    }
  }
};
</script>
