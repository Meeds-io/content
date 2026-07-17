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
  <v-app class="contentListView border-box-sizing" flat>
    <v-main class="d-flex flex-column fill-height">
      <div class="d-flex align-center pb-2">
        <span class="text-header-title text-truncate">{{ $t('content.list.title') }}</span>
      </div>
      <div class="d-flex flex-grow-1 align-center justify-center">
        <span class="text-subtitle text-color">{{ $t('content.list.empty') }}</span>
      </div>
    </v-main>
  </v-app>
</template>
<script>
export default {
  props: {
    canEdit: {
      type: Boolean,
      default: false,
    },
    saveSettingsUrl: {
      type: String,
      default: null,
    },
  },
};
</script>
