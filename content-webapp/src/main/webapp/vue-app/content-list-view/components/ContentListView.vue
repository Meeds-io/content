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
  <v-hover v-model="hover">
    <v-app
      class="contentListView border-box-sizing"
      flat>
      <v-main class="application-body application-layout-style border-box-sizing d-flex flex-column fill-height py-5 pt-0">
        <application-toolbar
          ref="toolbar"
          :right-text-filter="{
            minCharacters: 0,
            placeholder: $t('content.list.filter.placeholder'),
          }"
          :filters-count="advancedFiltersCount"
          compact
          @filter-text-input-end-typing="onSearchTextChanged"
          @filter-expand="filterExpanded = $event">
          <template v-if="!compact && showHeader" #left>
            <span class="text-header text-truncate">{{ headerTitle || $t('content.list.title') }}</span>
          </template>
          <template v-if="!filterExpanded" #right>
            <div class="d-flex align-center ms-auto">
              <v-btn
                v-if="canEdit && hover"
                icon
                small
                :aria-label="$t('content.list.settings.drawer.open')"
                @click="$refs.settingsDrawer.open()">
                <v-icon size="20">fas fa-cog</v-icon>
              </v-btn>
              <v-btn
                icon
                small
                class="ms-4"
                :aria-label="$t('content.list.filter.drawer.open')"
                @click="$refs.filterDrawer.open(advancedFilter)">
                <v-icon size="20" :class="advancedFiltersCount && 'primary--text' || 'icon-default-color'">fas fa-sliders-h</v-icon>
              </v-btn>
              <span v-if="advancedFiltersCount" class="primary--text text-caption">({{ advancedFiltersCount }})</span>
            </div>
          </template>
        </application-toolbar>
        <categories-filter
          v-if="allowFilteringPerCategory"
          :value="activeCategoryId"
          :category-ids="categoryIds"
          :exclude-category-ids="excludeCategoryIds"
          :category-depth="categoryDepth"
          class="px-4 pb-2"
          object-type="content"
          hide-on-empty
          @input="selectCategory" />
        <div v-if="loading" class="d-flex flex-grow-1 align-center justify-center">
          <v-progress-circular indeterminate color="primary" />
        </div>
        <div v-else-if="!items.length" class="d-flex flex-grow-1 flex-column align-center justify-center">
          <v-icon size="64" class="tertiary--text mb-4">fas fa-newspaper</v-icon>
          <span>{{ $t('content.list.empty') }}</span>
        </div>
        <div v-else class="d-flex flex-column px-4">
          <content-list-item
            v-for="contentItem in items"
            :key="`${contentItem.contentType}-${contentItem.id}`"
            :item="contentItem"
            :compact="compact"
            :expanded="expanded"
            class="border-box-sizing"
            @published="load"
            @delete="confirmDelete"
            @select-category="selectCategory" />
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
        <content-list-settings-drawer v-if="canEdit" ref="settingsDrawer" />
        <note-publication-target-drawer />
        <exo-confirm-dialog
          ref="deleteConfirmDialog"
          :message="$t('content.list.item.delete.confirm.message')"
          :title="$t('content.list.item.delete.confirm.title')"
          :ok-label="$t('content.list.item.delete.confirm.ok')"
          :cancel-label="$t('content.list.item.delete.confirm.cancel')"
          @ok="deleteConfirmed" />
      </v-main>
    </v-app>
  </v-hover>
