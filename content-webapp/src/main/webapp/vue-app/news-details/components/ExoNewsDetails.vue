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
    id="newsDetails"
    class="pa-5">
    <div
      class="application-body">
      <exo-news-details-toolbar
        :news="localNews"
        :news-id="newsId"
        :current-user="currentUser"
        :activity-id="activityId"
        :show-edit-button="showEditButton"
        :show-delete-button="showDeleteButton"
        :show-publish-button="showPublishButton"
        :show-copy-link-button="showCopyLinkButton"
        :show-refer-button="showReferButton"
        :article-page-url="articlePage?.url"
        @delete-article="deleteConfirmDialog"
        @edit-article="editLink"
        @open-publication-drawer="openPublicationDrawer" />
      <exo-news-details-body
        :current-user="currentUser"
        :news="localNews"
        :translations="translations"
        :selected-translation="selectedTranslation" />
    </div>
    <exo-confirm-dialog
      v-if="currentUser"
      ref="deleteConfirmDialog"
      :message="$t('news.message.confirmDeleteNews')"
      :title="$t('news.title.confirmDeleteNews')"
      :ok-label="$t('news.button.ok')"
      :cancel-label="$t('news.button.cancel')"
      @ok="deleteNews" />
    <note-publication-drawer
      ref="publicationDrawer"
      :is-publishing="isPublishing"
      :params="{
        spaceId: spaceId,
        allowedTargets: allowedTargets,
        canPublish: localNews?.canPublish,
        canSchedule: localNews?.canSchedule
      }"
      :edit-mode="true"
      @publish="publishArticle" />
    <note-publication-target-drawer />
    <news-mobile-action-menu
      :news="localNews"
      @edit-article="editLink"
      @delete-article="deleteConfirmDialog" />
    <note-treeview-drawer
      :settings="{
        saveButtonLabel: $t('content.article.refer.label'),
        drawerTitle: $t('content.article.refer.to.note'),
        showCurrentDestination: false,
        spaceDisplayName: currentSpace?.displayName
      }"
      ref="noteTreeview" />
  </div>
</template>
<script>

