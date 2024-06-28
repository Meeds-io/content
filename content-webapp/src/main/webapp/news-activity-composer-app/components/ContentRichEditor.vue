<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2024 Meeds Association contact@meeds.io

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
  <v-app class="notesEditor">
    <note-full-rich-editor
      ref="editor"
      :note="article"
      :draft-saving-status="draftSavingStatus"
      :note-id-param="articleId"
      :post-key="postKey"
      :body-placeholder="newsFormContentPlaceholder"
      :title-placeholder="newsFormTitlePlaceholder"
      :form-title="newsFormTitle"
      :app-name="appName"
      :web-page-note="false"
      :web-page-url="false"
      :languages="languages"
      :translations="translations"
      :selected-language="selectedLanguage"
      :lang-button-tooltip-text="langButtonTooltipText"
      :publish-button-text="publishButtonText"
      :is-mobile="isMobile"
      :editor-body-input-ref="editorBodyInputRef"
      :editor-title-input-ref="editorTitleInputRef"
      :space-group-id="spaceGroupId"
      :space-url="spaceUrl"
      :save-button-icon="saveButtonIcon"
      :editor-icon="editorIcon"
      @editor-closed="editorClosed"
      @open-treeview="openTreeView"
      @post-note="postArticleActions"
      @auto-save="autoSave"
      @editor-ready="editorReady"
      @update-data="updateArticleData" />
    <note-treeview-drawer
      ref="noteTreeview"
      @closed="closePluginsDrawer()" />
    <schedule-news-drawer
      v-if="canScheduleArticle"
      :posting-news="postingNews"
      :news-id="articleId"
      :news-type="articleType"
      @post-article="postArticle" />
  </v-app>
</template>

<script>