</template>
<script>
import * as queryParamUtils from '../js/queryParamUtils.js';

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
    expanded: {
      type: Boolean,
      default: true,
    },
    categoryId: {
      type: [String, Number],
      default: null,
    },
    showHeader: {
      type: Boolean,
      default: true,
    },
    headerTitle: {
      type: String,
      default: null,
    },
    allowFilteringPerCategory: {
      type: Boolean,
      default: true,
    },
    categoryIds: {
      type: Array,
      default: () => [],
    },
    excludeCategoryIds: {
      type: Array,
      default: () => [],
    },
    categoryDepth: {
      type: Number,
      default: 4,
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
      hover: false,
      filterExpanded: false,
      appliedSearchText: null,
      advancedFilter: {
        contentTypes: null,
        status: 'published',
        spaces: null,
        selectedSpaces: [],
      },
      activeCategoryId: this.categoryId,
    };
  },
  computed: {
    advancedFiltersCount() {
      let count = 0;
      if (this.advancedFilter.contentTypes?.length) {
        count++;
      }
      if (this.advancedFilter.status && this.advancedFilter.status !== 'published') {
        count++;
      }
      if (this.advancedFilter.spaces?.length) {
        count++;
      }
      return count;
    },
  },
  created() {
    const lang = eXo.env.portal.language;
    exoi18n.loadLanguageAsync(lang, `/content/i18n/locale.portlet.content.Content?lang=${lang}`);
    if (!this.compact) {
      this.appliedSearchText = queryParamUtils.getQueryParam('text');
      const contentTypesParam = queryParamUtils.getQueryParam('contentTypes');
      const spacesParam = queryParamUtils.getQueryParam('spaces');
      const spaceIds = spacesParam ? spacesParam.split(',') : null;
      this.advancedFilter = {
        contentTypes: contentTypesParam ? contentTypesParam.split(',') : null,
        status: queryParamUtils.getQueryParam('status') || 'published',
        spaces: spaceIds,
        selectedSpaces: [],
      };
      if (spaceIds?.length) {
        // The drawer's own picker needs the resolved Space objects (avatar,
        // display name), not just the ids restored from the URL - fetch
        // them once so reopening the drawer after a reload shows the
        // actually active filter instead of appearing reset to defaults.
        this.$spaceService.getSpacesByIds(spaceIds).then(data => this.advancedFilter.selectedSpaces = data?.spaces || []);
      }
      this.activeCategoryId = queryParamUtils.getQueryParam('categoryId') || this.categoryId;
    }
    this.load();
  },
  mounted() {
    if (this.appliedSearchText) {
      this.$refs.toolbar?.setTerm(this.appliedSearchText);
    }
  },
  methods: {
    onSearchTextChanged(term) {
      this.appliedSearchText = term || null;
      this.load();
    },
    syncQueryParams() {
      if (this.compact) {
        return;
      }
      queryParamUtils.updateQueryParam('text', this.appliedSearchText);
      queryParamUtils.updateQueryParam('contentTypes', this.advancedFilter.contentTypes?.join(','));
      queryParamUtils.updateQueryParam('status', this.advancedFilter.status !== 'published' ? this.advancedFilter.status : null);
      queryParamUtils.updateQueryParam('spaces', this.advancedFilter.spaces?.join(','));
      queryParamUtils.updateQueryParam('categoryId', this.activeCategoryId);
    },
    currentFilter() {
      return {
        offset: this.offset,
        limit: this.limit,
        categoryId: this.activeCategoryId,
        text: this.appliedSearchText,
        contentTypes: this.advancedFilter.contentTypes,
        status: this.advancedFilter.status,
        spaces: this.advancedFilter.spaces,
        includeCategoryIds: this.categoryIds,
        excludeCategoryIds: this.excludeCategoryIds,
      };
    },
    applyAdvancedFilter(filter) {
      this.advancedFilter = filter;
      this.load();
    },
    selectCategory(categoryId) {
      this.activeCategoryId = categoryId || null;
      this.load();
    },
    load() {
      this.loading = true;
      this.offset = 0;
      this.syncQueryParams();
      return this.$contentListService.getContentList(this.currentFilter())
        .then(data => {
          this.items = data?.items || [];
          this.hasMore = !!data?.hasMore;
        })
        .finally(() => this.loading = false);
    },
    loadMore() {
      this.loadingMore = true;
      this.offset += this.limit;
      return this.$contentListService.getContentList(this.currentFilter())
        .then(data => {
          this.items = [...this.items, ...(data?.items || [])];
          this.hasMore = !!data?.hasMore;
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
