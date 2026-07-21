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
    v-model="drawer"
    :loading="saving"
    allow-expand
    right>
    <template #title>
      {{ $t('content.list.settings.drawer.title') }}
    </template>
    <template v-if="drawer" #content>
      <div class="pa-5">
        <div class="mb-2 text-header">{{ $t('content.list.settings.drawer.displayOptions') }}</div>
        <div class="d-flex align-center text-start">
          <div>{{ $t('content.list.settings.drawer.showHeader') }}</div>
          <v-spacer />
          <v-switch
            v-model="showHeader"
            hide-details
            class="ma-0 width-fit-content" />
        </div>
        <translation-text-field
          v-if="showHeader"
          ref="headerTitleInput"
          id="headerTitleInput"
          v-model="headerTranslations"
          :default-language="language"
          :placeholder="$t('content.list.title')"
          :drawer-title="$t('content.list.settings.drawer.titleTranslation')"
          maxlength="150"
          class="mb-4"
          no-expand-icon
          back-icon
          required />

        <div class="mt-4 mb-2 text-header">{{ $t('content.list.settings.drawer.filterOptions') }}</div>
        <div class="d-flex align-center text-start">
          <div>{{ $t('content.list.settings.drawer.allowFilteringPerCategory') }}</div>
          <v-spacer />
          <v-switch
            v-model="allowFilteringPerCategory"
            hide-details
            class="ma-0 width-fit-content" />
        </div>
        <div v-if="allowFilteringPerCategory" class="d-flex full-width align-center text-start">
          <div>{{ $t('content.list.settings.drawer.categoryDepth') }}</div>
          <v-spacer />
          <number-input
            v-model="categoryDepth"
            :step="1"
            :min="0"
            :max="50" />
        </div>

        <div class="mt-4 mb-2 text-header">{{ $t('content.list.settings.drawer.filterList') }}</div>
        <div class="d-flex align-center text-start">
          <div>{{ $t('content.list.settings.drawer.filterList.include') }}</div>
          <v-spacer />
          <v-switch
            v-model="filterIncludeCategories"
            hide-details
            class="ma-0 width-fit-content" />
        </div>
        <content-list-category-picker
          v-if="filterIncludeCategories"
          v-model="includeCategories"
          sortable
          class="mt-2" />

        <div class="d-flex align-center text-start mt-4">
          <div>{{ $t('content.list.settings.drawer.filterList.exclude') }}</div>
          <v-spacer />
          <v-switch
            v-model="filterExcludeCategories"
            hide-details
            class="ma-0 width-fit-content" />
        </div>
        <content-list-category-picker
          v-if="filterExcludeCategories"
          v-model="excludeCategories"
          class="mt-2" />
      </div>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn
          :disabled="saving"
          class="btn ms-auto me-2"
          @click="close">
          {{ $t('content.list.settings.drawer.cancel') }}
        </v-btn>
        <v-btn
          :loading="saving"
          class="btn btn-primary"
          elevation="0"
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
    drawer: false,
    saving: false,
    translationObjectType: 'contentListView',
    headerTitleFieldName: 'headerTitleInput',
    language: 'en',
    showHeader: true,
    headerTranslations: null,
    allowFilteringPerCategory: true,
    categoryDepth: 4,
    includeCategories: [],
    excludeCategories: [],
    filterIncludeCategories: false,
    filterExcludeCategories: false,
  }),
  watch: {
    filterIncludeCategories() {
      if (this.drawer && !this.filterIncludeCategories) {
        this.includeCategories = [];
      }
    },
    filterExcludeCategories() {
      if (this.drawer && !this.filterExcludeCategories) {
        this.excludeCategories = [];
      }
    },
  },
  methods: {
    open() {
      this.language = this.$root.language || 'en';
      this.showHeader = this.$root.showHeader !== false;
      this.headerTranslations = this.$root.headerTranslations || {};
      this.allowFilteringPerCategory = this.$root.allowFilteringPerCategory !== false;
      this.categoryDepth = this.$root.categoryDepth || 4;
      this.includeCategories = [];
      this.excludeCategories = [];
      this.filterIncludeCategories = false;
      this.filterExcludeCategories = false;
      Promise.all([
        Promise.all((this.$root.categoryIds || []).map(id => this.$categoryService.getCategory(id).catch(() => null))),
        Promise.all((this.$root.excludeCategoryIds || []).map(id => this.$categoryService.getCategory(id).catch(() => null))),
      ]).then(([included, excluded]) => {
        this.includeCategories = included.filter(category => category);
        this.excludeCategories = excluded.filter(category => category);
        this.filterIncludeCategories = !!this.includeCategories.length;
        this.filterExcludeCategories = !!this.excludeCategories.length;
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
        headerTitle: this.headerTranslations?.[this.language] || this.headerTranslations?.[this.$root.defaultLanguage] || '',
        allowFilteringPerCategory: this.allowFilteringPerCategory,
        categoryDepth: this.categoryDepth,
        categoryIds: this.includeCategories.map(category => category.id).join(','),
        excludeCategoryIds: this.excludeCategories.map(category => category.id).join(','),
      };
      this.$contentListService.saveSettings(this.$root.saveSettingsURL, settings)
        .then(() => this.$translationService.saveTranslations(this.translationObjectType, this.$root.applicationId,
          this.headerTitleFieldName, this.headerTranslations))
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
