<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

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
  <v-list-item class="ps-2 pe-4 d-flex align-center" @click="open">
    <v-icon size="16" class="clickable icon-menu me-2">fa-solid fa-paper-plane</v-icon>
    <span class="text-color">{{ $t('content.list.item.publish') }}</span>
    <note-publication-drawer
      v-if="drawerOpened"
      ref="drawer"
      :is-publishing="isPublishing"
      :params="{
        spaceId: item.spaceId,
        allowedTargets: targets,
        canPublish: item.canPublish,
        canSchedule: item.canSchedule,
      }"
      edit-mode
      @publish="publish" />
  </v-list-item>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      required: true,
    },
  },
  data: () => ({
    drawerOpened: false,
    localNews: null,
    targets: [],
    isPublishing: false,
  }),
  methods: {
    async open() {
      this.$emit('close');
      const [news, targets] = await Promise.all([
        this.$newsServices.getNewsById(this.item.id, false, 'article', this.item.lang),
        this.$newsTargetingService.getAllowedTargets(),
      ]);
      this.localNews = news;
      this.targets = targets.map(target => ({
        name: target.name,
        label: target?.properties?.label,
        tooltipInfo: `${target?.properties?.label}: ${target?.properties?.description || ''}`,
        description: target?.properties?.description,
        restrictedAudience: target?.restrictedAudience,
      }));
      this.drawerOpened = true;
      await this.$nextTick();
      this.$refs.drawer.open(this.localNews);
    },
    async publish(publicationSettings, content, extensionsCallback) {
      this.localNews.activityPosted = publicationSettings?.post;
      this.localNews.published = publicationSettings?.publish;
      this.localNews.targets = publicationSettings?.selectedTargets;
      this.localNews.categories = publicationSettings?.selectedCategoryIds;
      this.localNews.audience = publicationSettings?.selectedAudience;
      const scheduleSettings = publicationSettings?.scheduleSettings;
      const editScheduleAction = scheduleSettings?.editScheduleAction;
      this.localNews.timeZoneId = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
      this.localNews.schedulePostDate = scheduleSettings?.postDate;
      this.localNews.scheduleUnpublishDate = scheduleSettings?.unpublishDate;
      this.isPublishing = true;
      let article;
      try {
        if (editScheduleAction === 'cancel_schedule') {
          this.localNews.schedulePostDate = 0;
          this.localNews.publicationState = 'draft';
          article = await this.$newsServices.saveNews(this.localNews);
        } else if (editScheduleAction === 'schedule') {
          this.localNews.publicationState = scheduleSettings?.postDate && 'staged' || '';
          article = await this.$newsServices.scheduleNews(this.localNews, 'article');
        } else if (editScheduleAction === 'publish_now') {
          this.localNews.schedulePostDate = 0;
          this.localNews.publicationState = 'posted';
          article = await this.$newsServices.saveNews(this.localNews);
        } else {
          this.localNews.publicationState = 'posted';
          article = await this.$newsServices.updateNews(this.localNews, this.localNews.activityPosted, 'article', 'POSTING_AND_PUBLISHING');
        }
        const metadata = await extensionsCallback.executeExtensions(article);
        await this.$newsServices.updateArticleMetadataProperties(article.id, metadata);
      } finally {
        this.isPublishing = false;
      }
      await this.$nextTick();
      document.dispatchEvent(new CustomEvent('alert-message', {
        detail: {
          alertType: 'success',
          alertMessage: this.$t('notes.publication.settings.update.success'),
        },
      }));
      this.$emit('published');
    },
  },
};
</script>
