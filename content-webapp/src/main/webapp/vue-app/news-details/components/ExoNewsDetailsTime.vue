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
  <div v-if="news" class="d-flex flex-row">
    <div class="flex-column my-auto">
      <v-icon x-small class="ms-4 me-1">far fa-clock</v-icon>
    </div>
    <div class="flex-column me-1 my-auto">
      <span>{{ postModeLabel }} </span>
    </div>
    <date-format
      v-if="publicationDate"
      :value="publicationDate"
      :format="dateFormat" />
    <span v-else-if="postedDate" class="flex-column my-auto">- {{ postedDate }}</span>
    <template v-else-if="publicationState === 'staged'">
      <date-format
        :value="scheduleDate"
        :format="dateFormat" />
      <span>-</span>
      <date-format
        :value="scheduleDate"
        :format="dateTimeFormat" />
    </template>
    <date-format
      v-else-if="updatedDate"
      :value="updatedDate"
      :format="dateFormat" />
    <div v-if="notSameUpdater" class="ms-1 flex-column me-1 my-auto"> {{ $t('news.activity.by') }}</div>
    <div v-if="notSameUpdater" class="flex-column my-auto">
      <a :href="updaterProfileURL">{{ updaterFullName }}</a>
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
  }),
  computed: {
    publicationDate() {
      return this.news && this.news.publicationDate && this.news.publicationDate && new Date(this.news.publicationDate);
    },
    updatedDate() {
      return this.news && this.news.updateDate && new Date(this.news.updateDate);
    },
    postedDate() {
      return this.news && this.news.postedDate;
    },
    publicationState() {
      return this.news && this.news.publicationState;
    },
    scheduleDate() {
      return this.news && this.news.schedulePostDate;
    },
    notSameUpdater() {
      return this.news && this.news.updater !=='__system' && this.news.updater !== this.news.author;
    },
    updaterProfileURL() {
      return this.news && `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/profile/${this.news.updater}`;
    },
    postModeLabel() {
      return this.publicationState === 'staged' && this.updatedDate ? this.$t('news.details.scheduled') :this.$t('news.activity.lastUpdated');
    },
    updaterFullName() {
      return this.news && this.news.updater !=='__system' ? this.news.updaterFullName : this.news.authorFullName;
    },
  }
};
</script>