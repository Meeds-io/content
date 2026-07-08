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
    class="newsDetailsTopBar">
    <v-btn
      class="go-back-button"
      icon
      :aria-label="this.$t('article.details.goBack.label')"
      @click.stop="goBack">
      <v-icon
        size="15">
        fas fa-arrow-left
      </v-icon>
    </v-btn>
    <exo-news-details-action-menu-app
      v-if="(showEditButton || showDeleteButton || showPublishButton || showCopyLinkButton || showCategoriesButton)"
      class="pull-right"
      :news="news"
      :current-app="currentApplication"
      :show-edit-button="showEditButton"
      :show-delete-button="showDeleteButton"
      :show-publish-button="showPublishButton"
      :show-copy-link-button="showCopyLinkButton"
      :show-refer-button="showReferButton"
      :show-properties-button="showEditButton"
      :show-export-pdf-button="true"
      :show-categories-button="showCategoriesButton"
      @delete-article="$emit('delete-article')"
      @export-pdf="$emit('export-pdf')"
      @edit-article="$emit('edit-article')"
      @open-properties="$emit('open-properties', $event)"
      @manage-categories="$emit('manage-categories')" />
    <v-btn
      v-if="publicationState === 'staged'"
      class="btn btn-primary pull-right me-3"
      outlined
      @click="$emit('open-publication-drawer')">
      <v-icon
        class="me-2"
        size="20">
        far fa-clock
      </v-icon>
      {{ $t("news.composer.btn.scheduleArticle") }}
    </v-btn>
    <exo-news-favorite-action
      v-if="displayFavoriteButton"
      :news="news"
      :activity-id="activityId"
      :icon-size="20"
      class="pull-right mt-1 me-2" />
    <extension-registry-components
      :params="params"
      name="NewsDetails"
      type="news-toolbar"
      parent-element="div"
      element="div"
      class="pull-right mt-1 d-inline-flex" />
    <v-tooltip
      v-if="canOpenInNotes"
      bottom>
      <template #activator="{ on, attrs }">
        <v-btn
          v-on="on"
          v-bind="attrs"
          :href="articlePageUrl"
          :aria-label="$t('content.article.open.in.notes')"
          class="pull-right me-2"
          link
          icon>
          <v-icon
            size="20"
            class="icon-default-color">
            fas fa-external-link-alt
          </v-icon>
        </v-btn>
      </template>
      {{ $t('content.article.open.in.notes') }}
    </v-tooltip>
    <div
      v-if="categoriesCount"
      class="d-flex align-center flex-wrap pull-right mt-2 me-2">
      <category-chip
        v-for="category in displayedCategories"
        :key="category.id"
        :category="category"
        chip-class="me-2"
        small />
      <v-btn
        v-if="remainingCategoriesCount > 0"
        :title="$t('categories.remainingCount', {0: remainingCategoriesCount})"
        class="flex-shrink-0 px-0 me-2"
        height="24"
        width="24"
        icon
        @click="openCategoriesListDrawer">
        <span class="primary--text text-subtitle-font-size">
          {{ $t('categories.remainingCount', {0: remainingCategoriesCount}) }}
        </span>
      </v-btn>
    </div>
    <categories-list-drawer
      v-if="categoriesListDrawerOpened"
      ref="categoriesListDrawer" />
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
    },
    showReferButton: {
      type: Boolean,
      default: false
    },
    showCategoriesButton: {
      type: Boolean,
      required: false,
      default: false
    },
    articlePageUrl: {
      type: String,
      default: null
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
      categories: [],
      categoriesListDrawerOpened: false,
    };
  },
  computed: {
    displayedCategories() {
      return this.categories?.slice(0, 2) || [];
    },
    categoriesCount() {
      return this.categories?.length || 0;
    },
    remainingCategoriesCount() {
      return this.categoriesCount - 2;
    },
    historyClearedBackUrl() {
      return this.news && this.news.spaceMember ? this.news.spaceUrl : `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}`;
    },
    publicationState() {
      return this.news && this.news.publicationState;
    },
    lastVisitedPage(){
      return history && history.length && history.length > 2;
    },
    displayFavoriteButton() {
      return this.currentUser !== '' && this.publicationState !== 'staged';
    },
    canOpenInNotes() {
      return this.news?.referred || this.news?.fromExternalPage;
    },
    params() {
      return {
        news: this.news,
      };
    },
  },
  watch: {
    'news.categories': {
      immediate: true,
      handler() {
        this.refreshCategories();
      },
    },
  },
  methods: {
    async refreshCategories() {
      if (this.news?.categories?.length) {
        const categories = await Promise.all(this.news.categories.map(id => this.$categoryService.getCategory(id).catch(() => null)));
        this.categories = categories.filter(category => category);
      } else {
        this.categories = [];
      }
    },
    async openCategoriesListDrawer() {
      this.categoriesListDrawerOpened = true;
      await this.$nextTick();
      this.$refs.categoriesListDrawer.open(this.categories);
    },
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
