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
  <v-list
    class="newsAdvancedSettings"
    subheader
    two-line>
    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.maxArticle') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <input
          v-model="limit"
          :disabled="disableMaxArticle"
          type="number"
          id="maxArticle"
          name="maxArticle"
          @change="emitLimitChange"
          class="maxArticle input-block-level ignore-vuetify-classes">
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showListHeader') }}
        </v-list-item-title>
        <v-list-item-subtitle class="advancedSettingsLabel">
          {{ $t('news.list.settings.drawer.advancedSettings.displayTextHeader') }}
        </v-list-item-subtitle>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showHeader"
          dense
          :disabled="displaySliderButton || displayMosaicButtons || displayStoriesButtons"
          @change="selectedOption('showHeader', showHeader)"
          class="displaySeeAllButton my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showSeeAll') }}
        </v-list-item-title>
        <v-list-item-subtitle class="advancedSettingsLabel">
          {{ $t('news.list.settings.drawer.advancedSettings.displaySeeAll') }}
        </v-list-item-subtitle>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showSeeAll"
          :disabled="disableSeeAll"
          dense
          @change="selectedOption('showSeeAll', showSeeAll)"
          class="displayHeaderTitle my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item v-if="showSeeAll">
      <v-list-item-content class="py-0">
        <v-list-item-action>
          <input
            v-model="seeAllUrl"
            type="url"
            id="seeLink"
            name="seeLink"
            autofocus
            required
            @keyup="$emit('see-all-url', seeAllUrl)"
            @change="$emit('see-all-url', seeAllUrl)"
            class="seeLink input-block-level ignore-vuetify-classes">
        </v-list-item-action>
      </v-list-item-content>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleTitle') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleTitle"
          dense
          @change="selectedOption('showArticleTitle', showArticleTitle)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleSummary') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleSummary"
          dense
          :disabled="displayAlertsButtons || displayMosaicButtons || displayStoriesButtons || displaySliderButton"
          @change="selectedOption('showArticleSummary', showArticleSummary)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleImage') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleImage"
          :disabled="displayAlertsButtons"
          dense
          @change="selectedOption('showArticleImage', showArticleImage)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleAuthor') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleAuthor"
          dense
          :disabled="displayAlertsButtons || displaySliderButton"
          @change="selectedOption('showArticleAuthor', showArticleAuthor)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleDate') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleDate"
          :disabled="displayStoriesButtons"
          dense
          @change="selectedOption('showArticleDate', showArticleDate)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleSpace') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleSpace"
          dense
          :disabled="displayStoriesButtons || displaySliderButton"
          @change="selectedOption('showArticleSpace',showArticleSpace)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>

    <v-list-item>
      <v-list-item-content>
        <v-list-item-title class="advancedSettingsLabel font-weight-regular">
          {{ $t('news.list.settings.drawer.advancedSettings.showArticleReactionsCounter') }}
        </v-list-item-title>
      </v-list-item-content>
      <v-list-item-action>
        <v-switch
          v-model="showArticleReactions"
          :disabled="displayAlertsButtons || displaySliderButton || displayStoriesButtons"
          dense
          @change="selectedOption('showArticleReactions', showArticleReactions)"
          class="my-auto" />
      </v-list-item-action>
    </v-list-item>
  </v-list>
</template>
<script>
export default {
  props: {
    showArticleSummary: {
      type: Boolean,
      default: false,
    },
    showArticleAuthor: {
      type: Boolean,
      default: false,
    },
    showHeader: {
      type: Boolean,
      default: false,
    },
    showSeeAll: {
      type: Boolean,
      default: false,
    },
    viewTemplate: {
      type: String,
      default: '',
    },
    seeAllUrl: {
      type: String,
      default: '',
    },
    articlesSourceOption: {
      type: String,
      default: 'posted',
    }
  },
  data: () => ({
    showArticleTitle: false,
    showArticleImage: false,
    showArticleSpace: false,
    showArticleDate: false,
    showArticleReactions: false,
    limit: null,
    urlRules: {
      required: value => value == null || !!(value?.length),
    },
  }),
  watch: {
    viewTemplate(_, oldTemplate) {
      if (this.viewTemplate === 'NewsMosaic' && this.limit > 3) {
        this.setLimit(3);
      } else if (this.limit === 3 && oldTemplate === 'NewsMosaic') {
        this.setLimit(4);
      }
    }
  },
  computed: {
    displaySliderButton() {
      return this.viewTemplate === 'NewsSlider';
    },
    displayAlertsButtons() {
      return this.viewTemplate === 'NewsAlert';
    },
    displayMosaicButtons() {
      return this.viewTemplate === 'NewsMosaic';
    },
    displayStoriesButtons() {
      return this.viewTemplate === 'NewsStories';
    },
    disableSeeAll() {
      return this.viewTemplate === 'NewsSlider' || this.viewTemplate === 'NewsAlert';
    },
    disableMaxArticle() {
      return this.articlesSourceOption === 'selectedList';
    }
  },
  created() {
    this.reset();
    this.$root.$on('limit-updated', this.setLimit);
  },
  beforeDestroy() {
    this.$root.$off('limit-updated', this.setLimit);
  },
  methods: {
    selectedOption(selectedOption, optionValue) {
      this.$emit('selected-option', selectedOption, optionValue);
    },
    reset() {
      this.viewExtensions = this.$root.viewExtensions;
      this.newsTarget = this.$root.newsTarget;
      this.newsHeader = this.$root.header;
      this.limit = this.viewTemplate === 'NewsMosaic' && this.limit> 3 ? 3 : this.$root.limit;
      this.showHeader = this.viewTemplate === 'NewsSlider' || this.viewTemplate === 'NewsMosaic' || this.viewTemplate === 'NewsStories' ? false : this.$root.showHeader;
      this.showSeeAll = this.$root.showSeeAll;
      this.showArticleTitle = this.$root.showArticleTitle;
      this.showArticleImage = this.viewTemplate === 'NewsAlert' ? false : this.$root.showArticleImage;
      this.showArticleSummary = this.viewTemplate === 'NewsSlider' || this.viewTemplate === 'NewsLatest' || this.viewTemplate === 'NewsAlert' || this.viewTemplate === 'NewsMosaic' || this.viewTemplate === 'NewsStories' ? false : this.$root.showArticleSummary;
      this.showArticleAuthor = this.viewTemplate === 'NewsSlider' ||this.viewTemplate === 'NewsAlert' ? false : this.$root.showArticleAuthor;
      this.showArticleSpace = this.viewTemplate === 'NewsSlider' || this.viewTemplate === 'NewsStories' ? false : this.$root.showArticleSpace;
      this.showArticleDate = this.viewTemplate === 'NewsSlider' || this.$root.showArticleDate;
      this.showArticleReactions =this.viewTemplate === 'NewsSlider' ||  this.viewTemplate === 'NewsAlert' ? false : this.$root.showArticleReactions;
      this.seeAllUrl = this.$root.seeAllUrl || '';
    },
    setLimit(limit) {
      this.limit = limit;
      this.emitLimitChange();
    },
    emitLimitChange() {
      this.$emit('limit-value', this.limit);
    }
  }
};
</script>
