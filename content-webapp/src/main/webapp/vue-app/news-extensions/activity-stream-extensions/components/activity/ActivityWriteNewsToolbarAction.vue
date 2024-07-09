<!--

    This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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

-->
<template>
  <v-btn
    id="writeNewsBtnToolbar"
    :ripple="false"
    class="d-flex flex-row align-center py-2"
    text
    @click="switchToNews">
    <v-icon
      color="grey darken-1"
      size="27">
      fa-newspaper
    </v-icon>
    <span class="font-weight-bold ms-5 dark-grey-color">
      {{ $t('news.title') }}
    </span>
  </v-btn>
</template>
<script>
export default {
  props: {
    activityId: {
      type: String,
      default: null,
    },
    message: {
      type: String,
      default: null,
    },
    templateParams: {
      type: Object,
      default: null,
    },
    files: {
      type: Array,
      default: null,
    },
    activityType: {
      type: Array,
      default: null,
    },
  },
  methods: {
    switchToNews() {
      let url = `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/news/editor`;
      if (eXo.env.portal.spaceId) {
        url += `?spaceId=${eXo.env.portal.spaceId}&type=draft`;
      }
      window.open(url, '_blank');
    },
  },
};
</script>
