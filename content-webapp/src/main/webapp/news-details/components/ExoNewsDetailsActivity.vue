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
    :news-id="newsId || sharedNewsId"
    :activity-id="activityId"
    :show-edit-button="showEditButton"
    :show-publish-button="showPublishButton"
    :show-delete-button="showDeleteButton" />
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
    if (this.newsId || this.sharedNewsId) {
      this.retrieveNews();
    }
  },
  methods: {
    retrieveNews() {
      if (this.activity.news) {
        this.news = this.activity.news;
      } else {
        this.$newsServices.getNewsByActivityId(this.activityId)
          .then(news => {
            this.news = news;
            if (!this.news) {
              this.$root.$emit('activity-extension-abort', this.activityId);
            }
            this.activity.news = news;
          })
          .catch(() => {
            this.$root.$emit('activity-extension-abort', this.activityId);
          });
      }
    },
  },
};
</script>