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
package io.meeds.content.link.plugin;

import static io.meeds.content.utils.ApplicationSettingsUtils.arraySchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.booleanSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.enumSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.getLanguages;
import static io.meeds.content.utils.ApplicationSettingsUtils.integerSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.notAccepted;
import static io.meeds.content.utils.ApplicationSettingsUtils.objectSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.requireKnownOptions;
import static io.meeds.content.utils.ApplicationSettingsUtils.stringSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.toBoolean;
import static io.meeds.content.utils.ApplicationSettingsUtils.toEnum;
import static io.meeds.content.utils.ApplicationSettingsUtils.toId;
import static io.meeds.content.utils.ApplicationSettingsUtils.toInt;
import static io.meeds.content.utils.ApplicationSettingsUtils.toTranslations;
import static io.meeds.content.utils.ApplicationSettingsUtils.toUrl;
import static io.meeds.content.utils.ApplicationSettingsUtils.translationsSchema;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;

import io.meeds.content.link.constant.LinkAlignType;
import io.meeds.content.link.constant.LinkDisplayType;
import io.meeds.content.link.model.Link;
import io.meeds.content.link.model.LinkData;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.model.LinkWithIconAttachment;
import io.meeds.content.link.model.LinkWithImageContent;
import io.meeds.content.link.service.LinkService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.cms.service.CMSService;

import jakarta.annotation.PostConstruct;

/**
 * Configures a Links window as its settings drawer does. The links are a
 * {@link LinkSetting} named by the window's {@code name} preference, which a
 * draft window shares with the live one: the first write on a window that
 * does not own its setting creates a copy for it, keyed on the live page.
 */
@Component
@Profile("layout")
public class LinkApplicationSettingsPlugin implements ApplicationSettingsPlugin {

  public static final String  PORTLET_NAME           = "Links";

  public static final String  NAME_PREFERENCE        = "name";

  public static final String  TYPE                   = "type";

  public static final String  HEADER                 = "header";

  public static final String  SHOW_ICON              = "show_icon";

  public static final String  ICON_SIZE              = "icon_size";

  public static final String  SHOW_NAME              = "show_name";

  public static final String  SHOW_DESCRIPTION       = "show_description";

  public static final String  SEE_MORE               = "see_more";

  public static final String  H_ALIGN                = "h_align";

  public static final String  V_ALIGN                = "v_align";

  public static final String  LINKS                  = "links";

  public static final String  LINK_ID                = "id";

  public static final String  LINK_NAME              = "name";

  public static final String  LINK_DESCRIPTION       = "description";

  public static final String  LINK_URL               = "url";

  public static final String  LINK_SAME_TAB          = "same_tab";

  public static final String  LINK_NOT_FOUND         = "does not exist: link";

  public static final int     MAX_TEXT_LENGTH        = 50;

  public static final int     MIN_ICON_SIZE          = 12;

  public static final int     MAX_ICON_SIZE          = 60;

  private static final String SETTING_NAME_PREFIX    = "Untitled";

  private static final List<String> OPTIONS          = Arrays.asList(TYPE,
                                                                     HEADER,
                                                                     SHOW_ICON,
                                                                     ICON_SIZE,
                                                                     SHOW_NAME,
                                                                     SHOW_DESCRIPTION,
                                                                     SEE_MORE,
                                                                     H_ALIGN,
                                                                     V_ALIGN,
                                                                     LINKS);

  private static final List<String> LINK_OPTIONS     = Arrays.asList(LINK_ID,
                                                                     LINK_NAME,
                                                                     LINK_DESCRIPTION,
                                                                     LINK_URL,
                                                                     LINK_SAME_TAB);

  @Autowired
  private LinkService                linkService;

  @Autowired
  private CMSService                 cmsService;

  @Autowired
  private UserACL                    userAcl;

  @Autowired
  private LocaleConfigService        localeConfigService;

  @Autowired(required = false)
  private ApplicationSettingsService applicationSettingsService;

