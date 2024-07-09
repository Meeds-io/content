<!--

    This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
    ref="newsSpacesFilters"
    id="newsSpacesFilters"
    right
    @opened="drawer = true"
    @closed="drawer = false">
    <template slot="title">
      {{ $t('news.app.filter.label') }}
    </template>
    <template slot="content">
      <news-filter-space-list
        ref="filterSpaceList"
        v-model="selectedOwnerIds"
        class="me-4" />
    </template>
  </exo-drawer>
</template>

<script>
export default {
  props: {
    value: {
      type: Array,
      default: () => [],
    },
  },
  data: () => ({
    drawer: false,
    selectedOwnerIds: [],
  }),
  watch: {
    selectedOwnerIds() {
      this.applyFilters();
    },
  },
  created() {
    this.$root.$on('news-space-selector-drawer-open', this.open);
    this.selectedOwnerIds = this.value;
  },
  methods: {
    applyFilters() {
      if (this.value !== this.selectedOwnerIds){
        this.value = this.selectedOwnerIds;
      }
      this.$emit('input', this.value);
    },
    close() {
      this.$refs.newsSpacesFilters.close();
    },
    open() {
      this.$refs.newsSpacesFilters.open();
      this.$nextTick().then(() => {
        this.$refs.filterSpaceList.reset();
      });
    },
  },
};
</script>