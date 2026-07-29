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
  <v-menu offset-y left>
    <template #activator="{on, attrs}">
      <v-btn
        v-on="on"
        v-bind="attrs"
        icon
        small
        :aria-label="$t('content.list.item.actions')">
        <v-icon size="18">fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-list class="pa-0 text-no-wrap width-fit-content contentActionMenuItems">
      <v-list-item class="ps-2 pe-4 d-flex align-center" @click="copyLink">
        <v-icon size="16" class="clickable icon-menu me-2">fas fa-link</v-icon>
        <span class="text-color">{{ $t('content.list.item.copyLink') }}</span>
      </v-list-item>
      <v-list-item
        v-if="item.canEdit"
        class="ps-2 pe-4 d-flex align-center"
        @click="$emit('edit', item)">
        <v-icon size="16" class="clickable icon-menu me-2">fas fa-edit</v-icon>
        <span class="text-color">{{ $t('content.list.item.edit') }}</span>
      </v-list-item>
      <v-list-item
        v-if="item.canPublish && !item.draft"
        class="ps-2 pe-4 d-flex align-center"
        @click="$emit('publish', item)">
        <v-icon size="16" class="clickable icon-menu me-2">fa-solid fa-paper-plane</v-icon>
        <span class="text-color">{{ $t('content.list.item.publish') }}</span>
      </v-list-item>
      <v-list-item
        v-if="item.canDelete"
        class="ps-2 pe-4 d-flex align-center"
        @click="$emit('delete', item)">
        <v-icon size="16" class="clickable icon-menu me-2">fas fa-trash</v-icon>
        <span class="text-color">{{ $t('content.list.item.delete') }}</span>
      </v-list-item>
    </v-list>
  </v-menu>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      required: true,
    },
  },
  methods: {
    copyLink() {
      navigator.clipboard.writeText(this.item.url)
        .then(() => document.dispatchEvent(new CustomEvent('alert-message', {
          detail: {
            alertType: 'success',
            alertMessage: this.$t('content.list.item.copyLink.success'),
          },
        })));
    },
  },
};
</script>
