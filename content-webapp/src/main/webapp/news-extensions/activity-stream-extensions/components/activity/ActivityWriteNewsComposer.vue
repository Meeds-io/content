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
  <v-card
    id="writeNewsComposerButton"
    class="mx-4 px-6 py-3 card-border-radius"
    outlined
    flat
    hover
    :title="displayTitle">
    <div
      class="d-flex flex-row align-center"
      @click="switchToNews">
      <v-icon
        color="grey darken-1"
        size="39"
        style="min-height:50px">
        fa-newspaper
      </v-icon>
      <span class="font-weight-bold ms-5">
        {{ $t('news.composer.write') }}
      </span>
    </div>
  </v-card>
</template>
<script>
export default {
  props: {
    activityId: {
      type: String,
      default: null,
    },
    message: {
      type: String,
      default: null,
    },
    maxMessageLength: {
      type: Number,
      default: 0,
    },
    templateParams: {
      type: Object,
      default: null,
    },
    files: {
      type: Array,
      default: null,
    },
    activityType: {
      type: Array,
      default: null,
    },
  },
  computed: {
    disableComposerButton() {
      return this.activityType && this.activityType.length !== 0;
    },
    displayTitle() {
      return this.disableComposerButton ? this.$t('news.composer.write.disable') : this.$t('news.composer.write.description');
    }
  },
  watch: {
    disableComposerButton(value) {
      if (value) {
        document.getElementById('writeNewsComposerButton').setAttribute('style', 'opacity: 0.5');
      }
      else {
        document.getElementById('writeNewsComposerButton').removeAttribute('style');
      }
    }
  },
  methods: {
    switchToNews() {
      if (!this.disableComposerButton) {
        let url = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news/editor`;
        if (eXo.env.portal.spaceId) {
          url += `?spaceId=${eXo.env.portal.spaceId}&type=draft`;
        }
        localStorage.setItem('exo-activity-composer-message', this.message || '');
        window.open(url, '_blank');
      }
    },
  },
};
</script>
