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
    id="article-list-view"
    class="pa-0"
    ref="articleListView">
    <v-row class="mx-0">
      <v-col
        class="flex-grow-0 px-2"
        :cols="numberOfColumns"
        v-for="(item, index) of newsInfo"
        :key="item">
        <div
          :id="`article-item-${index}`">
          <news-list-template-view-item
            :item="item"
            :selected-option="selectedOption"
            :key="index" />
        </div>
      </v-col>
    </v-row>
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
    canPublishNews: false,
    parentWidth: 0
  }),
  computed: {
    numberOfColumns(){
      const thresholds = this.$vuetify.breakpoint?.thresholds;
      return this.parentWidth < thresholds.sm ? 12 : this.parentWidth < thresholds.md ? 6 : this.parentWidth < thresholds.lg ? 4 : 3;
    },
    newsInfo(){
      return this.newsList && this.newsList.filter(news => !!news);
    }
  },
  created() {
    this.$newsServices.canPublishNews().then(canPublishNews => {
      this.canPublishNews = canPublishNews;
    });
  },
  mounted() {
    // get the initial width of the parent element
    this.parentWidth = this.$refs.articleListView.offsetWidth;

    // add a resize event listener to update the parent width
    window.addEventListener('resize', () => {
      this.parentWidth = this.$refs.articleListView.offsetWidth;
    });
    this.$nextTick().then(() => this.$root.$emit('application-loaded'));
  },
};
</script>
