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
  <v-menu
    v-model="menu"
    :content-class="`${menuId} position-absolute application-menu z-index-modal`"
    offset-y
    left>
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
      <v-list-item
        v-if="item.url"
        class="ps-2 pe-4 d-flex align-center"
        @click="copyLink">
        <v-icon size="16" class="clickable icon-menu me-2">fas fa-link</v-icon>
        <span class="text-color">{{ $t('content.list.item.copyLink') }}</span>
      </v-list-item>
      <v-list-item
        v-if="item.canEdit"
        class="ps-2 pe-4 d-flex align-center"
        @click="edit">
        <v-icon size="16" class="clickable icon-menu me-2">fas fa-edit</v-icon>
        <span class="text-color">{{ $t('content.list.item.edit') }}</span>
      </v-list-item>
      <component
        :is="publishActionExtension.componentName"
        v-if="item.canPublish && !item.draft && publishActionExtension"
        :item="item"
        @close="menu = false"
        @published="$emit('published', item)" />
      <v-list-item
        v-else-if="item.canPublish && !item.draft"
        class="ps-2 pe-4 d-flex align-center"
        @click="publish">
        <v-icon size="16" class="clickable icon-menu me-2">fa-solid fa-paper-plane</v-icon>
        <span class="text-color">{{ $t('content.list.item.publish') }}</span>
      </v-list-item>
      <v-list-item
        v-if="item.canDelete"
        class="ps-2 pe-4 d-flex align-center"
        @click="deleteItem">
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
  data: () => ({
    menu: false,
    menuId: `contentActionMenu${parseInt(Math.random() * 10000)}`,
    publishActionExtensions: [],
  }),
  computed: {
    publishActionExtension() {
      return this.publishActionExtensions.find(extension => extension.type === this.item.contentType);
    },
  },
  watch: {
    menu(opened) {
      if (opened) {
        this.$root.$emit('content-list-item-menu-opened', this.menuId);
      }
    },
  },
  created() {
    document.addEventListener('click', this.closeMenuOnClick);
    document.addEventListener('scroll', this.closeMenu, true);
    this.$root.$on('content-list-item-menu-opened', this.closeIfOther);
    this.publishActionExtensions = extensionRegistry.loadExtensions('ContentListItem', 'publishAction') || [];
  },
  beforeDestroy() {
    document.removeEventListener('click', this.closeMenuOnClick);
    document.removeEventListener('scroll', this.closeMenu, true);
    this.$root.$off('content-list-item-menu-opened', this.closeIfOther);
  },
  methods: {
    closeMenu() {
      this.menu = false;
    },
    closeMenuOnClick(e) {
      if (this.menu && e.target && !e.target.closest(`.${this.menuId}`)) {
        this.menu = false;
      }
    },
    closeIfOther(openedMenuId) {
      if (this.menu && openedMenuId !== this.menuId) {
        this.menu = false;
      }
    },
    copyLink() {
      this.menu = false;
      navigator.clipboard.writeText(`${window.location.origin}${this.item.url}`)
        .then(() => document.dispatchEvent(new CustomEvent('alert-message', {
          detail: {
            alertType: 'success',
            alertMessage: this.$t('content.list.item.copyLink.success'),
          },
        })));
    },
    edit() {
      this.menu = false;
      this.$emit('edit', this.item);
    },
    publish() {
      this.menu = false;
      this.$emit('publish', this.item);
    },
    deleteItem() {
      this.menu = false;
      this.$emit('delete', this.item);
    },
  },
};
</script>
