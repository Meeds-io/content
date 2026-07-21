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
  <v-card
    class="position-relative application-border
    application-border-radius z-index-zero border-box-sizing overflow-hidden me-2 d-block card elevation-1"
    ref="newsCard"
    :href="articleUrl"
    target="_self"
    :class="{ 'keyboard-slide': slideActive, 'border-color-black' : isCardFocused || isArticleLinkFocused}"
    height="275"
    width="240"
    tabindex="0"
    @keydown.esc="blurContentHover($event)"
    @focus="isCardFocused = true;"
    @blur="isCardFocused = false;"
    @mouseover="showDetails = true"
    @mouseleave="showDetails = false">
    <v-sheet
      class="position-relative overflow-hidden flex-shrink-0"
      width="240"
      height="135">
      <img
        :src="articleImage"
        :alt="featuredImageAltText"
        class="object-fit-cover d-block full-width full-height">
      <extension-registry-components
        :params="{
          parameters: item?.parameters
        }"
        name="ContentList"
        type="content-card-event-date-chip"
        element="span"
        class="mt-n12" />
      <div
        v-if="firstCategory"
        class="position-absolute b-0 r-0 ma-2 white rounded-pill">
        <category-chip
          :category="firstCategory"
          tabindex="-1"
          small />
      </div>
    </v-sheet>
    <div
      class="text-area articleLinkDetails"
      role="link"
      tabindex="0"
      :aria-label="$t('news.illustration.link.title', {0: item.title})"
      @focus="isArticleLinkFocused = true; showDetails = true"
      @blur="isArticleLinkFocused = false; showDetails =false"
      @keydown.enter.prevent="openArticle">
      <div
        class="upper-row pa-2 pt-1">
        <div
          v-if="!isHiddenSpace && showArticleSpace"
          :id="`space-link-${item.activityId}`"
          class="space-link d-flex align-center gap-1 overflow-hidden border-radius
          position-relative d-flex width-fit-content text-decoration-none"
          :aria-label="$t('news.space.icon.title', { 0: item.spaceDisplayName })">
          <div class="d-flex pt-1 align-center mb-2">
            <v-img
              class="me-2 my-auto rounded flex-shrink-0"
              :src="item.spaceAvatarUrl"
              width="21"
              height="21"
              alt="" />
            <div class="space-name text-subtitle text-truncate">
              {{ item.spaceDisplayName }}
            </div>
          </div>
        </div>
        <div class="articleLink">
          <div v-if="showArticleTitle" class="mb-3 overflow-hidden text-body text-truncate-2">
            {{ item.title }}
          </div>
          <div class="d-flex text-no-wrap text-truncate text-subtitle">
            <div
              v-if="showArticleAuthor"
              class="author-name d-flex align-center gap-2 overflow-hidden user-avatar-parent">
              <v-avatar size="20" class="flex-shrink-0">
                <img :src="item.authorAvatarUrl" :alt="item.authorDisplayName">
              </v-avatar>
              <span
                class="text-subtitle text-truncate">
                {{ item.authorDisplayName }}
              </span>
            </div>
          </div>
          <div v-if="showDetails">
            <extension-registry-components
              :params="{
                parameters: item?.parameters
              }"
              :class="{
                'mb-n2': hasSummary
              }"
              name="ContentList"
              type="content-card-event-date-chip"
              element="span"
              class="mt-2 mb-2" />
            <div
              v-if="firstCategory"
              class="d-flex justify-end mt-2 mb-2">
              <category-chip
                :category="firstCategory"
                tabindex="-1"
                small />
            </div>
            <div
              v-if="showArticleSummary && hasSummary"
              class="text-truncate-3 mt-4 text-subtitle">
              {{ item?.properties?.summary }}
            </div>
            <div
              class="mt-2 align-self-center text-subtitle font-weight-bold primary--text">
              {{ $t('news.cards.readMore') }}
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="news-card-reactions position-absolute full-width b-0 l-0 t-0 d-flex z-index-two align-end flex-shrink-0 px-2 py-1 border-top-color text-subtitle">
      <news-template-view-item-reactions
        :item="item"
        :show-article-reactions="showArticleReactions" />
      <div 
        v-if="showArticleDate" 
        class="text-subtitle ms-auto mt-auto">
        <date-format
          :value="displayDate"
          :format="dateFormat" />
      </div>
    </div>
  </v-card>
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
      month: 'short',
      day: 'numeric',
    },
    slideActive: false,
    isArticleLinkFocused: false,
    isCardFocused: false,
    showDetails: false,
    firstCategory: null
  }),
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
    firstCategoryId() {
      return this.item?.categories?.[0];
    },
    hasSummary() {
      return this.item?.properties?.summary?.length;
    },
    displayDate() {
      return this.item?.publicationDate && new Date(this.item.publicationDate);
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
    },
    blurContentHover(event) {
      event.target.blur();
      this.$refs.newsCard.focus();
    }
  }
};
</script>
