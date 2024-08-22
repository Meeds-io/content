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

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// should expose the locale ressources as REST API
const urls = [
  `/content/i18n/locale.portlet.news.News?lang=${lang}`,
];
// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('ScheduleNewsDrawer');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}
let scheduleNewsDrawer;
export function init(params) {
  const appId = `scheduleNewsDrawer-${params.newsId}`;

  const appElement = document.createElement('div');
  appElement.id = appId;

  exoi18n.loadLanguageAsync(lang, urls).then(i18n => {
    // init Vue app when locale ressources are ready
    scheduleNewsDrawer = new Vue({
      template: `<schedule-news-drawer
                  v-cacheable="{cacheId: '${appId}'}"
                  id="${appId}"/>`,
      i18n,
      vuetify,
    }).$mount(appElement);
  });
}

export function destroy() {
  if (scheduleNewsDrawer) {
    scheduleNewsDrawer.$destroy();
  }
}