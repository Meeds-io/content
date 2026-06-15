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
    <v-card
      height="210"
      width="140"
      class="me-2 white card-border-radius overflow-hidden elevation-0 py-0 position-relative border-box-sizing">
      <news-settings
        v-if="!showSeeAll && lastItem && hover"
        class="position-absolute r-0 z-index-modal mt-2"
        class-button-open-settings="white-background"
        setting-button-size="24"
        hide-see-all-button />
      <a
        class="articleLink"
        target="_self"
        :href="articleUrl">
        <v-img
          class="position-absolute border-box-sizing full-height full-width"
          :src="articleImage"
          :aspect-ratio="2/3"
          :alt="featuredImageAltText">
          <div class="absolute-full-size linear-gradient-black-overlay-background"></div>
          <div class="text-subtitle-font-size position-absolute mt-2 ms-2">
            <v-img
              v-if="showArticleAuthor"
              :src="item.authorAvatarUrl"
              width="30px"
              height="30px"
              class="ma-auto border-radius-circle"
              alt="" />
          </div>
        </v-img></a>
      <a
        target="_self"
        :href="articleUrl">
        <div class="d-flex flex-column justify-end position-absolute px-1 pb-1 b-0">
          <div 
            v-if="showArticleTitle" 
            class="text-font-size white--text text-truncate-2">
            {{ item.title }}
          </div>
        </div>
      </a>
    </v-card>
  </v-hover>
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
    lastItem: {
      type: Boolean,
      default: false
    },
  },
  data: ()=> ({
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      showSeeAll: false
    },
  }),
  computed: {
    displayDate() {
      return this.item.publishDate && new Date(this.item.publishDate);
    },
    showArticleAuthor() {
      return this.selectedOption && this.selectedOption.showArticleAuthor && this.item  && !this.item.properties.hideAuthor;
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
    articleImage() {
      return this.showArticleImage && this.item.illustrationURL !== null ? this.item.illustrationURL.concat('&size=140x210').toString() : '/content/images/news.png';
    },
    articleUrl() {
      return eXo.env.portal.userName !== '' ? this.item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${this.item.id}&type=article`;
    },
    featuredImageAltText() {
      return this.item?.properties?.featuredImage?.altText || '';
    },
  },
  created() {
    this.showSeeAll = this.$root.showSeeAll;
  }
};
</script>
