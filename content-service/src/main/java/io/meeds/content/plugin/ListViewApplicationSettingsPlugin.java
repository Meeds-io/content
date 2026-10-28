/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.content.plugin;

import static io.meeds.content.utils.ApplicationSettingsUtils.getLanguages;
import static io.meeds.content.utils.ApplicationSettingsUtils.notAccepted;
import static io.meeds.content.utils.ApplicationSettingsUtils.requireKnownOptions;
import static io.meeds.content.utils.ApplicationSettingsUtils.toLanguageLabels;
import static io.meeds.content.utils.ApplicationSettingsUtils.toLocaleLabels;
import static io.meeds.content.utils.ApplicationSettingsUtils.toTranslations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

/**
 * Configures a list view window (News list, Content list) as its settings
 * drawer does: the options are window preferences, written on the window as
 * its action URL stores them, and the header title is a translation keyed
 * by the window's {@code applicationId} preference, whose edit right
 * resolves the CMS setting through the application id mapping. A draft
 * window shares that key with the live one, so the first header write on a
 * window that does not own its keys creates new ones for it, keyed on the
 * live page.
 */
public abstract class ListViewApplicationSettingsPlugin implements ApplicationSettingsPlugin {

  public static final String           HEADER_TITLE                = "header_title";

  public static final String           NAME_PREFERENCE             = "name";

  public static final String           APPLICATION_ID_PREFERENCE   = "applicationId";

  private static final String          SETTING_NAME_PREFIX         = "Untitled";

  @Autowired
  protected CMSService                 cmsService;

  @Autowired
  protected TranslationService         translationService;

  @Autowired
  protected SettingService             settingService;

  @Autowired
  protected IdentityManager            identityManager;

  @Autowired
  protected UserACL                    userAcl;

  @Autowired
  protected LocaleConfigService        localeConfigService;

  @Autowired(required = false)
  protected ApplicationSettingsService applicationSettingsService;

  @PostConstruct
  public void init() {
    if (applicationSettingsService != null) {
      applicationSettingsService.addPlugin(this);
    }
  }

  @Override
  public boolean isConfigurableInSiteContainer() {
    return true;
  }

  @Override
  public Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException {
    Map<String, Object> settings = readPreferences(context.getPreferences());
    String applicationId = context.getPreferences().get(APPLICATION_ID_PREFERENCE);
    if (!context.isSiteContainer()) {
      settings.put(HEADER_TITLE, readHeader(applicationId, context.getUsername()));
    }
    return settings;
  }

  @Override
  public void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                         ObjectNotFoundException {
    Identity identity = requireEditor(context);
    toPreferences(context, settings, identity);
    toHeader(context, settings);
  }

