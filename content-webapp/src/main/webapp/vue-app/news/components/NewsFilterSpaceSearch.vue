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
  <v-scale-transition>
    <v-text-field
      v-model="query"
      :placeholder="$t('news.app.filter.bySpaces')"
      :append-icon="appendIcon"
      prepend-inner-icon="fa-filter"
      class="pa-0 my-auto"
      @click:append="query = null" />
  </v-scale-transition>
</template>

<script>
export default {
  props: {
    value: {
      type: Array,
      default: () => [],
    },
  },
  data: () => ({
    startSearchAfterInMilliseconds: 600,
    endTypingKeywordTimeout: 50,
    startTypingKeywordTimeout: 0,
    query: null,
    loading: false,
  }),
  computed: {
    appendIcon() {
      return this.query && 'mdi-close primary--text' || null;
    },
  },
  watch: {
    query() {
      if (!this.query) {
        this.loading = false;
        this.$emit('input', '');
        return;
      }
      this.startTypingKeywordTimeout = Date.now();
      if (!this.loading) {
        this.loading = true;
        this.waitForEndTyping();
      }
    },
  },
  methods: {
    waitForEndTyping() {
      window.setTimeout(() => {
        if (Date.now() - this.startTypingKeywordTimeout > this.startSearchAfterInMilliseconds) {
          this.loading = false;
          this.$emit('input', this.query);
        } else {
          this.waitForEndTyping();
        }
      }, this.endTypingKeywordTimeout);
    },
  },
};
</script>