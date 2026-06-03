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
        :show-arrows="news?.length > 1"
        :show-arrows-on-hover="news?.length > 1"
        :hide-delimiters="news?.length === 1"
        interval="10000"
        height="220"
        class="fill-height"
        cycle
        hide-delimiter-background>
        <v-carousel-item
          v-for="(item,i) in news"
          :key="i"
          :href="articleUrl(item)"
          class="carouselItem"
          link
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
              class="position-absolute b-0 line-height-normal" />
            <div class="px-10 mt-auto pb-13 no-min-width full-width flex-column">
              <div
                :class="$vuetify.rtl && 'l-0' || 'r-0'"
                class="flex flex-row t-0 position-absolute">
                <v-btn
                  v-if="$root.canManageNewsList && hover"
                  :aria-label="$t('news.latest.openSettings')"
                  icon
                  @click="openDrawer"
                  class="float-right mt-1 me-1">
                  <v-icon size="18">
                    fas fa-cog
                  </v-icon>
                </v-btn>
              </div>
              <v-card
                v-if="showArticleTitle"
                :href="articleUrl(item)"
                class="elevation-0 height-fit-content line-height-normal primary-background-opacity-8 mx-auto font-weight-bold application-border-radius pa-1 d-flex align-center width-fit-content">
                <span class="text-title white--text text-truncate">
                  {{ item.title }}
                </span>
              </v-card>
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
