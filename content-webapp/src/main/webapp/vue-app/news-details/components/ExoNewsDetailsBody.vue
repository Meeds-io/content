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
  <div v-if="news">
    <div
      class="newsDetails-description">
      <div
        class="newsDetails-header">
        <v-img
          v-if="illustrationURL"
          :lazy-src="`${illustrationURL}&size=0x400`"
          :alt="featuredImageAltText"
          :src="`${illustrationURL}&size=0x400`"
          contain
          class="mt-5"
          width="100%"
          max-height="400" />
      </div>
      <div class="newsDetails">
        <div class="news-top-information d-flex">
          <p class="text-color font-weight-bold articleTitle text-break mt-5 mb-0">
            <span>
              {{ newsTitle }}
            </span>
            <v-tooltip bottom v-if="newsViews">
              <template #activator="{ on, attrs }">
                <span v-on="on" v-bind="attrs">
                  <v-icon 
                    size="20"
                    class="ms-3 article-views-icon">
                    fas fa-eye
                  </v-icon>
                  <span class="article-views text-subtitle">
                    {{ newsViews }}
                  </span>
                </span>
              </template>
              <span class="caption">
                {{ newsViewsCount }}
              </span>
            </v-tooltip>
            <span>
              <content-translation-menu
                :translations="translations"
                :selected-translation="selectedTranslation"
                :article="news" />
            </span>
            <extension-registry-components
              name="NewsDetails"
              type="content-details-extension"
              :params="{
                entityId: news.latestVersionId,
                editMode: false,
                entityType: 'WIKI_PAGE_VERSIONS'
              }"
              element-class="ms-3"
              parent-element="span"
              element="span" />
          </p>
        </div>
        <p
          v-if="newsSummary"
          class="article-summary text-break text-sub-title mt-4 mb-0">
          {{ newsSummary }}
        </p>
        <div class="mt-4">
          <div
            v-if="!hiddenSpace && !isPublicAccess"
            class="d-flex">
            <exo-space-avatar
              :space-id="spaceId"
              size="30"
              link-style
              avatar />
            <exo-space-avatar
              :space-id="spaceId"
              size="30"
              extra-class="ms-4 fill-height text-truncate"
              fullname
              popover />
          </div>
          <div>
            <div
              v-if="showUpdaterInfo"
              class="d-flex">
              <exo-user-avatar
                :profile-id="articleUpdater"
                :size="25"
                :class="{'ms-4 mt-n3': !hiddenSpace}"
                avatar />
              <div
                :class="{'mt-n2': !hiddenSpace}"
                class="text-sub-title align-center d-flex">
                <exo-user-avatar
                  :profile-id="articleUpdater"
                  extra-class="ms-2"
                  fullname
                  small-font-size />
                <span class="px-1">-</span>
                <date-format
                  :value="updatedDate"
                  :format="dateFormat"
                  class="text-caption" />
              </div>
            </div>
            <div
              v-else
              :class="{'no-updater-info': !hiddenSpace && !isPublicAccess}"
              class="text-sub-title">
              <date-format
                :value="updatedDate"
                :format="dateFormat"
                class="text-caption" />
            </div>
          </div>
        </div>
        <div
          class="mt-8 rich-editor-content extended-rich-content"
          v-sanitized-html="newsBody">
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: null
    },
    currentUser: {
      type: String,
      required: false,
      default: null
    },
    translations: {
      type: Array,
      default: () => {
        return [];
      }
    },
    selectedTranslation: {
      type: Object,
      default: () => {
        return {};
      }
    }
  },
  data: () => ({
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
    dateTimeFormat: {
      hour: '2-digit',
      minute: '2-digit',
    },
    newsTitleContent: null,
    newsSummaryContent: null,
    newsBodyContent: null,
  }),
  created() {
    this.setNewsTitle(this.news?.title);
    this.setNewsSummary(this.news?.properties?.summary);
    this.setNewsContent(this.news?.body);
    this.$root.$on('update-news-title', this.setNewsTitle);
    this.$root.$on('update-news-summary', this.setNewsSummary);
    this.$root.$on('update-news-body', this.setNewsContent);
  },
  computed: {
    showUpdaterInfo() {
      return !this.isPublicAccess;
    },
    isPublicAccess() {
      return !eXo?.env?.portal?.userIdentityId;
    },
    params() {
      return {
        news: this.news,
      };
    },
    illustrationURL() {
      return this?.news.illustrationURL;
    },
    featuredImageAltText() {
      return this.news?.properties?.featuredImage?.altText || this.newsTitle;
    },
    newsTitle() {
      return this.news && this.newsTitleContent;
    },
    newsViewsCount() {
      return `${this.news?.viewsCount} ${this.$t('news.details.views')}`;
    },
    newsViews() {
      if (this.news?.viewsCount < 1000) {
        return this.news?.viewsCount;
      }
      if (this.news?.viewsCount < 10000) {
        return `${(this.news?.viewsCount / 1000).toFixed(1)}k`;
      }
      if (this.news?.viewsCount < 1000000) {
        return `${parseInt(this.news?.viewsCount / 1000)}k`;
      }
      return '+999k';
    },
    articleUpdater() {
      return this.news?.updater || this.news?.author;
    },
    hiddenSpace() {
      return this.news && this.news.hiddenSpace;
    },
    newsBody() {
      return this.news && this.newsBodyContent;
    },
    updaterFullName() {
      return this.news && this.news.updaterFullName;
    },
    publicationDate() {
      return this.news?.publicationDate && new Date(this.news.publicationDate);
    },
    updatedDate() {
      return this.news?.updateDate && new Date(this.news.updateDate);
    },
    newsSummary() {
      return this.news && this.newsSummaryContent;
    },
    spaceId() {
      return this.news && this.news.spaceId;
    },
    postedDate() {
      return this.news && this.news.postedDate;
    },
    summary() {
      return this.news?.properties?.summary;
    },
    attachmentsIds() {
      return this.news?.attachmentsIds;
    },
    publicationState() {
      return this.news && this.news.publicationState;
    }
  },
  methods: {
    setNewsTitle(title) {
      this.newsTitleContent = title;
    },
    setNewsSummary(content) {
      this.newsSummaryContent = content;
    },
    setNewsContent(translation) {
      this.newsBodyContent = translation;
    },
  }
};
</script>
