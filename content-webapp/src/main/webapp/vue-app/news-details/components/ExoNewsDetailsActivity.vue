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
  <exo-news-details
    v-if="news"
    :news="news"
    :key="`${news.id}${news.lang && '_' + news.lang || ''}`"
    :news-id="newsId || sharedNewsId"
    :activity-id="activityId"
    :show-edit-button="showEditButton"
    :show-publish-button="showPublishButton"
    :show-delete-button="showDeleteButton"
    :show-copy-link-button="true"
    :translations="translations"
    :selected-translation="selectedTranslation" />
</template>

<script>

export default {
  props: {
    activity: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    news: null,
    selectedTranslation: {value: eXo.env.portal.language},
    translations: [],
    languages: [],
    originalVersion: null,
    previousSelectedTranslation: null
  }),
  computed: {
    activityId() {
      return this.activity && this.activity.id;
    },
    sharedActivity() {
      return this.activity && this.activity.originalActivity;
    },
    sharedTemplateParams() {
      return this.sharedActivity && this.sharedActivity.templateParams;
    },
    templateParams() {
      return this.activity && this.activity.templateParams;
    },
    newsId() {
      return this.templateParams && this.templateParams.newsId;
    },
    sharedNewsId() {
      return this.sharedTemplateParams && this.sharedTemplateParams.newsId;
    },
    showDeleteButton() {
      return this.news && this.news.canDelete;
    },
    showEditButton() {
      return this.news && this.news.canEdit;
    },
    showPublishButton() {
      return this.news && this.news.canPublish;
    },
  },
  created() {
    this.getAvailableLanguages();
    const url = new URL(window.location.href);
    const params = new URLSearchParams(url.search);
    if (params.has('lang')) {
      this.selectedTranslation.value = params.get('lang');
    }
    this.originalVersion = { value: '', text: this.$root.$t('article.label.translation.originalVersion') };
    if (this.newsId || this.sharedNewsId) {
      this.retrieveNews();
    }
    this.$root.$on('change-article-translation', (lang) => {
      this.previousSelectedTranslation = this.selectedTranslation.value;
      this.changeTranslation(lang);
    });
    this.$root.$on('update-content-selected-translation', (translation) => {
      this.updateSelectedTranslation(translation);
      this.previousSelectedTranslation = translation.value;
    });
  },
  methods: {
    getAvailableLanguages() {
      return this.$notesService.getAvailableLanguages().then(data => {
        this.languages = data || [];
      });
    },
    retrieveNews() {
      if (this.activity.news) {
        this.activityId = this.activity.news.activityId;
        const newsId = this.activity.news.id;
        if (newsId && this.activity.news?.lang !== this.selectedTranslation.value) {
          this.getArticleVersionWithLang(newsId, this.selectedTranslation.value);
          this.fetchTranslation(newsId);
        } else {
          this.news = this.activity.news;
          this.fetchTranslation(newsId);
          if (this.news.lang) {
            this.addParamToUrl('lang', this.news.lang);
          } else {
            this.removeParamFromUrl('lang');
            this.selectedTranslation = this.originalVersion;
          }
        }
      } else {
        this.$newsServices.getNewsByActivityId(this.activityId, this.selectedTranslation.value)
          .then(news => {
            if (this.previousSelectedTranslation === 'autoTranslation') {
              // reset the news model after the automatic translation
              this.news = null;
            }
            this.news = news;
            if (this.news.lang) {
              this.addParamToUrl('lang', this.news.lang);
            } else {
              this.removeParamFromUrl('lang');
              this.selectedTranslation = this.originalVersion;
            }
          })
          .catch(() => {
            this.$root.$emit('activity-extension-abort', this.activityId);
          });
      }
    },
    getArticleVersionWithLang(id, lang) {
      if (this.previousSelectedTranslation === 'autoTranslation') {
        // reset the news model after the automatic translation
        this.news = null;
      }
      return this.$newsServices.getNewsById(id, false, 'article', lang).then((resp) => {
        this.news = resp;
        if (this.news.lang) {
          this.addParamToUrl('lang', this.news.lang);
        } else {
          this.removeParamFromUrl('lang');
          this.selectedTranslation = this.originalVersion;
        }
      });
    },
    addParamToUrl(paramName, paramValue) {
      const url = new URL(window.location.href);
      url.searchParams.set(paramName, paramValue);
      history.pushState({}, null, url.toString());
    },
    removeParamFromUrl(paramName) {
      const url = new URL(window.location.href);
      url.searchParams.delete(paramName);
      history.pushState({}, null, url.toString());
    },
    changeTranslation(translation) {
      this.selectedTranslation = translation;
      this.getArticleVersionWithLang(this.news.id, this.selectedTranslation.value);
      this.$forceUpdate();
    },
    fetchTranslation(articleId) {
      this.$newsServices.getArticleLanguages(articleId, false).then((resp) => {
        this.translations =  resp || [];
        if (this.translations.length>0) {
          this.translations = this.languages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
        }
        this.translations.unshift(this.originalVersion);
      });
    },
    updateSelectedTranslation(translation) {
      this.selectedTranslation = translation;
    },
  },
};
</script>