<!--
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
-->

<template>
  <v-chip class="ms-1 mb-1">
    {{ targetLabel }}
  </v-chip>
</template>

<script>
export default {
  data() {
    return {
      targetInfo: null
    };
  },
  props: {
    targetName: {
      type: String,
      default: null
    }
  },
  computed: {
    targetLabel() {
      return this.targetInfo?.properties?.label;
    }
  },
  created() {
    this.getTargetInfo().then(data => {
      this.targetInfo = data;
    });
  },
  methods: {
    async getTargetInfo() {
      try {
        const resp = await fetch(`/content/rest/targeting/${this.targetName}`, {
          method: 'GET',
          credentials: 'include',
        });
        if (!resp.ok) {
          return;
        }
        return await resp.json();
      } catch (error) {
        console.error('Error fetching article:', error);
        throw error;
      }
    }
  }
};
</script>
