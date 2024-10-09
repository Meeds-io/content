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
      :note-id-param="activityId"
      :post-key="postKey"
      :body-placeholder="contentFormContentPlaceholder"
      :title-placeholder="contentFormTitlePlaceholder"
      :form-title="contentFormTitle"
      :suggester-space-url="spacePrettyName"
      :app-name="appName"
      :languages="languages"
      :translations="translations"
      :selected-language="selectedLanguage"
      :lang-button-tooltip-text="langButtonTooltipText"
      :publish-button-text="publishButtonText"
      :is-mobile="isMobile"
      :editor-body-input-ref="editorBodyInputRef"
      :editor-title-input-ref="editorTitleInputRef"
      :space-group-id="spaceGroupId"
      :save-button-icon="saveButtonIcon"
      :save-button-disabled="disableSaveButton"
      :editor-icon="editorIcon"
      :space-id="spaceId"
      :can-publish="canScheduleArticle"
      :images-download-folder="'DRIVE_ROOT_NODE/News/images'"
      @editor-closed="editorClosed"
      @open-treeview="openTreeView"
      @post-note="postArticleActions"
      @auto-save="autoSaveActions"
      @editor-ready="editorReady"
      @update-data="updateArticleData" />
    <note-treeview-drawer
      ref="noteTreeview"
      @closed="closePluginsDrawer()" />
    <schedule-news-drawer
      v-if="canScheduleArticle && !newPublicationDrawerEnabled"
      :posting-news="postingNews"
      :news-id="articleId"
      :news-type="articleType"
      :space-id="spaceId"
      @post-article="postArticle" />
    <div
      v-for="(extension, i) in editorExtensions"
      :key="i">
      <extension-registry-component
        :component="extension"
        element="div" />
    </div>
  </v-app>
</template>

<script>

