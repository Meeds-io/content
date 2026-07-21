<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

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
  <div class="contentListItem d-flex py-3">
    <a
      :href="item.url"
      class="contentListItemIllustration d-flex align-center justify-center flex-shrink-0 rounded me-4"
      :style="illustrationStyle">
      <v-icon
        v-if="!item.illustrationUrl"
        color="white"
        size="28">
        {{ item.icon }}
      </v-icon>
    </a>
    <div class="d-flex flex-column flex-grow-1 overflow-hidden">
      <div class="d-flex align-center">
        <a :href="item.url" class="text-truncate text-title font-weight-bold text-color flex-grow-1">
          {{ item.title }}
        </a>
        <div class="d-flex align-center flex-shrink-0 ms-2">
          <category-chip
            v-if="firstCategory"
            :category="firstCategory"
            small
            tabindex="-1"
            class="ms-1" />
          <v-icon size="18" class="ms-2">fa-ellipsis-v</v-icon>
        </div>
      </div>
      <div class="d-flex align-center text-caption text-color mb-1">
        <exo-space-avatar
          v-if="item.spaceId"
          :space-id="item.spaceId"
          :size="20"
          small-font-size
          popover />
        <v-icon size="10" class="mx-1">fas fa-circle</v-icon>
        <span class="text-truncate">{{ item.authorDisplayName }}</span>
        <v-icon size="10" class="mx-1">fas fa-circle</v-icon>
        <date-format :value="item.date" :format="dateFormat" />
      </div>
      <p class="text-truncate-2 text-body mb-0">
        {{ item.summary }}
      </p>
      <div v-if="item.viewsCount" class="d-flex align-center text-caption text-color mt-1">
        <v-icon size="14" class="me-1">fas fa-eye</v-icon>
        {{ item.viewsCount }}
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      required: true,
    },
  },
  data: () => ({
    firstCategory: null,
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
  }),
  computed: {
    illustrationStyle() {
      return this.item.illustrationUrl && {
        backgroundImage: `url(${this.item.illustrationUrl})`,
        backgroundSize: 'cover',
      } || {
        backgroundColor: '#F5A623',
      };
    },
    firstCategoryId() {
      return this.item.categoryIds?.[0];
    },
  },
  watch: {
    firstCategoryId: {
      immediate: true,
      handler() {
        if (this.firstCategoryId) {
          this.$categoryService.getCategory(this.firstCategoryId)
            .then(category => this.firstCategory = category)
            .catch(() => this.firstCategory = null);
        } else {
          this.firstCategory = null;
        }
      },
    },
  },
};
</script>
