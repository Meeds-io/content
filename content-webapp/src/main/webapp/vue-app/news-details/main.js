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

Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('NewsDetails');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

// getting locale resources
export function init(params) {
  // getting language of the PLF
  const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';
  // should expose the locale resources as REST API
  const urls = [
    `/content/i18n/locale.portlet.news.News?lang=${lang}`,
  ];

  const appId = 'newsDetailsApp';

  exoi18n.loadLanguageAsync(lang, urls).then(i18n => {
    // init Vue app when locale resources are ready
    Vue.createApp({
      data: function() {
        return {
          news: params.news,
          newsId: params.news.newsId,
          activityId: params.activityId,
          newsType: params.newsType,
          showEditButton: params.news.canEdit,
          showPublishButton: params.news.canPublish,
          showCopyLinkButton: true,
          showDeleteButton: params.news.canDelete,
        };
      },
      template: `<exo-news-details
                    id="${appId}"
                    :news="news"
                    :news-id="newsId"
                    :activity-id="activityId"
                    :news-type="newsType"
                    :show-edit-button="showEditButton"
                    :show-publish-button="showPublishButton"
                    :show-delete-button="showDeleteButton" />`,
      i18n,
      vuetify
    }, `#${appId}`, 'Article Details');
  });
}
