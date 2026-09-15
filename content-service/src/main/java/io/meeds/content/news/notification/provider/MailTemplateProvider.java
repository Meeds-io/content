/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.content.news.notification.provider;

import java.util.Calendar;
import java.util.Locale;

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
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.TimeConvertUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;

import io.meeds.content.news.notification.plugin.MentionInNewsNotificationPlugin;
import io.meeds.content.news.notification.plugin.PostNewsNotificationPlugin;
import io.meeds.content.news.notification.plugin.PublishNewsNotificationPlugin;
import io.meeds.content.news.notification.utils.NotificationConstants;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = PostNewsNotificationPlugin.ID, template = "war:/notification/templates/mail/postNewsNotificationPlugin.gtmpl"),
    @TemplateConfig(pluginId = MentionInNewsNotificationPlugin.ID, template = "war:/notification/templates/mail/postNewsNotificationPlugin.gtmpl"),
    @TemplateConfig(pluginId = PublishNewsNotificationPlugin.ID, template = "war:/notification/templates/mail/postNewsNotificationPlugin.gtmpl") })
public class MailTemplateProvider extends TemplateProvider {
  protected static Log          log = ExoLogger.getLogger(MailTemplateProvider.class);

  private final IdentityManager identityManager;

  public MailTemplateProvider(InitParams initParams, IdentityManager identityManager) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(PostNewsNotificationPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(MentionInNewsNotificationPlugin.ID), new TemplateBuilder());
    this.templateBuilders.put(PluginKey.key(PublishNewsNotificationPlugin.ID), new TemplateBuilder());
    this.identityManager = identityManager;
  }

  protected class TemplateBuilder extends AbstractTemplateBuilder {
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
      String authorAvatarUrl = notification.getValueOwnerParameter(NotificationConstants.AUTHOR_AVATAR_URL);
      String baseUrl = PropertyManager.getProperty("gatein.email.domain.url");
      String illustrationUrl = baseUrl.concat("/content/images/newsImageDefault.png");
      String activityLink = notification.getValueOwnerParameter(NotificationConstants.ACTIVITY_LINK);
      String context = notification.getValueOwnerParameter(NotificationConstants.CONTEXT);

      HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
      templateContext.put("CONTENT_TITLE", encoder.encode(contentTitle));
      templateContext.put(NotificationConstants.CONTENT_SPACE, encoder.encode(contentSpaceName));
      templateContext.put("CONTENT_AUTHOR", encoder.encode(contentAuthor));
      templateContext.put("CURRENT_USER", currentUser);
      templateContext.put("ILLUSTRATION_URL", encoder.encode(illustrationUrl));
      templateContext.put("AUTHOR_AVATAR_URL", encoder.encode(authorAvatarUrl));
      templateContext.put("CONTEXT", encoder.encode(context));
      StringBuilder activityUrl = new StringBuilder();
      if (pluginId.equals(PublishNewsNotificationPlugin.ID)) {
        String portalName = PortalContainer.getCurrentPortalContainerName();
        String portalOwner = CommonsUtils.getCurrentPortalOwner();
        String currentDomain = CommonsUtils.getCurrentDomain();
        if (!currentDomain.endsWith("/")) {
          currentDomain += "/";
        }
        activityUrl.append(currentDomain)
                   .append(portalName)
                   .append("/")
                   .append(portalOwner)
                   .append("/news-detail?newsId=")
                   .append(newsId);
      } else {
        activityUrl.append(activityLink);
      }

      templateContext.put("ACTIVITY_LINK", encoder.encode(activityUrl.toString()));

      templateContext.put("READ",
                          Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read"
                                                                                                                                : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      Calendar lastModified = Calendar.getInstance();
      lastModified.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("LAST_UPDATED_TIME",
                          TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                                                                       "EE, dd yyyy",
                                                                       new Locale(language),
                                                                       TimeConvertUtils.YEAR));
      // Receiver
      Identity receiver = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo());
      if (receiver == null || receiver.getRemoteId().equals(notification.getFrom())) {
        return null;
      }
      templateContext.put("FIRST_NAME", encoder.encode(receiver.getProfile().getProperty(Profile.FIRST_NAME).toString()));
      // Footer
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));
      templateContext.put("COMPANY_LINK", LinkProviderUtils.getBaseUrl());
      String subject = TemplateUtils.processSubject(templateContext);
      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }
  }
}
