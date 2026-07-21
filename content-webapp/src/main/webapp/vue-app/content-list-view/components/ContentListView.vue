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
  <v-app class="contentListView border-box-sizing" flat>
    <v-main class="d-flex flex-column fill-height">
      <div class="d-flex align-center pb-2">
        <span class="text-header-title text-truncate">{{ $t('content.list.title') }}</span>
      </div>
      <div v-if="loading" class="d-flex flex-grow-1 align-center justify-center">
        <v-progress-circular indeterminate color="primary" />
      </div>
      <div v-else-if="!items.length" class="d-flex flex-grow-1 align-center justify-center">
        <span class="text-subtitle text-color">{{ $t('content.list.empty') }}</span>
      </div>
      <div v-else class="d-flex flex-column">
        <content-list-item
          v-for="contentItem in items"
          :key="`${contentItem.contentType}-${contentItem.id}`"
          :item="contentItem"
          class="border-box-sizing" />
        <div v-if="hasMore" class="d-flex justify-center pt-2">
          <v-btn
            :loading="loadingMore"
            text
            @click="loadMore">
            {{ $t('content.list.loadMore') }}
          </v-btn>
        </div>
      </div>
    </v-main>
  </v-app>
</template>
<script>
export default {
  props: {
    canEdit: {
      type: Boolean,
      default: false,
    },
    saveSettingsUrl: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    items: [],
    offset: 0,
    limit: 20,
    hasMore: false,
    loading: true,
    loadingMore: false,
  }),
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.offset = 0;
      return this.$contentListService.getContentList({offset: this.offset, limit: this.limit})
        .then(data => {
          this.items = data?.items || [];
          this.hasMore = (data?.size || 0) >= this.limit;
        })
        .finally(() => this.loading = false);
    },
    loadMore() {
      this.loadingMore = true;
      this.offset += this.limit;
      return this.$contentListService.getContentList({offset: this.offset, limit: this.limit})
        .then(data => {
          this.items = [...this.items, ...(data?.items || [])];
          this.hasMore = (data?.size || 0) >= this.limit;
        })
        .finally(() => this.loadingMore = false);
    },
  },
};
</script>
