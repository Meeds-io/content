<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <v-hover v-slot="{ hover }">
    <v-card
      flat
      class="pa-0"
      :aria-label="$t('search.access.to.result', {0 :newsTitleText})"
      :href="newsUrl">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="ms-n1 me-2">
            <v-icon size="32" class="icon-default-color mt-2">fas fa-newspaper</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title class="d-flex flex-row full-width align-center">
              <h1
                class="flex-grow-1 primary--text title pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                v-sanitized-html="newsTitle">
              </h1>
              <span v-show="hover || isMobile" class="ml-2">
                <news-favorite-action
                  :news="result"
                  @removed="$emit('refresh-favorite')" />
              </span>
            </v-list-item-title>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row mx-auto full-width">
                <a
                  v-bind="attrs"
                  v-on="on"
                  :href="spaceUrl"
                  class="flex-nowrap flex-shrink-0 d-flex spaceAvatar">
                  <v-avatar
                    :size="18"
                    tile
                    class="my-auto">
                    <img
                      :src="spaceAvatar"
                      alt=""
                      class="object-fit-cover ma-auto"
                      loading="lazy">
                  </v-avatar>
                  <p class="ms-2 my-auto text-subtitle">{{ spaceDisplayName }}</p>
                </a>
                <v-icon size="3" class="icon-default-color mx-3">fas fa-circle</v-icon>
                <exo-user-avatar
                  :profile-id="updaterUsername"
                  :size="18"
                  small-font-size
                  :avatar="isMobile"
                  :popover="false" />
                <v-icon
                  v-if="newsUpdateDate"
                  size="3"
                  class="icon-default-color mx-3">fas fa-circle</v-icon>
                <v-icon
                  v-if="newsUpdateDate"
                  size="12"
                  class="icon-default-color">fas fa-clock</v-icon>
                <date-format class="ms-1 my-auto" :value="newsUpdateDate" />
              </span>
              <div
                class="pt-2 text-wrap text-body-2 text-color text-break"
                :class="{
                  'text-truncate-2': isMobile,
                  'text-truncate-3': !isMobile,
                }"
                v-sanitized-html-no-embed="summary"></div>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-card>
  </v-hover>
</template>

<script>
export default {
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  computed: {
    newsUrl() {
      return this.result?.newsUrl;
    },
    excerpts() {
      return this.result?.excerpts;
    },
    excerptHtml() {
      return this.excerpts?.join('\r\n...');
    },
    newsTitle() {
      return this.result && this.result.title || '';
    },
    newsTitleText() {
      return $('<div />').html(this.newsTitle).text();
    },
    updaterUsername() {
      return this.result?.updaterUserName;
    },
    spaceId() {
      return this.result?.spaceId;
    },
    spaceDisplayName() {
      return this.result?.spaceDisplayName;
    },
    spaceAvatar() {
      return this.result?.spaceAvatar;
    },
    summary() {
      return this.result?.summary || this.excerptHtml || this.result.body || '';
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    },
    newsUpdateDate() {
      return this.result?.lastUpdatedTime;
    },
    spaceUrl() {
      if (this.spaceId) {
        return '#';
      }
      return `${eXo.env.portal.context}/s/${this.spaceId}`;
    }
  },
};
</script>
