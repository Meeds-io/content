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
    id="ContentFilterDrawer"
    ref="drawer"
    right>
    <template #title>
      {{ $t('content.list.filter.drawer.title') }}
    </template>
    <template #content>
      <div class="pa-4">
        <div class="text-header">{{ $t('content.list.filter.drawer.contentType') }}</div>
        <v-radio-group
          v-model="contentTypeOption"
          hide-details
          class="mt-0">
          <v-radio :label="$t('content.list.filter.any')" value="any" />
          <v-radio
            v-for="contentType in contentTypes"
            :key="contentType.type"
            :label="$t(contentType.labelKey)"
            :value="contentType.type" />
        </v-radio-group>

        <div class="text-header mt-4">{{ $t('content.list.filter.drawer.status') }}</div>
        <v-radio-group
          v-model="status"
          hide-details
          class="mt-0">
          <v-radio :label="$t('content.list.filter.status.published')" value="published" />
          <v-radio :label="$t('content.list.filter.status.myContent')" value="myContent" />
          <v-radio :label="$t('content.list.filter.status.scheduled')" value="scheduled" />
          <v-radio :label="$t('content.list.filter.status.draft')" value="draft" />
        </v-radio-group>

        <div class="text-header mb-2 mt-4">{{ $t('content.list.filter.drawer.spaces') }}</div>
        <v-radio-group
          v-model="spacesOption"
          hide-details
          class="mt-0">
          <v-radio :label="$t('content.list.filter.any')" value="any" />
          <v-radio :label="$t('content.list.filter.spaces.selected')" value="selected" />
        </v-radio-group>
        <div v-if="spacesOption === 'selected'" class="mt-2">
          <v-text-field
            v-model="spaceQuery"
            :placeholder="$t('content.list.filter.spaces.search')"
            prepend-inner-icon="fa-search"
            hide-details
            dense
            class="mb-2" />
          <v-list
            v-if="spaceSuggestions.length"
            dense
            class="pa-0">
            <v-list-item
              v-for="space in spaceSuggestions"
              :key="space.id"
              class="ps-2 pe-4"
              @click="addSpace(space)">
              <exo-space-avatar
                :space-id="space.id"
                :size="24"
                class="me-2" />
              <span class="text-truncate">{{ space.displayName }}</span>
            </v-list-item>
          </v-list>
          <div
            v-for="space in selectedSpaces"
            :key="space.id"
            class="d-flex align-center py-1">
            <exo-space-avatar
              :space-id="space.id"
              :size="24"
              class="me-2" />
            <span class="text-truncate flex-grow-1">{{ space.displayName }}</span>
            <v-btn
              icon
              small
              :aria-label="$t('content.list.filter.spaces.remove')"
              @click="removeSpace(space)">
              <v-icon size="16" color="error">fas fa-trash</v-icon>
            </v-btn>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn text @click="reset">
          {{ $t('content.list.filter.drawer.reset') }}
        </v-btn>
        <v-spacer />
        <v-btn class="btn btn-primary" @click="apply">
          {{ $t('content.list.filter.drawer.apply') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    contentTypeOption: 'any',
    status: 'published',
    spacesOption: 'any',
    spaceQuery: null,
    spaceSuggestions: [],
    selectedSpaces: [],
    contentTypes: [],
  }),
  created() {
    this.$contentListService.getContentTypes().then(contentTypes => this.contentTypes = contentTypes || []);
  },
  watch: {
    spaceQuery() {
      if (!this.spaceQuery) {
        this.spaceSuggestions = [];
        return;
      }
      this.$spaceService.getSpaces(this.spaceQuery, 0, 10, 'member', 'identity').then(data => {
        const selectedIds = this.selectedSpaces.map(space => space.id);
        this.spaceSuggestions = (data?.spaces || []).filter(space => !selectedIds.includes(space.id));
      });
    },
  },
  methods: {
    open(currentFilter) {
      this.contentTypeOption = currentFilter?.contentTypes?.length === 1 && currentFilter.contentTypes[0] || 'any';
      this.status = currentFilter?.status || 'published';
      this.selectedSpaces = currentFilter?.selectedSpaces || [];
      this.spacesOption = this.selectedSpaces.length ? 'selected' : 'any';
      this.spaceQuery = null;
      this.spaceSuggestions = [];
      this.$refs.drawer.open();
    },
    addSpace(space) {
      this.selectedSpaces = [...this.selectedSpaces, space];
      this.spaceQuery = null;
      this.spaceSuggestions = [];
    },
    removeSpace(space) {
      this.selectedSpaces = this.selectedSpaces.filter(selectedSpace => selectedSpace.id !== space.id);
    },
    reset() {
      this.contentTypeOption = 'any';
      this.status = 'published';
      this.spacesOption = 'any';
      this.selectedSpaces = [];
    },
    apply() {
      this.$emit('apply', {
        contentTypes: this.contentTypeOption === 'any' ? null : [this.contentTypeOption],
        status: this.status,
        spaces: this.spacesOption === 'selected' ? this.selectedSpaces.map(space => space.id) : null,
        selectedSpaces: this.spacesOption === 'selected' ? this.selectedSpaces : [],
      });
      this.$refs.drawer.close();
    },
  },
};
</script>
