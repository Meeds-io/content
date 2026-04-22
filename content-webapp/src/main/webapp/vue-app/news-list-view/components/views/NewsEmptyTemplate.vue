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
  <v-hover v-slot="{ hover }">
    <v-app
      v-show="displayEmptyTemplateOptions"
      class="newsEmptyTemplate border-box-sizing"
      flat>
      <v-main class="d-flex flex-column fill-height position-relative">
        <v-sheet
          class="d-flex justify-end"
          :height="40"
          :min-height="40">
          <v-btn
            v-if="hover && canManageNewsList"
            icon
            @click="openDrawer">
            <v-icon>mdi-cog</v-icon>
          </v-btn>
        </v-sheet>
        <div class="d-flex flex-grow-1 mb-6 align-center justify-center">
          <v-btn
            v-if="canCreateNews"
            class="btn btn-primary"
            outlined
            @click="openNewsEditor">
            {{ $t('news.list.settings.createNews') }}
          </v-btn>
          <v-btn
            v-else
            class="btn btn-primary"
            outlined
            @click="openDrawer">
            {{ $t('news.latest.openSettings') }}
          </v-btn>
        </div>
      </v-main>
    </v-app>
  </v-hover>
</template>

<script>

export default {
  computed: {
    canManageNewsList() {
      return this.$root.canManageNewsList;
    },
    canCreateNews() {
      return this.$root.canCreateNews;
    },
    displayEmptyTemplateOptions() {
      return this.canManageNewsList || this.canCreateNews ;
    }
  },
  methods: {
    openDrawer() {
      this.$root.$emit('news-settings-drawer-open');
    },
    openNewsEditor() {
      let url = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news-editor`;
      url += `?spaceId=${eXo.env.portal.spaceId}&spaceName=${eXo.env.portal.spaceName}&type=draft`;
      window.open(url, '_blank');
    }
  }
};
</script>
