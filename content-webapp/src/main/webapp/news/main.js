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
 
import './initComponents.js';

import {newsConstants} from '../js/newsConstants';

Vue.use(Vuetify);

const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('News');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
// should expose the locale resources as REST API
const urls = [
  `${newsConstants.PORTAL}/${newsConstants.PORTAL_REST}/i18n/bundle/locale.portlet.news.News-${lang}.json`,
  `${newsConstants.PORTAL}/${newsConstants.PORTAL_REST}/i18n/bundle/locale.social.Webui-${lang}.json`
];

document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
const appId = 'NewsApp';

export function init() {
  exoi18n.loadLanguageAsync(lang, urls).then(i18n => {
    new Vue({
      template: `<news-app id="${appId}"/>`,
      i18n,
      vuetify,
    }).$mount(`#${appId}`);
  });
}