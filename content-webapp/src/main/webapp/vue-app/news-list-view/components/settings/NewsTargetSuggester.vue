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
  <v-autocomplete
    v-if="!value"
    ref="selectAutoComplete"
    v-model="value"
    :items="newsTargetItems"
    :placeholder="$t('news.newsTarget.selector.placeholder')"
    append-icon=""
    menu-props="closeOnClick, closeOnContentClick, maxHeight = 100"
    class="identitySuggester identitySuggesterInputStyle mt-0"
    content-class="identitySuggesterContent"
    width="100%"
    max-width="100%"
    item-text="label"
    item-value="name"
    chips
    dense
    flat
    @update:search-input="searchTerm = $event">
    <template #no-data>
      <v-list-item class="pa-0">
        <v-list-item-title class="px-2">
          {{ $t('newsTargets.settings.noTargets') }}
        </v-list-item-title>
      </v-list-item>
    </template>
    <template #item="{ item }">
      <v-list-item-title class="text-truncate">
        {{ item.label }}
      </v-list-item-title>
    </template>
  </v-autocomplete>
  <div
    v-else-if="selectedTarget"
    class="d-flex flex-column">
    <span :title="selectedTarget?.toolTipInfo" class="d-flex align-center justify-space-between pe-2 py-1 text-body">
      {{ selectedTarget?.label }}
      <v-icon
        small
        class="ml-2 red--text"
        @click="clearSelection">
        fa-trash
      </v-icon>
    </span>
    <span :title="selectedTarget?.toolTipInfo" class="text-subtitle text-truncate">
      {{ selectedTarget?.description }}
    </span>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: null
    },
    allowedTargets: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      searchTerm: null,
    };
  },
  computed: {
    newsTargetItems() {
      return this.allowedTargets;
    },
    selectedTarget() {
      if (!this.value) {
        return null;
      }
      return this.newsTargetItems.find(item => item.name === this.value) || null;
    }
  },
  watch: {
    value() {
      this.$emit('input', this.value);
    }
  },
  methods: {
    clearSelection() {
      this.$emit('input', null);
    }
  }
};
</script>
