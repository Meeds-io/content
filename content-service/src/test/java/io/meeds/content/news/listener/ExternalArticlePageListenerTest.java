package io.meeds.content.news.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.wiki.model.Page;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class ExternalArticlePageListenerTest {

  @Mock
  private ListenerService             listenerService;

  @Mock
  private NewsService                 newsService;

  @Mock
  private IdentityRegistry            identityRegistry;

  @Mock
  private ActivityStorage             activityStorage;
  
  @Mock
  private MetadataService             metadataService;

  @Mock
  private CachedActivityStorage       cachedActivityStorage;

  private ExternalArticlePageListener externalArticlePageListener;

  @Before
  public void setUp() throws Exception {
    this.externalArticlePageListener = new ExternalArticlePageListener(listenerService,
                                                                       newsService,
                                                                       activityStorage,
                                                                       metadataService);
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
    note.setOwner("root");
    Identity identity = mock(Identity.class);

    Event<Object, Page> event = new Event<>("note.updated", "user", note);
    when(newsService.getNewsArticleById("1")).thenReturn(article);
    externalArticlePageListener.onEvent(event);
    verify(cachedActivityStorage, times(1)).clearActivityCached("1");

    List<MetadataItem> items = new ArrayList<>();
    items.add(new MetadataItem());
    when(metadataService.getMetadataItemsByObject(any(MetadataObject.class))).thenReturn(items);
    event = new Event<>("note.deleted", identity, note);
    externalArticlePageListener.onEvent(event);
    verify(newsService, times(1)).deleteNews("1", identity, "article");
  }
}
