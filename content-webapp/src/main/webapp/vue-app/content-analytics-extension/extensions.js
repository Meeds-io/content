/*
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

extensionRegistry.registerExtension('AnalyticsSamples', 'SampleItem', {
  type: 'contentPublishingTargets',
  options: {
    rank: 80,
    vueComponent: Vue.options.components['content-publish-targets-attribute'],
    match: (fieldName) => fieldName === 'contentPublishingTargets'
  },
});

extensionRegistry.registerExtension('AnalyticsSamples', 'SampleItem', {
  type: 'contentCreator',
  options: {
    rank: 90,
    vueComponent: Vue.options.components['content-creator-attribute'],
    match: (fieldName) => ['contentCreator', 'contentAuthor', 'contentLastModifier'].includes(fieldName)
  },
});

extensionRegistry.registerExtension('AnalyticsSamples', 'SampleItem', {
  type: 'contentScheduling',
  options: {
    rank: 100,
    vueComponent: Vue.options.components['content-schedule-date-attribute'],
    match: (fieldName) => ['contentScheduling', 'contentUpdatedDate'].includes(fieldName)
  },
});

extensionRegistry.registerExtension('AnalyticsChart', 'FieldValueName', {
  type: 'contentPublishingTargets',
  match: (fieldName) => fieldName === 'contentPublishingTargets',
  getLabel: async (fieldName, fieldValue) => {
    try {
      const resp = await fetch(`/content/rest/targeting/${fieldValue}`, {
        method: 'GET',
        credentials: 'include',
      });
      if (!resp.ok) {
        return fieldValue;
      }
      const target = await resp.json();
      return target?.properties?.label;
    } catch (error) {
      console.error('Error fetching target info:', error);
      throw error;
    }
  }
});

extensionRegistry.registerExtension('AnalyticsSamples', 'SampleItem', {
  type: 'contentId',
  options: {
    rank: 110,
    vueComponent: Vue.options.components['content-id-sample-attribute'],
    match: fieldName => fieldName === 'contentId',
  }
});

extensionRegistry.registerExtension('AnalyticsChart', 'FieldValueName', {
  type: 'contentIdChart',
  match: (fieldName) => fieldName === 'contentId',
  getLabel: async (fieldName, fieldValue) => {
    const articleUrl = `/content/rest/contents/${fieldValue}?editMode=false&type=article&lang=${eXo.env.portal.language}`;
    const pageUrl = `/portal/rest/notes/note/${fieldValue}?includeDeleted=true`;
    try {
      const article = await fetchData(articleUrl);
      if (article?.title) {
        return `${article.title} (${fieldValue})`;
      }
      const page = await fetchData(pageUrl);
      if (page?.title && !page?.deleted) {
        return `${page.title} (${fieldValue})`;
      }
      return exoi18n.i18n.t('analytics.deletedContent');
    } catch (e) {
      return exoi18n.i18n.t('analytics.deletedContent');
    }
  }
});

async function fetchData(url) {
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
  });
  return await response?.json?.();
}
