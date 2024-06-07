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
import './extensions.js';

import * as newsListService from './js/NewsListService.js';
import * as  newsServices from '../services/newsServices';
import * as newsTargetingService from '../services/newsTargetingService.js';

if (!Vue.prototype.$newsTargetingService) {
  window.Object.defineProperty(Vue.prototype, '$newsTargetingService', {
    value: newsTargetingService,
  });
}

if (!Vue.prototype.$newsServices) {
  window.Object.defineProperty(Vue.prototype, '$newsServices', {
    value: newsServices,
  });
}

if (!Vue.prototype.$newsListService) {
  window.Object.defineProperty(Vue.prototype, '$newsListService', {
    value: newsListService,
  });
}

// get override components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('NewsListView');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

// getting language of the PLF
const lang = eXo && eXo.env && eXo.env.portal.language || 'en';

// should expose the locale resources as REST API
const url = `/content/i18n/locale.portlet.news.News?lang=${lang}`;

// getting locale resources
export function init(params) {
  const applicationId = params.applicationId;
  const appId = params.appId;
  const viewTemplate = params.viewTemplate;
  const saveSettingsURL = params.saveSettingsURL;
  const newsTarget = params.newsTarget;
  const limit = params.limit === '' ? '4' : params.limit;
  const showHeader = viewTemplate === 'NewsSlider' ? false: params.showHeader === 'true';
  const showSeeAll = params.showSeeAll === 'true' && !!params.seeAllUrl?.length;
  const showArticleTitle = params.showArticleTitle === '' ? true : params.showArticleTitle === 'true';
  const showArticleSummary = viewTemplate === 'NewsLatest' ? false: params.showArticleSummary === 'true';
  const showArticleImage = params.showArticleImage === '' ? true : params.showArticleImage === 'true';
  const showArticleAuthor = viewTemplate === 'NewsLatest' ? false: params.showArticleAuthor === 'true';
  const showArticleSpace = params.showArticleSpace === '' ? true : params.showArticleSpace === 'true';
  const showArticleReactions = params.showArticleReactions === '' ? true : params.showArticleReactions === 'true';
  const showArticleDate  = params.showArticleDate === '' ? true : params.showArticleDate === 'true';
  const seeAllUrl = params.seeAllUrl;

  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    Vue.createApp({
      data: {
        applicationId,
        headerTranslations: null,
        saveSettingsURL,
        viewTemplate,
        newsTarget,
        headerTitle: null,
        limit,
        showHeader,
        showSeeAll,
        showArticleTitle,
        showArticleSummary,
        showArticleImage,
        showArticleAuthor,
        showArticleSpace,
        showArticleReactions,
        showArticleDate,
        seeAllUrl
      },
      computed: {
        newListTranslationEnabled() {
          return eXo?.env?.portal?.newsListTranslationEnabled;
        },
      },
      created() {
        if (!this.newListTranslationEnabled) {
          this.headerTitle = params.headerTitle;
          return;
        }
        Vue.prototype.$translationService.getTranslations('newsListView', applicationId, 'headerNameInput').then(translations => {
          this.headerTranslations = translations;
          this.headerTitle = translations?.[lang] || translations?.en
                                                  || params.headerTitle;
        });
      },
      template: `<news-list-view
                  id="${appId}"
                  :application-id="applicationId"
                  :header-translations="headerTranslations"
                  :view-template="viewTemplate"
                  :news-target="newsTarget"
                  :header-title="headerTitle"
                  :show-article-author="showArticleAuthor"
                  :show-article-image="showArticleImage"
                  :show-article-reactions="showArticleReactions"
                  :show-article-space="showArticleSpace"
                  :show-article-title="showArticleTitle"
                  :show-header="showHeader"
                  :show-see-all="showSeeAll"
                  :show-article-date="showArticleDate"
                  :show-article-summary="showArticleSummary"
                  :limit="limit"
                  :see-all-url="seeAllUrl"/>`,
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, `News List View - ${viewTemplate}`);
  });
}
