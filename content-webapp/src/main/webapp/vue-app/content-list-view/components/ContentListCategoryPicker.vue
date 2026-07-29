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
    <span v-if="label" class="text-body text-color">{{ label }}</span>
    <category-suggester
      v-model="categoryId"
      class="mt-1 mb-2 mx-0 pa-0" />
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
        :aria-label="$t('content.list.settings.drawer.filterList.deleteCategory')"
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
    categoryId: null,
  }),
  watch: {
    async categoryId() {
      if (this.categoryId) {
        if (!this.value.some(category => category.id === this.categoryId)) {
          const category = await this.$categoryService.getCategory(this.categoryId).catch(() => null);
          if (category) {
            this.$emit('input', [...this.value, category]);
          }
        }
        await this.$nextTick();
        this.categoryId = null;
      }
    },
  },
  methods: {
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
