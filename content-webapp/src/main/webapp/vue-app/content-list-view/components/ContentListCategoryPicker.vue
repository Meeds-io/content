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
      class="mt-1 mb-2 mx-0 pa-0"
      label="" />
    <v-list class="pa-0" dense>
      <v-list-item
        v-for="(category, index) in value"
        :key="category.id"
        class="pa-0"
        dense>
        <v-list-item-icon class="me-2 my-auto">
          <v-icon size="24">{{ category.icon }}</v-icon>
        </v-list-item-icon>
        <v-list-item-content class="me-2 pa-0 text-truncate">
          <v-list-item-title class="text-truncate">
            {{ category.name || category.id }}
          </v-list-item-title>
        </v-list-item-content>
        <v-list-item-action
          v-if="sortable"
          :class="index === (value.length - 1) && 'invisible'"
          class="ms-2 my-auto">
          <v-btn
            :title="$t('content.list.settings.drawer.filterList.moveDown')"
            icon
            @click="moveDown(index)">
            <v-icon size="18">fa-arrow-down</v-icon>
          </v-btn>
        </v-list-item-action>
        <v-list-item-action
          v-if="sortable"
          :class="index === 0 && 'invisible'"
          class="mx-0 my-auto">
          <v-btn
            :title="$t('content.list.settings.drawer.filterList.moveUp')"
            icon
            @click="moveUp(index)">
            <v-icon size="18">fa-arrow-up</v-icon>
          </v-btn>
        </v-list-item-action>
        <v-list-item-action class="mx-0 my-auto">
          <v-btn
            :title="$t('content.list.settings.drawer.filterList.deleteCategory')"
            icon
            @click="remove(category)">
            <v-icon size="18" color="error">fa-trash</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </v-list>
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
    sortable: {
      type: Boolean,
      default: false,
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
      if (index > 0) {
        const reordered = [...this.value];
        const item = reordered[index];
        reordered.splice(index, 1);
        reordered.splice(index - 1, 0, item);
        this.$emit('input', reordered);
      }
    },
    moveDown(index) {
      if (index < this.value.length - 1) {
        const reordered = [...this.value];
        const item = reordered[index];
        reordered.splice(index, 1);
        reordered.splice(index + 1, 0, item);
        this.$emit('input', reordered);
      }
    },
  },
};
</script>
