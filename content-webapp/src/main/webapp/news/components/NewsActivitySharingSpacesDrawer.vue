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
  <v-app class="newsSpacesSharedInDrawer">
    <exo-drawer
      ref="newsSpacesSharedInDrawer"
      body-classes="hide-scroll decrease-z-index-more"
      right>
      <template slot="title">
        {{ $t('news.app.sharedInSpaces') }}
      </template>
      <template slot="content">
        <v-list>
          <v-list-item
            v-for="(act) in sharedActivities"
            :key="act"
            class="spaceSharedIn">
            <v-list-item-avatar>
              <v-avatar size="28" class="rounded">
                <v-img :src="act.spaceAvatar" :alt="act.spaceAvatar" />
              </v-avatar>
            </v-list-item-avatar>
            <v-list-item-content class="text-truncate">
              {{ act.spaceDisplayName }}
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                :href="act.activityUrl"
                class="btn text-capitalize">
                {{ $t('news.app.viewArticle') }}
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list>
      </template>
    </exo-drawer>
  </v-app>
</template>

<script>
export default {
  data: () => ({
    sharedActivities: []
  }),
  created() {
    this.$root.$on('news-spaces-drawer-open', sharedActivities => {
      this.sharedActivities = sharedActivities;
      this.open();
    });
  },
  methods: {
    open() {
      if (this.$refs.newsSpacesSharedInDrawer) {
        this.$refs.newsSpacesSharedInDrawer.open();
      }
    },
  }
};
</script>