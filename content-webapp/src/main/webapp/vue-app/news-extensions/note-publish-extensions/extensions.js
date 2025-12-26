export function initPublishExtension(params) {
  extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
    id: 'publishNote',
    labelKey: 'notes.publication.publish.save.label',
    icon: 'fas fa-paper-plane',
    sortable: true,
    cssClass: 'ps-2 pe-4 action-menu-item',
    rank: 30,
    enabled: (note) => !note.draftPage && note.canManage && eXo.env.portal.spaceId,
    action: (vm) => {
      vm.$root.$emit('open-publish-drawer', {
        savedSettings: params.savedSettings,
        targets: params.targets,
        canPublish: params.canPublish,
        canSchedule: params.canSchedule,
      });
    }
  });
}
