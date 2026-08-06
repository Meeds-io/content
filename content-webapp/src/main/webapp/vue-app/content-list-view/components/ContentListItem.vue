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
  <div :class="isCompactDisplay ? 'py-2' : 'py-3'" class="contentListItem d-flex">
    <a
      :href="item.url"
      class="d-flex align-center justify-center flex-shrink-0 rounded position-relative me-4"
      :style="illustrationStyle"
      @click="onItemLinkClick">
      <v-icon
        v-if="!item.illustrationUrl"
        color="white"
        :size="illustrationIconSize">
        {{ item.icon }}
      </v-icon>
      <v-avatar
        size="20"
        color="white"
        class="position-absolute t-0 l-0 ma-1">
        <v-icon size="12" class="icon-default-color">{{ item.icon }}</v-icon>
      </v-avatar>
    </a>
    <div class="d-flex flex-column flex-grow-1 overflow-hidden">
      <div class="d-flex align-center">
        <a
          :href="item.url"
          class="no-min-width text-truncate text-left font-weight-bold text-color flex-grow-1"
          @click="onItemLinkClick">
          {{ item.title }}
        </a>
        <div class="d-flex align-center flex-shrink-0 ms-2">
          <category-chip
            v-for="category in filteredCategories"
            :key="category.id"
            :category="category"
            small
            class="flex-shrink-0 ms-1"
            @select="selectCategory" />
        </div>
        <v-btn
          v-if="remainingCount > 0"
          class="flex-shrink-0 flex-grow-0 px-0 ms-1"
          height="24"
          width="24"
          icon
          @click="openMoreCategoriesDrawer">
          <span class="primary--text text-subtitle-font-size">
            {{ $t('categories.remainingCount', { 0: remainingCount }) }}
          </span>
        </v-btn>
        <categories-list-drawer
          v-if="moreCategoriesDrawer"
          ref="moreCategoriesDrawer"
          @select="selectCategory" />
        <favorite-button
          v-if="canBookmark"
          :id="item.id"
          :favorite="item.favorite"
          :space-id="item.spaceId"
          :type="item.contentType"
          :type-label="item.contentType"
          class="flex-shrink-0 ms-1" />
        <content-action-menu-items
          :item="item"
          class="flex-shrink-0 ms-2"
          @edit="editItem"
          @publish="openItem"
          @published="$emit('published', item)"
          @delete="$emit('delete', item)" />
      </div>
      <div class="d-flex align-center text-caption text-color mb-1">
        <exo-space-avatar
          v-if="item.spaceId"
          :space-id="item.spaceId"
          :avatar="isCompactDisplay"
          :size="20"
          small-font-size
          popover
          class="me-2" />
        <v-icon size="2" class="me-2">fas fa-circle</v-icon>
        <exo-user-avatar
          :profile-id="item.authorUsername"
          :size="20"
          small-font-size
          class="me-2" />
        <v-icon size="2" class="me-2">fas fa-circle</v-icon>
        <v-icon size="12" class="me-2">far fa-clock</v-icon>
        <date-format :value="item.date" :format="dateFormat" />
      </div>
      <p class="text-truncate-2 text-body mb-0">
        {{ item.summary }}
      </p>
      <div v-if="!isCompactDisplay && (item.viewsCount || item.attachmentsCount)" class="d-flex align-center justify-end text-caption text-color mt-auto pt-1">
        <template v-if="item.viewsCount">
          <v-icon size="14" class="me-1">fas fa-eye</v-icon>
          <span class="me-3">{{ item.viewsCount }}</span>
        </template>
        <template v-if="item.attachmentsCount">
          <v-icon size="14" class="me-1">fas fa-paperclip</v-icon>
          <span>{{ item.attachmentsCount }}</span>
        </template>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      required: true,
    },
    compact: {
      type: Boolean,
      default: false,
    },
    expanded: {
      type: Boolean,
      default: true,
    },
  },
  data: () => ({
    categories: [],
    moreCategoriesDrawer: false,
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
  }),
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.mobile;
    },
    isCompactDisplay() {
      return this.isMobile || (this.compact && !this.expanded);
    },
    canBookmark() {
      return !this.isMobile && !this.item.draft && !this.item.scheduled;
    },
    illustrationIconSize() {
      if (this.isCompactDisplay) {
        return 24;
      }
      return !this.compact && 42 || this.expanded && 32 || 24;
    },
    illustrationStyle() {
      const width = this.isCompactDisplay ? '64px' : (!this.compact && '150px' || this.expanded && '112px' || '64px');
      const minHeight = this.isCompactDisplay ? '64px' : (!this.compact && '120px' || this.expanded && '90px' || '64px');
      return {
        width,
        minHeight,
        alignSelf: 'stretch',
        ...(this.item.illustrationUrl && {
          backgroundImage: `url(${this.item.illustrationUrl})`,
          backgroundSize: 'cover',
        } || {
          backgroundColor: '#F5A623',
        }),
      };
    },
    categoryIdsKey() {
      return this.item.categoryIds?.join(',');
    },
    maxVisibleCategories() {
      if (this.isCompactDisplay) {
        return 0;
      }
      return this.expanded ? 2 : 1;
    },
    filteredCategories() {
      return this.categories.slice(0, this.maxVisibleCategories);
    },
    remainingCount() {
      if (this.isCompactDisplay) {
        return this.categories.length;
      }
      return this.categories.length - this.maxVisibleCategories;
    },
  },
  watch: {
    categoryIdsKey: {
      immediate: true,
      handler() {
        if (this.item.categoryIds?.length) {
          Promise.all(this.item.categoryIds.map(id => this.$categoryService.getCategory(id).catch(() => null)))
            .then(categories => this.categories = categories.filter(category => category));
        } else {
          this.categories = [];
        }
      },
    },
  },
  methods: {
    openItem() {
      // Publish deep-links into the item's own detail page, where the
      // full publish flow already lives - the merged list is a listing
      // view, not a place to reimplement that flow.
      window.open(this.item.url, '_blank');
    },
    editItem() {
      // Unlike the view/publish URL, editing needs each content type's own
      // dedicated editor app (news-editor/notes-editor), not the item's own
      // view page - mirrors NewsApp.vue's getEditUrl() and
      // NotePage.vue's editNote() exactly.
      const editUrl = this.item.contentType === 'notes' ? this.getNoteEditUrl() : this.getNewsEditUrl();
      window.open(editUrl, '_blank');
    },
    getNewsEditUrl() {
      let editUrl = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news-editor?newsId=${this.item.id}`;
      if (this.item.spaceId) {
        editUrl += `&spaceId=${this.item.spaceId}`;
      }
      if (this.item.activityId) {
        editUrl += `&activityId=${this.item.activityId}`;
      }
      if (this.item.spaceUrl) {
        editUrl += `&spaceName=${this.item.spaceUrl.substring(this.item.spaceUrl.lastIndexOf('/') + 1)}`;
      }
      editUrl += `&type=${this.item.activityId && 'latest_draft' || 'draft'}`;
      if (this.item.lang) {
        editUrl += `&lang=${this.item.lang}`;
      }
      return editUrl;
    },
    getNoteEditUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?noteId=${this.item.id}`
        + `&spaceGroupId=${this.item.spaceGroupId || ''}`
        + `&isDraft=${this.item.draft ? 'true' : 'false'}`
        + `&parentNoteId=${this.item.parentId || ''}`
        + `&notePageUri=${encodeURIComponent(eXo.env.portal.selectedNodeUri || '')}`;
    },
    onItemLinkClick(event) {
      // A draft note has no standalone view page - the only meaningful
      // action is to open it back in its editor.
      if (!this.item.url) {
        event.preventDefault();
        this.editItem();
      }
    },
    async openMoreCategoriesDrawer() {
      this.moreCategoriesDrawer = true;
      await this.$nextTick();
      this.$refs.moreCategoriesDrawer?.open?.(this.categories);
    },
    selectCategory(category) {
      this.$emit('select-category', category?.id);
    },
  },
};
</script>
