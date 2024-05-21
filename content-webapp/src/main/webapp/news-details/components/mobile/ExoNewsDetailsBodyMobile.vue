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
  <div class="newsContent">
    <div :title="newsTitle" class="newsDetailsTitle mt-2 pl-2 pr-2 ms-2 font-weight-bold subtitle-1">
      {{ newsTitle }}
    </div>
    <div v-if="currentUser" class="d-flex flex-row pa-2 ms-2">
      <div v-if="news" class="flex-column newsAuthor">
        <exo-user-avatar
          :profile-id="newsAuthor"
          :size="32"
          class="align-center my-auto text-truncate flex-grow-0 flex"
          bold-title
          link-style />
      </div>
      <v-icon>
        mdi-chevron-right
      </v-icon>
      <div class="flex-column">
      </div>
      <v-list-item-title v-if="space" class="font-weight-bold body-2 mb-0">
        <exo-space-avatar
          :space="space"
          :size="32"
          class="align-center my-auto text-truncate flex-grow-0 flex"
          bold-title
          link-style />
      </v-list-item-title>
    </div>
    <div class="d-flex flex-row caption text-light-color">
      <exo-news-details-time :news="news" />
    </div>
    <div class="d-flex flex-row ms-2 me-2 caption font-italic grey--text text-darken-1 pa-4">
      <span v-sanitized-html="newsSummary"></span>
    </div>
    <v-divider class="mx-4" />
    <div class="d-flex flex-column pa-4 ms-2 me-2 newsBody">
      <div
        class="rich-editor-content extended-rich-content"
        v-html="newsBody"></div>
    </div>
    <extension-registry-components
      :params="{attachmentsIds: attachmentsIds}"
      name="NewsDetails"
      type="news-details-attachments"
      element="div" />
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
    newsId: {
      type: String,
      required: false,
      default: null
    },
    space: {
      type: Object,
      required: false,
      default: null
    },
    currentUser: {
      type: String,
      required: false,
      default: null
    },
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
  }),
  computed: {
    newsTitle() {
      return this.news && this.news.title;
    },
    spaceUrl() {
      return this.news && this.news.spaceAvatarUrl;
    },
    publicationState() {
      return this.news && this.news.publicationState;
    },
    newsSummary() {
      return this.news?.summary;
    },
    newsBody() {
      return this.news?.body;
    },
    newsAuthor() {
      return this.news && this.news.author;
    },
    attachmentsIds() {
      return this.news?.attachmentsIds;
    },
  }
};
</script>
