package io.meeds.news.listener;

import static io.meeds.analytics.utils.AnalyticsUtils.addSpaceStatistics;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import io.meeds.news.model.News;
import io.meeds.news.utils.NewsUtils;

@Asynchronous
@Component
public class AnalyticsContentListener extends Listener<String, News> {

  private static final String   CREATE_CONTENT_OPERATION_NAME  = "createContent";

  private static final String   UPDATE_CONTENT_OPERATION_NAME  = "updateContent";

  private static final String   DELETE_CONTENT_OPERATION_NAME  = "deleteContent";

  private static final String   VIEW_CONTENT_OPERATION_NAME    = "viewContent";

  private static final String   SHARE_CONTENT_OPERATION_NAME   = "shareContent";

  private static final String   LIKE_CONTENT_OPERATION_NAME    = "likeContent";

  private static final String   COMMENT_CONTENT_OPERATION_NAME = "commentContent";

  private static final String[] LISTENER_EVENTS                = { "exo.news.postArticle", "exo.news.updateArticle",
      "exo.news.deleteArticle", "exo.news.viewArticle", "exo.news.shareArticle", "exo.news.commentArticle",
      "exo.news.likeArticle" };

  @Autowired
  private IdentityManager       identityManager;

  @Autowired
  private SpaceService          spaceService;

  @Autowired
  private ListenerService       listenerService;

  @PostConstruct
  public void init() {
    for (String listener : LISTENER_EVENTS) {
      listenerService.addListener(listener, this);
    }
  }

  @Override
  public void onEvent(Event<String, News> event) throws Exception {
    News news = event.getData();
    String operation = mapEventNameToOperation(event.getEventName());
    long userId = 0;
    Identity identity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, event.getSource());
    if (identity != null) {
      userId = Long.parseLong(identity.getId());
    }
    StatisticData statisticData = new StatisticData();

    statisticData.setModule("contents");
    statisticData.setSubModule("contents");
    statisticData.setOperation(operation);
    statisticData.setUserId(userId);
    statisticData.addParameter("contentId", news.getId());
    statisticData.addParameter("contentTitle", news.getTitle());
    statisticData.addParameter("contentAuthor", news.getAuthor());
    statisticData.addParameter("contentLastModifier", news.getUpdater());
    statisticData.addParameter("contentType", "News");
    statisticData.addParameter("contentUpdatedDate", news.getUpdateDate());
    statisticData.addParameter("contentCreationDate", news.getCreationDate());
    statisticData.addParameter("contentPublication", news.isPublished() ? "Yes" : "No");
    if (news.isPublished()
        && (operation.equals(CREATE_CONTENT_OPERATION_NAME) || operation.equals(UPDATE_CONTENT_OPERATION_NAME))) {
      statisticData.addParameter("contentPublicationAudience",
                                 news.getAudience().equals(NewsUtils.ALL_NEWS_AUDIENCE) ? "All users" : "Only space members");
    }
    Space space = getSpaceService().getSpaceById(news.getSpaceId());
    if (space != null) {
      addSpaceStatistics(statisticData, space);
    }
    AnalyticsUtils.addStatisticData(statisticData);
  }

  private String mapEventNameToOperation(String eventName) {
    switch (eventName) {
    case "exo.news.postArticle":
      return CREATE_CONTENT_OPERATION_NAME;
    case "exo.news.updateArticle":
      return UPDATE_CONTENT_OPERATION_NAME;
    case "exo.news.deleteArticle":
      return DELETE_CONTENT_OPERATION_NAME;
    case "exo.news.viewArticle":
      return VIEW_CONTENT_OPERATION_NAME;
    case "exo.news.shareArticle":
      return SHARE_CONTENT_OPERATION_NAME;
    case "exo.news.commentArticle":
      return COMMENT_CONTENT_OPERATION_NAME;
    case "exo.news.likeArticle":
      return LIKE_CONTENT_OPERATION_NAME;
    default:
      throw new IllegalArgumentException("Unknown event: " + eventName);
    }
  }

  public IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = ExoContainerContext.getService(IdentityManager.class);
    }
    return identityManager;
  }

  public SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = ExoContainerContext.getService(SpaceService.class);
    }
    return spaceService;
  }
}
