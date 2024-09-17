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
  <v-menu
    v-model="actionMenu"
    attach
    eager
    bottom
    left
    offset-y
    min-width="108px"
    class="px-0 mx-2 pa-0 py-0 overflow-hidden">
    <template #activator="{ on, attrs }">
      <v-btn
        v-bind="attrs"
        :aria-label="$t('news.details.menu.open')"
        class="newsDetailsActionMenu pull-right"
        icon
        v-on="on">
        <v-icon>mdi-dots-vertical</v-icon>
      </v-btn>
    </template>

    <v-list class="pa-0">
      <v-list-item class="px-4 action-menu-item d-flex align-center" v-if="showEditButton" @click="$emit('edit-article', news)">
        <v-icon
          size="16">
          fas fa-edit
        </v-icon>
        <span class="ps-2 pt-1">
          {{ $t('news.details.header.menu.edit') }}
        </span>
      </v-list-item>
      <v-list-item  class="px-4 action-menu-item d-flex align-center" v-if="showShareButton && news.activityId" @click="$root.$emit('activity-share-drawer-open', news.activityId, currentApp)">
        <v-icon
          size="16">
          fa fa-share
        </v-icon>
        <span class="ps-2 pt-1">
          {{ $t('news.details.header.menu.share') }}
        </span>
      </v-list-item>
      <v-list-item class="px-4 action-menu-item d-flex align-center" v-if="showResumeButton" @click="$emit('edit-article', news)">
        <v-icon
          size="16">
          fas fa-edit
        </v-icon>
        <span class="ps-2 pt-1">
          {{ $t('news.details.header.menu.resume') }}
        </span>
      </v-list-item>
      <v-list-item class="px-4 action-menu-item" v-if="showPublishButton" @click="$root.$emit('open-edit-publishing-drawer')">
        <v-icon
          size="16">
          fa-solid fa-paper-plane
        </v-icon>
        <span class="ps-2 pt-1">
          {{ $t('news.details.header.menu.publish') }}
        </span>
      </v-list-item>
      <v-list-item class="px-4 action-menu-item d-flex align-center deleteArticleOption" v-if="showDeleteButton" @click="$emit('delete-article')">
        <v-icon
          size="16"
          class="clickable icon-menu deleteArticleIcon ">
          fas fa-trash
        </v-icon>
        <span class="ps-2 pt-1 deleteArticleText">
          {{ $t('news.details.header.menu.delete') }}
        </span>
      </v-list-item>
    </v-list>
  </v-menu>
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
  },
  data: () => ({
    actionMenu: null,
  }),
  created() {
    $(document).mousedown(() => {
      if (this.actionMenu) {
        window.setTimeout(() => {
          this.actionMenu = false;
        }, 200);
      }
    });
  },
  mounted() {
    $('#UIPortalApplication').parent().click(() => {
      this.actionMenu = false;
    });
  },
};
</script>