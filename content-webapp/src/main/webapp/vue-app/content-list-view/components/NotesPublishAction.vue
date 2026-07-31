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
      :has-featured-image="hasFeaturedImage"
      :is-publishing="isPublishing"
      :params="{
        spaceId: item.spaceId,
        allowedTargets: targets,
        canPublish: canPublish,
        canSchedule: canSchedule,
      }"
      :edit-mode="editMode"
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
    noteObject: null,
    targets: [],
    canPublish: false,
    canSchedule: false,
    editMode: false,
    isPublishing: false,
  }),
  computed: {
    hasFeaturedImage() {
      return !!this.noteObject?.properties?.featuredImage?.id;
    },
  },
  methods: {
    fetchNote(noteId) {
      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/note/${noteId}`, {
        method: 'GET',
        credentials: 'include',
      }).then(resp => resp?.ok && resp.json() || null);
    },
    async open() {
      this.$emit('close');
      const [note, canPublish, savedSettings, targets] = await Promise.all([
        this.fetchNote(this.item.id),
        this.$notePublishService.canPublish(this.item.spaceId),
        this.$notePublishService.getSavedNotePublicationSettings(this.item.id),
        this.$notePublishService.getNotePublishTargets(),
      ]);
      this.canPublish = canPublish;
      this.targets = targets;
      this.editMode = !!savedSettings;
      try {
        this.canSchedule = await this.$notePublishService.canSchedule(this.item.spaceId, this.item.id);
      } catch (e) {
        this.canSchedule = false;
      }
      this.noteObject = {
        ...note,
        ...savedSettings,
      };
      this.drawerOpened = true;
      await this.$nextTick();
      this.$refs.drawer.open(this.noteObject);
    },
    async publish(publicationSettings, note, extensionsCallback) {
      const scheduleSettings = publicationSettings?.scheduleSettings;
      const noteArticle = {
        ...(note || this.noteObject),
        schedulePostDate: scheduleSettings?.postDate,
        scheduleUnpublishDate: scheduleSettings?.unpublishDate,
        activityPosted: publicationSettings?.post,
        categories: publicationSettings?.selectedCategoryIds,
        published: publicationSettings?.publish,
        targets: publicationSettings?.selectedTargets,
        audience: publicationSettings?.selectedAudience,
      };
      this.isPublishing = true;
      let article;
      try {
        article = note
          ? await this.$notePublishService.saveNoteArticle(noteArticle, this.item.spaceId)
          : await this.$notePublishService.updateNotePublication(scheduleSettings, noteArticle, this.item.spaceId);
        const metadata = await extensionsCallback.executeExtensions(article);
        await this.$notePublishService.updatePublishedNoteMetadata(article.id, metadata);
      } finally {
        this.isPublishing = false;
      }
      await this.$nextTick();
      document.dispatchEvent(new CustomEvent('alert-message-html', {
        detail: {
          alertType: 'success',
          alertMessage: this.editMode && this.$t('notes.publication.settings.update.success')
            || this.$t('notes.publication.success.message'),
          alertLink: article?.url,
          alertLinkText: this.$t('notes.view.label'),
        },
      }));
      this.$emit('published');
    },
  },
};
</script>
