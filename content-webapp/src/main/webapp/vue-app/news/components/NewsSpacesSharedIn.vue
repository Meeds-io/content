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
  <div class="sharedIn">
    <span class="newsSpaceLabel">{{ $t('news.app.sharedIn') }}</span>
    <div class="sharedInSpaces">
      <a
        v-for="(act,index) in sharedActivities.slice(0, 2)"
        :key="index"
        class="space"
        target="_blank"
        @click="openSharedInSpacesDrawer()">
        <img
          :src="act.spaceAvatar"
          :title="act.spaceDisplayName"
          rel="tooltip"
          data-placement="top">
      </a>
      <a
        v-if="activitiesList.length > 2"
        class="plusSpaces"
        target="_blank"
        @click="openSharedInSpacesDrawer()">
        +{{ activitiesList.length - 2 }}
      </a>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    newsId: {
      type: String,
      default: ''
    },
    activities: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      sharedActivities: [],
      sharedInSpacesUpdated: false,
      open: false,
      maxDisplayedSpaces: 2,
      activitiesList: this.activities.split(';').filter((activity,index) => index > 0 && activity)
    };
  },
  watch: {
    activities() {
      this.activitiesList = this.activities.split(';').filter((activity,index) => index > 0 && activity);
      this.updateSharedInSpaces();
    }
  },
  created() {
    let activitiesList = this.activitiesList.slice();
    if (this.activitiesList.length > this.maxDisplayedSpaces){
      activitiesList = activitiesList.slice(0, this.maxDisplayedSpaces);
    }
    activitiesList.forEach(activity => {
      const spaceId = activity.split(':')[0];
      if (spaceId) {
        this.$newsServices.getSpaceById(spaceId).then(space => {
          space.avatarUrl = space.avatarUrl ? space.avatarUrl : '/eXoSkin/skin/images/system/SpaceAvtDefault.png';
          this.sharedActivities.push({
            spaceId: spaceId,
            spaceAvatar: space.avatarUrl,
            spaceDisplayName: space.displayName,
            activityUrl: `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${activity.split(':')[1]}`
          });
          this.sharedActivities.sort((a, b) => a.spaceDisplayName.toLowerCase().localeCompare(b.spaceDisplayName.toLowerCase()));
        });
      }
    });
  },
  methods: {
    openSharedInSpacesDrawer() {
      if (!this.sharedInSpacesUpdated) {
        this.updateSharedInSpaces();
      }
      this.$root.$emit('news-spaces-drawer-open', this.sharedActivities);
      this.open = true;
    },
    closeSharedInSpacesDrawer(){
      this.open = false;
      document.body.style.overflow = 'auto';
    },
    updateSharedInSpaces() {
      this.sharedActivities = [];
      this.$newsServices.getNewsSpaces(this.newsId).then(news => {
        this.activitiesList.forEach(activity => {
          const spaceId = activity.split(':')[0];
          if (spaceId && news.sharedInSpacesList.includes(spaceId)) {
            this.$newsServices.getSpaceById(spaceId).then(sharedInSpace => {
              if (sharedInSpace) {
                sharedInSpace.avatarUrl = sharedInSpace.avatarUrl ? sharedInSpace.avatarUrl : '/eXoSkin/skin/images/system/SpaceAvtDefault.png';
                this.sharedActivities.push({
                  spaceId: spaceId,
                  spaceAvatar: sharedInSpace.avatarUrl,
                  spaceDisplayName: sharedInSpace.displayName,
                  activityUrl: `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${activity.split(':')[1]}`
                });
                this.sharedActivities.sort((a, b) => a.spaceDisplayName.toLowerCase().localeCompare(b.spaceDisplayName.toLowerCase()));
              }
            });
          }
        });
        this.sharedInSpacesUpdated = true;
      });
    }
  }
};
</script>