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
import {newsConstants} from '../../services/newsConstants.js';

/**
 * The illustration URL sized for the box a template draws it in: twice the
 * box, so a HiDPI screen gets one stored pixel per device pixel. The server
 * never upscales, so a source smaller than the ask comes back as stored. The
 * width stays above the height: on a square or taller ask the server fits to
 * height and a portrait source would come back narrower than the box.
 *
 * @param {string} illustrationUrl the article's illustration URL, with its query
 * @param {number} boxWidth the drawn box width, in CSS pixels
 * @param {number} boxHeight the drawn box height, in CSS pixels
 * @returns {string} the URL with its `size` ask
 */
export function illustrationSrc(illustrationUrl, boxWidth, boxHeight) {
  const height = 2 * boxHeight;
  const width = Math.max(2 * boxWidth, height + 1);
  return `${illustrationUrl}&size=${width}x${height}`;
}

export function getNewsListByTarget(targetName, offset, limit, returnSize) {
  return fetch(`${newsConstants.CONTENT_API}/contents/byTarget/${targetName}?offset=${offset}&limit=${limit}&returnSize=${returnSize}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting news list by target name');
    }
  });
}

export function getPostedNews(offset, limit, returnSize) {
  let url = `${newsConstants.CONTENT_API}/contents?author=${newsConstants.userName}&filter=all&expand=activityReactions`;
  if (newsConstants.SPACE_ID) {
    url += `&spaces=${newsConstants.SPACE_ID}`;
  }
  if (!Number.isNaN(offset)) {
    url += `&offset=${offset}`;
  }
  if (!Number.isNaN(limit)) {
    url += `&limit=${limit}`;
  }
  if (returnSize) {
    url += `&returnSize=${returnSize}`;
  }
  return fetch(url, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting news list');
    }
  });
}

export function searchArticles(searchTerm) {
  const url = `/social/rest/contentLinks/news/search?query=${searchTerm}&offset=0&limit=10`;
  return fetch(url, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error searching news');
    }
  });
}

export function getSelectedNewsList(newsIds, lang) {
  const formData = new FormData();
  newsIds?.forEach(id => formData.append('id', id));
  if (lang) {
    formData.append('lang', lang);
  }
  formData.append('expand', 'activityReactions');
  const urlParams = new URLSearchParams(formData).toString();
  const url = `${newsConstants.CONTENT_API}/contents/list?${urlParams}`;
  return fetch(url, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting news list');
    }
  });
}

export function saveSettings(saveSettingsURL, settings) {
  const formData = new FormData();
  if (settings) {
    Object.keys(settings).forEach(name => {
      formData.append(name, settings[name]);
    });
  }
  return fetch(saveSettingsURL.replaceAll('&amp;', '&'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    }
  });
}
