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
  <div
    :class="{'me-5': !showEditButton}"
    class="newsDetailsTopBar mb-5">
    <a
      v-if="!articleNewLayoutEnabled"
      class="backBtn"
      @click="goBack">
      <i class="uiIconBack my-4"></i>
    </a>
    <v-btn
      v-else
      class="go-back-button"
      icon
      @click.stop="goBack">
      <v-icon
        size="15">
        fas fa-arrow-left
      </v-icon>
    </v-btn>
    <v-btn
      v-if="publicationState === 'staged'"
      class="btn pull-right"
      :class="{'newsDetailsActionMenu mt-6 mr-2 ': !articleNewLayoutEnabled}"
      @click="$root.$emit('open-schedule-drawer','editScheduledNews')">
      {{ $t("news.composer.btn.scheduleArticle") }}
    </v-btn>
    <exo-news-details-action-menu-app
      v-if="publicationState !== 'staged' && (showEditButton || showDeleteButton || showPublishButton || showCopyLinkButton)"
      class="pull-right"
      :news="news"
      :current-app="currentApplication"
      :show-edit-button="showEditButton"
      :show-delete-button="showDeleteButton"
      :show-publish-button="showPublishButton"
      :show-copy-link-button="showCopyLinkButton"
      @delete-article="$emit('delete-article')"
      @edit-article="$emit('edit-article')" />
    <exo-news-favorite-action
      v-if="displayFavoriteButton"
      :news="news"
      :activity-id="activityId"
      :class="[{'pull-right mt-1 me-2': articleNewLayoutEnabled},
               {'mt-6 pull-right': !articleNewLayoutEnabled}]" />
  </div>
</template>

<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: function() { return new Object(); }
    },
    newsId: {
      type: String,
      required: false,
      default: null
    },
    currentUser: {
      type: String,
      required: false,
      default: null
    },
    activityId: {
      type: String,
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
    }
  },
  data() {
    return {
      spaceId: null,
      currentApplication: 'newsDetails',
      updaterIdentity: null,
      BYTES_IN_MB: 1048576,
      dateFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      },
      dateTimeFormat: {
        hour: '2-digit',
        minute: '2-digit',
      },
    };
  },
  computed: {
    articleNewLayoutEnabled() {
      return eXo?.env?.portal?.articleNewLayoutEnabled;
    },
    historyClearedBackUrl() {
      return this.news && this.news.spaceMember ? this.news.spaceUrl : `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}`;
    },
    publicationState() {
      return this.news && this.news.publicationState;
    },
    newsTitle() {
      return this.news && this.news.title;
    },
    newsPublished() {
      return this.news && this.news.published;
    },
    lastVisitedPage(){
      return history && history.length && history.length > 2;
    },
    displayFavoriteButton() {
      return this.currentUser !== '' && this.publicationState !== 'staged';
    }
  },
  methods: {
    goBack() {
      if (this.lastVisitedPage) {
        history.back();
      }
      else {
        window.open(this.historyClearedBackUrl ,'_self');
      }
    },
  }
};
</script>