export default {
  data() {
    return {
      editor: null,
      article: {
        title: '',
        content: '',
        body: '',
        summary: '',
        illustration: []
      },
      originalArticle: {
        title: '',
        content: '',
        body: '',
        summary: '',
        illustration: []
      },
      autoSaveDelay: 1000,
      newsFormTitlePlaceholder: `${this.$t('news.composer.placeholderTitleInput')}*`,
      newsFormContentPlaceholder: `${this.$t('news.composer.placeholderContentInput')}*`,
      appName: 'content',
      translations: [],
      languages: [],
      allLanguages: [],
      postingNews: false,
      savingDraft: false,
      saveDraft: '',
      draftSavingStatus: '',
      initDone: false,
      canCreateArticle: false,
      loading: false,
      currentSpace: {},
      spaceUrl: null,
      currentUser: eXo.env.portal.userName,
      spaceDisplayName: null,
      oembedMinWidth: 300,
      editorBodyInputRef: 'articleContent',
      editorTitleInputRef: 'articleTitle',
      imagesURLs: new Map(),
      canScheduleArticle: false,
      postKey: 1,
      editorIcon: 'fas fa-newspaper',
    };
  },
  props: {
    articleId: {
      type: String,
      required: false,
      default: null
    },
    spaceId: {
      type: String,
      required: true
    },
    activityId: {
      type: String,
      required: false,
      default: null
    },
    articleType: {
      type: String,
      required: false,
      default: null
    },
    selectedLanguage: {
      type: String,
      default: null
    }
  },
  watch: {
    'article.title': function() {
      if (this.article.title !== this.originalArticle.title) {
        this.autoSave();
      }
    },
    'article.summary': function() {
      if (this.article.summary !== this.originalArticle.summary) {
        this.autoSave();
      }
    },
    'article.body': function() {
      if (this.article.body !== this.originalArticle.body) {
        this.autoSave();
      }
    },
    'article.illustration': function() {
      if (this.initIllustrationDone) {
        this.illustrationChanged = true;
        this.autoSave();
      }
    }
  },
  computed: {
    editMode() {
      return !!this.activityId;
    },
    spaceGroupId() {
      return this.currentSpace?.groupId;
    },
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    },
    langButtonTooltipText() {
      return this.articleId && this.$t('content.label.button.translations.options')
                            || this.$t('content.message.firstVersionShouldBeCreated');
    },
    newsFormTitle() {
      if (!this.editMode) {
        return this.$t('news.editor.label.create');
      } else {
        return this.$t('news.editor.label.edit');
      }
    },
    publishButtonText() {
      return !this.editMode && this.$t('news.editor.publish') || this.$t('news.editor.save');
    },
    saveButtonIcon() {
      return this.editMode && 'fas fa-save' || 'fa-solid fa-paper-plane';
    }
  },
  created() {
    this.getArticle();
    this.getAvailableLanguages();
    this.$root.$on('display-treeview-items', (/*filter*/) => {
      // TO DO
    });
    this.$root.$on('add-translation', this.addTranslation);
    this.$root.$on('lang-translation-changed', this.changeTranslation);
    this.$root.$on('delete-lang-translation', this.deleteTranslation);
  },
  mounted() {
    this.initEditor();
  },
  methods: {
    editorClosed() {
      window.close();
    },
    openTreeView(/*noteId, source, includeDisplay, filter*/) {
      // TO DO
    },
    addTranslation(/*lang*/) {
      // TO DO
    },
    changeTranslation(/*lang*/) {
      // TO DO
    },
    deleteTranslation(/*translation*/) {
      // TO DO
    },
    postArticle(schedulePostDate, postArticleMode, publish, isActivityPosted, selectedTargets, selectedAudience) {
      if (typeof isActivityPosted === 'undefined') {
        this.article.activityPosted = true;
      } else {
        this.article.activityPosted = isActivityPosted;
      }
      this.article.published = publish;
      this.article.targets = selectedTargets;
      if (selectedAudience !== null) {
        this.article.audience = selectedAudience === this.$t('news.composer.stepper.audienceSection.allUsers') ? 'all' : 'space';
      }
      this.doPostArticle(schedulePostDate);
    },
    doPostArticle(schedulePostDate) {
      this.postingNews = true;
      if (this.savingDraft) {
        this.$on('draftCreated', this.saveArticle);
        this.$on('draftUpdated', this.saveArticle);
      } else {
        this.saveArticle(schedulePostDate);
      }
    },
    saveArticle(schedulePostDate) {
      clearTimeout(this.saveDraft);
      this.$off('draftCreated', this.saveNews);
      this.$off('draftUpdated', this.saveNews);

      const article = {
        id: this.article.id,
        title: this.article.title,
        summary: this.article.summary,
        body: this.replaceImagesURLs(this.$noteUtils.getContentToSave(this.editorBodyInputRef, this.oembedMinWidth)),
        author: this.currentUser,
        published: this.article.published,
        targets: this.article.targets,
        spaceId: this.spaceId,
        publicationState: 'posted',
        schedulePostDate: null,
        timeZoneId: null,
        activityPosted: this.article.activityPosted,
        audience: this.article.audience,
      };

      if (schedulePostDate){
        article.publicationState ='staged';
        article.schedulePostDate = schedulePostDate;
        article.timeZoneId = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
      }
      if (this.article?.illustration?.length) {
        article.uploadId = this.article.illustration[0].uploadId;
      }
      if (article.publicationState ==='staged') {
        this.$newsServices.scheduleNews(article, this.articleType).then((scheduleArticle) => {
          if (scheduleArticle) {
            history.replaceState(null,'',scheduleArticle.spaceUrl);
            window.location.href = scheduleArticle.url;
          }
        });
      } else {
        this.$newsServices.saveNews(article).then((createdArticle) => {
          let createdArticleActivity = null;
          if (createdArticle.activities) {
            const createdArticleActivities = createdArticle.activities.split(';')[0].split(':');
            if (createdArticleActivities.length > 1) {
              createdArticleActivity = createdArticleActivities[1];
            }
          }
          if (createdArticleActivity) {
            history.replaceState(null, '', this.spaceUrl);
            window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${createdArticleActivity}`;
          } else {
            window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}`;
          }
        });
      }
    },
    postArticleActions(publish) {
      if (publish) {
        this.updateAndPostArticle(true);
        return;
      }
      if (this.editMode) {
        this.updateAndPostArticle(false);
        return;
      }
      if (this.canScheduleArticle) {
        this.scheduleMode = 'postScheduledNews';
        this.$root.$emit('open-schedule-drawer', this.scheduleMode);
        this.postKey++;
      } else {
        this.postArticle();
        this.postKey = 1;
      }
    },
    updateArticleData(article) {
      this.article.title = article.title;
      this.article.content = article.content;
      this.article.body = article.content;
    },
    editorReady(editor) {
      this.editor = editor;
    },
    initEditor() {
      this.$refs.editor.initCKEditor();
    },
    autoSave: function() {
      // No draft saving if init not done or in edit mode for the moment
      if (!this.initDone) {
        return;
      }
      // if the News is being posted, no need to autosave anymore
      if (this.postingNews) {
        return;
      }
      clearTimeout(this.saveDraft);
      this.saveDraft = setTimeout(() => {
        this.savingDraft = true;
        this.draftSavingStatus = this.$t('news.composer.draft.savingDraftStatus');
        this.$nextTick(() => {
          if (this.activityId) {
            this.doUpdateArticle('draft', false);
          } else {
            this.saveArticleDraft();
          }
        });
      }, this.autoSaveDelay);
    },
    saveArticleDraft() {
      const article = {
        title: this.article.title,
        summary: this.article.summary,
        body: this.replaceImagesURLs(this.$noteUtils.getContentToSave(this.editorBodyInputRef, this.oembedMinWidth)),
        author: this.currentUser,
        published: false,
        spaceId: this.spaceId,
        publicationState: ''
      };
      if (this.article?.illustration?.length) {
        article.uploadId = this.article.illustration[0].uploadId;
      } else {
        article.uploadId = '';
      }
      if (this.article.id) {
        if (this.article.title || this.article.summary || this.article.body || this.article.illustration?.length) {
          article.id = this.article.id;
          this.$newsServices.updateNews(article, false, this.articleType)
            .then((updatedArticle) => {
              if (this.article.body !== updatedArticle.body) {
                this.imagesURLs = this.extractImagesURLsDiffs(this.article.body, updatedArticle.body);
              }
            })
            .then(() => this.$emit('draftUpdated'))
            .then(() => this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus'));
        } else {
          this.$newsServices.deleteDraft(this.article.id)
            .then(() => this.$emit('draftDeleted'))
            .then(() => this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus'));
          this.article.id = null;
        }
        this.savingDraft = false;
      } else if (this.article.title || this.article.summary || this.article.body || this.article.illustration?.length) {
        article.publicationState = 'draft';
        this.$newsServices.saveNews(article).then((createdArticle) => {
          this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus');
          this.article.id = createdArticle.id;
          if (!this.articleId) {
            this.$root.articleId = createdArticle.id;
          }
          this.savingDraft = false;
          this.$emit('draftCreated');
        });
      } else {
        this.draftSavingStatus = '';
      }
    },
    doUpdateArticle: function (publicationState, post) {
      const updatedArticle = {
        id: this.article.id,
        title: this.article.title,
        summary: this.article.summary != null ? this.article.summary : '',
        body: this.replaceImagesURLs(this.$noteUtils.getContentToSave(this.editorBodyInputRef, this.oembedMinWidth)),
        published: this.article.published,
        publicationState: publicationState,
        activityPosted: this.article.activityPosted,
        audience: this.article.audience,
      };
      if (this.article?.illustration?.length) {
        updatedArticle.uploadId = this.article.illustration[0].uploadId;
      } else if (this.originalArticle.illustrationURL !== null) {
        // an empty uploadId means the illustration must be deleted
        updatedArticle.uploadId = '';
      }
      return this.updateArticle(updatedArticle, post, publicationState);
    },
    updateAndPostArticle(post) {
      this.doUpdateArticle('posted', post).then(() => {
        history.replaceState(null,'',this.spaceUrl);
        window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${this.activityId}`;
      }).catch (() =>{
        window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${this.activityId}`;
      });
    },
    updateArticle(updatedArticle, post, publicationState) {
      const activityType = this.parseActivityType(publicationState);
      return this.$newsServices.updateNews(updatedArticle, post, activityType).then((createdArticle) => {
        this.spaceUrl = createdArticle.spaceUrl;
        if (this.article.body !== createdArticle.body) {
          this.imagesURLs = this.extractImagesURLsDiffs(this.article.body, createdArticle.body);
        }
      }).then(() => this.$emit('draftUpdated'))
        .then(() => this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus'));
    },
    parseActivityType(publicationState) {
      let articleType = this.articleType;
      if (this.activityId && publicationState === this.$newsConstants.newsObjectType.DRAFT) {
        articleType = this.$newsConstants.newsObjectType.LATEST_DRAFT;
      }
      if (this.activityId && publicationState === 'posted') {
        articleType = this.$newsConstants.newsObjectType.ARTICLE;
      }
      return articleType;
    },
    replaceImagesURLs: function(content) {
      let updatedContent = content;
      const specialCharactersRegex = /[-/\\^$*+?.()|[\]{}]/g;
      this.imagesURLs.forEach(function(value, key) {
        const escapedKey = key.replace(specialCharactersRegex, '\\$&');
        const regex = new RegExp(`src="${escapedKey}"`);
        updatedContent = updatedContent.replace(regex, `src="${value}"`);
      });
      return updatedContent;
    },
    extractImagesURLsDiffs: function(originalHTMLString, updatedHTMLString) {
      const imagesURLs = new Map();

      const originalHTML = $(originalHTMLString);
      const updatedHTML = $(updatedHTMLString);
      const originalImages = originalHTML.find('img');
      const updatedImages = updatedHTML.find('img');

      originalImages.each(function(index, element) {
        const originalImageURL = $(element).attr('src');
        const updatedImageURL = $(updatedImages[index]).attr('src');
        if (updatedImageURL !== originalImageURL) {
          imagesURLs.set(originalImageURL, updatedImageURL);
        }
      });
      return imagesURLs;
    },
    getArticle() {
      this.loading = true;
      this.$newsServices.getSpaceById(this.spaceId).then(space => {
        this.currentSpace = space;
        this.spaceDisplayName = space.displayName;
        this.spaceUrl = this.currentSpace?.prettyName;
        this.$newsServices.canUserCreateNews(this.currentSpace.id).then(canCreateArticle => {
          this.canCreateArticle = canCreateArticle || this.articleId;
          this.$nextTick(() => {
            if (this.canCreateArticle) {
              if (this.articleId) {
                this.fillArticle(this.articleId);
              } else {
                const message = localStorage.getItem('exo-activity-composer-message');
                if (message) {
                  this.article.content = message;
                  this.setEditorData(this.article?.content);
                  localStorage.removeItem('exo-activity-composer-message');
                }
                this.initDone = true;
              }
            }
            this.loading = false;
          });
        });
        this.$newsServices.canScheduleNews(this.currentSpace.id).then(canScheduleArticle => {
          this.canScheduleArticle = canScheduleArticle;
        });
      });
    },
    fillArticle(articleId) {
      this.$newsServices.getNewsById(articleId, true, this.articleType).then(article => {
        if (article === 401){
          this.unAuthorizedAccess = true;
        } else {
          this.article.id = article.id;
          this.article.title = article.title;
          this.article.summary = article.summary;
          this.article.content = this.$noteUtils.getContentToEdit(article.body);
          this.article.body = article.body;
          this.article.published = article.published;
          this.article.spaceId = article.spaceId;
          this.article.publicationState = article.publicationState;
          this.article.activityId = article.activityId;
          this.article.updater = article.updater;
          this.article.draftUpdaterDisplayName = article.draftUpdaterDisplayName;
          this.article.draftUpdaterUserName = article.draftUpdaterUserName;
          this.article.draftUpdateDate = article.draftUpdateDate;
          this.article.activityPosted = article.activityPosted;
          this.article.audience = article.audience;
          this.parseIllustration(article);
          this.originalArticle = structuredClone(this.article);
          this.setEditorData(this.article?.content);
        }
        this.initDone = true;
      });
    },
    setEditorData(content) {
      this.$refs.editor.setEditorData(content);
    },
    parseIllustration(article) {
      if (article.illustrationURL) {
        this.$newsServices.importFileFromUrl(article.illustrationURL)
          .then(resp => resp.blob())
          .then(fileData => {
            const illustrationFile = new File([fileData], `illustration${this.articleId}`);
            const fileDetails = {
              id: null,
              uploadId: null,
              name: illustrationFile.name,
              size: illustrationFile.size,
              src: article.illustrationURL,
              progress: null,
              file: illustrationFile,
              finished: true,
            };
            this.article.illustration.push(fileDetails);
            this.originalArticle.illustration.push(fileDetails);
          })
          .then(() => this.initIllustrationDone = true);
      } else {
        this.initIllustrationDone = true;
      }
    },
    closePluginsDrawer() {
      this.$refs.editor.closePluginsDrawer();
    },
    getAvailableLanguages() {
      return this.$notesService.getAvailableLanguages().then(data => {
        this.languages = data || [];
        this.languages.sort((a, b) => a.text.localeCompare(b.text));
        this.allLanguages = this.languages;
        this.languages.unshift({value: '', text: this.$t('notes.label.chooseLangage')});
        if (this.translations) {
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
      });
    },
  }
};
</script>
