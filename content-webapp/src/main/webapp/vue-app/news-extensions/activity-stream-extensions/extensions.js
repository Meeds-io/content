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

const lang = eXo.env.portal.language || 'en';
const url = `/content/i18n/locale.portlet.news.News?lang=${lang}`;

const i18nPromise = exoi18n.loadLanguageAsync(lang, url).then(i18n => new Vue({i18n}));

const newsActivityTypeExtensionOptions = {
  name: 'News',
  getExtendedComponent: (activity, isActivityDetail) => {
    if (activity && isActivityDetail) {
      return {
        component: Vue.options.components['exo-news-details-activity'],
        overrideHeader: true,
        overrideFooter: false,
      };
    }
  },
  extendSharedActivity: (activity, isActivityDetail) => isActivityDetail,
  showSharedInformationFooter: (activity, isActivityDetail) => isActivityDetail,
  init: (activity, isActivityDetail) => {
    let activityId = activity.id;
    if (activity.parentActivity) {
      activityId = activity.parentActivity.id;
    }
    if (!activity.news || isActivityDetail) {
      return Vue.prototype.$newsServices.getNewsByActivityId(activityId)
        .then(news => activity.news = news);
    }
  },
  canEdit: () => false,
  canShare: () => true,
  hideOnDelete: true,
  supportsThumbnail: true,
  summaryLinesToDisplay: 3,
  noEmbeddedLinkView: true,
  windowTitlePrefixKey: 'news.window.title',
  addMargin: true,
  getThumbnail: (activity) => activity?.news?.illustrationURL && `${activity?.news?.illustrationURL}&size=305x285` || '/content/images/news.png',
  getThumbnailProperties: (activity) => !(activity?.news?.illustrationURL) && {
    height: '90px',
    width: '90px',
    noBorder: true,
  } || null,
  isUseSameViewForMobile: (activity) => !activity?.news?.illustrationURL,
  getTitle: (activity) => {
    const news = activity?.news;
    if (news?.title) {
      return news.title;
    }
    return '';
  },
  getSourceLink: (activity) => `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${!activity.parentActivity ? activity.id : activity.parentActivity.id}`,
  getSummary: (activity) => {
    const news = activity?.news;
    if (news?.summary) {
      return news.summary;
    } else if (news?.body) {
      return Vue.prototype.$utils.htmlToText(news.body);
    }
    return '';
  },
  getTooltip: (activity, isActivityDetail) => !isActivityDetail && activity && 'news.activity.clickToShowDetail',
};

export function initExtensions() {
  extensionRegistry.registerExtension('activity', 'type', {
    type: 'news',
    options: newsActivityTypeExtensionOptions,
  });
  if (eXo.env.portal.spaceId) {
    Vue.prototype.$newsServices.canUserCreateNews(eXo.env.portal.spaceId).then(canCreateNews => {
      if (canCreateNews) {
        return i18nPromise.then(() => {
          extensionRegistry.registerComponent('ActivityComposerAction', 'activity-composer-action', {
            id: 'switchNewsButton',
            vueComponent: Vue.options.components['activity-switch-to-news'],
            rank: 10,
          });
          extensionRegistry.registerComponent('ActivityComposerFooterAction', 'activity-composer-footer-action', {
            id: 'writeNewsButton',
            vueComponent: Vue.options.components['activity-write-news-composer'],
            rank: 30,
          });
          extensionRegistry.registerComponent('ActivityToolbarAction', 'activity-toolbar-action', {
            id: 'writeNewsToolbarButton',
            vueComponent: Vue.options.components['activity-write-news-toolbar-action'],
            rank: 30,
          });
        });
      }
    });
  }
}