  @PostConstruct
  public void init() {
    if (applicationSettingsService != null) {
      applicationSettingsService.addPlugin(this);
    }
  }

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public Map<String, Object> getSettingsSchema() {
    Map<String, Object> link = new LinkedHashMap<>();
    link.put(LINK_ID, integerSchema(1, Integer.MAX_VALUE, "Id of a link the read returned, to keep it and its icon; omitted for a new link"));
    link.put(LINK_NAME, translationsSchema(MAX_TEXT_LENGTH, "Name per language tag, required in the default language"));
    link.put(LINK_DESCRIPTION, translationsSchema(MAX_TEXT_LENGTH, "Description per language tag, shown in the COLUMN type"));
    link.put(LINK_URL, stringSchema("Target: an http(s) URL or a path of the platform; a stored target sent back unchanged is kept"));
    link.put(LINK_SAME_TAB, booleanSchema("Opens the link in the same tab"));
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(TYPE, enumSchema(enumNames(LinkDisplayType.class), "Display type"));
    properties.put(HEADER, translationsSchema(MAX_TEXT_LENGTH, "Header per language tag, replacing every language; {} removes the header"));
    properties.put(SHOW_ICON, booleanSchema("Shows the link icons"));
    properties.put(ICON_SIZE, integerSchema(MIN_ICON_SIZE, MAX_ICON_SIZE, "Icon size in pixels"));
    properties.put(SHOW_NAME, booleanSchema("Shows the link names"));
    properties.put(SHOW_DESCRIPTION, booleanSchema("Shows the link descriptions, in the COLUMN type"));
    properties.put(SEE_MORE, stringSchema("See more target: an http(s) URL or a path of the platform; an empty text removes it"));
    properties.put(H_ALIGN, enumSchema(enumNames(LinkAlignType.class), "Horizontal alignment"));
    properties.put(V_ALIGN, enumSchema(enumNames(LinkAlignType.class), "Vertical alignment"));
    properties.put(LINKS, arraySchema(objectSchema(link), "The links, in order, replacing the stored list; [] removes them all"));
    return objectSchema(properties);
  }

  @Override
  public Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException {
    String name = context.getPreferences().get(NAME_PREFERENCE);
    if (StringUtils.isBlank(name)) {
      return null; // NOSONAR
    }
    LinkSetting setting = linkService.getLinkSettingByName(name, null, getIdentity(context));
    if (setting == null) {
      return null; // NOSONAR
    }
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put(TYPE, setting.getType() == null ? null : setting.getType().name());
    settings.put(HEADER, withoutBlankTexts(setting.getHeader()));
    settings.put(SHOW_ICON, setting.isShowIcon());
    settings.put(ICON_SIZE, setting.getIconSize());
    settings.put(SHOW_NAME, setting.isShowName());
    settings.put(SHOW_DESCRIPTION, setting.isShowDescription());
    settings.put(SEE_MORE, StringUtils.defaultString(setting.getSeeMore()));
    settings.put(H_ALIGN, setting.getHAlign() == null ? null : setting.getHAlign().name());
    settings.put(V_ALIGN, setting.getVAlign() == null ? null : setting.getVAlign().name());
    settings.put(LINKS,
                 linkService.getLinks(name, null, true)
                            .stream()
                            .map(this::toLinkSettings)
                            .toList());
    return settings;
  }

  @Override
  public void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                         ObjectNotFoundException {
    Identity identity = requireEditor(context);
    LinkSetting source = getReadableSource(context, identity);
    mergeSetting(source, settings);
    mergeLinks(getSourceLinks(source), settings);
  }

