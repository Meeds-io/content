<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2026 Meeds Association contact@meeds.io
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
  <div class="d-flex position-relative overflow-hidden text-no-wrap text-truncate flex-shrink-0 text-subtitle">
    <div
      v-if="showReactions"
      class="d-flex">
      <div
        :aria-label="likesLabel"
        class="d-flex"
        role="img">
        <v-icon
          :class="iconColorClass"
          class="my-auto me-1"
          aria-hidden="true"
          size="12">
          fas fa-thumbs-up
        </v-icon>
        <div
          :class="textColorClass"
          class="me-2 my-auto"
          aria-hidden="true">
          {{ item.likesCount }}
        </div>
      </div>
      <div
        :aria-label="commentsLabel"
        class="d-flex"
        role="img">
        <v-icon
          :class="iconColorClass"
          class="my-auto me-1"
          aria-hidden="true"
          size="12">
          fas fa-comment
        </v-icon>
        <div
          :class="textColorClass"
          class="me-2 my-auto"
          aria-hidden="true">
          {{ item.commentsCount }}
        </div>
      </div>
      <div
        :aria-label="viewsLabel"
        class="d-flex"
        role="img">
        <v-icon
          :class="iconColorClass"
          class="my-auto me-1"
          aria-hidden="true"
          size="12">
          fas fa-eye
        </v-icon>
        <div
          :class="textColorClass"
          class="my-auto"
          aria-hidden="true">
          {{ item.viewsCount }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    textColorClass: {
      type: String,
      default: ''
    },
    iconColorClass: {
      type: String,
      default: 'icon-default-color'
    },
    item: {
      type: Object,
      default: () => ({})
    },
    showArticleReactions: {
      type: Boolean,
      default: false
    },
  },
  computed: {
    showReactions () {
      return !this.item?.properties?.hideReaction && this.showArticleReactions;
    },
    likesLabel() {
      return this.countLabel(this.item?.likesCount, 'news.list.reaction.like', 'news.list.reaction.likes');
    },
    commentsLabel() {
      return this.countLabel(this.item?.commentsCount, 'news.list.reaction.comment', 'news.list.reaction.comments');
    },
    viewsLabel() {
      return this.countLabel(this.item?.viewsCount, 'news.details.view', 'news.details.views');
    },
  },
  methods: {
    countLabel(count, singularKey, pluralKey) {
      const value = Number(count) || 0;
      return this.$t(value === 1 && singularKey || pluralKey, {0: value});
    },
  }
};
</script>