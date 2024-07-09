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
 
import ExoNewsActivityComposer  from './components/ExoNewsActivityComposer.vue';
import ExoNewsFileDrop from './components/ExoNewsFileDrop.vue';
import * as  newsServices from '../services/newsServices.js';
import * as newsConstants from '../services/newsConstants.js';

const components = {
  'exo-news-activity-composer': ExoNewsActivityComposer,
  'exo-news-file-drop': ExoNewsFileDrop,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

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
