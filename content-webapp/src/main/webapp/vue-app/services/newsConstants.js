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
 
export const newsConstants = {
  PORTAL: eXo.env.portal.context || '',
  PORTAL_NAME: eXo.env.portal.portalName || '',
  CONTAINER_NAME: eXo.env.portal.containerName || '',
  PORTAL_REST: eXo.env.portal.rest,
  NEWS_API: `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/news`,
  SOCIAL_ACTIVITY_API: `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/activities`,
  SOCIAL_SPACE_API: `${eXo.env.portal.context}/${eXo.env.portal.rest}/v1/social/spaces`,
  SOCIAL_SPACES_SUGGESTION_API: `${eXo.env.portal.context}/${eXo.env.portal.rest}${eXo.env.portal.context}/social/spaces/suggest.json`,
  SOCIAL_SPACES_SEARCH_API: `${eXo.env.portal.context}/${eXo.env.portal.rest}/space/user/searchSpace/`,
  SPACE_ID: eXo.env.portal.spaceId,
  HOST_NAME: window.location.host,
  UPLOAD_API: `${eXo.env.portal.context}/upload`,
  MAX_UPLOAD_SIZE: 10,
  MAX_UPLOAD_FILES: 1,
  userName: eXo.env.portal.userName
};

export const newsUpdateType = {
  CONTENT: 'content',
  POSTING_AND_PUBLISHING: 'postingAndPublishing',
  SCHEDULE: 'schedule'
};

export const newsObjectType = {
  DRAFT: 'draft',
  ARTICLE: 'article',
  LATEST_DRAFT: 'latest_draft'
};
