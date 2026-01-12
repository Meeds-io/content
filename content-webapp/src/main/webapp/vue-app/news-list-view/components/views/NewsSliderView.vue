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
    <div class="newsSlider">
      <v-carousel
        cycle
        show-arrows-on-hover
        interval="10000"
        height="220"
        hide-delimiter-background
        class="sliderNewsItems fill-height">
        <v-carousel-item
          v-for="(item,i) in news"
          :key="i"
          class="carouselItem"
          eager
          dark>
          <img
            :src="showArticleImage && item.illustrationURL !== null
              ? item.illustrationURL.concat('&size=1420x222')
              : '/content/images/news.png'"
            :alt="item?.properties?.featuredImage?.altText || ''"
            class="articleImage object-fit-cover width-full full-height">
          <v-container class="slide-text-container d-flex text-center">
            <div class="flex d-flex flex-column carouselNewsInfo">
              <div
                :class="$vuetify.rtl && 'l-0' || 'r-0'"
                class="flex flex-row position-absolute">
                <v-btn
                  v-if="$root.canManageNewsList && hover"
                  :aria-label="$t('news.latest.openSettings')"
                  icon
                  @click="openDrawer"
                  class="float-right settingNewsButton">
                  <v-icon>mdi-cog</v-icon>
                </v-btn>
              </div>
              <a
                v-if="showArticleTitle"
                :href="articleUrl(item)"
                class="flex flex-row flex-grow-1 align-center justify-center headLinesTruncate"
                :class="extraClass.concat($root.canManageNewsList ? 'mt-12' : '')">
                <span class="text-h4 font-weight-medium white--text text-truncate-2">
                  {{ item.title }}
                </span>
              </a>
              <div class="flex flex-row flex-grow-1 align-center mx-4 my-2">
                <span v-if="showArticleSummary" class="white--text articleSummary"> {{ item?.properties?.summary }}</span>
                <news-slider-view-item
                  :author="item.author"
                  :author-display-name="item.authorDisplayName"
                  :properties="item.properties"
                  :space-display-name="item.spaceDisplayName"
                  :space-url="item.spaceUrl"
                  :space-avatar-url="item.spaceAvatarUrl"
                  :publish-date="item.publishDate"
                  :author-avatar-url="item.authorAvatarUrl"
                  :activity-id="item.activityId"
                  :likes-count="item.likesCount"
                  :comments-count="item.commentsCount"
                  :views-count="item.viewsCount"
                  :hidden-space="item.hiddenSpace"
                  :space-member="item.spaceMember"
                  :selected-option="selectedOption"
                  class="d-flex flex-row newsSliderItem align-center justify-center pa-2 ms-2" />
              </div>
            </div>
          </v-container>
        </v-carousel-item>
      </v-carousel>
    </div>
  </v-hover>
</template>
<script>
export default {
  props: {
    newsList: {
      type: Array,
      default: () => {
        return [];
      }
    },
    selectedOption: {
      type: Object,
      default: () => {
        return {};
      }
    },
  },
  computed: {
    news(){
      return this.newsList && this.newsList.filter(news => !!news);
    },
    extraClass() {
      return this.$vuetify.breakpoint.width > 550 ? (!this.$root.canManageNewsList && 'mt-7' || '') : '' ;
    },
    articleUrl() {
      return (item) => {
        return eXo.env.portal.userName !== '' ? item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${item.id}&type=article`;
      };
    },
    showArticleTitle() {
      return this.selectedOption.showArticleTitle;
    },
    showArticleImage() {
      return this.selectedOption.showArticleImage;
    },
    showArticleSummary() {
      return this.selectedOption.showArticleSummary;
    },
  },
  methods: {
    openDrawer() {
      this.$root.$emit('news-settings-drawer-open');
    },
  }
};
</script>