  @Override
  public void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    Identity identity = requireEditor(context);
    LinkSetting source = getReadableSource(context, identity);
    LinkSetting setting = mergeSetting(source, settings);
    if (source != null && applicationSettingsService.isOwner(context)
        && StringUtils.equals(source.getPageReference(), context.getPageReference())) {
      linkService.saveLinkSetting(setting, mergeLinks(getSourceLinks(source), settings), identity);
    } else {
      List<Link> links = mergeLinks(getCopiedLinks(source), settings);
      links.forEach(link -> link.setId(0));
      String name = generateSettingName(context);
      linkService.initLinkSetting(name, context.getPageReference(), context.getSpaceId());
      setting.setName(name);
      linkService.saveLinkSetting(setting, links, identity);
      applicationSettingsService.markOwner(context, Collections.singletonMap(NAME_PREFERENCE, name));
    }
  }

  private Identity requireEditor(ApplicationSettingsContext context) throws IllegalAccessException {
    Identity identity = getIdentity(context);
    if (identity == null || StringUtils.isBlank(context.getPageReference())
        || !cmsService.hasEditPermission(identity, context.getPageReference(), context.getSpaceId())) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    return identity;
  }

  /**
   * @return the setting the window's {@code name} preference designates,
   *         with its header translations, null when it designates none
   * @throws IllegalAccessException when the user may not read it: the
   *                                  preference is client-writable
   */
  private LinkSetting getReadableSource(ApplicationSettingsContext context, Identity identity) throws IllegalAccessException {
    String name = context.getPreferences().get(NAME_PREFERENCE);
    if (StringUtils.isBlank(name) || !linkService.hasLinkSetting(name)) {
      return null;
    }
    if (!linkService.hasAccessPermission(name, identity)) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    return linkService.getLinkSettingByName(name, null, true);
  }

  private List<Link> getSourceLinks(LinkSetting source) {
    return source == null ? Collections.emptyList() : linkService.getLinks(source.getName(), null, true);
  }

  /**
   * @return the source's links, each carrying its icon's content, to be
   *         created in a copy once their ids are reset
   */
  private List<Link> getCopiedLinks(LinkSetting source) throws ObjectNotFoundException {
    if (source == null) {
      return Collections.emptyList();
    }
    LinkData linkData = linkService.getLinkDataByName(source.getName());
    List<Link> links = new ArrayList<>();
    for (LinkWithImageContent link : linkData.getLinks()) {
      LinkWithIconAttachment copy = new LinkWithIconAttachment(link);
      copy.setIconFileId(0);
      if (StringUtils.isNotBlank(link.getImageContent())) {
        copy.setInputStream(new ByteArrayInputStream(Base64.decodeBase64(link.getImageContent())));
      }
      links.add(copy);
    }
    return links;
  }

  private LinkSetting mergeSetting(LinkSetting source, Map<String, Object> settings) {
    requireKnownOptions(settings, OPTIONS);
    LinkSetting setting = source == null ? newSetting() : source.clone();
    Set<String> languages = getLanguages(localeConfigService);
    if (settings.containsKey(TYPE)) {
      setting.setType(toEnum(settings.get(TYPE), TYPE, LinkDisplayType.class));
    }
    if (settings.containsKey(HEADER)) {
      setting.setHeader(toTranslations(settings.get(HEADER), HEADER, MAX_TEXT_LENGTH, languages));
    }
    if (settings.containsKey(SHOW_ICON)) {
      setting.setShowIcon(toBoolean(settings.get(SHOW_ICON), SHOW_ICON));
    }
    if (settings.containsKey(ICON_SIZE)) {
      setting.setIconSize(toInt(settings.get(ICON_SIZE), ICON_SIZE, MIN_ICON_SIZE, MAX_ICON_SIZE));
    }
    if (settings.containsKey(SHOW_NAME)) {
      setting.setShowName(toBoolean(settings.get(SHOW_NAME), SHOW_NAME));
    }
    if (settings.containsKey(SHOW_DESCRIPTION)) {
      setting.setShowDescription(toBoolean(settings.get(SHOW_DESCRIPTION), SHOW_DESCRIPTION));
    }
    if (settings.containsKey(SEE_MORE)) {
      Object seeMore = settings.get(SEE_MORE);
      if ("".equals(seeMore)) {
        setting.setSeeMore(null);
      } else if (!seeMore.equals(setting.getSeeMore())) {
        setting.setSeeMore(toUrl(seeMore, SEE_MORE));
      }
    }
    if (settings.containsKey(H_ALIGN)) {
      setting.setHAlign(toEnum(settings.get(H_ALIGN), H_ALIGN, LinkAlignType.class));
    }
    if (settings.containsKey(V_ALIGN)) {
      setting.setVAlign(toEnum(settings.get(V_ALIGN), V_ALIGN, LinkAlignType.class));
    }
    return setting;
  }

  /**
   * @param  baseLinks               the links to start from, carrying the
   *                                   ids the read returned
   * @return                         the base links when the settings do not
   *                                 give the list, else the given list in
   *                                 order, a link given with an id being the
   *                                 base link of that id with the given
   *                                 values
   * @throws ObjectNotFoundException when a given id is not a base link
   */
  private List<Link> mergeLinks(List<Link> baseLinks, Map<String, Object> settings) throws ObjectNotFoundException {
    if (!settings.containsKey(LINKS)) {
      return baseLinks;
    }
    if (!(settings.get(LINKS) instanceof List<?> items)) {
      throw notAccepted(LINKS);
    }
    Set<String> languages = getLanguages(localeConfigService);
    String defaultLanguage = localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag();
    List<Long> givenIds = new ArrayList<>();
    List<Link> links = new ArrayList<>();
    for (Object item : items) {
      if (!(item instanceof Map<?, ?> map)) {
        throw notAccepted(LINKS);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> values = (Map<String, Object>) map;
      requireKnownOptions(values, LINK_OPTIONS);
      Link link;
      if (values.containsKey(LINK_ID)) {
        long id = toId(values.get(LINK_ID), LINK_ID);
        if (givenIds.contains(id)) {
          throw notAccepted(LINK_ID);
        }
        link = baseLinks.stream().filter(l -> l.getId() == id).findFirst().orElse(null);
        if (link == null) {
          throw new ObjectNotFoundException(LINK_NOT_FOUND);
        }
        givenIds.add(id);
      } else {
        link = new Link(0, new HashMap<>(), new HashMap<>(), null, null, false, 0, 0);
      }
      if (values.containsKey(LINK_NAME)) {
        link.setName(toTranslations(values.get(LINK_NAME), LINK_NAME, MAX_TEXT_LENGTH, languages));
      }
      if (values.containsKey(LINK_DESCRIPTION)) {
        link.setDescription(toTranslations(values.get(LINK_DESCRIPTION), LINK_DESCRIPTION, MAX_TEXT_LENGTH, languages));
      }
      if (values.containsKey(LINK_URL) && !values.get(LINK_URL).equals(link.getUrl())) {
        link.setUrl(toUrl(values.get(LINK_URL), LINK_URL));
      }
      if (values.containsKey(LINK_SAME_TAB)) {
        link.setSameTab(toBoolean(values.get(LINK_SAME_TAB), LINK_SAME_TAB));
      }
      if (link.getName() == null || StringUtils.isBlank(link.getName().get(defaultLanguage))) {
        throw notAccepted(LINK_NAME);
      }
      if (StringUtils.isBlank(link.getUrl())) {
        throw notAccepted(LINK_URL);
      }
      link.setOrder(links.size());
      links.add(link);
    }
    return links;
  }

  private Map<String, Object> toLinkSettings(Link link) {
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put(LINK_ID, link.getId());
    settings.put(LINK_NAME, withoutBlankTexts(link.getName()));
    settings.put(LINK_DESCRIPTION, withoutBlankTexts(link.getDescription()));
    settings.put(LINK_URL, link.getUrl());
    settings.put(LINK_SAME_TAB, link.isSameTab());
    return settings;
  }

  private LinkSetting newSetting() {
    LinkSetting setting = new LinkSetting();
    setting.setType(LinkDisplayType.ROW);
    setting.setHAlign(LinkAlignType.CENTER);
    setting.setVAlign(LinkAlignType.CENTER);
    setting.setShowIcon(true);
    setting.setIconSize(34);
    setting.setHeader(Collections.emptyMap());
    return setting;
  }

  private String generateSettingName(ApplicationSettingsContext context) {
    String name;
    do {
      name = SETTING_NAME_PREFIX + "-" + context.getApplicationId() + "-" + UUID.randomUUID();
    } while (linkService.hasLinkSetting(name));
    return name;
  }

  private Identity getIdentity(ApplicationSettingsContext context) {
    return userAcl.getUserIdentity(context.getUsername());
  }

  private static Map<String, String> withoutBlankTexts(Map<String, String> texts) {
    Map<String, String> result = new LinkedHashMap<>();
    if (texts != null) {
      texts.forEach((language, text) -> {
        if (StringUtils.isNotBlank(text)) {
          result.put(language, text);
        }
      });
    }
    return result;
  }

  private static <E extends Enum<E>> List<String> enumNames(Class<E> type) {
    return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
  }

}
