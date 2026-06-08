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
const i18nPromise = exoi18n.loadLanguageAsync(lang, url);

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
  hideViewersCounter: () => true,
  showSharedInformationFooter: (activity, isActivityDetail) => isActivityDetail,
  init: (activity, isActivityDetail) => {
    let activityId = activity.id;
    if (activity.parentActivity) {
      activityId = activity.parentActivity.id;
    }
    if (activity.newsTranslations) {
      const newsTranslationsMap = activity.newsTranslations;
      const newsTranslationKey = `news_${lang}`;
      activity.news = newsTranslationsMap[newsTranslationKey] || activity.news;
    }
    if (!activity.news || isActivityDetail) {
      return Vue.prototype.$newsServices.getNewsByActivityId(activityId, lang)
        .then(news => activity.news = news);
    }
    // metadata object id for translation is a concatenation of the news id and the lang
    if (activity.news.lang) {
      activity.templateParams.metadataObjectId = `${activity.news.id}-${activity.news.lang}`;
    }
  },
  canEdit: () => false,
  canShare: () => true,
  canDelete: () => false,
  canHide: () => true,
  hideOnDelete: true,
  supportsThumbnail: true,
  summaryLinesToDisplay: 2,
  windowTitlePrefixKey: 'news.window.title',
  addMargin: true,
  showFooter: true,
  getThumbnail: (activity) => activity?.news?.illustrationURL && `${activity?.news?.illustrationURL}&size=305x285` || '/content/images/news.webp',
  getThumbnailProperties: () => {
    return {
      height: '124px',
      width: '220px',
      noBorder: true,
    };},
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
    if (news?.properties?.summary) {
      return news?.properties?.summary;
    } else if (news?.body) {
      return Vue.prototype.$utils.htmlToText(news.body);
    }
    return '';
  },
  getActivityViews: (activity) => {
    if (activity?.news?.viewsCount > 0) {
      const news = activity?.news;
      const viewsCount = newsViews(news);
      return {
        tooltip: news?.viewsCount === 1 ? 'news.details.view' : 'news.details.views',
        originalViewsCount: news.viewsCount,
        viewsCount: viewsCount,
      };
    }
    return null;
  },
  reactionEnabled: (activity) => !activity?.news?.properties?.hideReaction
};

export async function initExtensions() {
  await i18nPromise;
  extensionRegistry.registerExtension('activity', 'type', {
    type: 'news',
    options: newsActivityTypeExtensionOptions,
  });
  extensionRegistry.registerComponent('ActivityComposerAction', 'activity-composer-action', {
    id: 'switchNewsButton',
    vueComponent: Vue.options.components['activity-switch-to-news'],
    rank: 10,
    isEnabled: () => !!eXo?.env?.portal?.spaceId
  });
  extensionRegistry.registerComponent('ActivityComposerFooterAction', 'activity-composer-footer-action', {
    id: 'writeNewsButton',
    vueComponent: Vue.options.components['activity-write-news-composer'],
    rank: 30,
    isEnabled: () => !!eXo?.env?.portal?.spaceId
  });
  extensionRegistry.registerComponent('ActivityToolbarAction', 'activity-toolbar-action', {
    id: 'writeNewsToolbarButton',
    vueComponent: Vue.options.components['activity-write-news-toolbar-action'],
    rank: 30,
    isEnabled: () => !!eXo?.env?.portal?.spaceId
  });

}
function newsViews(news) {
  if (news?.viewsCount < 1000) {
    return news?.viewsCount;
  }
  if (news?.viewsCount < 10000) {
    return `${(news?.viewsCount / 1000).toFixed(1)}k`;
  }
  if (news?.viewsCount < 1000000) {
    return `${parseInt(news?.viewsCount / 1000)}k`;
  }
  return '+999k';
}
