<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2026 Meeds Association contact@meeds.io

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
  <v-dialog
    v-model="dialog"
    width="500px"
    content-class="uiPopup news-delete-note-dialog"
    max-width="100vw">
    <v-card class="elevation-12 transparent">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <a
          href="#"
          class="uiIconClose pull-right"
          :aria-label="$t('news.deleteNote.close')"
          :title="$t('news.deleteNote.close')"
          @click.prevent="answer(null)"></a>
        <span class="ignore-vuetify-classes text-title">{{ $t('news.deleteNote.title') }}</span>
      </div>
      <v-card-text>{{ $t('news.deleteNote.message') }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          class="ignore-vuetify-classes btn btn-primary me-2 delete-note-button"
          @click="answer('delete')">
          {{ $t('news.deleteNote.deleteNote') }}
        </button>
        <button
          class="ignore-vuetify-classes btn ms-2 keep-note-button"
          @click="answer('keep')">
          {{ $t('news.deleteNote.keepNote') }}
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
/**
 * Asks, before an article linked to a note is deleted, whether to delete the
 * note too. open() resolves with 'delete' or 'keep', or with null when the
 * dialog is closed without a choice (close icon, Escape, outside click): the
 * deletion is then abandoned.
 * Neither shared dialog fits: exo-confirm-dialog's cancel button and close icon
 * run the same close(), so "Keep the note" could not be told from a close, and
 * exo-modal emits no event when it closes.
 */
export default {
  data: () => ({
    dialog: false,
    resolveChoice: null,
  }),
  watch: {
    dialog() {
      if (!this.dialog) {
        this.answer(null);
      }
    },
  },
  methods: {
    open() {
      this.answer(null);
      this.dialog = true;
      return new Promise(resolve => this.resolveChoice = resolve);
    },
    answer(choice) {
      // The first answer wins: closing the dialog after a choice answers null
      const resolve = this.resolveChoice;
      this.resolveChoice = null;
      this.dialog = false;
      if (resolve) {
        resolve(choice);
      }
    },
  },
};
</script>
