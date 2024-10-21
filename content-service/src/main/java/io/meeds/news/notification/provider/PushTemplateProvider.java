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
package io.meeds.news.notification.provider;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

import org.gatein.common.text.EntityEncoder;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.utils.TimeConvertUtils;

import io.meeds.news.notification.plugin.MentionInNewsNotificationPlugin;
import io.meeds.news.notification.plugin.PostNewsNotificationPlugin;
import io.meeds.news.notification.plugin.PublishNewsNotificationPlugin;
import io.meeds.news.notification.utils.NotificationConstants;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = PostNewsNotificationPlugin.ID, template = "war:/notification/templates/push/postNewsNotificationPlugin.gtmpl"),
    @TemplateConfig(pluginId = MentionInNewsNotificationPlugin.ID, template = "war:/notification/templates/push/postNewsNotificationPlugin.gtmpl"),
    @TemplateConfig(pluginId = PublishNewsNotificationPlugin.ID, template = "war:/notification/templates/push/postNewsNotificationPlugin.gtmpl") })
public class PushTemplateProvider extends TemplateProvider {
  protected static Log       log = ExoLogger.getLogger(PushTemplateProvider.class);

  private final SpaceService spaceService;

  public PushTemplateProvider(InitParams initParams, SpaceService spaceService) {

    super(initParams);
    this.spaceService = spaceService;
    this.templateBuilders.put(PluginKey.key(PostNewsNotificationPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(MentionInNewsNotificationPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(PublishNewsNotificationPlugin.ID), new TemplateBuilder());
  }

  private class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String pluginId = notification.getKey().getId();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);

      String newsId = notification.getValueOwnerParameter(NotificationConstants.NEWS_ID);
      String contentAuthor = notification.getValueOwnerParameter(NotificationConstants.CONTENT_AUTHOR);
      String currentUser = notification.getValueOwnerParameter(NotificationConstants.CURRENT_USER);
      String contentTitle = notification.getValueOwnerParameter(NotificationConstants.CONTENT_TITLE);
      String contentSpaceName = notification.getValueOwnerParameter(NotificationConstants.CONTENT_SPACE);
      String illustrationUrl = notification.getValueOwnerParameter(NotificationConstants.ILLUSTRATION_URL);
      String authorAvatarUrl = notification.getValueOwnerParameter(NotificationConstants.AUTHOR_AVATAR_URL);
      String activityLink = notification.getValueOwnerParameter(NotificationConstants.ACTIVITY_LINK);
      String context = notification.getValueOwnerParameter(NotificationConstants.CONTEXT);
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();
      templateContext.put("CONTENT_TITLE", encoder.encode(contentTitle));
      templateContext.put(NotificationConstants.CONTENT_SPACE, encoder.encode(contentSpaceName));
      templateContext.put("CONTENT_AUTHOR", encoder.encode(contentAuthor));
      templateContext.put("CURRENT_USER", currentUser);
      templateContext.put("ILLUSTRATION_URL", encoder.encode(illustrationUrl));
      templateContext.put("AUTHOR_AVATAR_URL", encoder.encode(authorAvatarUrl));
      Space space = spaceService.getSpaceById(String.valueOf(notification.getSpaceId()));
      StringBuilder activityUrl = new StringBuilder();
      String portalName = PortalContainer.getCurrentPortalContainerName();
      String portalOwner = CommonsUtils.getCurrentPortalOwner();
      if (pluginId.equals(PublishNewsNotificationPlugin.ID) && !spaceService.isMember(space, notification.getTo())) {
        activityUrl.append("/").append(portalName).append("/").append(portalOwner).append("/news/detail?newsId=").append(newsId);
      } else {
        activityUrl.append(activityLink);
      }
      templateContext.put("ACTIVITY_LINK", encoder.encode(activityUrl.toString()));
      templateContext.put("CONTEXT", encoder.encode(context));
      templateContext.put("READ",
                          Boolean.TRUE.equals(Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey()))) ? "read"
                                                                                                                                                     : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      Calendar lastModified = Calendar.getInstance();
      lastModified.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("LAST_UPDATED_TIME",
                          TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                                                                       "EE, dd yyyy",
                                                                       new Locale(language),
                                                                       TimeConvertUtils.YEAR));
      //
      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      messageInfo.subject(activityUrl.toString());
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }

  }
}
