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
  <div id="newsAppItem">
    <a
      :href="news.url"
      :style="{ 'background-image': 'url(' + illustrationUrl + ')' }"
      class="newsSmallIllustration"
      :target="news.target"
      :aria-label="$t('news.illustration.link.title', {0: news.title})"></a>
    <div class="newsItemContent">
      <div class="newsItemContentHeader">
        <h3>
          <a :href="news.url" :target="news.target">{{ news.title }} </a>
        </h3>
        <news-spaces-shared-in
          v-if="news.activities && news.activities.split(';')[1]"
          :news-id="news.newsId"
          :activities="news.activities" />
        <exo-news-details-action-menu-app
          v-if="!news.schedulePostDate"
          :news="news"
          :show-edit-button="news.canEdit && !isDraftsFilter"
          :show-delete-button="news.canDelete"
          :show-share-button="showShareButton && !isDraftsFilter"
          :show-resume-button="news.draft && isDraftsFilter"
          :current-app="currentApplication"
          @delete-article="deleteConfirmDialog"
          @edit-article="editLink(news)" />
      </div>
      <div class="newsInfo d-flex pb-1">
        <div class="newsOwner d-flex align-center pe-4">
          <exo-user-avatar
            :profile-id="newsAuthor"
            :size="25"
            class="align-center width-full my-auto text-truncate flex-grow-0 flex"
            small-font-size
            popover />
          <v-icon
            v-if="!news.hiddenSpace"
            size="15"
            class="text-color">
            fas fa-chevron-right
          </v-icon>
          <exo-space-avatar
            v-if="!news.hiddenSpace"
            :space-id="spaceId"
            class="width-full text-truncate"
            :size="25"
            extra-class="ps-1"
            small-font-size
            popover />
        </div>
        <div class="newsDate pe-4" v-if="news.activityId">
          <i v-if="displayClock" class="uiIconClock"></i>
          <span v-if="news && news.schedulePostDate">
            <date-format
              :value="news.schedulePostDate"
              :format="dateFormat"
              class="newsTime caption" />
            -
            <date-format
              :value="news.schedulePostDate"
              :format="dateTimeFormat"
              class="newsTime caption" />
          </span>
          <span v-else>
            <date-format
              :value="news.updatedDate"
              :format="dateFormat"
              class="newsTime caption" />
          </span>
        </div>
        <div class="newsViews " v-if="!news.scheduled && news.activityId">
          <i class="uiIconWatch"></i>
          <span class="viewsCount">{{ news.viewsCount }}  {{ $t('news.app.views') }}</span>
        </div>
      </div>
      <div class="newsItemContentDetails">
        <a :href="news.url" :target="news.target">
          <p class="newsSummary" v-sanitized-html="news.newsText"></p>
        </a>
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
    newsFilter: {
      type: String,
      required: false,
      default: null
    },
    newsList: {
      type: Array,
      required: false,
      default: null
    },
  },
  data: () => ({
    showShareButton: true,
    currentApplication: 'newsApp',
    dateTimeFormat: {
      hour: '2-digit',
      minute: '2-digit',
    },
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
  }),
  computed: {
    isDraftsFilter() {
      return this.newsFilter === 'drafts';
    },
    displayClock() {
      return this.news && (this.news.schedulePostDate || this.news.updatedDate);
    },
    spaceId() {
      return this.news && this.news.spaceId;
    },
    newsAuthor() {
      return this.news && this.news.authorProfileURL && this.news.authorProfileURL.split('/').pop();
    },
    illustrationUrl() {
      return this.news?.illustrationURL ? this.news.illustrationURL.concat('&size=150x150').toString() : '/content/images/news.png';
    },
  },
  methods: {
    getEditUrl(news) {
      return this.$parent.$parent.getEditUrl(news);
    },
    editLink(news) {
      const editUrl = this.getEditUrl(news);
      window.open(editUrl, '_blank');
      this.$refs?.mobileActionMenu?.close();
    },
    deleteConfirmDialog() {
      this.$emit('open-delete-confirm-dialog', this.news);
    },
  }
};
</script>
