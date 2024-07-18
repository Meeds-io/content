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
    <div class="newsDetails-description">
      <div :class="[illustrationURL ? 'newsDetails-header' : '']" class="newsDetails-header">
        <div v-if="illustrationURL" class="illustration center">
          <img
            :src="`${illustrationURL}&size=0x400`"
            class="newsDetailsImage illustrationPicture"
            :alt="featuredImageAltText"
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
                  <extension-registry-component
                    v-if="translateExtension"
                    :component="translateExtension"
                    :params="params"
                    element="div" />
                  <span v-else-if="postedDate" class="newsInformationValue newsPostedDate news-details-information">- {{ postedDate }}</span>
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
    translateExtension: null,
    newsTitleContent: null,
    newsSummaryContent: null,
    newsBodyContent: null,
    illustrationBaseUrl: `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/illustration/`,
  }),
  created() {
    this.setNewsTitle(this.news?.title);
    this.setNewsSummary(this.news?.properties?.summary);
    this.setNewsContent(this.news?.body);
    this.refreshTranslationExtensions();
    document.addEventListener('automatic-translation-extensions-updated', () => {
      this.refreshTranslationExtensions();
    });
    this.$root.$on('update-news-title', this.setNewsTitle);
    this.$root.$on('update-news-summary', this.setNewsSummary);
    this.$root.$on('update-news-body', this.setNewsContent);
  },
  computed: {
    params() {
      return {
        news: this.news,
      };
    },
    isDraft() {
      return this.news?.publicationState === 'draft';
    },
    lang() {
      return this.news?.lang;
    },
    hasFeaturedImage() {
      return this.news?.properties?.featuredImageId;
    },
    featuredImageAltText() {
      return this.news?.properties?.featuredImageAltText || this.newsTitle;
    },
    featureImageUpdatedDate() {
      return this.news?.properties?.featuredImageUpdatedDate;
    },
    illustrationURL() {
      const langParam = this.lang && `&lang=${this.lang}` || '';
      return this.hasFeaturedImage && `${this.illustrationBaseUrl}${this.news?.id}?v=${this.featureImageUpdatedDate}&isDraft=${this.isDraft}${langParam}` || '';
    },
    newsTitle() {
      return this.news && this.newsTitleContent;
    },
    showUpdateInfo() {
      return this.news && this.news.updateDate && this.news.updater !=='__system' && this.news.updateDate !== 'null' && this.news.publicationDate && this.news.publicationDate !== 'null' && new Date(this.news.updateDate).getTime() > new Date(this.news.publicationDate).getTime();
    },
    authorProfile() {
      return this.news && this.news.author;
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
      return this.news && this.news.publicationDate && new Date(this.news.publicationDate);
    },
    updatedDate() {
      return this.news && this.news.updateDate && new Date(this.news.updateDate);
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
      return this.news && this.news.summary;
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
    refreshTranslationExtensions() {
      this.translateExtension = extensionRegistry.loadExtensions('news', 'translation-menu-extension')[0];
    }
  }
};
</script>
