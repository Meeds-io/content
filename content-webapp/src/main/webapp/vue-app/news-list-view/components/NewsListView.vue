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
  <v-hover v-slot="{ hover }">
    <v-app v-show="!hideEmptyNewsTemplateForNonPublisher" class="news-list-view-app position-relative">
      <v-card
        :class="newsListViewClass"
        class="application-body application-layout-style overflow-hidden"
        height="100%"
        flat>
        <v-card-text class="pa-0">
          <news-settings v-if="displayHeader" :is-hovering="hover" />
          <extension-registry-component
            v-if="selectedViewExtension"
            element-class="news-list-view"
            :component="selectedViewComponent"
            :params="viewComponentParams" />
        </v-card-text>
      </v-card>
      <news-settings-drawer
        v-if="canPublishNews"
        :saved-header-translations="headerTranslations"
        :language="language"
        :application-id="applicationId"/>
      <news-publish-targets-management-drawer v-if="canManageNewsPublishTargets" />
    </v-app>
  </v-hover>
</template>

<script>
export default {
  props: {
    applicationId: {
      type: String,
      default: null,
    },
    headerTranslations: {
      type: Object,
      default: null
    },
    viewTemplate: {
      type: String,
      default: null,
    },
    newsTarget: {
      type: Array,
      default: null,
    },
    headerTitle: {
      type: String,
      default: null,
    },
    limit: {
      type: String,
      default: null,
    },
    showHeader: {
      type: Boolean,
      default: false,
    },
    showSeeAll: {
      type: Boolean,
      default: false,
    },
    showArticleTitle: {
      type: Boolean,
      default: false,
    },
    showArticleSummary: {
      type: Boolean,
      default: false,
    },
    showArticleImage: {
      type: Boolean,
      default: false,
    },
    showArticleAuthor: {
      type: Boolean,
      default: false,
    },
    showArticleReactions: {
      type: Boolean,
      default: false,
    },
    showArticleSpace: {
      type: Boolean,
      default: false,
    },
    showArticleDate: {
      type: Boolean,
      default: false,
    },
    seeAllUrl: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    extensionApp: 'NewsList',
    extensionType: 'views',
    newsList: [null],
    viewExtensions: {},
    loading: false,
    hasMore: false,
    offset: 0,
    canPublishNews: false,
    canManageNewsPublishTargets: eXo.env.portal.canManageNewsPublishTargets,
    language: eXo?.env?.portal?.language,
  }),
  computed: {
    displayHeader() {
      return this.viewTemplate &&
            this.selectedViewExtension?.id !== 'NewsEmptyTemplate' &&
            this.viewTemplate !== 'NewsSlider' && 
            this.viewTemplate !== 'NewsAlert' && 
            this.viewTemplate !== 'NewsStories' && 
            (this.viewTemplate !== 'NewsMosaic' || this.isMobile);
    },
    selectedViewExtension() {
      if (this.viewTemplate) {
        if (this.newsList.length === 0) {
          const sortedViewExtensions = Object.values(this.viewExtensions).sort();
          return sortedViewExtensions[0];
        } else {
          return this.viewExtensions[this.viewTemplate];
        }
      } else if (Object.keys(this.viewExtensions).length && (this.newsList.length > 0 || this.newsTarget)) {
        const sortedViewExtensions = Object.values(this.viewExtensions).sort();
        return sortedViewExtensions[0];
      }
      return null;
    },
    selectedViewComponent() {
      return this.selectedViewExtension && {
        componentName: this.selectedViewExtension.id,
        componentOptions: this.selectedViewExtension,
      };
    },
    isMobile() {
      return this.$vuetify.breakpoint.width < 651;
    },
    viewComponentParams() {
      return {
        viewExtension: this.selectedViewExtension,
        newsTarget: this.newsTarget,
        newsList: this.newsList,
        headerTranslations: this.headerTranslations,
        headerTitle: this.headerTitle,
        limit: this.limit,
        showHeader: this.showHeader,
        showSeeAll: this.showSeeAll,
        showArticleTitle: this.showArticleTitle,
        showArticleSummary: this.showArticleSummary,
        showArticleImage: this.showArticleImage,
        showArticleAuthor: this.showArticleAuthor,
        showArticleSpace: this.showArticleSpace,
        showArticleReactions: this.showArticleReactions,
        showArticleDate: this.showArticleDate,
        seeAllUrl: this.seeAllUrl,
        hasMore: this.hasMore,
        loading: this.loading,
      };
    },
    hideEmptyNewsTemplateForNonPublisher() {
      return this.selectedViewExtension?.id === 'NewsEmptyTemplate' && !this.canPublishNews;
    },
    newsListViewClass() {
      let newsListViewClass = 'list-view-card';
      const displayPadding = this.viewTemplate && !['NewsStories', 'NewsSlider', 'NewsAlert', 'NewsMosaic'].includes(this.viewTemplate) || this.selectedViewExtension?.id === 'NewsEmptyTemplate';
      const backgroundTransparent = this.viewTemplate === 'NewsStories' && this.selectedViewExtension?.id !== 'NewsEmptyTemplate';
      if (displayPadding) {
        newsListViewClass = `${newsListViewClass} border-box-sizing pa-4`;
      }
      if (backgroundTransparent) {
        newsListViewClass = `${newsListViewClass} background-transparent`;
      }
      return newsListViewClass;
    },
  },
  watch: {
    viewExtensions() {
      this.$root.viewExtensions = this.viewExtensions;
    },
  },
  created() {
    this.$newsServices.canPublishNews().then(canPublishNews => {
      this.canPublishNews = canPublishNews;
    });
    this.$root.$on('saved-news-settings', (newsTarget, selectedOptions) => {
      this.seeAllUrl = selectedOptions.seeAllUrl;
      this.showSeeAll = selectedOptions.showSeeAll;
      this.showHeader = selectedOptions.showHeader;
      this.newsTarget = newsTarget;
      this.limit = this.$root.limit;
      this.retrieveNewsList();
    });
    this.seeAllUrl = this.$root.seeAllUrl;
    this.showSeeAll = this.$root.showSeeAll;
    this.showHeader = this.$root.showHeader;
    this.newsTarget = this.$root.newsTarget;
    this.limit = this.$root.limit;
    this.retrieveNewsList().finally(() => this.$root.$applicationLoaded());
    document.addEventListener(`component-${this.extensionApp}-${this.extensionType}-updated`, this.refreshViewExtensions);
    this.refreshViewExtensions();
  },
  beforeDestroy() {
    document.removeEventListener(`component-${this.extensionApp}-${this.extensionType}-updated`, this.refreshViewExtensions);
  },
  methods: {
    retrieveNewsList() {
      this.loading = true;
      return this.$newsListService.getNewsList(this.newsTarget, this.offset, this.limit, true)
        .then(newsList => {
          this.newsList = newsList.news.filter(news => !!news) || [];
          this.hasMore = this.newsList.length > this.limit;
        })
        .finally(() => this.loading = false);
    },
    refreshViewExtensions() {
      const extensions = extensionRegistry.loadComponents(this.extensionApp)
        .filter(component => component.componentName === this.extensionType)
        .map(component => component.componentOptions);
      extensions.forEach(extension => {
        if (extension.id && (!this.viewExtensions[extension.id] || this.viewExtensions[extension.id] !== extension)) {
          this.$set(this.viewExtensions, extension.id, extension);
        }
      });
    },
  },
};
</script>
