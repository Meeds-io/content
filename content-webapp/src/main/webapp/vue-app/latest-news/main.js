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

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('latestNews');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

Vue.use(Vuetify);

const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

const appId = 'latestNewsDetails';

let latestNewsDetails;
export function initLatestNews(params) {
  // getting language of the PLF
  const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
  // should expose the locale resources as REST API
  const url = `/content/i18n/locale.portlet.news.News?lang=${lang}`;
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {  
  // init Vue app when locale resources are ready
    latestNewsDetails = Vue.createApp({
      data: function() {
        return {
          newsInfo: params.newsInfo,
          seeAllLabel: params.seeAllLabel,
          header: params.header,
          url: params.url,
          isShowHeader: params.isShowHeader
        };
      },
      template: `<exo-news-latest id="${appId}" v-cacheable :news-info="newsInfo" :header="header" :see-all="seeAllLabel" :url="url"  :is-show-header="isShowHeader"></exo-news-latest>`,
      i18n,
      vuetify,
    }, `#${appId}`, 'news');
  });
}

export function destroy() {
  if (latestNewsDetails) {
    latestNewsDetails.$destroy();
  }
}