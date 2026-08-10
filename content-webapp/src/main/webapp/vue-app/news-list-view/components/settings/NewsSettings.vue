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
  <v-sheet 
    v-if="showSettingsContainer"
    height="30"
    class="background-transparent d-flex flex-row px-2">
    <div class="d-flex latestNewsTitleContainer flex-column flex-grow-1 min-width-0 my-1">
      <span
        v-if="showHeader"
        class="text-header"
        :title="headerTitle">
        <span class="text-truncate">{{ headerTitle }}</span>
      </span>
    </div>
    <div v-if="showSeeAll && !hideSeeAllButton" class="d-flex flex-column my-auto me-2">
      <v-btn
        v-if="$root.canManageNewsList && isHovering"
        class="primary--text my-auto"
        icon
        small
        @click="seeAllNews">
        <v-icon
          size="18"
          icon>
          fas fa-external-link-alt
        </v-icon>
      </v-btn>
      <v-btn
        v-else-if="!isHovering || !$root.canManageNewsList"
        color="primary"
        class="pa-0 text-font-size"
        small
        text
        link
        @click="seeAllNews">
        <span class="primary--text text-none">
          {{ $t('news.published.seeAll') }}
        </span>
      </v-btn>
    </div>
    <div
      :class="[showHeader && headerTitle ? 'd-flex flex-column' : 'd-flex flex-column']"
      class="my-auto">
      <v-btn
        v-if="$root.canManageNewsList && showSettingsIcon"
        :class="classButtonOpenSettings"
        class="icon-default-color white-background"
        :title="$t('news.list.openSettings.title')"
        :aria-label="$t('news.latest.openSettings')"
        :width="settingButtonSize"
        :height="settingButtonSize"
        :min-width="settingButtonSize"
        :small="!settingButtonSize"
        icon
        @click="openDrawer">
        <v-icon
          size="18"
          icon>
          fas fa-cog
        </v-icon>
      </v-btn>
    </div>
  </v-sheet>
</template>
<script>
export default {
  props: {
    settingButtonSize: {
      type: Number,
      default: null
    },
    isHovering: {
      type: Boolean,
      required: false,
      default: true
    },
    hideOpenSettingButton: {
      type: Boolean,
      required: false,
      default: false
    },
    hideSeeAllButton: {
      type: Boolean,
      required: false,
      default: false
    },
    classButtonOpenSettings: {
      type: String,
      default: 'button-open-settings'
    }
  },
  data: () => ({
    seeAllUrl: '',
    showHeader: false,
    showSeeAll: false,
    language: eXo?.env?.portal?.language,
  }),
  computed: {
    headerTitle() {
      return this.$root.headerTitle || '';
    },
    showSettingsContainer(){
      return this.showHeader || this.showSeeAll || this.$root.canManageNewsList ;
    },
    showSettingsIcon() {
      return this.isHovering && !this.hideOpenSettingButton;
    }
  },
  created() {
    this.$root.$on('saved-news-settings', () => {
      this.seeAllUrl = this.$root.seeAllUrl;
      this.showSeeAll = this.$root.showSeeAll;
      this.showHeader = this.$root.showHeader;
    });
    this.seeAllUrl = this.$root.seeAllUrl;
    this.showSeeAll = this.$root.showSeeAll;
    this.showHeader = this.$root.showHeader;
  },
  methods: {
    openDrawer() {
      this.$root.$emit('news-settings-drawer-open');
    },
    seeAllNews() {
      const target = this.seeAllUrl.startsWith(`${eXo.env.portal.context}/`) ? '_self' : '_blank';
      window.open(this.seeAllUrl, target);
    }
  },
};
</script>
