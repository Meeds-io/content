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
  <div class="newsSlider hover-reveal-parent position-relative">
    <v-carousel
      v-model="slider"
      :show-arrows="false"
      :hide-delimiters="news?.length === 1"
      interval="10000"
      height="220"
      class="fill-height"
      cycle
      hide-delimiter-background>
      <v-carousel-item
        v-for="(item,i) in news"
        :key="i"
        class="carouselItem"
        eager
        dark>
        <v-img
          :src="showArticleImage && !!item.illustrationURL
            ? item.illustrationURL.concat('&size=1420x222')
            : '/content/images/news.png'"
          :alt="item?.properties?.featuredImage?.altText || ''"
          :aspect-ratio="8"
          class="articleImage object-fit-cover full-height width-full" />
        <v-container class="position-absolute pa-0 t-0 l-0 r-0 align-center full-width full-height d-flex text-center">
          <extension-registry-components
            :params="{
              parameters: item?.parameters,
              chipSize: 'auto',
            }"
            name="ContentList"
            type="content-card-event-date-chip"
            element="span"
            class="stretched-link-above position-absolute b-0 line-height-normal" />
          <div
            v-if="firstCategory(item)"
            class="stretched-link-above position-absolute b-0 r-0 ma-2 white rounded-pill">
            <category-chip
              :category="firstCategory(item)"
              small />
          </div>
          <div class="px-10 mt-auto pb-13 no-min-width full-width flex-column">
            <a
              :href="articleUrl(item)"
              class="stretched-link">
              <span
                :class="showArticleTitle && 'd-flex elevation-0 height-fit-content line-height-normal primary-background-opacity-8 mx-auto font-weight-bold application-border-radius pa-1 align-center width-fit-content' || 'd-sr-only'">
                <span :class="showArticleTitle && 'text-title white--text text-truncate'">
                  {{ item.title }}
                </span>
              </span>
            </a>
          </div>
        </v-container>
      </v-carousel-item>
    </v-carousel>
    <template v-if="news?.length > 1">
      <v-btn
        :aria-label="$t('news.alertView.leftArrowButtonTitle')"
        :class="$vuetify.rtl && 'r-0' || 'l-0'"
        class="news-slider-previous hover-reveal absolute-vertical-center z-index-two"
        color="grey darken-4"
        fab
        x-small
        depressed
        dark
        @click="previous">
        <v-icon size="16">{{ $vuetify.rtl ? 'fas fa-chevron-right' : 'fas fa-chevron-left' }}</v-icon>
      </v-btn>
      <v-btn
        :aria-label="$t('news.alertView.rightArrowButtonTitle')"
        :class="$vuetify.rtl && 'l-0' || 'r-0'"
        class="news-slider-next hover-reveal absolute-vertical-center z-index-two"
        color="grey darken-4"
        fab
        x-small
        depressed
        dark
        @click="next">
        <v-icon size="16">{{ $vuetify.rtl ? 'fas fa-chevron-left' : 'fas fa-chevron-right' }}</v-icon>
      </v-btn>
    </template>
    <div
      v-if="$root.canManageNewsList"
      :class="$vuetify.rtl && 'l-0' || 'r-0'"
      class="position-absolute t-0 mt-1 mx-1 z-index-two">
      <v-btn
        :aria-label="$t('news.latest.openSettings')"
        class="news-slider-settings hover-reveal"
        icon
        dark
        @click="openDrawer">
        <v-icon size="18">fas fa-cog</v-icon>
      </v-btn>
    </div>
  </div>
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
  data: () => ({
    slider: 0,
    firstCategories: {},
  }),
  computed: {
    news(){
      return this.newsList && this.newsList.filter(news => !!news);
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
  watch: {
    news: {
      immediate: true,
      handler() {
        this.refreshCategories();
      },
    },
  },
  methods: {
    previous() {
      this.slider = (this.slider + this.news.length - 1) % this.news.length;
    },
    next() {
      this.slider = (this.slider + 1) % this.news.length;
    },
    openDrawer() {
      this.$root.$emit('news-settings-drawer-open');
    },
    refreshCategories() {
      (this.news || []).forEach(item => {
        const categoryId = item?.categories?.[0];
        if (categoryId && !this.firstCategories[item.id]) {
          this.$categoryService.getCategory(categoryId)
            .then(category => this.firstCategories = {...this.firstCategories, [item.id]: category})
            .catch(() => null);
        }
      });
    },
    firstCategory(item) {
      return this.firstCategories[item.id];
    },
  }
};
</script>
