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

import ExoNewsDetails  from './components/ExoNewsDetails.vue';
import ExoNewsDetailsActionMenuApp from '../news/components/ExoNewsDetailsActionMenuApp.vue';
import ExoNewsDetailsActivity from './components/ExoNewsDetailsActivity.vue';
import ExoNewsDetailsToolBar from './components/ExoNewsDetailsToolBar.vue';
import ExoNewsDetailsToolBarMobile from './components/mobile/ExoNewsDetailsToolBarMobile.vue';
import ExoNewsDetailsBodyMobile from './components/mobile/ExoNewsDetailsBodyMobile.vue';
import ExoNewsDetailsBody from './components/ExoNewsDetailsBody.vue';
import ExoNewsDetailsTime from './components/ExoNewsDetailsTime.vue';
import ExoNewsEditPublishingDrawer from './components/ExoNewsEditPublishingDrawer.vue';
import ExoNewsFavoriteAction from './components/ExoNewsFavoriteAction.vue';
import NewsActionMenuItems from '../news/components/NewsActionMenuItems.vue';
import NewsMobileActionMenu from '../news/components/NewsMobileActionMenu.vue';

const components = {
  'exo-news-details': ExoNewsDetails,
  'exo-news-details-activity': ExoNewsDetailsActivity,
  'exo-news-details-action-menu-app': ExoNewsDetailsActionMenuApp,
  'exo-news-details-toolbar': ExoNewsDetailsToolBar,
  'exo-news-details-toolbar-mobile': ExoNewsDetailsToolBarMobile,
  'exo-news-details-body-mobile': ExoNewsDetailsBodyMobile,
  'exo-news-details-body': ExoNewsDetailsBody,
  'exo-news-details-time': ExoNewsDetailsTime,
  'exo-news-edit-publishing-drawer': ExoNewsEditPublishingDrawer,
  'exo-news-favorite-action': ExoNewsFavoriteAction,
  'news-action-menu-items': NewsActionMenuItems,
  'news-mobile-action-menu': NewsMobileActionMenu
};

for (const key in components) {
  Vue.component(key, components[key]);
}

import * as  newsServices from '../services/newsServices.js';
import * as newsConstants from '../services/newsConstants.js';

if (!Vue.prototype.$newsServices) {
  window.Object.defineProperty(Vue.prototype, '$newsServices', {
    value: newsServices,
  });
}
if (!Vue.prototype.$newsConstants) {
  window.Object.defineProperty(Vue.prototype, '$newsConstants', {
    value: newsConstants,
  });
}