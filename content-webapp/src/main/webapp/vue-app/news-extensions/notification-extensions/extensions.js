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
extensionRegistry.registerExtension('WebNotification', 'notification-group-extension', {
  rank: 50,
  name: 'news',
  plugins: [
    'MentionInNewsNotificationPlugin',
    'NewsSpaceWebNotificationPlugin',
    'PostNewsNotificationPlugin',
    'PublishNewsNotificationPlugin'
  ],
  icon: 'fa-newspaper',
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'MentionInNewsNotificationPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-post-news'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'PostNewsNotificationPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-post-news'],
});
extensionRegistry.registerExtension('WebNotification', 'notification-content-extension', {
  type: 'PublishNewsNotificationPlugin',
  rank: 10,
  vueComponent: Vue.options.components['user-notification-post-news'],
});
