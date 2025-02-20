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
    match: (fieldName) => fieldName === 'contentScheduling'
  },
});

extensionRegistry.registerExtension('AnalyticsChart', 'FieldValueName', {
  type: 'contentPublishingTargets',
  match: (fieldName) => fieldName === 'contentPublishingTargets',
  getLabel: (fieldName, fieldValue) =>  fieldValue
});
