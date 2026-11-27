<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io

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
  <v-app
    v-if="canView">
    <v-card
      :class="$root.isCard ? 'px-5 pt-5' : 'pa-5'"
      min-width="100%"
      max-width="100%"
      class="hover-reveal-parent hover-swap-trigger d-flex flex-column align-center border-box-sizing position-relative application-body"
      flat>
      <links-header
        v-if="$root.initialized"
        :settings="$root.settings"
        :has-links="$root.hasLinks"
        :can-edit="canEdit"
        class="full-width"
        @edit="editSettings()"
        @add="editSettings(true)"
        @hidden="markHidden" />
      <links-list
        v-if="hasLinks"
        :settings="$root.settings"
        :links="$root.links"
        @edit="editSettings()" />
    </v-card>
    <links-settings-drawer
      v-if="canEdit && edit" />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    loading: true,
    edit: false,
    previewMode: false,
  }),
  computed: {
    hasLinks() {
      return this.$root.links?.length;
    },
    canView() {
      // On the page, the application shows while one of its links is
      // available, to editors too. The page layout editor always shows it,
      // with every link (main.js), so that an editor can fix the links that
      // lead to pages that don't exist on this instance, and an editor also
      // sees an application with no link yet, to fill it. A user who hid it
      // sees it in the page layout editor only
      return this.$root.initialized
        && (this.$root.hasLinks
          || this.$root.inLayoutEditor
          || (this.canEdit && !this.$root.settings?.links?.length))
        && (!this.hiddenByUser || this.$root.inLayoutEditor);
    },
    // The current user hid the application, which the page layout editor still
    // displays
    hiddenByUser() {
      return !!this.$root.settings?.allowHide && !!this.$root.settings?.hidden;
    },
    canEdit() {
      return !this.previewMode && this.$root.canEdit;
    },
  },
  watch: {
    canView() {
      this.$root.$updateApplicationVisibility(this.canView);
    },
    '$root.initialized'() {
      // canView stays false when every link is unavailable, or for a user who
      // hid the application: apply it once loaded
      this.$root.$updateApplicationVisibility(this.canView);
    },
  },
  created() {
    document.addEventListener('cms-preview-mode', this.switchToPreview);
    document.addEventListener('cms-edit-mode', this.switchToEdit);
    this.$root.$on('links-refresh', this.retrieveSettings);
  },
  mounted() {
    const element = this.$el?.closest ? this.$el : this.$el?.parentElement;
    this.$root.inLayoutEditor = !!element?.closest?.('.layout-application[data-storage-id]');
  },
  beforeDestroy() {
    this.$root.$off('links-refresh', this.retrieveSettings);
    document.removeEventListener('cms-edit-mode', this.switchToEdit);
    document.removeEventListener('cms-preview-mode', this.switchToPreview);
  },
  methods: {
    markHidden() {
      this.$root.settings = {...this.$root.settings, hidden: true};
    },
    retrieveSettings(settings) {
      if (settings) {
        this.$root.settings = settings;
        return;
      }
      this.loading = true;
      this.$linkService.getSettings(this.$root.name, this.$root.language)
        .then(settings => {
          if (!settings.vAlign) {
            settings.vAlign = settings.type === 'COLUMN' ? 'START' : 'CENTER';
          }
          if (!settings.hAlign) {
            settings.hAlign = settings.type === 'COLUMN' ? 'START' : 'CENTER';
          }
          this.$root.settings = settings;
        })
        .then(() => this.loading = false);
    },
    editSettings(openForm) {
      this.edit = false;
      this.$nextTick().then(() => {
        this.edit = true;
        this.$nextTick().then(() => {
          this.$root.$emit('links-settings-drawer', openForm);
        });
      });
    },
    switchToPreview() {
      this.previewMode = true;
    },
    switchToEdit() {
      this.previewMode = false;
    },
  },
};
</script>
