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
  <v-app class="newsApp" role="main">
    <div class="application-body pa-5">
      <v-toolbar
        color="transparent"
        flat
        dense>
        <div class="flex d-flex flex-row">
          <v-spacer />
          <div
            class="d-flex flex-row justify-end my-auto flex-nowrap">
            <div :class="searchInputDisplayed ? '' : 'newsAppHideSearchInput'">
              <div class="inputNewsSearchWrapper">
                <v-scale-transition>
                  <v-text-field
                    v-model="searchText"
                    :placeholder="$t('news.app.searchPlaceholder')"
                    prepend-inner-icon="fa-filter"
                    class="pa-0 my-auto" />
                </v-scale-transition>
              </div>
            </div>
          </div>
          <div
            class="d-flex flex-row justify-end my-auto flex-nowrap">
            <select
              v-model="newsFilter"
              class="width-auto my-auto ms-4 subtitle-1 ignore-vuetify-classes">
              <option
                v-for="(option, index) in filterOptions"
                :key="index"
                :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <div class="d-flex align-center">
              <v-btn
                icon
                class="d-flex flex-row my-auto flex-nowrap primary--text ms-2"
                @click="$root.$emit('news-space-selector-drawer-open')">
                <i
                  :class="spacesFilter && spacesFilter.length && !spacesFilter.includes('-1') && 'primary--text' || 'text-color'"
                  class="fa fa-sliders-h uiIcon24x24"></i>
              </v-btn>
              <span
                v-if="spacesFilter && spacesFilter.length && !spacesFilter.includes('-1')"
                class="primary--text">
                ({{ spacesFilter.length }})
              </span>
            </div>
          </div>
        </div>
      </v-toolbar>
      <div class="newsAppFilterOptions">
        <news-filter-space-drawer
          v-model="spacesFilter" />
      </div>
      <div>
        <v-progress-circular
          v-if="loadingNews && newsList.length === 0"
          :size="40"
          :width="4"
          indeterminate
          class="loadingRing" />
      </div>
      <div
        v-if="newsList.length"
        id="newsListItems"
        class="newsListItems">
        <news-app-item
          v-for="news in newsList"
          :key="news.newsId"
          :news="news"
          :news-filter="newsFilter"
          class="newsItem"
          @update-news-list="updateNewsList"
          @delete-news="deleteNews"
          @open-delete-confirm-dialog="deleteConfirmDialog" />
      </div>
      <div v-if="newsList.length === 0 && !loadingNews" class="articleNotFound">
        <span class="iconNotFound"></span>
        <h3>{{ notFoundMessage }}</h3>
      </div>
      <div v-if="showLoadMoreButton" class="newsListPagination">
        <v-btn
          class="btn full-width"
          @click="loadMore">
          {{ $t('news.app.loadMore') }}
        </v-btn>
      </div>
      <exo-confirm-dialog
        ref="deleteConfirmDialog"
        :message="confirmDeleteNewsDialogMessage"
        :title="confirmDeleteNewsDialogTitle"
        :ok-label="$t('news.button.ok')"
        :cancel-label="$t('news.button.cancel')"
        @ok="deleteByConfirm" />
      <news-activity-sharing-spaces-drawer />
      <activity-share-drawer />
      <news-mobile-action-menu
        ref="mobileActionMenu"
        @edit-article="editLink"
        @delete-article="deleteConfirmDialog" />
    </div>
  </v-app>
</template>

