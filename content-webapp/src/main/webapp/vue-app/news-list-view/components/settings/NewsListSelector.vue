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
  <v-flex :id="id">
    <v-autocomplete
      ref="selectAutoComplete"
      v-model="selectedArticles"
      :placeholder="$t('news.list.settings.source.selectedList.placeholder')"
      :items="articles"
      :loading="loadingSuggestions"
      :hide-no-data="hideNoData"
      append-icon=""
      menu-props="closeOnClick, closeOnContentClick, maxHeight = 100"
      class="identitySuggester identitySuggesterInputStyle mt-0"
      :class="selectedArticles?.length && 'mb-0' || 'mb-2'"
      content-class="identitySuggesterContent"
      width="100%"
      max-width="100%"
      item-text="title"
      item-value="id"
      multiple
      hide-selected
      return-object
      dense
      flat
      chips
      @update:search-input="searchTerm = $event">
      <template #no-data>
        <v-list-item class="pa-0">
          <v-list-item-title class="px-2">
            {{ noDataLabel }}
          </v-list-item-title>
        </v-list-item>
      </template>
      <template #item="{ item }">
        <v-list-item-title class="text-truncate">
          {{ item.title }}
        </v-list-item-title>
      </template>
      <template #selection>
      </template>
    </v-autocomplete>
    <v-list dense class="ma-0 pa-0">
      <v-list-item
        class="px-0"
        v-for="(article, index) in selectedArticles"
        :class="index === lastSelectedIndex && 'mb-2' || 'mb-n2'"
        :key="article.id">
        <v-list-item-title class="text-truncate pa-0 ma-0">
          {{ article.title }}
        </v-list-item-title>
        <v-list-item-action class="flex-row ma-0 align-center justify-end">
          <v-btn
            v-if="index !== 0"
            icon
            small
            @click="moveUp(index)">
            <v-icon small>fa-arrow-up</v-icon>
          </v-btn>
          <v-btn
            v-if="index !== lastSelectedIndex"
            icon
            small
            @click="moveDown(index)">
            <v-icon small>fa-arrow-down</v-icon>
          </v-btn>
          <v-btn
            icon
            small
            class="ml-2"
            @click="remove(article)">
            <v-icon class="error-color" small>
              fa-trash
            </v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </v-list>
  </v-flex>
</template>

<script>
export default {
  props: {
    value: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      id: `ArticleSuggester${crypto.randomUUID()}`,
      searchTerm: null,
      loadingSuggestions: false,
      searchStarted: false,
      startSearchAfterInMilliseconds: 500,
      endTypingKeywordTimeout: 50,
      startTypingKeywordTimeout: 0,
      typing: false,
      articles: [],
      selectedArticles: [],
    };
  },
  computed: {
    lastSelectedIndex() {
      return this.selectedArticles.length - 1;
    },
    noDataLabel() {
      return this.loadingSuggestions && this.$t('Search.label.inProgress') || this.$t('news.article.selector.noResultsFound');
    },
    hideNoData() {
      return !this.searchStarted;
    },
  },
  watch: {
    value: {
      immediate: true,
      handler(val) {
        this.selectedArticles = val || [];
      },
    },
    selectedArticles: {
      deep: true,
      handler(val) {
        this.$emit('input', val);
      },
    },
    searchTerm() {
      this.$refs.selectAutoComplete.focus();
      this.startTypingKeywordTimeout =
          Date.now() + this.startSearchAfterInMilliseconds;
      if (!this.typing) {
        this.typing = true;
        this.waitForEndTyping();
      }
    },
    loadingSuggestions() {
      if (this.loadingSuggestions && !this.searchStarted) {
        this.searchStarted = true;
      }
    },
  },
  methods: {
    waitForEndTyping() {
      window.setTimeout(() => {
        if (Date.now() > this.startTypingKeywordTimeout) {
          this.typing = false;
          this.searchArticles();
        } else {
          this.waitForEndTyping();
        }
      }, this.endTypingKeywordTimeout);
    },
    async searchArticles() {
      if (this.searchTerm?.length) {
        this.loadingSuggestions = true;
        this.articles = [];
        const result = await this.$newsListService.searchArticles(this.searchTerm) || [];
        this.articles = result.map(item => {
          return {
            id: item?.objectId || item?.id,
            title: item.title,
          };
        });
        this.loadingSuggestions = false;
        this.searchStarted = true;
      } else {
        this.searchStarted = false;
        this.articles = [];
      }
    },
    remove(article) {
      this.selectedArticles = this.selectedArticles.filter(
        (a) => a.id !== article.id
      );
    },
    moveUp(index) {
      const temp = this.selectedArticles[index - 1];
      this.$set(this.selectedArticles, index - 1, this.selectedArticles[index]);
      this.$set(this.selectedArticles, index, temp);
    },
    moveDown(index) {
      const temp = this.selectedArticles[index + 1];
      this.$set(this.selectedArticles, index + 1, this.selectedArticles[index]);
      this.$set(this.selectedArticles, index, temp);
    },
  },
};
</script>
