/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
import ContentListView from './components/ContentListView.vue';
import ContentListItem from './components/ContentListItem.vue';
import ContentListDrawer from './components/ContentListDrawer.vue';
import ContentActionMenuItems from './components/ContentActionMenuItems.vue';
import ContentFilterDrawer from './components/ContentFilterDrawer.vue';
import ContentListSettingsDrawer from './components/ContentListSettingsDrawer.vue';
import ContentListCategoryPicker from './components/ContentListCategoryPicker.vue';
import NotesPublishAction from './components/NotesPublishAction.vue';
import NewsPublishAction from './components/NewsPublishAction.vue';

const components = {
  'content-list-view': ContentListView,
  'content-list-item': ContentListItem,
  'content-list-drawer': ContentListDrawer,
  'content-action-menu-items': ContentActionMenuItems,
  'content-filter-drawer': ContentFilterDrawer,
  'content-list-settings-drawer': ContentListSettingsDrawer,
  'content-list-category-picker': ContentListCategoryPicker,
  'notes-content-list-publish-action': NotesPublishAction,
  'news-content-list-publish-action': NewsPublishAction,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

extensionRegistry.registerExtension('ContentListItem', 'publishAction', {
  type: 'notes',
  componentName: 'notes-content-list-publish-action',
});
extensionRegistry.registerExtension('ContentListItem', 'publishAction', {
  type: 'news',
  componentName: 'news-content-list-publish-action',
});
