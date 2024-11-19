package io.meeds.news.listener;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.wiki.model.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExternalArticlePageListenerTest {

  @Mock
  private ListenerService             listenerService;

  @Mock
  private NewsService                 newsService;

  @Mock
  private ActivityStorage             activityStorage;

  @Mock
  private CachedActivityStorage       cachedActivityStorage;

  private ExternalArticlePageListener externalArticlePageListener;

  @Before
  public void setUp() throws Exception {
    this.externalArticlePageListener = new ExternalArticlePageListener(listenerService, newsService, activityStorage);
    Field field = this.externalArticlePageListener.getClass().getDeclaredField("cachedActivityStorage");
    field.setAccessible(true);
    field.set(externalArticlePageListener, cachedActivityStorage);
  }

  @Test
  public void onEvent() throws Exception {
    News article = new News();
    article.setId("1");
    article.setActivityId("1");
    Page note = new Page();
    note.setId("1");
    note.setWikiOwner("/spaces/space");
    note.setContent("tes");
    Event<String, Page> event = new Event<>("note.updated", "user", note);
    when(newsService.getNewsArticleById("1")).thenReturn(article);
    externalArticlePageListener.onEvent(event);
    verify(cachedActivityStorage, times(1)).clearActivityCached("1");
  }
}
