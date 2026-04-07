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
  <div class="flex-column align-start mt-4">
    <v-radio-group
      id="news-source-label"
      class="ma-0 pa-0"
      v-model="choice"
      hide-details
      mandatory>
      <template #label>
        <span class="text-header me-1 my-auto">
          {{ $t('news.list.settings.newsTarget') }}
        </span>
      </template>
      <v-radio
        value="posted"
        class="mb-1 ms-n1">
        <template #label>
          <div class="d-flex flex-column">
            <div class="text-body">
              {{ $t('news.list.settings.source.posted') }}
            </div>
          </div>
        </template>
      </v-radio>
      <v-radio
        value="selectedList"
        class="mt-0 mb-1 ms-n1">
        <template #label>
          <div class="d-flex flex-column">
            <div class="text-body">
              {{ $t('news.list.settings.source.selectedList') }}
            </div>
          </div>
        </template>
      </v-radio>
      <div v-if="isSelectedLIstChoice" class="d-flex flex-column mt-0">
        <news-list-selector v-model="selectedArticles" />
      </div>
      <v-radio
        value="target"
        class="mt-0 ms-n1">
        <template #label>
          <div class="d-flex flex-column">
            <div class="text-body">
              {{ $t('news.list.settings.source.target') }}
            </div>
          </div>
        </template>
      </v-radio>
    </v-radio-group>
    <div v-if="isTargetChoice" class="d-flex flex-column mt-0">
      <news-target-suggester v-model="selectedNewsTarget" :allowed-targets="newsTargets" />
      <span v-if="canCreateNewsTarget" class="d-flex flex-row clickable text-decoration-underline">
        <a @click="createNewTarget"> {{ $t('news.list.settings.drawer.createNewTarget') }} </a>
      </span>
      <div v-if="newsTargets.length === 0" class="d-flex flex-row grey--text">
        <i class="fas fa-exclamation-triangle mt-2"></i>
        <span class="mx-2"> {{ $t('news.composer.stepper.selectedTarget.noTargetAllowed') }}</span>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    newsTargets: {
      type: Array,
      default: () => []
    },
    newsTarget: {
      type: String,
      default: () => null
    },
    articles: {
      type: Array,
      default: () => []
    },
    articlesSourceOption: {
      type: String,
      default: () => null
    }
  },
  data: () => ({
    choice: 'posted',
    selectedNewsTarget: null,
    selectedArticles: [],
    defaultLimit: 4
  }),
  computed: {
    isTargetChoice() {
      return this.choice === 'target';
    },
    isSelectedLIstChoice() {
      return this.choice === 'selectedList';
    },
    canCreateNewsTarget() {
      return this.$root?.canCreateNewsTarget;
    }
  },
  watch: {
    choice() {
      switch (this.choice) {
      case 'posted':
        this.selectedNewsTarget = null;
        this.selectedArticles = [];
        this.updateLimitValue(this.defaultLimit);
        break;
      case 'target':
        this.updateLimitValue(this.defaultLimit);
        break;
      case 'selectedList':
        this.updateLimitValue(this.selectedArticles?.length || 0);
        break;
      }
      this.emitSourceOptionUpdate();
    },
    selectedNewsTarget() {
      if (this.selectedNewsTarget) {
        this.selectedArticles = [];
      }
      this.emitSourceOptionUpdate();
    },
    selectedArticles() {
      if (this.selectedArticles?.length) {
        this.selectedNewsTarget = null;
        this.updateLimitValue(this.selectedArticles.length);
      }
      this.emitSourceOptionUpdate();
    },
    newsTarget() {
      this.selectedNewsTarget = this.newsTarget;
    }
  },
  created() {
    this.selectedNewsTarget = this.newsTarget;
    this.selectedArticles = this.articles;
    this.choice = this.articlesSourceOption;
  },
  methods: {
    createNewTarget() {
      this.$root.$emit('open-news-publish-targets-management-drawer');
    },
    emitSourceOptionUpdate() {
      this.$emit('update-source-option', {
        articlesSourceOption: this.choice,
        newsTarget: this.selectedNewsTarget,
        selectedArticles: this.selectedArticles
      });
    },
    updateLimitValue(limit) {
      this.$root.$emit('limit-updated', limit);
    }
  }

};
</script>
