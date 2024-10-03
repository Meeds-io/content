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
  <div v-if="news">
    <div
      v-if="articleNewLayoutEnabled"
      class="newsDetails-description">
      <div
        class="newsDetails-header">
        <v-img
          v-if="illustrationURL"
          :lazy-src="`${illustrationURL}&size=0x400`"
          :alt="featuredImageAltText"
          :src="`${illustrationURL}&size=0x400`"
          contain
          class="mt-5"
          width="100%"
          max-height="400" />
      </div>
      <div class="newsDetails">
        <div class="news-top-information d-flex">
          <p class="text-color font-weight-bold articleTitle text-break mt-5 mb-0">
            <span>
              {{ newsTitle }}
            </span>
            <span class="ms-3">
              <content-translation-menu
                :translations="translations"
                :selected-translation="selectedTranslation"
                :article="news" />
            </span>
          </p>
        </div>
        <p
          v-if="newsSummary"
          class="article-summary text-break text-sub-title mt-4 mb-0">
          {{ newsSummary }}
        </p>
        <div class="mt-4">
          <div
            v-if="!hiddenSpace && !isPublicAccess"
            class="d-flex">
            <exo-space-avatar
              :space-id="spaceId"
              size="30"
              link-style
              avatar />
            <exo-space-avatar
              :space-id="spaceId"
              size="30"
              extra-class="ms-4 fill-height"
              fullname
              popover />
          </div>
          <div>
            <div
              v-if="showUpdaterInfo"
              class="d-flex">
              <exo-user-avatar
                :profile-id="articleUpdater"
                :size="25"
                :class="{'ms-4 mt-n3': !hiddenSpace}"
                avatar />
              <div
                :class="{'mt-n2': !hiddenSpace}"
                class="text-sub-title align-center d-flex">
                <exo-user-avatar
                  :profile-id="articleUpdater"
                  extra-class="ms-2"
                  fullname
                  small-font-size />
                <span class="px-1">-</span>
                <date-format
                  :value="updatedDate"
                  :format="dateFormat"
                  class="text-caption" />
              </div>
            </div>
            <div
              v-else
              :class="{'no-updater-info': !hiddenSpace && !isPublicAccess}"
              class="text-sub-title">
              <date-format
                :value="updatedDate"
                :format="dateFormat"
                class="text-caption" />
            </div>
          </div>
        </div>
        <div
          class="mt-8 rich-editor-content extended-rich-content"
          v-sanitized-html="newsBody">
          <extension-registry-components
            :params="{attachmentsIds: attachmentsIds}"
            name="NewsDetails"
            type="news-details-attachments"
            element="div" />
        </div>
      </div>
    </div>
    <div
      v-else
      class="newsDetails-description">
      <div :class="[illustrationURL ? 'newsDetails-header' : '']" class="newsDetails-header">
        <div v-if="illustrationURL" class="illustration center">
          <img
            :src="`${illustrationURL}&size=0x400`"
            :alt="featuredImageAltText"
            class="newsDetailsImage illustrationPicture"
            longdesc="#newsSummary">
        </div>
        <div class="newsDetails">
          <div class="news-top-information d-flex">
            <div id="titleNews" class="newsTitle newsTitleMobile">
              <a class="activityLinkColor newsTitleLink text-title">{{ newsTitle }}</a>
            </div>
          </div>
          <div class="newsInformationBackground center">
            <div :class="[showUpdateInfo ? 'news-update-details-header' : 'news-details-header']" class="news-header-content  d-inline-flex align-center">
              <div :class="[ showUpdateInfo ? 'newsUpdateInfo' : '']" v-if="currentUser">
                <exo-user-avatar
                  :profile-id="authorProfile"
                  :size="50"
                  class="me-1"
                  popover
                  avatar />
              </div>
              <div id="informationNews" class="newsInformation pa-1">
                <div class="newsPosted d-flex align-center">
                  <exo-user-avatar
                    v-if="currentUser"
                    :profile-id="authorProfile"
                    extra-class="me-1"
                    fullname
                    link-style
                    popover />
                  <span v-if="!hiddenSpace && currentUser"> {{ $t('news.activity.in') }} </span>
                  <exo-space-avatar
                    v-if="!hiddenSpace && currentUser"
                    :space-id="spaceId"
                    fullname
                    extra-class="me-1 ms-n1"
                    link-style
                    popover />
                  <template v-if="publicationDate">
                    <span v-if="currentUser"> - </span>
                    <date-format
                      :value="publicationDate"
                      :format="dateFormat"
                      class="newsInformationValue newsPostedDate news-details-information ms-1" />
                  </template>
                  <div class="mb-1 ml-2">
                    <content-translation-menu
                      :translations="translations"
                      :selected-translation="selectedTranslation"
                      :article="news" />
                  </div>
                </div>
                <div class="newsUpdater text-subtitle">
                  <div v-if="publicationState !== 'staged' && showUpdateInfo">
                    {{ $t('news.activity.lastUpdated') }}
                  </div>
                  <div v-else-if="publicationState === 'staged'">
                    {{ $t('news.details.scheduled') }}
                  </div>
                  <div>
                    <template v-if="publicationState !== 'staged' && updatedDate && showUpdateInfo">
                      <date-format
                        :value="updatedDate"
                        :format="dateFormat"
                        class="newsInformationValue newsUpdatedDate" />
                    </template>
                    <template v-else-if="publicationState === 'staged'">
                      <date-format
                        :value="scheduleDate"
                        :format="dateFormat"
                        class="newsInformationValue newsUpdatedDate" />
                      <span class="newsInformationValue">-</span>
                      <date-format
                        :value="scheduleDate"
                        :format="dateTimeFormat"
                        class="newsInformationValue newsUpdatedDate ml-1 me-1" />
                    </template>
                    <div v-if="notSameUpdater && showUpdateInfo && currentUser" class="text-subtitle">
                      <span> {{ $t('news.activity.by') }} </span>
                      <exo-user-avatar
                        :profile-id="newsUpdater"
                        extra-class="ms-1"
                        fullname
                        link-style
                        popover />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="newsSummary"
            id="newsSummary"
            class="summary text-header center ms-13 me-13">
            <span v-sanitized-html="newsSummary"></span>
          </div>

          <div
            id="newsBody"
            :class="[!summary ? 'fullDetailsBodyNoSummary' : '']"
            class="fullDetailsBody ms-13 me-13 clearfix">
            <div
              class="rich-editor-content extended-rich-content"
              v-html="newsBody"></div>
          </div>
          <extension-registry-components
            :params="{attachmentsIds: attachmentsIds}"
            name="NewsDetails"
            type="news-details-attachments"
            element="div" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: null
    },
    currentUser: {
      type: String,
      required: false,
      default: null
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
  data: () => ({
    dateFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    },
    dateTimeFormat: {
      hour: '2-digit',
      minute: '2-digit',
    },
    newsTitleContent: null,
    newsSummaryContent: null,
    newsBodyContent: null,
  }),
  created() {
    this.setNewsTitle(this.news?.title);
    this.setNewsSummary(this.news?.properties?.summary);
    this.setNewsContent(this.news?.body);
    this.$root.$on('update-news-title', this.setNewsTitle);
    this.$root.$on('update-news-summary', this.setNewsSummary);
    this.$root.$on('update-news-body', this.setNewsContent);
  },
  computed: {
    articleNewLayoutEnabled() {
      return eXo?.env?.portal?.articleNewLayoutEnabled;
    },
    showUpdaterInfo() {
      return !this.isPublicAccess;
    },
    isPublicAccess() {
      return !eXo?.env?.portal?.userIdentityId;
    },
    params() {
      return {
        news: this.news,
      };
    },
    illustrationURL() {
      return this?.news.illustrationURL;
    },
    featuredImageAltText() {
      return this.news?.properties?.featuredImage?.altText || this.newsTitle;
    },
    newsTitle() {
      return this.news && this.newsTitleContent;
    },
    showUpdateInfo() {
      return this.news && this.news.updateDate && this.news.updater !== '__system' && this.news.updateDate !== 'null' && this.news.publicationDate && this.news.publicationDate !== 'null' && new Date(this.news.updateDate).getTime() > new Date(this.news.publicationDate).getTime();
    },
    authorProfile() {
      return this.news && this.news.author;
    },
    articleUpdater() {
      return this.news?.updater || this.news?.author;
    },
    hiddenSpace() {
      return this.news && this.news.hiddenSpace;
    },
    newsBody() {
      return this.news && this.newsBodyContent;
    },
    updaterFullName() {
      return this.news && this.news.updaterFullName;
    },
    updaterProfileURL() {
      return this.news && `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/profile/${this.news.updater}`;
    },
    newsUpdater() {
      return this.news && this.news.updater;
    },
    publicationDate() {
      return this.news?.publicationDate && new Date(this.news.publicationDate);
    },
    updatedDate() {
      return this.news?.updateDate && new Date(this.news.updateDate);
    },
    newsSummary() {
      return this.news && this.newsSummaryContent;
    },
    spaceId() {
      return this.news && this.news.spaceId;
    },
    postedDate() {
      return this.news && this.news.postedDate;
    },
    summary() {
      return this.news?.properties?.summary;
    },
    attachmentsIds() {
      return this.news?.attachmentsIds;
    },
    publicationState() {
      return this.news && this.news.publicationState;
    },
    notSameUpdater() {
      return this.news && this.news.updater !== this.news.author;
    },
    scheduleDate() {
      return this.news && this.news.schedulePostDate;
    },
  },
  methods: {
    setNewsTitle(title) {
      this.newsTitleContent = title;
    },
    setNewsSummary(content) {
      this.newsSummaryContent = content;
    },
    setNewsContent(translation) {
      this.newsBodyContent = translation;
    },
  }
};
</script>
