<template>
  <div
    id="newsDetails"
    class="pa-5">
    <div v-if="notFound" class="articleNotFound">
      <i class="iconNotFound"></i>
      <h3>{{ $t('news.details.restricted') }}</h3>
    </div>
    <exo-news-details
      v-if="news && !notFound"
      id="newsFullDetails"
      :news="news"
      :key="`${news.id}${news.lang && '_' + news.lang || ''}`"
      :news-id="newsId"
      :activity-id="news?.activityId"
      :news-type="newsType"
      :show-edit-button="showEditButton"
      :show-publish-button="showPublishButton"
      :show-delete-button="showDeleteButton"
      :translations="translations"
      :selected-translation="selectedTranslation"
      :show-copy-link-button="true" />
  </div>
</template>

<script>
const UNAUTHORIZED_CODE = 401;
export default {
  props: {
    newsId: {
      type: String,
      default: ''
    },
    newsType: {
      type: String,
      default: ''
    },
  },
  data: () => ({
    news: null,
    notFound: false,
    showEditButton: false,
    showPublishButton: false,
    showDeleteButton: false,
    selectedTranslation: {value: eXo.env.portal.language},
    translations: [],
    languages: [],
    originalVersion: null,
    previousSelectedTranslation: null
  }),
  created() {
    this.getAvailableLanguages();
    const url = new URL(window.location.href);
    const params = new URLSearchParams(url.search);
    if (params.has('lang')) {
      this.selectedTranslation.value = params.get('lang');
    }
    this.originalVersion = { value: '', text: this.$root.$t('article.label.translation.originalVersion') };
    this.getArticleVersionWithLang(this.newsId, this.selectedTranslation.value);
    this.fetchTranslation(this.newsId);
    this.$root.$on('change-article-translation', (translation) => {
      this.previousSelectedTranslation = this.selectedTranslation.value;
      this.changeTranslation(translation);
    });
    this.$root.$on('update-content-selected-translation', (translation) => {
      this.updateSelectedTranslation(translation);
      this.previousSelectedTranslation = translation.value;
    });
  },
  methods: {
    getAvailableLanguages() {
      return this.$newsServices.getAvailableLanguages().then(data => {
        this.languages = data || [];
      });
    },
    getArticleVersionWithLang(id, lang) {
      if (this.previousSelectedTranslation === 'autoTranslation') {
        // reset the news model after the automatic translation
        this.news = null;
      }
      return this.$newsServices.getNewsById(id, false, 'article', lang).then((resp) => {
        if (resp !== null && resp !== UNAUTHORIZED_CODE) {
          this.news = resp;
          this.showEditButton = this.news.canEdit;
          this.showPublishButton = this.news.canPublish;
          this.showDeleteButton = this.news.canDelete;
          if (this.news.lang) {
            this.addParamToUrl('lang', this.news.lang);
          } else {
            this.removeParamFromUrl('lang');
            this.selectedTranslation = this.originalVersion;
          }
          if (this.news.spaceMember) {
            this.$root.$emit('restricted-space', this.news.spaceDisplayName, this.news.hiddenSpace);
            const message = this.news.hiddenSpace ? this.$t('news.activity.notAuthorizedUserForSpaceHidden'): this.$t('news.activity.notAuthorizedUser').replace('{0}',  this.news.spaceDisplayName);
            this.$root.$emit('alert-message', message, 'warning');
          }
        } else {
          this.notFound = true;
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
  }
};
</script>
