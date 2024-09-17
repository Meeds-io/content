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
  <div id="newsDetails">
    <div v-if="isMobile" class="application-body">
      <exo-news-details-toolbar-mobile
        :news="news"
        :show-edit-button="showEditButton"
        :show-delete-button="showDeleteButton"
        :show-publish-button="showPublishButton" />
      <exo-news-details-body-mobile
        :news="news"
        :news-id="newsId"
        :space="currentSpace" />
    </div>
    <div v-else class="application-body">
      <exo-news-details-toolbar
        :news="news"
        :news-id="newsId"
        :current-user="currentUser"
        :activity-id="activityId"
        :show-edit-button="showEditButton"
        :show-delete-button="showDeleteButton"
        :show-publish-button="showPublishButton"
        @delete-article="deleteConfirmDialog"
        @edit-article="editLink" />
      <exo-news-details-body
        :current-user="currentUser"
        :news="news"
        :translations="translations"
        :selected-translation="selectedTranslation" />
    </div>
    <schedule-news-drawer
      v-if="currentUser"
      @post-article="postNews"
      :news-id="newsId"
      :news-type="processedNewsType" />
    <exo-confirm-dialog
      v-if="currentUser"
      ref="deleteConfirmDialog"
      :message="$t('news.message.confirmDeleteNews')"
      :title="$t('news.title.confirmDeleteNews')"
      :ok-label="$t('news.button.ok')"
      :cancel-label="$t('news.button.cancel')"
      @ok="deleteNews" />
    <exo-news-edit-publishing-drawer
      v-if="news && currentUser"
      :news="news"
      @refresh-news="getNewsById(newsId)" />
  </div>
</template>
<script>

const USER_TIMEZONE_ID = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
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
      iframelyOriginRegex: /^https?:\/\/if-cdn.com/
    };
  },
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.name === 'xs' || this.$vuetify.breakpoint.name === 'sm';
    },
    processedNewsType() {
      return this.activityId && this.activityId !== '' ? this.$newsConstants.newsObjectType.ARTICLE : this.newsType;
    }
  },
  created() {
    if (!this.news || !this.news.spaceId) {
      this.getNewsById(this.newsId);
    } else {
      this.spaceId = this.news.spaceId;
      this.getSpaceById(this.spaceId );
      if (!this.news.newsId) {
        this.news.newsId = this.newsId;
      }
      this.$root.$emit('application-loaded');
    }
    window.addEventListener('message', (event) => {
      if (this.iframelyOriginRegex.exec(event.origin)) {
        const data = JSON.parse(event.data);
        if (data.method === 'open-href') {
          window.open(data.href, '_blank');
        }
      }
    });
  },
  mounted() {
    this.markNewsAsRead(this.newsId);
  },
  methods: {
    markNewsAsRead(newsId) {
      if (newsId) {
        this.$newsServices.markNewsAsRead(newsId);
      }
    },
    getSpaceById(spaceId) {
      this.$spaceService.getSpaceById(spaceId, 'identity')
        .then((space) => {
          if (space && space.identity && space.identity.id) {
            this.currentSpace = space;
          }
        }).catch(error => {
          console.warn(`Could not get the space with id ${spaceId} : ${error}`);
        });
    },
    editLink() {
      const newsType = this.activityId && this.activityId !== '' ? this.$newsConstants.newsObjectType.LATEST_DRAFT : this.newsType;
      let editUrl = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news/editor?spaceId=${this.spaceId}&newsId=${this.newsId}&activityId=${this.activityId}&spaceName=${this.currentSpace.prettyName}&type=${newsType}`;
      if (this.news.lang) {
        editUrl = `${editUrl}&lang=${this.news.lang}`;
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
          this.$root.$emit('confirm-news-deletion', this.news);
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
          window.location.href = this.news.spaceUrl;
        }
      }, redirectionTime);
    },

    postNews(schedulePostDate, postArticleMode, publish, isActivityPosted, selectedTargets, selectedAudience) {
      this.news.timeZoneId = USER_TIMEZONE_ID;
      this.news.activityPosted = isActivityPosted;
      this.news.published = publish;
      this.news.targets = selectedTargets;
      if (selectedAudience !== null) {
        this.news.audience = selectedAudience === this.$t('news.composer.stepper.audienceSection.allUsers') ? 'all' : 'space';
      }
      if (postArticleMode === 'later') {
        this.news.schedulePostDate = schedulePostDate;
        this.$newsServices.scheduleNews(this.news, this.newsType).then((scheduleNews) => {
          if (scheduleNews) {
            window.location.href = scheduleNews.url;
          }
        });
      } else if (postArticleMode === 'immediate') {
        this.news.publicationState = 'posted';
        this.$newsServices.saveNews(this.news).then((createdNews) => {
          let createdNewsActivity = null;
          if (createdNews.activities) {
            const createdNewsActivities = createdNews.activities.split(';')[0].split(':');
            if (createdNewsActivities.length > 1) {
              createdNewsActivity = createdNewsActivities[1];
            }
          }
          if (createdNewsActivity) {
            window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${createdNewsActivity}`;
          } else {
            window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}`;
          }
        });
      } else {
        this.news.publicationState = 'draft';
        this.$newsServices.saveNews(this.news).then((createdNews) => {
          this.news.id = createdNews.id;
          this.$emit('draftCreated');
          if (createdNews) {
            window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news?filter=drafts`;
          }
        });
      }
    },
    getNewsById(newsId) {
      this.$newsServices.getNewsById(newsId, false, this.processedNewsType, this.selectedTranslation.value)
        .then(news => {
          this.spaceId = news.spaceId;
          this.getSpaceById(this.spaceId);
          if (!this.news) {
            this.news = news;
          }
          if (!this.news.newsId) {
            this.news.newsId = newsId;
          }
          return this.$nextTick();
        })
        .finally(() => {
          document.title = this.$root.$t('news.window.title', {0: this.news.title});
          this.$root.$emit('application-loaded');
        });
    },
    undoDeleteNews() {
      return this.$newsServices.undoDeleteNews(this.newsId)
        .then(() => {
          const message = this.$t('news.details.deleteCanceled');
          this.$root.$emit('alert-message', message, 'success');
        });
    }
  }
};
</script>
