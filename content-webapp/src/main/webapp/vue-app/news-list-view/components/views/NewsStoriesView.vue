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
    id="top-news-stories"
    class="d-flex flex-column">
    <card-carousel
      v-if="news.length"
      class="align-left d-flex">
      <news-stories-view-item
        v-for="(item, index) in news"
        :key="index"
        :item="item"
        :last-item="news.length - 1 === index"
        :selected-option="selectedOption" />
      <v-hover v-slot="{ hover }">
        <div 
          v-if="showSeeAll">
          <news-settings 
            :hide-see-all-button="true"
            :is-hovering="hover"
            class="position-absolute r-0 z-index-modal mt-1" />
          <a
            class="see-all-link"
            target="_self"
            :href="seeAllUrl">
            <v-card
              height="210"
              width="140"
              class="elevation-0 d-flex align-center justify-center flex-column">
              <v-btn
                class="grey-lighten1-background"
                icon>
                <v-icon
                  class="white--text">
                  fas fa-ellipsis-h
                </v-icon>
              </v-btn>
              <div class="text-sub-title mt-2">
                {{ $t('news.published.seeAll') }}
              </div>
            </v-card>
          </a>
        </div>
      </v-hover>
    </card-carousel>
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
  computed: {
    news(){
      return this.newsList && this.newsList.filter(news => !!news);
    },
    showSeeAll() {
      return this.selectedOption.showSeeAll;
    },
    seeAllUrl() {
      return this.selectedOption.seeAllUrl;
    },
  },
};
</script>
