import * as newsTargetingService from '../../services/newsTargetingService.js';
import * as newsService from '../../services/newsServices.js';

export async function getNotePublishTargets() {
  const targets = await newsTargetingService.getAllowedTargets();
  return targets.map(target => ({
    name: target.name,
    label: target?.properties?.label,
    tooltipInfo: `${target?.properties?.label}: ${target?.properties?.description || ''}`,
    description: target?.properties?.description,
    restrictedAudience: target?.restrictedAudience,
  }));
}

export function canPublish(spaceId) {
  return newsService.canPublishNews(spaceId);
}

export function canSchedule(spaceId, articleId) {
  return newsService.canScheduleNews(spaceId, articleId, true);
}

export async function getSavedNotePublicationSettings(id, lang) {
  try {
    const article = await newsService.getNewsById(id, false, 'article', lang);
    if (!article || article.status === 404) {
      return null;
    }
    return {
      activityPosted: article.activityPosted,
      published: article.published,
      targets: article.targets,
      audience: article.audience,
      schedulePostDate: article.schedulePostDate,
      scheduleUnpublishDate: article.scheduleUnpublishDate,
      fromExternalPage: article.fromExternalPage,
      properties: article.properties ?? {}
    };
  } catch (error) {
    console.error(error);
    throw error;
  }
}

export function getNoteHomePage(type, owner) {
  return newsService.getPageByTypeOwnerAndName(type, owner, 'Home');
}

export function updateArticlePage(page) {
  return newsService.updateArticlePage(page);
}

export function saveNoteArticle(article, spaceId) {
  article = noteToArticle(article, spaceId);
  if (article?.schedulePostDate) {
    article.publicationState = 'staged';
  }
  if (article.scheduleUnpublishDate || article?.schedulePostDate) {
    article.timeZoneId = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
  }
  if (article.publicationState === 'staged') {
    return newsService.scheduleNews(article, 'existing_page');
  } else {
    return newsService.saveNews(article);
  }
}

export function updateNotePublication(scheduleSettings, article, spaceId) {
  const editScheduleAction = scheduleSettings?.editScheduleAction;
  article = noteToArticle(article, spaceId);
  article.timeZoneId = new window.Intl.DateTimeFormat().resolvedOptions().timeZone;
  switch (editScheduleAction) {
  case 'schedule':
    article.publicationState = scheduleSettings?.postDate ? 'staged' : '';
    return newsService.scheduleNews(article, 'article');
  case 'cancel_schedule':
    article.schedulePostDate = 0;
    article.publicationState = 'draft';
    return newsService.saveNews(article);
  case 'publish_now':
    article.schedulePostDate = 0;
    article.publicationState = 'posted';
    return newsService.saveNews(article);
  default:
    article.publicationState = 'posted';
    return newsService.updateNews(article, article.activityPosted, 'article', 'POSTING_AND_PUBLISHING');
  }
}

function noteToArticle(note, spaceId) {
  return {
    id: note.id,
    title: note.title,
    body: note.content,
    author: note.author,
    published: note.published,
    targets: note.targets,
    spaceId: spaceId,
    publicationState: 'posted',
    schedulePostDate: note.schedulePostDate,
    timeZoneId: null,
    activityPosted: note.activityPosted,
    audience: note.audience,
    draftPage: false,
    fromExternalPage: true,
    scheduleUnpublishDate: note.scheduleUnpublishDate,
    properties: note?.properties
  };
}
