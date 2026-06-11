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
    <div
      class="d-flex application-layout-style"
      id="alerts-slider"
      v-show="!emptyTemplate">
      <div class="error-color-background full-height white--text z-index-one fill-height align-center d-flex text-header">
        <div class="px-3 py-2">
          <v-icon
            size="20"  
            color="white">
            fas fa-exclamation-triangle
          </v-icon>
        </div>
        <span
          v-if="showHeader"
          class="d-none my-auto d-md-block pe-4 font-weight-bold">
          {{ headerTitle }}
        </span>
      </div>

      <div class="alerts-viewer no-min-width ps-4 primary flex-grow-1">
        <v-carousel
          v-model="slider"
          hide-delimiters
          cycle
          :show-arrows="false"
          interval="10000"
          height="38"
          min-height="38">
          <v-carousel-item
            v-for="(item,i) in news"
            :key="i">
            <a :href="articleUrl(item)" class="article-link d-flex align-center full-height flex-grow-1 no-min-width">
              <div class="alerts-article d-flex text-body white--text no-min-width">
                <span v-if="showArticleDate" class="text-no-wrap text-capitalize flex-shrink-0">
                  <date-format
                    :value="new Date(item?.publishDate)"
                    :format="dateFormat" />
                </span>
                <span 
                  v-if="showArticleDate && showArticleTitle && !!item?.publishDate" 
                  class="mx-2 flex-shrink-0 line-height-1">.</span>
                <span v-if="showArticleTitle" class="font-weight-bold text-truncate">{{ item.title }}</span>
              </div>
            </a>
          </v-carousel-item>
        </v-carousel>
      </div>
      <div class="slider-buttons primary d-flex pe-2">
        <v-btn
          v-if="$root.canManageNewsList && hover"
          :aria-label="$t('news.latest.openSettings')"
          icon
          @click="openDrawer">
          <v-icon
            class="white--text"
            size="16">
            fas fa-cog
          </v-icon>
        </v-btn>
        <div
          class="d-flex"
          v-if="news?.length > 1">
          <v-btn
            :aria-label="$t('news.alertView.leftArrowButtonTitle')"
            @click="slider--"
            icon>
            <v-icon
              size="16"
              class="white--text">
              fas fa-chevron-left
            </v-icon>
          </v-btn>
          <v-btn
            :aria-label="$t('news.alertView.rightArrowButtonTitle')"
            @click="slider++"
            icon>
            <v-icon
              size="16"
              class="white--text">
              fas fa-chevron-right
            </v-icon>
          </v-btn>
        </div>
      </div>
    </div>
  </v-hover>
</template>

<script>
export default {
  props: {
    headerTitle: {
      type: String,
      required: false,
      default: null
    },
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
  data () {
    return {
      containerNewsAlertView: [],
      slider: 0,
      dateFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      },
    };
  },
  computed: {
    emptyTemplate() {
      return !(this.news && this.news.length);
    },
    articleUrl() {
      return (item) => {
        return eXo.env.portal.userName !== '' ? item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${item.id}&type=article`;
      };
    },
    news(){
      return this.newsList && this.newsList.filter(news => !!news);
    },
    showArticleTitle() {
      return this.selectedOption.showArticleTitle;
    },
    showHeader() {
      return this.selectedOption.showHeader;
    },
    showArticleDate() {
      return this.selectedOption.showArticleDate;
    },
  },
  methods: {
    disabledContainerNewsAlertView(element,index){
      const el = element.querySelector('#critical-alerts-slider');
      if (el){
        this.containerNewsAlertView[index].style.display='none';
      }
    },
    openDrawer() {
      this.$root.$emit('news-settings-drawer-open');
    },
  }
};
</script>
