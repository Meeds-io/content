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
import './initComponents.js';
import * as contentListService from './js/ContentListService.js';
import * as newsServices from '../services/newsServices.js';
import * as newsTargetingService from '../services/newsTargetingService.js';
import * as notePublishService from '../news-extensions/note-publish-extensions/publishService.js';

if (!Vue.prototype.$contentListService) {
  Vue.prototype.$contentListService = contentListService;
}

if (!Vue.prototype.$newsServices) {
  window.Object.defineProperty(Vue.prototype, '$newsServices', {
    value: newsServices,
  });
}

if (!Vue.prototype.$newsTargetingService) {
  window.Object.defineProperty(Vue.prototype, '$newsTargetingService', {
    value: newsTargetingService,
  });
}

if (!Vue.prototype.$notePublishService) {
  window.Object.defineProperty(Vue.prototype, '$notePublishService', {
    value: notePublishService,
  });
}

// getting language of the PLF
const lang = eXo && eXo.env && eXo.env.portal.language || 'en';

// should expose the locale resources as REST API
const url = `/content/i18n/locale.portlet.content.Content?lang=${lang}`;

export async function init(params) {
  const appId = params.appId;
  const applicationId = params.applicationId;
  const saveSettingsURL = params.saveSettingsURL;
  const canEdit = params.canEdit;
  const showHeader = params.showHeader !== 'false';
  const allowFilteringPerCategory = params.allowFilteringPerCategory !== 'false';
  const parsedCategoryDepth = parseInt(params.categoryDepth);
  const categoryDepth = Number.isNaN(parsedCategoryDepth) ? 4 : parsedCategoryDepth;
  const categoryIds = params.categoryIds ? params.categoryIds.split(',').map(id => parseInt(id)) : [];
  const excludeCategoryIds = params.excludeCategoryIds ? params.excludeCategoryIds.split(',').map(id => parseInt(id)) : [];
  const defaultLanguage = eXo?.env?.portal?.defaultLanguage;

  // Reference data (i18n + header title translations) is awaited before
  // mounting, matching how the other portlets (activity stream, documents...)
  // boot: the header title is present at first render instead of popping in,
  // and mounting after these awaits means the dynamic page layout has
  // finished (re-)installing this application's container by the time the
  // app attaches to it.
  const i18n = await exoi18n.loadLanguageAsync(lang, url);
  const headerTranslations = await Vue.prototype.$translationService
    .getTranslations('contentListView', applicationId, 'headerTitleInput')
    .catch(() => null);
  const headerTitle = headerTranslations?.[lang] || headerTranslations?.[defaultLanguage] || params.headerTitle || null;

  await Vue.createApp({
    data: {
      applicationId,
      headerTranslations,
      saveSettingsURL,
      canEdit,
      showHeader,
      headerTitle,
      allowFilteringPerCategory,
      categoryDepth,
      categoryIds,
      excludeCategoryIds,
      language: lang,
      defaultLanguage,
    },
    template: `<content-list-view
      id="${appId}"
      :can-edit="canEdit"
      :save-settings-url="saveSettingsURL"
      :show-header="showHeader"
      :header-title="headerTitle"
      :allow-filtering-per-category="allowFilteringPerCategory"
      :category-depth="categoryDepth"
      :category-ids="categoryIds"
      :exclude-category-ids="excludeCategoryIds" />`,
    vuetify: Vue.prototype.vuetifyOptions,
    i18n,
  }, `#${appId}`, 'Content List View');
}
