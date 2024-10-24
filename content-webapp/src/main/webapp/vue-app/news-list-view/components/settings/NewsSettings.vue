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
  <div v-if="showSettingsContainer" class="settings-container d-flex flex-row px-2 pt-2 pb-1">
    <div class="d-flex latestNewsTitleContainer flex-column flex-grow-1 my-1 text-truncate">
      <span
        v-if="showHeader"
        class="news-text-header text-truncate"
        :title="headerTitle">{{ headerTitle }}</span>
    </div>
    <div :class="[showHeader && headerTitle ? 'd-flex flex-column me-2 mt-1' : 'd-flex flex-column me-2']">
      <v-icon
        v-if="canPublishNews && showSettingsIcon"
        :class="classButtonOpenSettings"
        :aria-label="$t('news.latest.openSettings')"
        size="24"
        icon
        @click="openDrawer">
        mdi-cog
      </v-icon>
    </div>
    <div v-if="showSeeAll && !hideSeeAllButton" class="d-flex flex-column my-auto me-2">
      <v-btn
        depressed
        small
        class="button-see-all-news caption text-uppercase text-light-color my-auto me-2"
        @click="seeAllNews">
        {{ $t('news.published.seeAll') }}
      </v-btn>
    </div>
  </div>
</template>
<script>
export default {
  props: {
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
    canPublishNews: false,
    language: eXo?.env?.portal?.language,
  }),
  computed: {
    headerTitle() {
      return this.$root.headerTitle || '';
    },
    showSettingsContainer(){
      return this.showHeader || this.showSeeAll || this.canPublishNews ;
    },
    showSettingsIcon() {
      return this.isHovering && !this.hideOpenSettingButton;
    }
  },
  created() {
    this.$newsServices.canPublishNews().then(canPublishNews => {
      this.canPublishNews = canPublishNews;
    });
    this.$root.$on('saved-news-settings', (newsTarget, selectedOptions) => {
      this.seeAllUrl = selectedOptions.seeAllUrl;
      this.showSeeAll = selectedOptions.showSeeAll;
      this.showHeader = selectedOptions.showHeader;
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
