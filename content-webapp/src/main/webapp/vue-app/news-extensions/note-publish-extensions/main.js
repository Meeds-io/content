import * as publishService from './publishService';
import {initPublishExtension} from './extensions.js';


document.addEventListener('note-navigation-updated', handleNoteNavigationUpdate);
document.addEventListener('publish-note', publishNote);

const spaceId = eXo.env.portal.spaceId;
const publicationParams = {};

function publishNote(event) {
  const {editPublication, article, scheduleSettings} = event.detail;
  if (editPublication) {
    publishService.updateNotePublication(scheduleSettings, article, spaceId).then(() => {
      emitNotePublished(true);
      updateSavedPublicationSettings(article.id);
    });
  } else {
    publishService.saveNoteArticle(article, spaceId).then((article) => {
      emitNotePublished(false, !!article?.schedulePostDate, article?.url);
      updateSavedPublicationSettings(article.id);
    });
  }
}

function handleNoteNavigationUpdate(event) {
  const {noteId} = event.detail;
  publishService.getSavedNotePublicationSettings(noteId).then(settings => {
    publicationParams.savedSettings = settings;
    publishService.canSchedule(spaceId, noteId).then(canSchedule => {
      publicationParams.canSchedule = canSchedule;
    }).catch(() => {
      initPublishExtension(publicationParams);
    });
  });
}

function updateSavedPublicationSettings(noteId) {
  publishService.getSavedNotePublicationSettings(noteId).then(settings => {
    publicationParams.savedSettings = settings;
    initPublishExtension(publicationParams);
  });
}

function extractNoteIdFormUrl() {
  const url = window.location.href;
  const regex = /\/notes\/(\d+)$/;
  const match = regex.exec(url);
  return match?.[1];
}

function emitNotePublished(edit, isPublishSchedule, link) {
  document.dispatchEvent(new CustomEvent('note-published', {
    detail: {
      editPublication: edit,
      isPublishSchedule: isPublishSchedule,
      link: link
    }
  }));
}

export async function init() {
  try {
    const noteId = extractNoteIdFormUrl();
    const [canPublish, savedSettings, targets] = await Promise.all([
      publishService.canPublish(spaceId),
      publishService.getSavedNotePublicationSettings(noteId),
      publishService.getNotePublishTargets()
    ]);
    publicationParams.canPublish = canPublish;
    publicationParams.savedSettings = savedSettings;
    publicationParams.targets = targets;
    try {
      publicationParams.canSchedule = await publishService.canSchedule(spaceId, noteId);
    } catch (err) {
      publicationParams.canSchedule = false;
    }
    initPublishExtension(publicationParams);
  } catch (err) {
    console.error('Failed to initialize publishing:', err);
    initPublishExtension(publicationParams);
  }
}

