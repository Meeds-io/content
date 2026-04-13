import * as publishService from './publishService.js';
import {initPublishExtension} from './extensions.js';

const treeViewNavigationTemplate = `
        <div 
          id="note-children-container" 
          class="navigation-img-wrapper" 
          contenteditable="false">
          <figure class="image-navigation" contenteditable="false">
            <img src="/notes/images/children.png" role="presentation"/>
            <img src="/notes/images/trash.png" id="remove-treeview" alt="remove treeview"/>
            <figcaption class="note-navigation-label">Navigation</figcaption>
          </figure>
        </div>
        <p></p>`;

document.addEventListener('note-navigation-updated', handleNoteNavigationUpdate);
document.addEventListener('publish-note', publishNote);

const spaceId = eXo.env.portal.spaceId;
const publicationParams = {};

function publishNote(event) {
  const {editPublication, article, scheduleSettings, extensionsCallback} = event.detail;
  if (editPublication) {
    publishService.updateNotePublication(scheduleSettings, article, spaceId).then(async () => {
      await executePublishExtensions(extensionsCallback, article.id);
      emitNotePublished(true);
      updateSavedPublicationSettings(article.id);
    });
  } else {
    checkInsertTocNavigationTemplate(article);
    publishService.saveNoteArticle(article, spaceId).then(async (article) => {
      await executePublishExtensions(extensionsCallback, article.id);
      emitNotePublished(false, !!article?.schedulePostDate, article?.url);
      updateSavedPublicationSettings(article.id);
    });
  }
}

async function executePublishExtensions(extensionsCallback, articleId) {
  const metadata = await extensionsCallback.executeExtensions();
  await publishService.updatePublishedNoteMetadata(articleId, metadata);
}

function checkInsertTocNavigationTemplate(article) {
  if (article.isHomeDefaultContent && article.hasChildren) {
    article.content = treeViewNavigationTemplate;
    publishService.updateArticlePage(article);
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

function extractNoteIdFromUrl() {
  const url = window.location.href;
  const regex = /\/notes\/(\d+)(?:\?|$)/;
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
    const wikiOwner = `/spaces/${eXo.env.portal.spaceGroup}`;
    const noteId = extractNoteIdFromUrl() ?? (await publishService.getNoteHomePage('group', wikiOwner))?.id;
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

