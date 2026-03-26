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
  <v-radio-group
    v-model="choice"
    mandatory>
    <template #label>
      <span class="text-header">
        {{ $t('news.list.settings.newsTarget') }}
      </span>
    </template>
    <v-radio
      value="posted"
      class="mt-0 mb-2">
      <template #label>
        <div class="d-flex flex-column">
          <div class="text-body">
            {{ $t('news.list.settings.source.posted') }}
          </div>
        </div>
      </template>
    </v-radio>
    <v-radio
      value="target"
      class="mt-0">
      <template #label>
        <div class="d-flex flex-column">
          <div class="text-body">
            {{ $t('news.list.settings.source.target') }}
          </div>
        </div>
      </template>
    </v-radio>
    <div v-if="isTargetChoice" class="d-flex flex-column mt-0 px-2">
      <news-target-suggester v-model="selectedNewsTarget" :allowed-targets="newsTargets" />
      <span v-if="$root.canManageNewsTarget" class="d-flex flex-row clickable text-decoration-underline">
        <a @click="createNewTarget"> {{ $t('news.list.settings.drawer.createNewTarget') }} </a>
      </span>
      <div v-if="newsTargets.length === 0" class="d-flex flex-row grey--text">
        <i class="fas fa-exclamation-triangle mt-2"></i>
        <span class="mx-2"> {{ $t('news.composer.stepper.selectedTarget.noTargetAllowed') }}</span>
      </div>
    </div>
  </v-radio-group>
</template>
<script>
export default {
  data: () => ({
    choice: 'posted',
    newsTargets: [],
    selectedNewsTarget: null,
  }),
  computed: {
    isTargetChoice() {
      return this.choice === 'target';
    }
  },
  watch: {
    choice() {
      switch (this.choice) {
      case 'posted':
        this.selectedNewsTarget = null;
        this.emitSourceOptionUpdate();
        break;
      case 'target':
        this.emitSourceOptionUpdate();
        break;
      }
    },
    selectedNewsTarget() {
      this.emitSourceOptionUpdate();
    }
  },
  created() {
    this.init();
    this.$root.$on('new-news-target-created', (target) => {
      const newTarget = {
        name: target.name,
        label: target?.properties?.label && target.properties.label.length > 35 ? target.properties.label.substring(0, 35).concat('...'): target.properties.label,
        toolTipInfo: target?.properties?.label,
        description: target?.properties?.description
      };
      this.newsTargets.push(newTarget);
      this.selectedNewsTarget = newTarget.name;
    });
  },
  methods: {
    async init() {
      this.selectedNewsTarget = this.$root.newsTarget;
      this.choice = this.$root.articlesSourceOption;
      const newsTargets = await this.$newsTargetingService.getAllowedTargets();
      this.newsTargets = newsTargets.map(newsTarget => ({
        name: newsTarget.name,
        label: newsTarget.properties && newsTarget.properties.label && newsTarget.properties.label.length > 35 ? newsTarget.properties.label.substring(0, 35).concat('...'): newsTarget.properties.label,
        toolTipInfo: newsTarget.properties && newsTarget.properties.label,
        description: newsTarget.properties && newsTarget.properties.description
      }));
    },
    createNewTarget() {
      this.$root.$emit('open-news-publish-targets-management-drawer');
    },
    emitSourceOptionUpdate() {
      this.$emit('update-source-option', {
        articlesSourceOption: this.choice,
        newsTarget: this.selectedNewsTarget
      });
    }
  }

};
</script>
