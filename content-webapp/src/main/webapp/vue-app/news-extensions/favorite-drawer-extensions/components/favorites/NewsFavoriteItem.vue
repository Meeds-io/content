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
  <v-list-item
    :href="url"
    @keydown.enter="setAsViewed"
    @auxclick="setAsViewed"
    @click="setAsViewed">
    <v-list-item-icon class="me-3 my-auto">
      <v-card
        :min-width="iconWidth"
        class="d-flex justify-center no-border-radius"
        color="transparent"
        flat>
        <v-icon :size="iconSize">fa-newspaper</v-icon>
      </v-card>
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title class="text-color">{{ title }}</v-list-item-title>
      <v-list-item-subtitle v-if="expanded" class="d-flex align-center full-width overflow-hidden pt-2px">
        <template v-if="spaceId">
          <favorite-space-avatar
            :space-id="spaceId"
            :size="16"
            class="flex-grow-0 flex-shrink-1 text-truncate"
            link-style />
          <v-icon class="flex-grow-0 flex-shrink-0 mx-2" size="2">fa-circle</v-icon>
        </template>
        <date-format class="flex-grow-0 flex-shrink-0" :value="date" />
        <template v-if="updater">
          <v-icon class="flex-grow-0 flex-shrink-0 mx-2" size="2">fa-circle</v-icon>
          <favorite-user-avatar
            :profile-id="updater"
            :size="16"
            class="flex-grow-1 flex-shrink-1 text-truncate" />
        </template>
      </v-list-item-subtitle>
    </v-list-item-content>
    <v-list-item-action>
      <favorite-button
        :id="id"
        :favorite="isFavorite"
        :top="top"
        :right="right"
        type="news"
        type-label="News"
        @removed="removed"
        @remove-error="removeError" />
    </v-list-item-action>
  </v-list-item>
</template>
<script>
export default {
  props: {
    id: {
      type: String,
      default: () => null,
    },
    clickCallback: {
      type: Function,
      default: null,
    },
    expanded: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    news: null,
    isFavorite: true,
    newsObjectType: 'article',
  }),
  computed: {
    iconWidth() {
      return this.expanded ? 40 : 30;
    },
    iconSize() {
      return this.expanded ? 34 : 24;
    },
    title() {
      return this.news?.title ? this.$utils.htmlToText(this.news.title) : '';
    },
    url() {
      return this.news?.url;
    },
    spaceId() {
      return this.news?.spaceId;
    },
    updater() {
      return this.news ? this.news?.updater || this.news?.author : null;
    },
    date() {
      return this.news?.updateDate || this.news?.publicationDate || this.news?.creationDate;
    },
  },
  created() {
    let newsId = this.id;
    let lang = null;
    if (this.id.includes('-')) {
      const parts = this.id.split('-');
      newsId = parts[0];
      lang = parts[1];
    }
    this.$newsServices.getNewsById(newsId, false, this.newsObjectType, lang)
      .then(news => this.news = news);
  },
  methods: {
    removed() {
      this.isFavorite = !this.isFavorite;
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite', {0: this.$t('news.label')}));
      this.$emit('removed');
      this.$root.$emit('refresh-favorite-list');
    },
    removeError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', {0: this.$t('news.label')}), 'error');
    },
    displayAlert(message, type) {
      this.$root.$emit('alert-message', message, type || 'success');
    },
    setAsViewed(event) {
      if (event.which === 1 || event.which === 2) {
        this.clickCallback('news', this.id);
      }
    },
  }
};
</script>
