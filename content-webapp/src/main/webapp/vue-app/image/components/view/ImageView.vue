<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io

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
  <component
    v-bind="linkUrl && {
      href: linkUrl,
      target: $root.imageLinkTarget,
      rel,
    }"
    :is="linkUrl ? 'a' : 'div'"
    class="d-block full-width full-height"
    @focus="isFocused = true"
    @blur="isFocused = false">
    <img
      v-if="$root.imageUrl"
      :src="$root.imageUrl"
      :alt="$root.imageAltText"
      :aria-label="(!!linkUrl && !$root.imageAltText) ? $t('image.label.accessLink') : null"
      :width="$root.fixedHeight && `${width}px` || '100%'"
      :height="$root.fixedHeight && `${$root.fixedHeight}px` || '100%'"
      :class="cssClass"
      :style="[cssStyle, imageFocusStyle]"
      class="border-box-sizing">
  </component>
</template>
<script>
export default {
  data() {
    return {
      isFocused: false
    };
  },
  computed: {
    appWidth() {
      return this.$root.imageAspectRatio * this.$root.imageHeight;
    },
    imgWidth() {
      return this.$root.formatAspectRatio * this.$root.imageHeight;
    },
    width() {
      return Math.max(this.appWidth, this.imgWidth);
    },
    appHeight() {
      return this.$root.fixedHeight;
    },
    imgHeight() {
      return this.width / this.$root.formatAspectRatio;
    },
    height() {
      return Math.max(this.appHeight, this.imgHeight);
    },
    cssClass() {
      return this.$root.fixedHeight
        && ((this.imgWidth < this.appWidth) && 'absolute-vertical-center' || 'absolute-horizontal-center t-0')
        || 'fill-height fill-width';
    },
    cssStyle() {
      return this.$root.fixedHeight && {
        height: `${this.height}px`,
        'width': `${this.width}px`,
        'min-width': `${this.width}px`,
      };
    },
    linkUrl() {
      return this.$utils.toLinkUrl(this.$root.imageLinkUrl, {
        urls: true,
        email: true,
        phone: true,
      });
    },
    rel() {
      return this.$root.imageLinkTarget === '_blank' && 'noopener noreferrer' || null;
    },
    imageFocusStyle() {
      return this.isFocused && { border: '2px dashed var(--allPagesPrimaryColor, @primaryColorDefault)'};
    }
  }
};
</script>