<script>
export default {
  name: 'NewsApp',
  data() {
    return {
      newsList: [],
      newsPerPage: 10,
      showLoadMoreButton: false,
      errors: [],
      showDropDown: false,
      NEWS_TEXT_MAX_LENGTH: 300,
      searchText: '',
      searchNews: '',
      searchDelay: 300,
      searchInputDisplayed: false,
      newsFilter: '',
      spacesFilter: [],
      newsStatusLabel: this.$t('news.app.filter.all'),
      showShareButton: true,
      loadingNews: true,
      initialized: false,
      dateTimeFormat: {
        hour: '2-digit',
        minute: '2-digit',
      },
      dateFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      },
      newsScheduleAndFilterDisplaying: false,
      newsToDelete: null
    };
  },
  computed: {
    filterOptions() {
      return [
        { value: 'all', label: this.$t('news.app.filter.all') },
        { value: 'pinned', label: this.$t('news.app.filter.pinned') },
        { value: 'myPosted', label: this.$t('news.app.filter.myPosted') },
        { value: 'drafts', label: this.$t('news.app.filter.drafts') },
        { value: 'scheduled', label: this.$t('news.app.filter.scheduled') }
      ];
    },
    notFoundMessage() {
      if (this.searchText.trim().length) {
        return this.$t('news.app.searchNotFound').replace('{0}', this.searchText);
      } else {
        return this.$t('news.app.noNews');
      }
    },
    isDraftsFilter() {
      return this.newsFilter === 'drafts';
    },
    confirmDeleteNewsDialogMessage() {
      return this.isDraftsFilter ? this.$t('news.message.confirmDeleteDraftNews') : this.$t('news.message.confirmDeleteNews');
    },
    confirmDeleteNewsDialogTitle() {
      return this.isDraftsFilter ? this.$t('news.title.confirmDeleteDraftNews') : this.$t('news.title.confirmDeleteNews');
    },
  },
  watch: {
    searchText() {
      if (this.searchText && this.searchText.trim().length) {
        clearTimeout(this.searchNews);
        this.searchNews = setTimeout(() => {
          this.fetchNews(false);
        }, this.searchDelay);
      } else {
        this.fetchNews(false);
      }
    },
    newsFilter() {
      window.history.pushState('', 'News' , this.setQueryParam('filter', this.newsFilter));

      this.newsStatusLabel = this.$t(`news.app.filter.${this.newsFilter}`);

      this.fetchNews(false);
    },
    loadingNews() {
      if (this.loadingNews) {
        document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      } else {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      }
    },
    initialized() {
      if (this.initialized) {
        this.$root.$emit('application-loaded');
      }
    },
    spacesFilter: {
      handler: function () {
        this.fetchNews(false);
        if (this.spacesFilter === false) {
          this.spacesFilter = ['-1'];
        }
        if (this.spacesFilter.length > 0) {
          window.history.pushState('', 'News', this.setQueryParam('spaces', this.spacesFilter.toString().replace(/,/g, '_')));
        } else {
          window.history.pushState('', 'News', this.removeQueryParam('spaces'));
        }

      },
      deep: true
    }
  },
  created() {
    const filterQueryParam = this.getQueryParam('filter');
    const searchQueryParam = this.getQueryParam('search');
    const spacesFilterParam = this.getQueryParam('spaces')?.split('_');
    if (filterQueryParam || searchQueryParam || spacesFilterParam) {
      if (filterQueryParam) {
        // set filter value, which will trigger news fetching
        this.newsFilter = filterQueryParam;
      }
      if (searchQueryParam) {
        // set search value
        this.searchText = searchQueryParam;
      }
      if (spacesFilterParam) {
        // set search value
        this.spacesFilter = spacesFilterParam;
      }

    } else if (filterQueryParam === null) {
      this.newsFilter = this.filterOptions[0].value;
    } else {
      this.fetchNews();
    }

    this.$root.$on('activity-shared', (activityId, spaces, selectedApps) => {
      if (selectedApps === 'newsApp' && activityId && spaces && spaces.length > 0) {
        const spacesList = spaces.map(space => space.displayName);
        const message = `${this.$t('news.share.message')} ${spacesList.join(', ')}`;
        document.dispatchEvent(new CustomEvent('alert-message', {detail: {
          alertType: 'success',
          alertMessage: message ,
        }}));
      }
      this.fetchNews(false);
    });
  },
  methods: {
    editLink(news) {
      const editUrl = this.getEditUrl(news);
      window.open(editUrl, '_blank');
      this.$refs?.mobileActionMenu?.close();
    },
    deleteByConfirm() {
      this.deleteNews(this.newsToDelete);
    },
    deleteConfirmDialog(news) {
      this.newsToDelete = news;
      this.$refs.deleteConfirmDialog.open();
      this.$refs.mobileActionMenu.close();
    },
    getEditUrl(news) {
      let editUrl = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news/editor?newsId=${news.targetPageId || news.newsId}`;
      if (news.spaceId) {
        editUrl += `&spaceId=${news.spaceId}`;
      }
      if (news.activityId) {
        editUrl += `&activityId=${news.activityId}`;
      }
      if (news.spaceUrl) {
        editUrl += `&spaceName=${news.spaceUrl.substring(news.spaceUrl.lastIndexOf('/') + 1)}`;
      }
      editUrl += `&type=${news.activityId && 'latest_draft' || 'draft'}`;
      if (news.lang) {
        editUrl += `&lang=${news.lang}`;
      }
      return editUrl;
    },
    getDraftUrl(item) {
      let draftUrl = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news/editor`;
      draftUrl += `?newsId=${item.activityId && item.targetPageId || item.id}`;
      if (item.spaceId) {
        draftUrl += `&spaceId=${item.spaceId}`;
      }
      if (item.activityId) {
        draftUrl += `&activityId=${item.activityId}`;
      }
      if (item.spaceUrl) {
        draftUrl += `&spaceName=${item.spaceUrl.substring(item.spaceUrl.lastIndexOf('/') + 1)}`;
      }
      draftUrl += `&type=${item.activityId && 'latest_draft' || 'draft'}`;
      return draftUrl;
    },
    getNewsText(newsSummary, newsBody) {
      let text = newsSummary;
      if (!text) {
        text = newsBody;
      }

      text = text.length > this.NEWS_TEXT_MAX_LENGTH ? `${text.substring(0, this.NEWS_TEXT_MAX_LENGTH)}...` : text;

      const div = document.createElement('div');
      div.innerHTML = text;
      text = div.textContent || div.innerText || '';

      return text;
    },
    updateNewsList(data, append = true) {
      const result = [];

      data.forEach((item) => {
        const newsPublicationDate = item.publicationDate != null ? new Date(item.publicationDate) : null;
        const newsUpdateDate = new Date(item.updateDate);
        result.push({
          newsId: item.id,
          newsText: this.getNewsText(item?.properties?.summary, item.body),
          illustrationURL: item.illustrationURL,
          title: item.title,
          updatedDate: this.isDraftsFilter ? newsPublicationDate : newsUpdateDate,
          spaceDisplayName: item.spaceDisplayName,
          spaceUrl: item.spaceUrl,
          url: this.isDraftsFilter ? this.getDraftUrl(item) : item.url,
          authorFullName: item.authorDisplayName,
          authorProfileURL: `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/profile/${item.author}`,
          viewsCount: item.viewsCount == null ? 0 : item.viewsCount,
          activityId: item.activityId,
          canEdit: item.canEdit,
          canDelete: item.canDelete,
          draft: item.publicationState === 'draft',
          scheduled: item.publicationState === 'staged',
          schedulePostDate: item.schedulePostDate,
          published: item.published,
          activities: item.activities,
          authorAvatarUrl: item.authorAvatarUrl,
          spaceAvatarUrl: item.spaceAvatarUrl,
          hiddenSpace: item.hiddenSpace,
          spaceId: item.spaceId,
          target: this.newsFilter === 'drafts' ? '_blank' : '_self',
          type: this.newsFilter === 'drafts' ? 'draft' : 'article'
        });
      });
      if (append) {
        this.newsList = this.newsList.concat(result);
      } else {
        this.newsList = result;
      }
    },
    fetchNews(append = true) {
      this.loadingNews = true;
      const searchTerm = this.searchText.trim().toLowerCase();
      const offset = append ? this.newsList.length : 0;
      return this.$newsServices.getNews(this.newsFilter, this.spacesFilter, searchTerm, offset, this.newsPerPage + 1, false).then(data => {
        if (data.news && data.news.length) {
          if (data.news.length > this.newsPerPage) {
            this.showLoadMoreButton = true;
            data.news.pop();
            this.updateNewsList(data.news, append);
          } else {
            this.showLoadMoreButton = false;
            this.updateNewsList(data.news, append);
          }
        } else if (!append) {
          this.showLoadMoreButton = false;
          this.newsList = [];
        }
        if (searchTerm){
          window.history.pushState('', 'News', this.setQueryParam('search', this.searchText));
        } else {
          window.history.pushState('', 'News', this.removeQueryParam('search'));
        }
        return this.$nextTick();
      }).catch(() => this.loadingNews = false)
        .finally(() => {
          this.loadingNews = false;
          this.initialized = true;
        });
    },
    deleteNews(news) {
      const deleteDelay = 6;
      const redirectionTime = 8100;
      let newsObjectType;
      if (this.newsFilter === 'drafts') {
        if (news.activityId) {
          newsObjectType = 'latest_draft';
        } else {
          newsObjectType = 'draft';
        }
      } else  {
        newsObjectType = 'article';
      }
      this.$newsServices.deleteNews(news.newsId, newsObjectType, deleteDelay)
        .then(() => {
          const clickMessage = this.$t('news.details.undoDelete');
          const message = this.isDraftsFilter ? this.$t('news.details.deleteDraftSuccess') : this.$t('news.details.deleteSuccess');
          document.dispatchEvent(new CustomEvent('alert-message', {detail: {
            alertType: 'success',
            alertMessage: message ,
            alertLinkText: clickMessage ,
            alertLinkCallback: () => this.undoDeleteNews(news.newsId, this.isDraftsFilter),
          }}));
        });
      setTimeout(() => {
        const deletedNews = localStorage.getItem('deletedNews');
        if (deletedNews != null) {
          this.fetchNews(false);
        }
      }, redirectionTime);
    },
    loadMore: function() {
      this.fetchNews();
    },
    getQueryParam(paramName) {
      const uri = window.location.search.substring(1);
      const params = new URLSearchParams(uri);
      return params.get(paramName);
    },
    setQueryParam(paramName, paramValue) {
      const url = new URL(window.location);
      url.searchParams.set(paramName, paramValue);
      return url.href;
    },
    removeQueryParam(paramName) {
      const url = new URL(window.location);
      url.searchParams.delete(paramName);
      return url.href;
    },
    undoDeleteNews(newsId, isDraftsFilter) {
      return this.$newsServices.undoDeleteNews(newsId)
        .then(() => {
          this.$root.$emit('close-alert-message');
          const message = isDraftsFilter ? this.$t('news.details.deleteDraftCanceled') : this.$t('news.details.deleteCanceled');
          document.dispatchEvent(new CustomEvent('alert-message', {detail: {
            alertType: 'success',
            alertMessage: message
          }}));
        });
    }
  },
};
</script>