const USER_TIMEZONE_ID = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: () => {
        return {};
      }
    },
    newsId: {
      type: String,
      required: false,
      default: null
    },
    activityId: {
      type: String,
      required: false,
      default: ''
    },
    newsType: {
      type: String,
      required: false,
      default: ''
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
    translations: {
      type: Array,
      default: () => {
        return [];
      }
    },
    selectedTranslation: {
      type: Object,
      default: () => {
        return {};
      }
    }
  },
  data() {
    return {
      localNews: null,
      currentSpace: null,
      currentUser: `${eXo.env.portal.userName}`,
      spaceId: null,
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
      iframelyOriginRegex: /^https?:\/\/if-cdn.com/,
      isPublishing: false,
      allowedTargets: [],
      articlePage: null
    };
  },
  computed: {
    processedNewsType() {
      return this.activityId && this.activityId !== '' ? this.$newsConstants.newsObjectType.ARTICLE : this.newsType;
    },
    scheduled() {
      return !!this.localNews?.schedulePostDate || this.staged;
    },
    staged() {
      return this.localNews?.publicationState === 'staged';
    }
  },
  created() {
    this.localNews = { ...this.news };
    this.getAllowedTargets();
    if (!this.news || !this.news.spaceId) {
      this.getNewsById(this.newsId);
    } else {
      this.spaceId = this.news.spaceId;
      this.getSpaceById(this.spaceId );
      if (!this.localNews.newsId) {
        this.localNews.newsId = this.newsId;
      }
      this.$root.$emit('application-loaded');
    }
    this.getArticlePage();
    window.addEventListener('message', (event) => {
      if (this.iframelyOriginRegex.exec(event.origin)) {
        const data = JSON.parse(event.data);
        if (data.method === 'open-href') {
          window.open(data.href, '_blank');
        }
      }
    });
    this.$root.$on('open-edit-publishing-drawer', this.openPublicationDrawer);
    this.$root.$on('refer-article-to-note', this.referArticle);
    this.$root.$on('move-page', this.moveArticlePage);
    this.bindTreeViewNavigationClickListener();
  },
  mounted() {
    const urlHash = window.location.hash;
    if (urlHash) {
      const elementId = urlHash.substring(1);
      const targetElement = document.getElementById(elementId);
      if (targetElement) {
        targetElement.scrollIntoView({behavior: 'smooth'});
      }
    }
  },
  watch: {
    news() {
      this.localNews = { ...this.news };
    }
  },
  methods: {
    moveArticlePage(page, newParentPage) {
      const previousParentPageId = page.parentPageId;
      page.parentPageId = newParentPage.id;
      this.localNews.referred = true;
      this.$refs.noteTreeview.isLoading = true;
      return this.$newsServices.updateNews(this.localNews, false, this.$newsConstants.newsObjectType.ARTICLE,
        this.$newsConstants.newsUpdateType.PAGE_REFERENCE).then(() => {
        return this.$newsServices.moveArticlePage(page, newParentPage).then(() => {
          this.localNews.deReferPageId = previousParentPageId;
          this.$refs.noteTreeview.isLoading = true;
          this.$refs.noteTreeview.close();
          this.displayMessage({
            message: this.$t('content.article.referred.success'),
            type: 'success',
            linkText: this.$t('notes.view.label'),
            alertLink: this.articlePage.url
          });
        });
      }).catch(() => {
        this.displayMessage({
          message: this.$t('content.article.referred.error'),
          type: 'error'});
      });
    },
    referArticle() {
      if (this.localNews.referred) {
        return this.$newsServices.getArticlePage(this.localNews.deReferPageId).then((deReferPage) => {
          this.articlePage.parentPageId = deReferPage.id;
          return this.$newsServices.moveArticlePage(this.articlePage, deReferPage).then(() => {
            this.localNews.referred = false;
            return this.$newsServices.updateNews(this.localNews, false, this.$newsConstants.newsObjectType.ARTICLE,
              this.$newsConstants.newsUpdateType.PAGE_REFERENCE).then(() => {
              this.displayMessage({
                message: this.$t('content.article.deReferred.success'),
                type: 'success'});
            });
          });
        });
      } else {
        this.$refs.noteTreeview.open(this.articlePage, 'movePage');
      }
    },
    openPublicationDrawer() {
      this.$refs?.publicationDrawer?.open(this.localNews);
    },
    getArticlePage() {
      return this.$newsServices.getArticlePage(this.localNews?.id || this.newsId).then((page) => {
        this.articlePage = page;
      });
    },
    getSpaceById(spaceId) {
      return this.$spaceService.getSpaceById(spaceId, 'identity')
        .then((space) => {
          if (space && space.identity && space.identity.id) {
            this.currentSpace = space;
          }
        }).catch(error => {
          console.warn(`Could not get the space with id ${spaceId} : ${error}`);
        });
    },
    editLink() {
      const newsType = this.activityId || this.scheduled ? this.$newsConstants.newsObjectType.LATEST_DRAFT : this.newsType;
      let editUrl = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news-editor?spaceId=${this.spaceId}&newsId=${this.newsId}&activityId=${this.activityId}&spaceName=${this.currentSpace.prettyName}&type=${newsType}`;
      if (this.localNews.lang) {
        editUrl = `${editUrl}&lang=${this.localNews.lang}`;
      }
      window.open(editUrl, '_target');
    },
    deleteConfirmDialog() {
      this.$refs.deleteConfirmDialog.open();
    },
    deleteNews() {
      const deleteDelay = 6;
      const redirectionTime = 6100;
      this.$newsServices.deleteNews(this.newsId, this.$newsConstants.newsObjectType.ARTICLE, deleteDelay)
        .then(() => {
          this.$root.$emit('confirm-news-deletion', this.localNews);
          const clickMessage = this.$t('news.details.undoDelete');
          const message = this.$t('news.details.deleteSuccess');
          document.dispatchEvent(new CustomEvent('alert-message', {detail: {
            alertType: 'success',
            alertMessage: message ,
            alertLinkText: clickMessage ,
            alertLinkCallback: () => this.undoDeleteNews(),
          }}));
        });
      setTimeout(() => {
        const deletedNews = localStorage.getItem('deletedNews');
        if (deletedNews != null) {
          window.location.href = this.localNews.spaceUrl;
        }
      }, redirectionTime);
    },
    getAllowedTargets() {
      this.$newsTargetingService.getAllowedTargets()
        .then(targets => {
          this.allowedTargets = targets.map(target => ({
            name: target.name,
            label: target?.properties?.label,
            tooltipInfo: `${target?.properties?.label}: ${target?.properties?.description || ''}`,
            description: target?.properties?.description,
            restrictedAudience: target?.restrictedAudience,
          }));
        });
    },
    publish(editScheduleAction, scheduleSettings) {
      if (editScheduleAction === 'schedule') {
        this.localNews.publicationState = scheduleSettings?.postDate && 'staged' || '';
        return this.$newsServices.scheduleNews(this.localNews, this.$newsConstants.newsObjectType.ARTICLE);
      } else if (editScheduleAction === 'publish_now') {
        this.localNews.schedulePostDate = 0;
        this.localNews.publicationState = 'posted';
        return this.$newsServices.saveNews(this.localNews);
      } else {
        this.localNews.publicationState = 'posted';
        return this.$newsServices.updateNews(this.localNews, this.localNews.activityPosted,
          this.$newsConstants.newsObjectType.ARTICLE, this.$newsConstants.newsUpdateType.POSTING_AND_PUBLISHING);
      }
    },
    redirectToDrafts() {
      window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news?filter=drafts`;
    },
    publishArticle(publicationSettings, content, extensionsCallback) {
      this.isPublishing = true;
      this.localNews.isPublishing = true;
      this.localNews.activityPosted = publicationSettings?.post;
      this.localNews.published = publicationSettings?.publish;
      this.localNews.targets = publicationSettings?.selectedTargets;
      this.localNews.categories = publicationSettings?.selectedCategoryIds;
      this.localNews.audience = publicationSettings?.selectedAudience;
      const scheduleSettings = publicationSettings?.scheduleSettings;
      const editScheduleAction = scheduleSettings?.editScheduleAction;
      this.localNews.timeZoneId = USER_TIMEZONE_ID;
      this.localNews.schedulePostDate = scheduleSettings?.postDate;
      this.localNews.scheduleUnpublishDate = scheduleSettings?.unpublishDate;
      if (editScheduleAction === 'cancel_schedule') {
        this.localNews.schedulePostDate = 0;
        this.localNews.publicationState = 'draft';
        this.$newsServices.saveNews(this.localNews).then((createdNews) => {
          this.localNews.id = createdNews.id;
          this.$emit('draftCreated');
          this.redirectToDrafts();
        });
      } else {
        const successMessage = this.$t('notes.publication.settings.update.success');
        const errorMessage = this.$t('notes.publication.settings.update.error');

        this.publish(editScheduleAction, scheduleSettings).then(async (article) => {
          this.displayMessage({message: successMessage, type: 'success'});
          history.replaceState({}, article.url);
          this.localNews = { ...article };
          await this.executePublishExtensions(extensionsCallback, article.id);
        }).catch(() => {
          this.displayMessage({message: errorMessage, type: 'error'});
        }).finally(() => {
          this.$refs?.publicationDrawer?.close?.();
          this.isPublishing = false;
        });
      }
    },
    async executePublishExtensions(extensionsCallback, articleId) {
      const metadata = await extensionsCallback.executeExtensions();
      const parameters = await this.$newsServices.updateArticleMetadataProperties(articleId, metadata);
      const cleanParameters = Object.fromEntries(
        Object.entries(parameters).filter(([, v]) => v != null)
      );

      this.localNews = {
        ...this.localNews,
        parameters: cleanParameters
      };
    },
    getNewsById(newsId) {
      this.$newsServices.getNewsById(newsId, false, this.processedNewsType, this.selectedTranslation.value)
        .then(news => {
          this.spaceId = news.spaceId;
          this.getSpaceById(this.spaceId);
          this.localNews = { ...news, newsId: news.newsId || newsId };
          return this.$nextTick();
        })
        .finally(() => {
          document.title = this.$root.$t('news.window.title', {0: this.localNews.title});
          this.$root.$emit('application-loaded');
        });
    },
    undoDeleteNews() {
      return this.$newsServices.undoDeleteNews(this.newsId)
        .then(() => {
          const message = this.$t('news.details.deleteCanceled');
          this.$root.$emit('alert-message', message, 'success');
        });
    },
    displayMessage(message) {
      setTimeout(() => {
        document.dispatchEvent(new CustomEvent('alert-message-html', {
          detail: {
            alertMessage: message?.message,
            alertType: message?.type,
            alertLinkText: message?.linkText,
            alertLinkCallback: message?.linkCallback,
            alertLink: message.alertLink
          }
        }));
      }, 200);
    },
    bindTreeViewNavigationClickListener() {
      const self = this;
      document.addEventListener('click', function (event) {
        if (event.target.closest('.image-navigation')) {
          window.location.href = self.articlePage.url;
        }
      });
    },
  }
};
</script>
