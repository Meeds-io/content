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
  <exo-modal
    ref="modal"
    :title="$t('news.deleteNote.title')"
    class="news-delete-note-dialog"
    hide-actions
    @dialog-closed="answer(null)">
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
  </exo-modal>
</template>
<script>
/**
 * Asks, before an article linked to a note is deleted, whether to delete the
 * note too. open() resolves with 'delete' or 'keep', or with null when the
 * modal is closed without a choice (close icon, Escape, outside click): the
 * deletion is then abandoned. exo-confirm-dialog is not used: its cancel
 * button and its close icon run the same close(), so "Keep the note" could not
 * be told from a close.
 */
export default {
  data: () => ({
    resolveChoice: null,
  }),
  methods: {
    open() {
      this.answer(null);
      this.$refs.modal.open();
      return new Promise(resolve => this.resolveChoice = resolve);
    },
    answer(choice) {
      // The first answer wins: the dialog-closed that follows a choice
      // answers null to no one
      const resolve = this.resolveChoice;
      this.resolveChoice = null;
      this.$refs.modal.close();
      if (resolve) {
        resolve(choice);
      }
    },
  },
};
</script>
