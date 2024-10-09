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
  <v-menu
    v-model="actionMenu"
    attach
    eager
    bottom
    left
    offset-y
    class="px-0 mx-2 pa-0 py-0 overflow-hidden newsMenu">
    <template #activator="{ on, attrs }">
      <v-btn
        v-bind="attrs"
        :aria-label="$t('news.details.menu.open')"
        :class="['pull-right', {'newsDetailsActionMenu': !articleNewLayoutEnabled}, { 'pl-4': isMobile && !articleNewLayoutEnabled }]"
        icon
        v-on="on"
        @click.prevent="openBottomMenu">
        <v-icon size="20">
          fas fa-ellipsis-v
        </v-icon>
      </v-btn>
    </template>
    <news-action-menu-items
      v-if="!isMobile"
      :news="news"
      :show-copy-link-button="showCopyLinkButton"
      :show-delete-button="showDeleteButton"
      :show-edit-button="showEditButton"
      :current-app="currentApp"
      :show-publish-button="showPublishButton"
      :show-resume-button="showResumeButton"
      :show-share-button="showShareButton"
      @copy-link="copyLink"
      @edit-article="$emit('edit-article', news)"
      @delete-article="$emit('delete-article')" />
  </v-menu>
</template>

<script>
export default {
  props: {
    news: {
      type: Object,
      required: false,
      default: null
    },
    showShareButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showEditButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showResumeButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showDeleteButton: {
      type: Boolean,
      required: false,
      default: false
    },
    showPublishButton: {
      type: Boolean,
      required: false,
      default: false
    },
    currentApp: {
      type: String,
      required: false,
      default: null
    },
    showCopyLinkButton: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data: () => ({
    actionMenu: null,
  }),
  created() {
    $(document).mousedown(() => {
      if (this.actionMenu) {
        window.setTimeout(() => {
          this.actionMenu = false;
        }, 200);
      }
    });
  },
  mounted() {
    $('#UIPortalApplication').parent().click(() => {
      this.actionMenu = false;
    });
  },
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.name === 'xs' || this.$vuetify.breakpoint.name === 'sm';
    },
    articleNewLayoutEnabled() {
      return eXo?.env?.portal?.articleNewLayoutEnabled;
    }
  },
  methods: {
    copyLink() {
      let newsLink = window.location.href.split(eXo.env.portal.metaPortalName)[0];
      if (this.news?.published && this.news.audience === 'all') {
        newsLink = newsLink.concat(eXo.env.portal.metaPortalName).concat(`/news-detail?newsId=${this.news.id}&type=article`);
      } else {
        newsLink = newsLink.concat(eXo.env.portal.metaPortalName).concat(`/activity?id=${this.news.activityId}`);
      }
      if (this.news?.lang) {
        newsLink = newsLink.concat(`&lang=${this.news.lang}`);
      }
      navigator.clipboard.writeText(newsLink);
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertType: 'success',
        alertMessage: this.$t('news.alert.success.label.linkCopied') ,
      }}));
      if (this.isMobile) {
        this.$root.$emit('close-news-mobile-action-menu');
      }
    },
    openBottomMenu() {
      if (this.isMobile) {
        this.$root.$emit('open-news-mobile-action-menu', {
          news: this.news,
          showShareButton: this.showShareButton,
          showEditButton: this.showEditButton,
          showResumeButton: this.showResumeButton,
          showDeleteButton: this.showDeleteButton,
          showPublishButton: this.showCopyLinkButton,
          showCopyLinkButton: this.showCopyLinkButton,
          currentApp: this.currentApp
        });
      }
    }
  }
};
</script>
