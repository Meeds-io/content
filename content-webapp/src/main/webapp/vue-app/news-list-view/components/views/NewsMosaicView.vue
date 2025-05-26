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
    <div id="top-news-mosaic" ref="top-news-mosaic">
      <news-settings
        :is-hovering="hover"
        v-if="!isSmallBreakpoint"
        :class="isMobile ? '' : 'settingNewsContainer'"
        class="mt-3 mr-1" />
      <div :class="`mosaic-container ${smallHeightClass}`">
        <div
          v-for="(item, index) of news"
          :key="index"
          :class="isSmallWidth ? 'articleSmallWidth' : 'article'"
          :id="`articleItem-${index}`"
          class="card-border-radius">
          <a
            class="articleLink d-block"
            target="_self"
            :href="articleUrl(item)">
            <img
              :src="showArticleImage && item.illustrationURL !== null ? illustrationURL(item,index) : '/content/images/news.png'"
              alt=""
              class="card-border-radius">
            <div class="titleArea">
              <div v-if="showArticleDate" class="articleDate">
                <date-format
                  :value="new Date(item?.publishDate)"
                  :format="dateFormat" />
              </div>
              <div
                v-if="showArticleTitle"
                :class="styleArticleTitle()">
                {{ item.title }}
              </div>
            </div>
          </a>
        </div>
      </div>
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
  data () {
    return {
      dateFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      },
      isSmallWidth: false
    };
  },
  mounted() {
    this.isSmallWidth =  this.$refs?.['top-news-mosaic']?.clientWidth < 600;
    window.addEventListener('resize', () => {
      this.isSmallWidth = this.$refs?.['top-news-mosaic']?.clientWidth < 600;
    });
  },
  computed: {
    showArticleTitle() {
      return this.selectedOption.showArticleTitle;
    },
    showArticleImage() {
      return this.selectedOption.showArticleImage;
    },
    showArticleDate() {
      return this.selectedOption.showArticleDate;
    },
    isMobile() {
      return this.$vuetify.breakpoint.name === 'xs' || this.$vuetify.breakpoint.name === 'sm' || this.$vuetify.breakpoint.name === 'md';
    },
    isSmallBreakpoint() {
      return this.$vuetify.breakpoint.width < 651;
    },
    smallHeightClass() {
      return this.isMobile && this.news && this.news.length === 1 && 'small-mosaic-container';
    },
    articleUrl() {
      return (item) => {
        return eXo.env.portal.userName !== '' ? item.url : `${eXo.env.portal.context}/${eXo.env.portal.portalName}/news-detail?newsId=${item.id}&type=article`;
      };
    },
    news(){
      return this.newsList && this.newsList.filter(news => !!news);
    },
  },
  methods: {
    minLength(lengthNews){
      return lengthNews < 5 && lengthNews > 0 ? 100 / lengthNews : 25;
    },
    styleArticleTitle(){
      return  (this.isSmallWidth ? 'articleTitle ' : '').concat(this.isSmallBreakpoint ? 'text-truncate' : 'articleTitleTruncate');
    },
    illustrationURL(item,index){
      if (this.news.length > 1) {
        if (index === 0){
          return item.illustrationURL.concat('&size=712x404').toString();
        } else {
          return item.illustrationURL.concat('&size=712x201').toString();
        }
      } else {
        return item.illustrationURL.concat('&size=1426x404').toString();
      }
    }
  }
};
</script>
