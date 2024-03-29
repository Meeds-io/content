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
  <v-list-item class="px-0">
    <v-list-item-content :title="spaceDisplayName" class="flex-grow-1 pa-0">
      <v-checkbox
        v-model="checked"
        :label="spaceDisplayName"
        class="newsCheckbox ms-4"
        @click="changeSelection" />
    </v-list-item-content>
  </v-list-item>
</template>

<script>
export default {
  props: {
    space: {
      type: Object,
      default: null,
    },
    spaceIds: {
      type: Array,
      default: () => [],
    },
    selectedIds: {
      type: Array,
      default: () => [],
    },
  },
  data: () => ({
    saving: false,
    checked: false,
  }),
  computed: {
    selected() {
      return this.selectedIds !== false && (!this.selectedIds.length || this.selectedIds.find(ownerId => Number(ownerId) === this.spaceId));
    },
    spaceDisplayName() {
      return this.space.displayName;
    },
    spaceId() {
      return Number(this.space.id);
    },
  },
  watch: {
    selected() {
      this.checked = this.selected;
    },
  },
  mounted() {
    this.checked = this.selected;
  },
  methods: {
    changeSelection() {
      if (this.selectedIds) {
        if (this.selected) {
          if (this.selectedIds && !this.selectedIds.length) {
            this.selectedIds = this.spaceIds.slice();
          }
          const index = this.selectedIds.findIndex(spaceId => Number(spaceId) === this.spaceId);
          if (index >= 0) {
            this.selectedIds.splice(index, 1);
            if (!this.selectedIds.length) {
              this.selectedIds = false;
            }
          }
        } else {
          this.selectedIds.push(this.space.id);
        }
      } else if (!this.selected) {
        this.selectedIds = [this.space.id];
      }
      this.$emit('changeSelection', this.selectedIds);
    },

  },
};
</script>