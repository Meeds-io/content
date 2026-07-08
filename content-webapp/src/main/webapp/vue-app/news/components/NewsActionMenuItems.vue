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
  <v-list class="pa-0 text-no-wrap width-fit-content newsActionMenuItems">
    <v-list-item
      v-if="showCopyLinkButton"
      class="ps-2 pe-4 action-menu-item d-flex align-center"
      @click="$emit('copy-link')">
      <v-icon
        size="16"
        class="clickable icon-menu">
        fas fa-link
      </v-icon>
      <span class="pt-1 text-color">
        {{ $t('news.details.header.menu.copy.link') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="showCategoriesButton"
      class="ps-2 pe-4 action-menu-item d-flex align-center"
      @click="$emit('manage-categories', news)">
      <v-icon
        size="16"
        class="clickable icon-menu">
        fa-th-large
      </v-icon>
      <span class="pt-1 text-color">
        {{ $t('news.details.header.menu.manageCategories') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="showEditButton"
      class="ps-2 pe-4 action-menu-item d-flex align-center"
      @click="$emit('edit-article', news)">
      <v-icon
        size="16"
        class="clickable icon-menu">
        fas fa-edit
      </v-icon>
      <span class="pt-1 text-color">
        {{ $t('news.details.header.menu.edit') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="showShareButton && news.activityId"
      class="ps-2 pe-4 action-menu-item d-flex align-center"
      @click="$root.$emit('activity-share-drawer-open', news.activityId, currentApp)">
      <v-icon
        size="16"
        class="clickable icon-menu">
        fa fa-share
      </v-icon>
      <span class="pt-1 text-color">
        {{ $t('news.details.header.menu.share') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="showResumeButton"
      class="ps-2 pe-4 action-menu-item d-flex align-center"
      @click="$emit('edit-article', news)">
      <v-icon
        size="16"
        class="clickable icon-menu">
        fas fa-edit
      </v-icon>
      <span class="pt-1 text-color">
        {{ $t('news.details.header.menu.resume') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="showPublishButton"
      class="ps-2 pe-4 action-menu-item"
      @click="openPublicationDrawer">
      <v-icon
        size="16"
        class="clickable icon-menu">
        fa-solid fa-paper-plane
      </v-icon>
      <span class="pt-1 text-color">
        {{ $t('news.details.header.menu.publish') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="showReferButton"
      class="ps-2 pe-4 action-menu-item"
      @click="$root.$emit('refer-article-to-note')">
      <v-icon
        size="16"
        class="clickable icon-menu">
        {{ !news.referred && 'fas fa-sitemap' || 'fas fa-unlink' }}
      </v-icon>
      <span class="pt-1 text-color">
        {{ !news.referred && $t('content.article.refer.to.note')
          || $t('content.article.deRefer.label') }}
      </span>
    </v-list-item>
    <template v-if="menuExtensions?.length">
      <v-list-item
        v-for="extension in menuExtensions"
        :key="extension.id"
        :class="extension.class"
        class="ps-2 pe-4 action-menu-item d-flex align-center"
        @click="extension.click(news)">
        <v-icon
          :class="extension.iconClass"
          class="clickable icon-menu"
          size="16">
          {{ extension.icon }}
        </v-icon>
        <span
          :class="extension.labelClass"
          class="pt-1">
          {{ $t(extension.labelKey) }}
        </span>
      </v-list-item>
    </template>
    <v-list-item
      v-if="showDeleteButton"
      class="ps-2 pe-4 action-menu-item d-flex align-center deleteArticleOption"
      @click="$emit('delete-article')">
      <v-icon
        size="16"
        class="clickable icon-menu deleteArticleIcon">
        fas fa-trash
      </v-icon>
      <span class="pt-1 deleteArticleText">
        {{ $t('news.details.header.menu.delete') }}
      </span>
    </v-list-item>
  </v-list>
</template>
<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: null
    },
    showShareButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showEditButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showResumeButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showDeleteButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showPublishButton: {
      type: Boolean,
      required: false,
      default: false
    },
    currentApp: {
      type: String,
      required: false,
      default: null
    },
    showCopyLinkButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showReferButton: {
      type: Boolean,
      default: false
    },
    showCategoriesButton: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data: () => ({
    menuExtensions: null,
    extensionApp: 'news-detail',
    extensionType: 'other-actions',
  }),
  created() {
    document.addEventListener(`extension-${this.extensionApp}-${this.extensionType}-updated`, this.refreshExtensions);
    Vue.prototype.$utils.includeExtensions('NewsDetailExtension');
    this.refreshExtensions();
  },
  beforeDestroy() {
    document.addEventListener(`extension-${this.extensionApp}-${this.extensionType}-updated`, this.refreshExtensions);
  },
  methods: {
    openPublicationDrawer() {
      this.$root.$emit('open-edit-publishing-drawer');
    },
    refreshExtensions() {
      this.menuExtensions = extensionRegistry.loadExtensions(this.extensionApp, this.extensionType);
    },
  },
};
</script>
