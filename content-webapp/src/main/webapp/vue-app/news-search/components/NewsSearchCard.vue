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
      @click="openNews">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="ms-n1 me-2">
            <v-icon size="32" class="icon-default-color mt-2">fas fa-newspaper</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title class="d-flex flex-row full-width align-center" :title="newsTitle">
              <p
                :title="newsTitleText"
                class="flex-grow-1 title font-weight-bold pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                v-sanitized-html="newsTitle"></p>
              <span v-show="hover || isMobile" class="ml-2">
                <news-favorite-action
                  :news="result"
                  @removed="$emit('refresh-favorite')" />
              </span>
            </v-list-item-title>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row mx-auto full-width">
                <exo-space-avatar
                  :space-id="spaceId"
                  size="18"
                  text-truncate-class="text-truncate text-sub-title"
                  small-font-size
                  subtitle-new-line-class
                  :avatar="isMobile"
                  popover />
                <v-icon size="3" class="icon-default-color mx-3">fas fa-circle</v-icon>
                <exo-user-avatar
                  :profile-id="updaterUsername"
                  :size="18"
                  small-font-size
                  :avatar="isMobile"
                  :popover="!isMobile" />
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
                class="pt-2 text-wrap text-body text-break"
                :title="summaryText"
                :class="{
                  'text-truncate-2': isMobile,
                  'text-truncate-3': !isMobile,
                  'mt-n3': !hasSummary
                }"
                v-sanitized-html="summary"></div>
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
    summaryText() {
      return this.excerpt && $('<div />').html(this.excerpt).text() || $('<div />').html(this.summary).text();
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
    summary() {
      return this.result?.summary || this.excerptHtml || this.result.body;
    },
    hasSummary() {
      return this.result?.summary || this.excerptHtml;
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    },
    newsUpdateDate() {
      return this.result?.lastUpdatedTime;
    }
  },
  methods: {
    openNews() {
      if (this.newsUrl) {
        window.location.href = this.newsUrl;
      }
    },
  }
};
</script>
