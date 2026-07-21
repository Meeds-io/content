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
  <exo-drawer
    id="ContentListSettingsDrawer"
    ref="drawer"
    right>
    <template #title>
      {{ $t('content.list.settings.drawer.title') }}
    </template>
    <template #content>
      <div class="pa-4">
        <div class="d-flex align-center justify-space-between">
          <span class="text-subtitle text-color">{{ $t('content.list.settings.drawer.showHeader') }}</span>
          <v-switch
            v-model="showHeader"
            hide-details
            class="mt-0" />
        </div>
        <v-text-field
          v-if="showHeader"
          v-model="headerTitle"
          :placeholder="$t('content.list.title')"
          hide-details
          dense
          class="mb-4" />

        <div class="text-subtitle text-color mt-4 mb-2">{{ $t('content.list.settings.drawer.filterOptions') }}</div>
        <div class="d-flex align-center justify-space-between">
          <span class="text-body text-color">{{ $t('content.list.settings.drawer.allowFilteringPerCategory') }}</span>
          <v-switch
            v-model="allowFilteringPerCategory"
            hide-details
            class="mt-0" />
        </div>
        <div v-if="allowFilteringPerCategory" class="d-flex align-center justify-space-between mt-2">
          <span class="text-body text-color">{{ $t('content.list.settings.drawer.categoryDepth') }}</span>
          <div class="d-flex align-center">
            <v-btn
              icon
              small
              :disabled="categoryDepth <= 0"
              @click="categoryDepth--">
              <v-icon size="16">fas fa-minus</v-icon>
            </v-btn>
            <span class="mx-2">{{ categoryDepth }}</span>
            <v-btn
              icon
              small
              :disabled="categoryDepth >= 50"
              @click="categoryDepth++">
              <v-icon size="16">fas fa-plus</v-icon>
            </v-btn>
          </div>
        </div>
        <div class="d-flex align-center justify-space-between mt-4">
          <span class="text-body text-color">{{ $t('content.list.settings.drawer.showFilterOptions') }}</span>
          <v-switch
            v-model="showFilterOptions"
            hide-details
            class="mt-0" />
        </div>

        <div class="text-subtitle text-color mt-4 mb-2">{{ $t('content.list.settings.drawer.filterList') }}</div>
        <content-list-category-picker
          v-model="includeCategories"
          :label="$t('content.list.settings.drawer.filterList.include')" />
        <content-list-category-picker
          v-model="excludeCategories"
          :label="$t('content.list.settings.drawer.filterList.exclude')"
          class="mt-4" />
      </div>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn text @click="close">
          {{ $t('content.list.settings.drawer.cancel') }}
        </v-btn>
        <v-spacer />
        <v-btn
          :loading="saving"
          class="btn btn-primary"
          @click="save">
          {{ $t('content.list.settings.drawer.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    saving: false,
    showHeader: true,
    headerTitle: null,
    showFilterOptions: true,
    allowFilteringPerCategory: true,
    categoryDepth: 4,
    includeCategories: [],
    excludeCategories: [],
  }),
  methods: {
    open() {
      this.showHeader = this.$root.showHeader !== false;
      this.headerTitle = this.$root.headerTitle;
      this.showFilterOptions = this.$root.showFilterOptions !== false;
      this.allowFilteringPerCategory = this.$root.allowFilteringPerCategory !== false;
      this.categoryDepth = this.$root.categoryDepth || 4;
      this.includeCategories = [];
      this.excludeCategories = [];
      Promise.all([
        Promise.all((this.$root.categoryIds || []).map(id => this.$categoryService.getCategory(id).catch(() => null))),
        Promise.all((this.$root.excludeCategoryIds || []).map(id => this.$categoryService.getCategory(id).catch(() => null))),
      ]).then(([included, excluded]) => {
        this.includeCategories = included.filter(category => category);
        this.excludeCategories = excluded.filter(category => category);
      });
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    save() {
      this.saving = true;
      const settings = {
        showHeader: this.showHeader,
        headerTitle: this.headerTitle || '',
        showFilterOptions: this.showFilterOptions,
        allowFilteringPerCategory: this.allowFilteringPerCategory,
        categoryDepth: this.categoryDepth,
        categoryIds: this.includeCategories.map(category => category.id).join(','),
        excludeCategoryIds: this.excludeCategories.map(category => category.id).join(','),
      };
      this.$contentListService.saveSettings(this.$root.saveSettingsURL, settings)
        .then(() => window.location.reload())
        .catch(() => document.dispatchEvent(new CustomEvent('alert-message', {
          detail: {
            alertType: 'error',
            alertMessage: this.$t('content.list.settings.drawer.saveError'),
          },
        })))
        .finally(() => this.saving = false);
    },
  },
};
</script>
