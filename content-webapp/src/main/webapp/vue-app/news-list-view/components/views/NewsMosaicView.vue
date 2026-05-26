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
        class="mt-3 me-1 position-absolute r-0 z-index-two" />
      <div :class="`application-border-radius mosaic-container ${smallHeightClass}`">
        <news-mosaic-template-view-item
          v-for="(item, index) of news"
          :key="index"
          :item="item"
          :index="index"
          :is-small-width="isSmallWidth"
          :total-count="news.length"
          :show-article-title="showArticleTitle"
          :show-article-image="showArticleImage"
          :show-article-date="showArticleDate"
          :show-article-summary="showArticleSummary"
          :show-article-author="showArticleAuthor"
          :show-article-space="showArticleSpace"
          :show-article-reactions="showArticleReactions" />
      </div>
    </div>
  </v-hover>
</template>

<script>
export default {
  props: {
    newsList: {
      type: Array,
      default: () => []
    },
    selectedOption: {
      type: Object,
      default: () => ({})
    },
  },
  data() {
    return {
      isSmallWidth: false,
      showArticleTitle: true,
      showArticleImage: true,
      showArticleDate: true,
      showArticleSummary: true,
      showArticleAuthor: true,
      showArticleSpace: true,
      showArticleReactions: true,
      seeAllUrl: null,
    };
  },
  computed: {
    news() {
      return this.newsList?.filter(news => !!news) || [];
    },
    isMobile() {
      return ['xs', 'sm', 'md'].includes(this.$vuetify.breakpoint.name);
    },
    isSmallBreakpoint() {
      return this.$vuetify.breakpoint.width < 651;
    },
    smallHeightClass() {
      return this.isMobile && this.news.length === 1 ? 'small-mosaic-container' : '';
    },
  },
  created() {
    this.init();
  },
  mounted() {
    this.isSmallWidth = this.$refs?.['top-news-mosaic']?.clientWidth < 600;
    window.addEventListener('resize', () => {
      this.isSmallWidth = this.$refs?.['top-news-mosaic']?.clientWidth < 600;
    });
  },
  methods: {
    init() {
      this.showArticleTitle = this.$root.showArticleTitle;
      this.showArticleImage = this.$root.showArticleImage;
      this.showArticleSummary = this.$root.showArticleSummary;
      this.showArticleAuthor = this.$root.showArticleAuthor;
      this.showArticleSpace = this.$root.showArticleSpace;
      this.showArticleDate = this.$root.showArticleDate;
      this.showArticleReactions = this.$root.showArticleReactions;
      this.seeAllUrl = this.$root.seeAllUrl;
    },
  },
};
</script>