/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
const CONTENT_LIST_API = '/content/rest/contents/all';

export function getContentTypes() {
  return fetch(`${CONTENT_LIST_API}/types`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting content types');
    }
  });
}

export function getContentList(filter) {
  const params = new URLSearchParams();
  if (filter?.contentTypes?.length) {
    params.set('contentTypes', filter.contentTypes.join(','));
  }
  if (filter?.status) {
    params.set('status', filter.status);
  }
  if (filter?.spaces?.length) {
    params.set('spaces', filter.spaces.join(','));
  }
  if (filter?.categoryId) {
    params.set('categoryId', filter.categoryId);
  }
  if (filter?.text) {
    params.set('text', filter.text);
  }
  if (filter?.includeCategoryIds?.length) {
    params.set('includeCategoryIds', filter.includeCategoryIds.join(','));
  }
  if (filter?.excludeCategoryIds?.length) {
    params.set('excludeCategoryIds', filter.excludeCategoryIds.join(','));
  }
  params.set('offset', filter?.offset || 0);
  params.set('limit', filter?.limit || 20);

  return fetch(`${CONTENT_LIST_API}?${params.toString()}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting content list');
    }
  });
}

export function saveSettings(saveSettingsURL, settings) {
  const formData = new FormData();
  if (settings) {
    Object.keys(settings).forEach(name => formData.append(name, settings[name]));
  }
  return fetch(saveSettingsURL.replaceAll('&amp;', '&'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error saving content list settings');
    }
  });
}

export function deleteContent(item) {
  const params = new URLSearchParams();
  params.set('contentType', item.contentType);
  if (item.draft) {
    params.set('status', 'draft');
  }
  return fetch(`${CONTENT_LIST_API}/${item.id}?${params.toString()}`, {
    method: 'DELETE',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error deleting content');
    }
  });
}
