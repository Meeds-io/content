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
  <v-progress-circular
    v-if="loading"
    size="24"
    color="primary"
    indeterminate />
  <div v-else-if="!isDeleted"><a :href="contentUrl">{{ contentTitle }}</a></div>
  <div v-else class="d-flex">
    <span class="text-no-wrap text-sub-title my-auto ml-1">
      {{ contentTitle }} ({{ $t('analytics.deleted') }})
    </span>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: Object,
      default: function () {
        return null;
      },
    }
  },
  data: () => ({
    loading: true,
    content: null,
    lang: eXo.env.portal.language
  }),
  computed: {
    contentTitle() {
      return this.content?.title;
    },
    contentUrl() {
      return this.content?.url;
    },
    isDeleted() {
      return this.content?.deleted;
    }
  },
  created() {
    if (this.value) {
      this.loading = true;
      this.$newsServices.getNewsById(this.value, false, 'article', this.lang).then(content => {
        this.content = content;
        if (!this.content) {
          this.getArticlePage(this.value).then(page => {
            this.content = page;
          });
        }
      }).finally(() => this.loading = false);
    }
  },
  methods: {
    async getArticlePage(id) {
      try {
        const resp = await fetch(`/portal/rest/notes/note/${id}?includeDeleted=true`, {
          method: 'GET',
          credentials: 'include',
        });
        if (!resp.ok) {
          return ;
        }
        return await resp.json();
      } catch (error) {
        console.error('Error fetching article:', error);
        throw error;
      }
    }
  }
};
</script>
