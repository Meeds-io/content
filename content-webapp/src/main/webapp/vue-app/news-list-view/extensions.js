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
extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsEmptyTemplate',
  vueComponent: Vue.options.components['news-empty-template'],
  rank: 10,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsSlider',
  vueComponent: Vue.options.components['news-slider-view'],
  rank: 20,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsLatest',
  vueComponent: Vue.options.components['news-latest-view'],
  rank: 30,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsAlert',
  vueComponent: Vue.options.components['news-alert-view'],
  rank: 40,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsList',
  vueComponent: Vue.options.components['news-list-template-view'],
  rank: 50,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsMosaic',
  vueComponent: Vue.options.components['news-mosaic-view'],
  rank: 60,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsStories',
  vueComponent: Vue.options.components['news-stories-view'],
  rank: 70,
});

extensionRegistry.registerComponent('NewsList', 'views', {
  id: 'NewsCards',
  vueComponent: Vue.options.components['news-cards-view'],
  rank: 80,
});
