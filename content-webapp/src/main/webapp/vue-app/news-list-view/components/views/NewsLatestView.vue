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
  <div
    id="news-latest-view"
    ref="news-latest-view"
    class="px-2 py-2"
    :class="extraClass">
    <div :class="hasSmallWidthContainer ? 'article-small-container':'article-container'">
      <v-progress-circular
        v-if="loading"
        :size="50"
        class="loader"
        color="primary"
        indeterminate />
      <div
        v-for="(item, index) of newsInfo"
        :key="item"
        :class="hasSmallWidthContainer ? 'smallWidthContainer' : 'article'"
        :id="`articleItem-${index}`">
        <news-latest-view-item
          :news="newsInfo"
          :item="item"
          :selected-option="selectedOption"
          :index="index"
          :key="index" />
      </div>
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
    loading: {
      type: Boolean,
      default: false
    },
    selectedOption: {
      type: Object,
      default: () => {
        return {};
      }
    },
  },
  data: ()=> ({
    hasSmallWidthContainer: false,
    canPublishNews: false,
  }),
  computed: {
    newsInfo() {
      return this.newsList && this.newsList.filter(news => !!news);
    },
    extraClass() {
      return (!this.selectedOption.showHeader && !this.selectedOption.showSeeAll && !this.canPublishNews ) && 'mt-5' || ' ';
    }
  },
  created() {
    this.$newsServices.canPublishNews().then(canPublishNews => {
      this.canPublishNews = canPublishNews;
    });
  },
  mounted() {
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
    this.hasSmallWidthContainer = (this.$refs['news-latest-view']?.clientWidth *100 / window.screen.width) < 33;
  },
};
</script>