  @Override
  public void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    Identity identity = requireEditor(context);
    Map<String, String> preferences = toPreferences(context, settings, identity);
    Map<String, String> header = toHeader(context, settings);
    if (header == null) {
      if (!preferences.isEmpty()) {
        applicationSettingsService.updatePreferences(context, preferences);
      }
      return;
    }
    preferences.putAll(getHeaderPreferences(header));
    if (ownsHeaderKeys(context)) {
      saveHeader(context.getPreferences().get(APPLICATION_ID_PREFERENCE), header, context.getUsername());
      if (!preferences.isEmpty()) {
        applicationSettingsService.updatePreferences(context, preferences);
      }
    } else {
      String name = createSetting(context);
      String applicationId = createApplicationId();
      settingService.set(getMappingContext(), getMappingScope(), applicationId, SettingValue.create(name));
      saveHeader(applicationId, header, context.getUsername());
      preferences.put(NAME_PREFERENCE, name);
      preferences.put(APPLICATION_ID_PREFERENCE, applicationId);
      applicationSettingsService.markOwner(context, preferences);
    }
  }

  /**
   * @return the CMS setting type, the portlet's content-type
   */
  protected abstract String getCmsType();

  /**
   * @return the translation object type of the header title
   */
  protected abstract String getTranslationObjectType();

  /**
   * @return the translation field name of the header title
   */
  protected abstract String getHeaderField();

  protected abstract int getHeaderMaxLength();

  protected abstract Context getMappingContext();

  protected abstract Scope getMappingScope();

  /**
   * @return the options this view accepts, {@link #HEADER_TITLE} included
   */
  protected abstract List<String> getOptions();

  /**
   * @param  preferences the window's stored preferences
   * @return             the options as the drawer shows them, its defaults
   *                     applied, without the header title
   */
  protected abstract Map<String, Object> readPreferences(Map<String, String> preferences);

  /**
   * @param  context                 the window and the user
   * @param  settings                the settings to change
   * @param  identity                the user
   * @return                         the preferences the drawer's action
   *                                 stores for the given options
   * @throws IllegalAccessException  when a referenced object is not readable
   * @throws ObjectNotFoundException when a referenced object does not exist
   */
  protected abstract Map<String, String> toPreferences(ApplicationSettingsContext context,
                                                       Map<String, Object> settings,
                                                       Identity identity) throws IllegalAccessException,
                                                                          ObjectNotFoundException;

  /**
   * @param  header the header title per language tag
   * @return        the preferences the drawer stores beside the header
   *                translation
   */
  protected Map<String, String> getHeaderPreferences(Map<String, String> header) {
    return new LinkedHashMap<>();
  }

  protected String getDefaultLanguage() {
    return localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag();
  }

  /**
   * The right a page window asks for: CMS edit on its live page, the rule
   * its CMS setting applies once keyed on that page. A site container has no
   * page: only the site writer's edit right applies there.
   */
  private Identity requireEditor(ApplicationSettingsContext context) throws IllegalAccessException {
    Identity identity = userAcl.getUserIdentity(context.getUsername());
    if (identity == null || (!context.isSiteContainer()
                             && (StringUtils.isBlank(context.getPageReference())
                                 || !cmsService.hasEditPermission(identity,
                                                                  context.getPageReference(),
                                                                  context.getSpaceId())))) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    return identity;
  }

  private Map<String, String> toHeader(ApplicationSettingsContext context, Map<String, Object> settings) {
    requireKnownOptions(settings, getOptions());
    if (!settings.containsKey(HEADER_TITLE)) {
      return null; // NOSONAR
    } else if (context.isSiteContainer()) {
      throw notAccepted(HEADER_TITLE);
    }
    Map<String, String> header = toTranslations(settings.get(HEADER_TITLE),
                                                HEADER_TITLE,
                                                getHeaderMaxLength(),
                                                getLanguages(localeConfigService));
    if (StringUtils.isBlank(header.get(getDefaultLanguage()))) {
      throw notAccepted(HEADER_TITLE);
    }
    return header;
  }

  private Map<String, String> readHeader(String applicationId, String username) throws IllegalAccessException {
    if (StringUtils.isBlank(applicationId)) {
      return new LinkedHashMap<>();
    }
    try {
      TranslationField field = translationService.getTranslationField(getTranslationObjectType(),
                                                                      applicationId,
                                                                      getHeaderField(),
                                                                      username);
      return field == null ? new LinkedHashMap<>() : toLanguageLabels(field.getLabels());
    } catch (ObjectNotFoundException e) {
      return new LinkedHashMap<>();
    }
  }

  private void saveHeader(String applicationId, Map<String, String> header, String username) throws IllegalAccessException,
                                                                                              ObjectNotFoundException {
    translationService.saveTranslationLabels(getTranslationObjectType(),
                                             applicationId,
                                             getHeaderField(),
                                             toLocaleLabels(header),
                                             username,
                                             false);
  }

  /**
   * @return whether the window carries the ownership marker and keys that
   *         resolve to a CMS setting of its live page: its application id
   *         maps to its setting name, whose page is the context's
   */
  private boolean ownsHeaderKeys(ApplicationSettingsContext context) {
    String name = context.getPreferences().get(NAME_PREFERENCE);
    String applicationId = context.getPreferences().get(APPLICATION_ID_PREFERENCE);
    if (!applicationSettingsService.isOwner(context) || StringUtils.isBlank(name) || StringUtils.isBlank(applicationId)) {
      return false;
    }
    SettingValue<?> mapped = settingService.get(getMappingContext(), getMappingScope(), applicationId);
    CMSSetting setting = cmsService.getSetting(getCmsType(), name);
    return mapped != null && name.equals(String.valueOf(mapped.getValue())) && setting != null
           && StringUtils.equals(setting.getPageReference(), context.getPageReference());
  }

  private String createSetting(ApplicationSettingsContext context) throws IllegalAccessException {
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     identityManager.getOrCreateUserIdentity(context.getUsername());
    if (userIdentity == null) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    String name;
    do {
      name = SETTING_NAME_PREFIX + "-" + context.getApplicationId() + "-" + UUID.randomUUID();
    } while (cmsService.isSettingNameExists(getCmsType(), name));
    try {
      cmsService.saveSettingName(getCmsType(),
                                 name,
                                 context.getPageReference(),
                                 context.getSpaceId(),
                                 Long.parseLong(userIdentity.getId()));
    } catch (ObjectAlreadyExistsException e) {
      throw new IllegalStateException(ApplicationSettingsService.APPLICATION_NOT_SAVED, e);
    }
    return name;
  }

  private String createApplicationId() {
    String applicationId;
    do {
      applicationId = String.valueOf(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
    } while (settingService.get(getMappingContext(), getMappingScope(), applicationId) != null);
    return applicationId;
  }

}
