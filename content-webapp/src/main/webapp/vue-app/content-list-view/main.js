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

if (!Vue.prototype.$contentListService) {
  Vue.prototype.$contentListService = contentListService;
}

// getting language of the PLF
const lang = eXo && eXo.env && eXo.env.portal.language || 'en';

// should expose the locale resources as REST API
const url = `/content/i18n/locale.portlet.content.Content?lang=${lang}`;

export function init(params) {
  const appId = params.appId;
  const saveSettingsURL = params.saveSettingsURL;
  const canEdit = params.canEdit;

  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    Vue.createApp({
      data: {
        saveSettingsURL,
        canEdit,
      },
      template: `<content-list-view id="${appId}" :can-edit="canEdit" :save-settings-url="saveSettingsURL" />`,
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Content List View');
  });
}
