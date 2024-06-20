/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.news.listener;

import static io.meeds.news.utils.NewsUtils.NewsObjectType.ARTICLE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class NewsActivityListenerTest {

  @Mock
  private ActivityManager activityManager;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private SpaceService    spaceService;

  @Mock
  private NewsService     newsService;

  @InjectMocks
  NewsActivityListener    newsActivityListener;

  @Before
  public void setUp() {
    newsActivityListener.init();
  }
  @Test
  public void testNotShareWhenActivityNotFound() {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    newsActivityListener.shareActivity(event);

    verifyNoInteractions(newsService);
  }

  @Test
  public void testNotShareWhenActivityNotHavingTemplates() {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    when(event.getActivity()).thenReturn(activity);
    newsActivityListener.shareActivity(event);

    verifyNoInteractions(newsService);
  }

  @Test
  public void testNotShareWhenActivityNotSharedOne() {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    when(event.getActivity()).thenReturn(activity);
    Map<String, String> templateParams = mock(Map.class);
    when(activity.getTemplateParams()).thenReturn(templateParams);

    newsActivityListener.shareActivity(event);

    verifyNoInteractions(newsService);
  }

  @Test
  public void testNotShareWhenSharedActivityNotFound() {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    when(event.getActivity()).thenReturn(activity);
    Map<String, String> templateParams = mock(Map.class);
    when(activity.getTemplateParams()).thenReturn(templateParams);
    when(templateParams.containsKey("originalActivityId")).thenReturn(true);

    String originalActivityId = "originalActivityId";
    when(templateParams.get("originalActivityId")).thenReturn(originalActivityId);

    newsActivityListener.shareActivity(event);

    verifyNoInteractions(newsService);
  }

  @Test
  public void testNotShareWhenSharedActivityNotNewsActivity() {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    when(event.getActivity()).thenReturn(activity);
    Map<String, String> templateParams = mock(Map.class);
    when(activity.getTemplateParams()).thenReturn(templateParams);
    when(templateParams.containsKey("originalActivityId")).thenReturn(true);

    String originalActivityId = "originalActivityId";
    when(templateParams.get("originalActivityId")).thenReturn(originalActivityId);

    ExoSocialActivity sharedActivity = mock(ExoSocialActivity.class);
    when(activityManager.getActivity(originalActivityId)).thenReturn(sharedActivity);

    newsActivityListener.shareActivity(event);

    verifyNoInteractions(newsService);
  }

  @Test
  public void testNotShareWhenSharedActivityWhenNewsNotFound() throws Exception {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    when(event.getActivity()).thenReturn(activity);
    Map<String, String> templateParams = mock(Map.class);
    when(activity.getTemplateParams()).thenReturn(templateParams);
    when(templateParams.containsKey("originalActivityId")).thenReturn(true);

    String originalActivityId = "originalActivityId";
    when(templateParams.get("originalActivityId")).thenReturn(originalActivityId);

    ExoSocialActivity sharedActivity = mock(ExoSocialActivity.class);
    when(activityManager.getActivity(originalActivityId)).thenReturn(sharedActivity);

    Map<String, String> sharedTemplateParams = mock(Map.class);
    when(sharedActivity.getTemplateParams()).thenReturn(sharedTemplateParams);
    when(sharedTemplateParams.containsKey("newsId")).thenReturn(true);

    String newsId = "newsId";
    when(sharedTemplateParams.get("newsId")).thenReturn(newsId);
    org.exoplatform.services.security.Identity currentIdentity = new org.exoplatform.services.security.Identity("john");
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    newsActivityListener.shareActivity(event);

    verify(newsService, times(1)).getNewsById(newsId, currentIdentity, false, ARTICLE.name().toLowerCase());
    verify(newsService, never()).shareNews(nullable(News.class),
                                           nullable(Space.class),
                                           nullable(Identity.class),
                                           nullable(String.class));
  }

  @Test
  public void testShareWhenNewsFound() throws Exception {
    ActivityLifeCycleEvent event = mock(ActivityLifeCycleEvent.class);
    ExoSocialActivity activity = mock(ExoSocialActivity.class);
    when(event.getActivity()).thenReturn(activity);
    Map<String, String> templateParams = mock(Map.class);
    when(activity.getTemplateParams()).thenReturn(templateParams);
    when(templateParams.containsKey("originalActivityId")).thenReturn(true);

    String originalActivityId = "originalActivityId";
    when(templateParams.get("originalActivityId")).thenReturn(originalActivityId);

    ExoSocialActivity sharedActivity = mock(ExoSocialActivity.class);
    when(activityManager.getActivity(originalActivityId)).thenReturn(sharedActivity);

    ActivityStream activityStream = mock(ActivityStream.class);
    when(activity.getActivityStream()).thenReturn(activityStream);

    String spacePrettyName = "space1";
    when(activityStream.getPrettyId()).thenReturn(spacePrettyName);

    Map<String, String> sharedTemplateParams = mock(Map.class);
    when(sharedActivity.getTemplateParams()).thenReturn(sharedTemplateParams);
    when(sharedTemplateParams.containsKey("newsId")).thenReturn(true);

    String newsId = "newsId";
    when(sharedTemplateParams.get("newsId")).thenReturn(newsId);

    org.exoplatform.services.security.Identity currentIdentity = new org.exoplatform.services.security.Identity("john");
    ConversationState.setCurrent(new ConversationState(currentIdentity));

    News news = new News();
    when(newsService.getNewsById(newsId, currentIdentity, false, ARTICLE.name().toLowerCase())).thenReturn(news);

    newsActivityListener.shareActivity(event);

    verify(newsService, times(1)).getNewsById(newsId, currentIdentity, false, ARTICLE.name().toLowerCase());
    verify(newsService, times(1)).shareNews(eq(news), nullable(Space.class), nullable(Identity.class), nullable(String.class));
  }
}
