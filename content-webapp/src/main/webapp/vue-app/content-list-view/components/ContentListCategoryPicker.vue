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
  <div>
    <span class="text-body text-color">{{ label }}</span>
    <v-text-field
      v-model="query"
      :placeholder="$t('content.list.settings.drawer.filterList.search')"
      prepend-inner-icon="fa-search"
      hide-details
      dense
      class="mt-1 mb-1" />
    <v-list
      v-if="suggestions.length"
      dense
      class="pa-0">
      <v-list-item
        v-for="category in suggestions"
        :key="category.id"
        class="ps-2 pe-4"
        @click="add(category)">
        <span class="text-truncate">{{ category.name }}</span>
      </v-list-item>
    </v-list>
    <div
      v-for="(category, index) in value"
      :key="category.id"
      class="d-flex align-center py-1">
      <span class="text-truncate flex-grow-1">{{ category.name || category.id }}</span>
      <v-btn
        icon
        small
        @click="moveUp(index)"
        v-if="index > 0">
        <v-icon size="14">fas fa-arrow-up</v-icon>
      </v-btn>
      <v-btn
        icon
        small
        @click="remove(category)">
        <v-icon size="16" color="error">fas fa-trash</v-icon>
      </v-btn>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    value: {
      type: Array,
      default: () => [],
    },
    label: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    query: null,
    suggestions: [],
  }),
  watch: {
    query() {
      if (!this.query) {
        this.suggestions = [];
        return;
      }
      this.$categoryService.findCategories({query: this.query, limit: 10}).then(data => {
        const selectedIds = this.value.map(category => category.id);
        this.suggestions = (data || []).filter(category => !selectedIds.includes(category.id));
      });
    },
  },
  methods: {
    add(category) {
      this.$emit('input', [...this.value, category]);
      this.query = null;
      this.suggestions = [];
    },
    remove(category) {
      this.$emit('input', this.value.filter(selected => selected.id !== category.id));
    },
    moveUp(index) {
      const reordered = [...this.value];
      [reordered[index - 1], reordered[index]] = [reordered[index], reordered[index - 1]];
      this.$emit('input', reordered);
    },
  },
};
</script>
