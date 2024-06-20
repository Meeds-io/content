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

export function getAllTargets() {
  return fetch(`${newsConstants.CONTENT_API}/targeting`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}
export function getAllowedTargets() {
  return fetch(`${newsConstants.CONTENT_API}/targeting/allowed`, {
    credentials: 'include',
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    }
  });
}

export function deleteTargetByName(targetName, delay) {
  if (delay > 0) {
    localStorage.setItem('deletedNewsTarget', targetName);
  }
  return fetch(`${newsConstants.CONTENT_API}/targeting/${targetName}?delay=${delay || 0}`, {
    credentials: 'include',
    method: 'DELETE'
  }).then((resp) => {
    if (resp && !resp.ok) {
      throw new Error('Error when deleting news target');
    }
  });
}

export function undoDeleteTarget(targetName) {
  return fetch(`${newsConstants.CONTENT_API}/targeting/${targetName}/undoDelete`, {
    method: 'POST',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      localStorage.removeItem('deletedNewsTarget');
    } else {
      throw new Error('Error when undoing deleting news target');
    }
  });
}

export function createTarget(target) {
  return fetch(`${newsConstants.CONTENT_API}/targeting`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(target),
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else  {
      throw new Error(resp.status);
    }
  });
}

export function updateTarget(target, originalTargetName) {
  return fetch(`${newsConstants.CONTENT_API}/targeting/${originalTargetName}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(target),
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.status;
    } else {
      throw new Error('Error when updating news target');
    }
  });
}