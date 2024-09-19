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
  <v-app-bar
    absolute
    flat
    :src="illustrationUrl"
    :alt="featuredImageAltText"
    prominent
    class="news-details-toolbar application-border-radius-top">
    <v-app-bar-nav-icon>
      <v-btn
        class="newsDetailsMenuBtn my-2"
        fab
        dark
        x-small
        :href="backURL">
        <v-btn icon>
          <v-icon>mdi-arrow-left</v-icon>
        </v-btn>
      </v-btn>
    </v-app-bar-nav-icon>
    <v-spacer />
    <v-btn
      v-if="publicationState !== 'staged'"
      class="newsDetailsMenuBtn my-2"
      fab
      dark
      x-small>
      <exo-news-details-action-menu-app
        v-if="publicationState !== 'staged'"
        :news="news"
        :show-edit-button="showEditButton"
        :show-delete-button="showDeleteButton"
        :show-publish-button="showPublishButton"
        :show-copy-link-button="showCopyLinkButton"
        :news-published="newsPublished"
        @edit-article="$emit('edit-article', news)"
        @delete-article="$emit('delete-article')" />
    </v-btn>
    <v-btn
      v-if="publicationState === 'staged'"
      class="btn newsDetailsActionMenu my-2 pull-right"
      @click="$root.$emit('open-schedule-drawer','editScheduledNews')">
      {{ $t("news.composer.btn.scheduleArticle") }}
    </v-btn>
  </v-app-bar>
</template>
<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: null
    },
    showEditButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showPublishButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showDeleteButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showCopyLinkButton: {
      type: Boolean,
      required: false,
      default: false
    },
  },
  computed: {
    backURL() {
      return this.news && this.news.spaceMember ? this.news.spaceUrl : `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}`;
    },
    featuredImageAltText() {
      return this.news?.properties?.featuredImage?.altText;
    },
    illustrationUrl() {
      return this?.news.illustrationURL ? this.news.illustrationURL.concat('&size=315x128').toString() : '/content/images/news.png';
    },
    publicationState() {
      return this.news?.publicationState;
    },
    newsPublished() {
      return this.news?.published;
    }
  },
};
</script>
