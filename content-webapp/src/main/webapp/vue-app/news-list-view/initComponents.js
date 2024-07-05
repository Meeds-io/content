/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import NewsListView from './components/NewsListView.vue';
import NewsLatestView from './components/views/NewsLatestView.vue';
import NewsSliderView from './components/views/NewsSliderView.vue';
import NewsSettings from './components/settings/NewsSettings.vue';
import NewsSettingsDrawer from './components/settings/NewsSettingsDrawer.vue';
import NewsSliderViewItem from './components/views/NewsSliderViewItem.vue';
import NewsEmptyTemplate from './components/views/NewsEmptyTemplate.vue';
import NewsLatestViewItem from './components/views/NewsLatestViewItem.vue';
import NewsAdvancedSettings from './components/settings/NewsAdvancedSettings.vue';
import NewsAlertView from './components/views/NewsAlertView.vue';
import NewsListTemplateView from './components/views/NewsListTemplateView.vue';
import NewsListTemplateViewItem from './components/views/NewsListTemplateViewItem.vue';
import NewsMosaicView from './components/views/NewsMosaicView.vue';
import NewsStoriesView from './components/views/NewsStoriesView.vue';
import NewsStoriesViewItem from './components/views/NewsStoriesViewItem.vue';
import NewsCardsView from './components/views/NewsCardsView.vue';
import NewsCardsViewItem from './components/views/NewsCardsViewItem.vue';


const components = {
  'news-list-view': NewsListView,
  'news-latest-view': NewsLatestView,
  'news-slider-view': NewsSliderView,
  'news-settings': NewsSettings,
  'news-settings-drawer': NewsSettingsDrawer,
  'news-slider-view-item': NewsSliderViewItem,
  'news-empty-template': NewsEmptyTemplate,
  'news-latest-view-item': NewsLatestViewItem,
  'news-advanced-settings': NewsAdvancedSettings,
  'news-alert-view': NewsAlertView,
  'news-list-template-view': NewsListTemplateView,
  'news-list-template-view-item': NewsListTemplateViewItem,
  'news-mosaic-view': NewsMosaicView,
  'news-stories-view': NewsStoriesView,
  'news-stories-view-item': NewsStoriesViewItem,
  'news-cards-view': NewsCardsView,
  'news-cards-view-item': NewsCardsViewItem,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
