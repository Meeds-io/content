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
    languages: JSON.parse(eXo.env.portal.availableLanguages),
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
    this.originalVersion = { value: '', text: this.$root.$t('article.label.translation.originalVersion') };
    this.removeParamFromUrl('lang');
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
    retrieveNews() {
      if (this.activity.news) {
        this.activityId = this.activity.news.activityId;
        if (this.activity.news.id) {
          this.fetchTranslation(this.activity.news);
        }
      } else {
        this.$newsServices.getNewsByActivityId(this.activityId)
          .then(news => {
            if (news?.id) {
              this.fetchTranslation(news);
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
    fetchTranslation(originalArticle) {
      this.$newsServices.getArticleLanguages(originalArticle.id, false).then((resp) => {
        this.translations =  resp || [];
        if (this.translations.length>0) {
          this.translations = this.languages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
        }
        this.translations.unshift(this.originalVersion);
        const exists = this.translations.some(obj => obj.value.toLowerCase() === this.selectedTranslation.value.toLowerCase());
        if (exists) {
          this.getArticleVersionWithLang(originalArticle.id, this.selectedTranslation.value);
        } else {
          this.news = originalArticle;
          this.selectedTranslation = this.originalVersion;
        }
      });
    },
    updateSelectedTranslation(translation) {
      this.selectedTranslation = translation;
    },
  },
};
</script>