export default {
  data() {
    return {
      editor: null,
      article: {
        id: 0,
        title: '',
        content: '',
        body: '',
        properties: {},
        lang: ''
      },
      originalArticle: {
        id: 0,
        title: '',
        content: '',
        body: '',
        properties: {}
      },
      autoSaveDelay: 1000,
      contentFormTitlePlaceholder: `${this.$t('news.composer.placeholderTitleInput')}*`,
      contentFormContentPlaceholder: `${this.$t('news.composer.placeholderContentInput')}*`,
      contentFormTitle: '',
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
      articleId: null,
      spaceId: null,
      activityId: null,
      articleType: null,
      selectedLanguage: null,
      currentArticleInitDone: false,
      isSpaceMember: false,
      spacePrettyName: null,
      editorExtensions: null,
    };
  },
  watch: {
    'article.title': function() {
      if (this.article.title !== this.originalArticle.title) {
        this.autoSave();
      }
    },
    'article.content': function() {
      if (!this.isSameArticleContent()) {
        this.autoSave();
      }
    },
    postingNews() {
      this.$refs.editor.setPublishing(this.postingNews);
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
    publishButtonText() {
      return !this.editMode && this.$t('news.editor.publish') || this.$t('news.editor.save');
    },
    saveButtonIcon() {
      return this.editMode && 'fas fa-save' || 'fa-solid fa-paper-plane';
    },
    disableSaveButton() {
      return this.postingNews || !this.article.title
                              || this.savingDraft
                              || (!this.propertiesModified
                              && this.articleNotChanged
                              && this.article.publicationState !== 'draft');
    },
    articleNotChanged() {
      return this.originalArticle?.title === this.article.title && this.isSameArticleContent()
                                                                && !this.propertiesModified;
    },
    propertiesModified() {
      return JSON.stringify(this.article?.properties) !== JSON.stringify(this.originalArticle?.properties);
    },
    newPublicationDrawerEnabled() {
      return eXo?.env?.portal?.newPublicationDrawerEnabled;
    }
  },
  created() {
    this.getAvailableLanguages();
    this.initDataPropertiesFromUrl();
    this.getArticle();
    this.refreshTranslationExtensions();
    document.addEventListener('automatic-translation-extensions-updated', () => {
      this.refreshTranslationExtensions();
    });
    this.$root.$on('display-treeview-items', filter => this.openTreeView(filter));
    this.$root.$on('add-translation', this.addTranslation);
    this.$root.$on('lang-translation-changed', this.changeTranslation);
    this.$root.$on('delete-lang-translation', this.deleteTranslation);
    this.contentFormTitle = this.articleId && this.$t('news.editor.label.edit') || this.$t('news.editor.label.create');
  },
  mounted() {
    this.initEditor();
  },
  methods: {
    editorClosed() {
      window.close();
    },
    openTreeView(filter) {
      if (this.initDone) {
        this.$notesService.getNote('group', this.spaceGroupId, 'Home').then(note => {
          this.$refs.noteTreeview.open(note, 'includePages', null, filter);
        });
      } else if (this.spaceId) {
        this.$newsServices.getSpaceById(this.spaceId).then(space => {
          this.$notesService.getNote('group', space.groupId, 'Home').then(note => {
            this.$refs.noteTreeview.open(note, 'includePages', null, filter);
          });
        });
      }
    },
    addTranslation(lang) {
      this.initDone = false;
      const originNoteContent = {
        title: this.article.title,
        content: this.article.content,
        properties: structuredClone(this.article?.properties),
        lang: lang?.value
      };
      this.article.title = '';
      this.article.content = '';
      if (this.article.properties === null) {
        this.article.properties = {};
      }
      this.article.properties.summary = '';
      this.article.properties.featuredImage = {};
      this.setEditorData('');
      this.languages = this.languages.filter(item => item.value !== lang?.value);
      this.selectedLanguage = lang?.value;
      this.translations.unshift(lang);
      this.article.lang = this.selectedLanguage;
      document.dispatchEvent(new CustomEvent('translation-added',{ detail: originNoteContent }));
      this.$nextTick(() => {
        this.initDone = true;
      });
    },
    changeTranslation(lang) {
      this.currentArticleInitDone = false;
      this.selectedLanguage = lang.value;
      if (lang.value) {
        this.translations=this.translations.filter(item => item.value !== lang.value);
        this.translations.unshift(lang);
      }
      const articleId = !this.article.targetPageId ? this.article.id : this.article.targetPageId;
      this.fillArticle(articleId, true,lang.value).then(() => {
        this.updateUrl();
        this.draftSavingStatus = '';
        this.currentArticleInitDone = true;
      });
    },
    deleteTranslation(translation) {
      const articleId = this.article?.targetPageId || this.article?.id;
      return this.$newsServices.deleteArticleTranslation(articleId,translation.value).then(() => {
        this.translations=this.translations.filter(item => item.value !== translation.value);
        const messageObject = {
          type: 'success',
          message: this.$t('content.alert.success.label.translation.deleted')
        };
        this.displayAlert(messageObject);
      });
    },
    autoSaveActions() {
      this.autoSave();
    },
    autoSave: function() {
      // No draft saving if init not done or in edit mode for the moment
      if (!this.initDone || !this.currentArticleInitDone || this.postingNews || this.articleNotChanged || this.isEmptyDraft()) {
        return;
      }
      clearTimeout(this.saveDraft);
      this.saveDraft = setTimeout(() => {
        this.savingDraft = true;
        this.draftSavingStatus = this.$t('news.composer.draft.savingDraftStatus');
        this.$nextTick(() => {
          if (this.activityId) {
            this.saveDraftForExistingArticle();
          } else {
            this.saveArticleDraft();
          }
        });
      }, this.autoSaveDelay);
    },
    saveDraftForExistingArticle() {
      const updatedArticle = this.getArticleToBeUpdated();
      if (updatedArticle?.properties) {
        updatedArticle.properties.draft = true;
      }
      updatedArticle.publicationState = 'draft';
      return this.$newsServices.updateNews(updatedArticle, false, this.articleType).then((createdArticle) => {
        this.spaceUrl = createdArticle.spaceUrl;
        this.articleId = this.article.id = createdArticle.id;
        this.article.targetPageId = createdArticle.targetPageId;
        this.article.properties = createdArticle.properties;
        this.article.draftPage = true;
        this.article.lang = createdArticle.lang;
        if (this.article.body !== createdArticle.body) {
          this.imagesURLs = this.extractImagesURLsDiffs(this.article.body, createdArticle.body);
        }
      }).then(() => this.$emit('draftUpdated'))
        .then(() => this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus'))
        .finally(() => {
          this.savingDraft = false;
          this.enableClickOnce();
          if (this.articleType === 'latest_draft' && this.selectedLanguage) {
            this.updateUrl();
          }
        });
    },
    updateAndPostArticle() {
      this.postingNews = true;
      const updatedArticle = this.getArticleToBeUpdated();
      updatedArticle.publicationState = 'posted';
      return this.$newsServices.updateNews(updatedArticle, true, 'article').then((createdArticle) => {
        this.spaceUrl = createdArticle.spaceUrl;
        if (this.article.body !== createdArticle.body) {
          this.imagesURLs = this.extractImagesURLsDiffs(this.article.body, createdArticle.body);
        }
        this.fillArticle(createdArticle.id, false, createdArticle.lang);
        let alertLink = this.isSpaceMember ? `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${createdArticle.activityId}` : `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news-detail?newsId=${createdArticle.id}`;
        if (createdArticle.lang) {
          alertLink = `${alertLink}&lang=${createdArticle.lang}`;
        }
        this.displayAlert({
          message: this.$t('news.save.success.message'),
          type: 'success',
          alertLinkText: this.$t('news.view.label'),
          alertLink: alertLink
        });
      }).then(() => {
        this.draftSavingStatus = '';
        this.enableClickOnce();
      }).catch((error) => {
        this.displayAlert({type: 'error', message: this.$t('news.save.error.message', error?.message)});
        this.enableClickOnce();
        this.draftSavingStatus = '';
      });
    },
    getArticleToBeUpdated() {
      const updatedArticle = {
        id: this.article.activityId && this.article.targetPageId || this.article.id,
        targetPageId: this.article.targetPageId,
        title: this.article.title,
        body: this.replaceImagesURLs(this.$noteUtils.getContentToSave(this.editorBodyInputRef, this.oembedMinWidth)),
        published: this.article.published,
        activityPosted: this.article.activityPosted,
        audience: this.article.audience,
        properties: this.article?.properties,
        lang: this.article?.lang
      };
      return updatedArticle;
    },
    saveArticleDraft() {
      const properties = this.article?.properties;
      if (properties) {
        properties.draft = true;
      }
      const article = {
        title: this.article.title,
        body: this.replaceImagesURLs(this.$noteUtils.getContentToSave(this.editorBodyInputRef, this.oembedMinWidth)),
        author: this.currentUser,
        published: false,
        spaceId: this.spaceId,
        publicationState: '',
        properties: properties
      };
      if (this.article.id) {
        if (this.article.title || this.article.body) {
          article.id = this.article.id;
          this.$newsServices.updateNews(article, false, this.articleType)
            .then((updatedArticle) => {
              if (this.article.body !== updatedArticle.body) {
                this.imagesURLs = this.extractImagesURLsDiffs(this.article.body, updatedArticle.body);
              }
              this.article.properties = updatedArticle?.properties;
              this.article.draftPage = true;
            })
            .then(() => this.$emit('draftUpdated'))
            .then(() => {
              this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus');
              this.savingDraft = false;
            });
        } else {
          this.$newsServices.deleteDraft(this.article.id)
            .then(() => this.$emit('draftDeleted'))
            .then(() => {
              this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus');
              this.savingDraft = false;
            });
          this.article.id = null;
        }
      } else if (this.article.title || this.article.body) {
        article.publicationState = 'draft';
        this.$newsServices.saveNews(article).then((createdArticle) => {
          this.draftSavingStatus = this.$t('news.composer.draft.savedDraftStatus');
          this.article.id = createdArticle.id;
          this.article.draftPage = true;
          this.article.properties = createdArticle?.properties;
          if (!this.articleId) {
            this.articleId = createdArticle.id;
          }
          this.$emit('draftCreated');
          this.savingDraft = false;
        });
      } else {
        this.draftSavingStatus = '';
      }
    },
    postArticle(schedulePostDate, postArticleMode, publish, isActivityPosted, selectedTargets, selectedAudience) {
      this.article.activityPosted = isActivityPosted;
      this.article.published = publish;
      this.article.targets = selectedTargets;
      if (selectedAudience !== null) {
        this.article.audience = selectedAudience === this.$t('news.composer.stepper.audienceSection.allUsers') ? 'all' : 'space';
      }
      this.doPostArticle(schedulePostDate);
    },
    doPostArticle(schedulePostDate) {
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
        draftPage: this.article.publicationState === 'draft',
        properties: this.article?.properties
      };

      if (schedulePostDate){
        article.publicationState ='staged';
        article.schedulePostDate = schedulePostDate;
        article.timeZoneId = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
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
          this.articleType = 'latest_draft';
          this.fillArticle(createdArticle.id, false, createdArticle.lang || this.selectedLanguage).then(() => {
            this.updateUrl();
            this.initDataPropertiesFromUrl();
          });
          this.displayAlert({
            message: this.$t('news.publish.success.message'),
            type: 'success',
            alertLinkText: this.$t('news.view.label'),
            alertLink: this.isSpaceMember ? `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${createdArticle.activityId}` : `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news-detail?newsId=${createdArticle.id}`
          });
          this.enableClickOnce();
        }).catch(error => {
          this.displayAlert({type: 'error', message: this.$t('news.save.error.message', error.message)});
          this.enableClickOnce();
        }).finally(() => this.draftSavingStatus = '');
      }
    },
    updateUrl(){
      const url = new URL(window.location.href);
      const params = new URLSearchParams(url.search);
      params.delete('newsId');
      params.delete('type');
      if (params.has('activityId')) {
        params.delete('activityId');
      }
      params.append('newsId', this.article?.targetPageId ? this.article?.targetPageId : this.article?.id);
      if (this.article.activityId){
        params.append('activityId', this.article.activityId);
        params.append('type', 'latest_draft');
      } else {
        params.append('type', 'draft');
      }
      if (params.has('lang')) {
        params.delete('lang');
      }
      if (this.article.lang) {
        params.append('lang', this.article.lang);
      }
      window.history.pushState('news', '', `${url.origin}${url.pathname}?${params.toString()}`);

    },
    postAndPublish(editMode, publicationSettings) {
      if (editMode) {
        this.article.activityPosted = publicationSettings?.post;
        this.updateAndPostArticle();
        return;
      }
      this.postingNews = true;
      this.postArticle(null, null, false, publicationSettings?.post);
    },
    postArticleActions(publicationSettings) {
      if (this.newPublicationDrawerEnabled) {
        this.postAndPublish(this.editMode, publicationSettings);
        return;
      }
      if (this.editMode) {
        this.updateAndPostArticle();
        return;
      }
      if (this.canScheduleArticle) {
        this.scheduleMode = 'postScheduledNews';
        this.$root.$emit('open-schedule-drawer', this.scheduleMode);
        this.postKey++;
      } else {
        this.postAndPublish();
      }
    },
    updateArticleData(article) {
      if (this.initDone && this.currentArticleInitDone) {
        this.article.title = article.title;
        this.article.content = article.content;
        this.article.properties = article.properties;
      }
    },
    editorReady(editor) {
      this.editor = editor;
      this.setEditorData(this.article?.content);
      this.currentArticleInitDone = true;
    },
    initEditor() {
      this.$refs.editor.initCKEditor();
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
        this.isSpaceMember = space.isMember;
        this.spaceUrl = this.currentSpace?.prettyName;
        this.$newsServices.canUserCreateNews(this.currentSpace.id).then(canCreateArticle => {
          this.canCreateArticle = canCreateArticle || this.articleId;
          if (this.canCreateArticle) {
            if (this.articleId) {
              this.fillArticle(this.articleId, true, this.selectedLanguage).then(() => {
                this.updateUrl();
                if (!this.article.lang) {
                  this.selectedLanguage = '';
                }
              });
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
        this.$newsServices.canScheduleNews(this.currentSpace.id).then(canScheduleArticle => {
          this.canScheduleArticle = canScheduleArticle;
        });
      });
    },
    fillArticle(articleId, setData, lang) {
      this.initDone = false;
      return this.$newsServices.getNewsById(articleId, true, this.articleType, lang).then(article => {
        if (article === 401) {
          this.unAuthorizedAccess = true;
        } else {
          this.article.id = article?.id;
          this.article.targetPageId = article?.targetPageId;
          this.article.title = article.title;
          this.article.content = this.$noteUtils.getContentToEdit(article.body);
          this.article.body = article.body;
          this.article.published = article.published;
          this.article.spaceId = article.spaceId;
          this.article.publicationState = article.publicationState;
          this.article.draftPage =  article.publicationState === 'draft';
          this.article.activityId = article.activityId;
          this.article.updater = article.updater;
          this.article.draftUpdaterDisplayName = article.draftUpdaterDisplayName;
          this.article.draftUpdaterUserName = article.draftUpdaterUserName;
          this.article.draftUpdateDate = article.draftUpdateDate;
          this.article.activityPosted = article.activityPosted;
          this.article.audience = article.audience;
          this.article.url = article.url;
          this.article.publicationState = article.publicationState;
          this.article.properties = article.properties;
          this.article.lang = article.lang;
          this.originalArticle = structuredClone(this.article);
          if (setData) {
            this.setEditorData(this.article?.content);
          }
        }
      }).finally(() => {
        this.getArticleLanguages();
        this.initDone = true;
      });
    },
    getArticleLanguages(){
      const articleId= this.article.targetPageId ? this.article.targetPageId : this.article.id;
      return this.$newsServices.getArticleLanguages(articleId,true).then(data => {
        this.translations =  data || [];
        if (this.translations.length>0) {
          this.translations = this.allLanguages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
          this.languages = this.allLanguages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
        if (this.isMobile) {
          //TODO
        }
        if (!this.selectedLanguage){
          const lang = this.translations.find(item => item.value === this.selectedLanguage);
          if (lang){
            this.translations=this.translations.filter(item => item.value !== lang.value);
            this.translations.unshift(lang);
          }
        }
      });
    },
    setEditorData(content) {
      this.$refs.editor.setEditorData(content);
    },
    closePluginsDrawer() {
      this.$refs.editor.closePluginsDrawer();
    },
    getAvailableLanguages() {
      return this.$newsServices.getAvailableLanguages().then(data => {
        this.languages = data || [];
        this.languages.sort((a, b) => a.text.localeCompare(b.text));
        this.allLanguages = this.languages;
        this.languages.unshift({value: '', text: this.$t('article.label.chooseLanguage')});
        if (this.translations) {
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
      });
    },
    displayAlert(detail) {
      document.dispatchEvent(new CustomEvent('alert-message-html', {detail: {
        alertType: detail?.type,
        alertMessage: detail?.message,
        alertLinkText: detail?.alertLinkText,
        alertLink: detail?.alertLink
      }}));
    },
    initDataPropertiesFromUrl() {
      this.articleId = this.getURLQueryParam('newsId');
      this.activityId = this.getURLQueryParam('activityId');
      this.articleType = this.getURLQueryParam('type');
      this.spaceId = this.getURLQueryParam('spaceId');
      this.spacePrettyName = this.getURLQueryParam('spaceName');
      this.selectedLanguage = this.getURLQueryParam('lang');
    },
    enableClickOnce() {
      this.postingNews = false;
      this.postKey++;
    },
    getURLQueryParam(paramName) {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has(paramName)) {
        return urlParams.get(paramName);
      }
    },
    isSameArticleContent() {
      return this.$noteUtils.isSameContent(this.article.content, this.originalArticle.content);
    },
    refreshTranslationExtensions() {
      this.editorExtensions = extensionRegistry.loadExtensions('contentEditor', 'translation-extension');
    },
    isEmptyDraft() {
      const isTitleEmpty = !this.article?.title;
      const isContentEmpty = !this.article?.content;
      const isSummaryEmpty = !this.article?.properties || !this.article?.properties?.summary;
      const isFeaturedImageEmpty = !this.article.properties || !this.article?.properties?.featuredImage || this.article?.properties?.featuredImage.id === null || this.article?.properties?.featuredImage?.id <= 0 ;
      return isTitleEmpty && isContentEmpty && isSummaryEmpty && isFeaturedImageEmpty;
    },
  },
};
</script>
