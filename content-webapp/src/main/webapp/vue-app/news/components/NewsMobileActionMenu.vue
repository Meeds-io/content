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
    id="newsActionMenuBottomDrawer"
    ref="newsMobileActionMenu"
    :bottom="true">
    <template slot="content">
      <news-action-menu-items
        :news="news"
        :show-copy-link-button="showCopyLinkButton"
        :show-delete-button="showDeleteButton"
        :show-edit-button="showEditButton"
        :current-app="currentApp"
        :show-publish-button="showPublishButton"
        :show-resume-button="showResumeButton"
        :show-share-button="showShareButton"
        @copy-link="$emit('copy-link')"
        @edit-article="$emit('edit-article', news)"
        @delete-article="$emit('delete-article')" />
    </template>
  </exo-drawer>
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
    }
  },
  created() {
    this.$root.$on('open-news-mobile-action-menu', this.open);
    this.$root.$on('close-news-mobile-action-menu',this.close);
  },
  methods: {
    open() {
      this.$refs.newsMobileActionMenu.open();
    },
    close() {
      this.$refs.newsMobileActionMenu.close();
    }
  }
};
</script>