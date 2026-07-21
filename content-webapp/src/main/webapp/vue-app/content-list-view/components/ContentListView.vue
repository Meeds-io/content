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
  <v-app
    class="contentListView border-box-sizing"
    :class="{'contentListViewCompact': compact}"
    flat>
    <v-main class="d-flex flex-column fill-height">
      <div class="d-flex align-center pb-2">
        <template v-if="!filterMode">
          <span v-if="!compact" class="text-header-title text-truncate flex-grow-1">{{ $t('content.list.title') }}</span>
          <v-spacer v-else />
          <v-btn
            icon
            small
            :aria-label="$t('content.list.filter.open')"
            @click="filterMode = true">
            <v-icon size="18" :class="appliedSearchText && 'primary--text'">fa-filter</v-icon>
          </v-btn>
          <v-btn
            icon
            small
            :aria-label="$t('content.list.filter.drawer.open')"
            @click="$refs.filterDrawer.open(advancedFilter)">
            <v-icon size="18" :class="hasAdvancedFilter && 'primary--text'">fa-bars</v-icon>
          </v-btn>
        </template>
        <template v-else>
          <v-btn
            icon
            small
            :aria-label="$t('content.list.filter.close')"
            @click="filterMode = false">
            <v-icon size="18">fas fa-arrow-left</v-icon>
          </v-btn>
          <v-text-field
            v-model="searchText"
            :placeholder="$t('content.list.filter.placeholder')"
            prepend-inner-icon="fa-filter"
            clearable
            hide-details
            dense
            class="flex-grow-1 mx-2" />
        </template>
      </div>
      <categories-breadcrumb
        v-if="breadcrumb"
        :breadcrumb="breadcrumb"
        :selected-id="activeCategoryId"
        class="pb-2"
        @select="selectCategory" />
      <category-chips-group
        v-else-if="categories.length"
        :categories="categories"
        class="pb-2"
        @select="selectCategory" />
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
          class="border-box-sizing"
          @delete="confirmDelete" />
        <div v-if="hasMore" class="d-flex justify-center pt-2">
          <v-btn
            :loading="loadingMore"
            text
            @click="loadMore">
            {{ $t('content.list.loadMore') }}
          </v-btn>
        </div>
      </div>
      <content-filter-drawer ref="filterDrawer" @apply="applyAdvancedFilter" />
      <exo-confirm-dialog
        ref="deleteConfirmDialog"
        :message="$t('content.list.item.delete.confirm.message')"
        :title="$t('content.list.item.delete.confirm.title')"
        :ok-label="$t('content.list.item.delete.confirm.ok')"
        :cancel-label="$t('content.list.item.delete.confirm.cancel')"
        @ok="deleteConfirmed" />
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
    compact: {
      type: Boolean,
      default: false,
    },
    categoryId: {
      type: [String, Number],
      default: null,
    },
  },
  data() {
    return {
      items: [],
      offset: 0,
      limit: 20,
      hasMore: false,
      loading: true,
      loadingMore: false,
      itemToDelete: null,
      filterMode: false,
      searchText: null,
      appliedSearchText: null,
      searchTimeout: null,
      advancedFilter: {
        contentTypes: null,
        status: 'published',
        spaces: null,
        selectedSpaces: [],
      },
      activeCategoryId: this.categoryId,
      categoryIds: [],
      categories: [],
      breadcrumb: null,
    };
  },
  computed: {
    hasAdvancedFilter() {
      return !!(this.advancedFilter.contentTypes?.length || this.advancedFilter.status !== 'published' || this.advancedFilter.spaces?.length);
    },
  },
  watch: {
    searchText() {
      clearTimeout(this.searchTimeout);
      this.searchTimeout = setTimeout(() => {
        this.appliedSearchText = this.searchText;
        this.load();
      }, 300);
    },
  },
  created() {
    this.load();
  },
  methods: {
    currentFilter() {
      return {
        offset: this.offset,
        limit: this.limit,
        categoryId: this.activeCategoryId,
        text: this.appliedSearchText,
        contentTypes: this.advancedFilter.contentTypes,
        status: this.advancedFilter.status,
        spaces: this.advancedFilter.spaces,
      };
    },
    applyAdvancedFilter(filter) {
      this.advancedFilter = filter;
      this.load();
    },
    selectCategory(category) {
      this.activeCategoryId = category?.id || null;
      this.load();
    },
    async refreshCategories(categoryIds) {
      this.categoryIds = categoryIds || [];
      if (this.activeCategoryId) {
        const [category, ancestorIds] = await Promise.all([
          this.$categoryService.getCategory(this.activeCategoryId),
          this.$categoryService.getAncestorIds(this.activeCategoryId),
        ]);
        const ancestors = await Promise.all((ancestorIds || []).map(id => this.$categoryService.getCategory(id)));
        this.breadcrumb = [...ancestors.reverse(), category].filter(cat => cat && cat.parentId !== 0);
        this.categories = [];
      } else {
        this.breadcrumb = null;
        const resolvedCategories = await Promise.all(this.categoryIds.map(id => this.$categoryService.getCategory(id).catch(() => null)));
        this.categories = resolvedCategories.filter(cat => cat);
      }
    },
    load() {
      this.loading = true;
      this.offset = 0;
      return this.$contentListService.getContentList(this.currentFilter())
        .then(data => {
          this.items = data?.items || [];
          this.hasMore = (data?.size || 0) >= this.limit;
          return this.refreshCategories(data?.categoryIds);
        })
        .finally(() => this.loading = false);
    },
    loadMore() {
      this.loadingMore = true;
      this.offset += this.limit;
      return this.$contentListService.getContentList(this.currentFilter())
        .then(data => {
          this.items = [...this.items, ...(data?.items || [])];
          this.hasMore = (data?.size || 0) >= this.limit;
        })
        .finally(() => this.loadingMore = false);
    },
    confirmDelete(item) {
      this.itemToDelete = item;
      this.$refs.deleteConfirmDialog.open();
    },
    deleteConfirmed() {
      const item = this.itemToDelete;
      if (!item) {
        return;
      }
      return this.$contentListService.deleteContent(item)
        .then(() => this.items = this.items.filter(contentItem => contentItem.id !== item.id || contentItem.contentType !== item.contentType))
        .catch(() => document.dispatchEvent(new CustomEvent('alert-message', {
          detail: {
            alertType: 'error',
            alertMessage: this.$t('content.list.item.delete.error'),
          },
        })));
    },
  },
};
</script>
