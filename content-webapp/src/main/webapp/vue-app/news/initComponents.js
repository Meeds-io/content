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
 
import NewsApp  from './components/NewsApp.vue';
import NewsSpacesSharedIn from './components/NewsSpacesSharedIn.vue';
import NewsActivitySharingSpacesDrawer from './components/NewsActivitySharingSpacesDrawer.vue';
import NewsFilterSpaceDrawer from './components/NewsFilterSpaceDrawer.vue';
import NewsFilterSpaceItem from './components/NewsFilterSpaceItem.vue';
import NewsFilterSpaceList from './components/NewsFilterSpaceList.vue';
import NewsFilterSpaceSearch from './components/NewsFilterSpaceSearch.vue';
import ExoNewsDetailsActionMenuApp from './components/ExoNewsDetailsActionMenuApp.vue';
import NewsAppItem from './components/NewsAppItem.vue';
import NewsActionMenuItems from './components/NewsActionMenuItems.vue';
import NewsMobileActionMenu from './components/NewsMobileActionMenu.vue';

const components = {
  'news-app': NewsApp,
  'news-spaces-shared-in': NewsSpacesSharedIn,
  'news-activity-sharing-spaces-drawer': NewsActivitySharingSpacesDrawer,
  'news-filter-space-drawer': NewsFilterSpaceDrawer,
  'news-filter-space-item': NewsFilterSpaceItem,
  'news-filter-space-list': NewsFilterSpaceList,
  'news-filter-space-search': NewsFilterSpaceSearch,
  'exo-news-details-action-menu-app': ExoNewsDetailsActionMenuApp,
  'news-app-item': NewsAppItem,
  'news-action-menu-items': NewsActionMenuItems,
  'news-mobile-action-menu': NewsMobileActionMenu
};

for (const key in components) {
  Vue.component(key, components[key]);
}

import * as  newsServices from '../services/newsServices';

if (!Vue.prototype.$newsServices) {
  window.Object.defineProperty(Vue.prototype, '$newsServices', {
    value: newsServices,
  });
}