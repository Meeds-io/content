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
 
import {newsConstants} from '../services/newsConstants.js';
import {newsUpdateType} from '../services/newsConstants.js';

export function getNewsById(id, editMode, type, lang) {
  return fetch(`${newsConstants.CONTENT_API}/contents/${id}?editMode=${editMode || ''}&type=${type || ''}&lang=${lang || ''}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if ((resp && resp.ok)) {
      return resp.json();
    } else if ( resp.status === 401) {
      return resp.status;
    }
  }).catch((error) => {
    return error;
  });
}

export function getNewsByActivityId(activityId, lang) {
  return fetch(`${newsConstants.CONTENT_API}/contents/byActivity/${activityId}?lang=${lang || ''}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    } else {
      return resp.json();
    }
  });
}

export function getNewsSpaces(newsId) {
  return fetch(`${newsConstants.CONTENT_API}/contents/${newsId}?fields=spaces&type=article`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => resp.json()).then(resp => {
    return resp;
  });
}

export function markNewsAsRead(newsId){
  return fetch(`${newsConstants.CONTENT_API}/contents/markAsRead/${newsId}`, {
    credentials: 'include',
    method: 'POST',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.text();
    } else {
      throw new Error('Error while marking news as read');
    }
  });
}

export function getNews(filter, spaces, searchText, offset, limit, returnSize) {
  let url = `${newsConstants.CONTENT_API}/contents?author=${newsConstants.userName}&filter=${filter}`;
  if (searchText) {
    if (searchText.indexOf('#') === 0) {
      searchText = searchText.replace('#', '%23');
    } 
    url += `&text=${searchText}`;
  }
  if (spaces) {
    url += `&spaces=${spaces}`;
  }
  if (!isNaN(offset)) {
    url += `&offset=${offset}`;
  }
  if (!isNaN(limit)) {
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
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error getting news list');
    }
  });
}

export function saveNews(news) {
  return fetch(`${newsConstants.CONTENT_API}/contents`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'POST',
    body: JSON.stringify(news)
  }).then((data) => {
    return data.json();
  });
}

export function scheduleNews(news, newsType) {
  return fetch(`${newsConstants.CONTENT_API}/contents/schedule?type=${newsType || ''}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'PATCH',
    body: JSON.stringify(news)
  }).then((data) => {
    return data.json();
  });
}

export function importFileFromUrl(url) {
  return fetch(url, {
    headers: {
      'Content-Type': 'blob',
    },
    credentials: 'include',
    method: 'GET',
  });
}

export function updateNews(news, post, type, updateType) {
  return fetch(`${newsConstants.CONTENT_API}/contents/${news.id}?post=${post}&type=${type || ''}&newsUpdateType=${updateType || newsUpdateType.CONTENT_AND_TITLE}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'PUT',
    body: JSON.stringify(news)
  }).then(resp => {
    if (resp && resp.ok) {
      return resp.json();
    }
    throw new Error(`Error when updating article with id ${news.id}`);
  });
}

export function findUserSpaces(spaceName) {
  return fetch(`${newsConstants.SOCIAL_SPACES_SUGGESTION_API}?conditionToSearch=${spaceName}&currentUser=${newsConstants.userName}&typeOfRelation=confirmed`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then(resp => resp.json()).then(json => json.options);
}

export function deleteDraft(newsId) {
  return fetch(`${newsConstants.CONTENT_API}/contents/${newsId}`, {
    credentials: 'include',
    method: 'DELETE'
  });
}

export function getSpaceById(id) {
  return fetch(`${newsConstants.SOCIAL_SPACE_API}/${id}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error(`Error getting space with id ${id}`);
    }
  });
}

export function getUserSpaces(offset, limit , filterType) {
  return fetch(`${newsConstants.SOCIAL_SPACE_API}?offset=${offset}&limit=${limit}&returnSize=true&filterType=${filterType}`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => resp.json()).then(resp => {
    return resp;
  });
}

export function searchSpaces(searchText) {
  return fetch(`${newsConstants.SOCIAL_SPACES_SEARCH_API}?fields=id,url,displayName,avatarUrl&keyword=${searchText}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then(resp => resp.json());
}

export function escapeHTML(unsafeText) {
  const div = document.createElement('div');
  div.innerText = unsafeText;
  return div.innerHTML;
}

export function canUserCreateNews(spaceId) {
  return fetch(`${newsConstants.CONTENT_API}/contents/canCreateNews/${eXo.env.portal.spaceId || spaceId}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => resp && resp.ok && resp.json());
}

export function canScheduleNews(spaceId) {
  return fetch(`${newsConstants.CONTENT_API}/contents/canScheduleNews/${eXo.env.portal.spaceId || spaceId}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => resp && resp.ok && resp.json());
}

export function deleteNews(newsId, newsObjectType, delay) {
  if (delay > 0) {
    localStorage.setItem('deletedNews', newsId);
  }
  return fetch(`${newsConstants.CONTENT_API}/contents/${newsId}?type=${newsObjectType || ''}&delay=${delay || 0}`, {
    credentials: 'include',
    method: 'DELETE'
  }).then((resp) => {
    if (resp && !resp.ok) {
      throw new Error('Error when deleting news');
    }
  });
}

export function undoDeleteNews(newsId) {
  return fetch(`${newsConstants.CONTENT_API}/contents/${newsId}/undoDelete`, {
    method: 'POST',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      localStorage.removeItem('deletedNews');
    } else {
      throw new Error('Error when undoing deleting news');
    }
  });
}

export function canPublishNews(spaceId) {
  return fetch(`${newsConstants.CONTENT_API}/contents/canPublishNews?spaceId=${spaceId || eXo.env.portal.spaceId}`, {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then((resp) => resp.json()).then(resp => {
    return resp;
  });
}

export function deleteArticleTranslation(newsId, lang) {
  return fetch(`${newsConstants.CONTENT_API}/contents/translation/${newsId}?lang=${lang}`, {
    credentials: 'include',
    method: 'DELETE'
  }).then((resp) => {
    if (resp && !resp.ok) {
      throw new Error('Error when deleting article translation');
    }
  });
}

export function getAvailableLanguages() {
  const lang = eXo?.env.portal.language || 'en';
  return fetch(`${newsConstants.PORTAL}/${newsConstants.PORTAL_REST}/notes/languages?lang=${lang}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp || !resp.ok) {
      throw new Error('Response code indicates a server error', resp);
    } else {
      return resp.json();
    }
  });
}

export function getArticleLanguages(articleId, withDrafts) {
  return fetch(`${newsConstants.CONTENT_API}/contents/translation/${articleId}?withDrafts=${withDrafts}`, {
    credentials: 'include',
    method: 'GET'
  }).then((resp) => {
    if (resp && !resp.ok) {
      throw new Error('Error when getting article languages');
    }
    return resp.json();
  });
}
