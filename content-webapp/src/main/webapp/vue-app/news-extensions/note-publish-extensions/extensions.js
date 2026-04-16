export function initPublishExtension(params) {
  extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
    id: 'publishNote',
    labelKey: 'notes.publication.publish.save.label',
    icon: 'fas fa-paper-plane',
    sortable: true,
    cssClass: 'ps-2 pe-4 action-menu-item',
    rank: 30,
    enabled: (note) => !note.draftPage && note.canManage && note?.wikiType === 'group' && note?.wikiOwner?.startsWith('/spaces/'),
    action: (vm) => {
      vm.$root.$emit('open-publish-drawer', {
        savedSettings: params.savedSettings,
        targets: params.targets,
        canPublish: params.canPublish,
        canSchedule: params.canSchedule,
      });
    }
  });

  extensionRegistry.registerExtension('Publication', 'note-publication-settings', {
    id: 'note-publication-settings',
    rank: 1,
    enabled: () => !!params,
    getSettings: () => {
      return params;
    }
  });
  document.dispatchEvent(new CustomEvent('publication-extensions-updated'));
}
