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

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.service.LinkService;
import io.meeds.social.cms.utils.PageContentBlockUtils;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.hidden.plugin.HiddenApplicationPlugin;

/**
 * Lets each user hide a links application whose editor allowed it
 */
@Service("linksHiddenApplicationPlugin")
public class LinkHiddenApplicationPlugin implements HiddenApplicationPlugin {

  public static final String    LINKS_BUNDLE_NAME      = "locale.portlet.Links";

  public static final String    DEFAULT_LIST_NAME_KEY  = "links.label.defaultListName";

  @Autowired
  private LinkService           linkService;

  @Autowired
  private ResourceBundleService resourceBundleService;

  @Autowired
  private LayoutService         layoutService;

  @Override
  public String getApplicationType() {
    return LinkService.LINK_APPLICATION_TYPE;
  }

  @Override
  public HiddenApplication getApplication(String name, String language) {
    LinkSetting linkSetting = linkService.getLinkSettingByName(name, language, true);
    if (linkSetting == null) {
      return null;
    }
    return new HiddenApplication(getApplicationType(),
                                 name,
                                 getTitle(linkSetting.getHeader(), language),
                                 getListedPageReference(name, linkSetting.getPageReference()),
                                 linkSetting.isAllowHide());
  }

  /**
   * A window in a site's layout shows on every page of the site, while its
   * setting records one page, which doesn't hold it: it is listed with no
   * page, as social's listing expects for a window that is not on a page. A
   * window that neither its page nor its site's layout holds keeps its page,
   * so that the listing drops it.
   */
  private String getListedPageReference(String name, String pageReference) {
    PageKey pageKey = toPageKey(pageReference);
    if (pageKey == null) {
      return pageReference;
    }
    Page page = layoutService.getPage(pageKey);
    if (page != null && PageContentBlockUtils.hasWidgetWithSettingName(layoutService, page, name)) {
      return pageReference;
    }
    PortalConfig site = layoutService.getPortalConfig(pageKey.getSite());
    boolean inSiteLayout = site != null
                           && site.getPortalLayout() != null
                           && PageContentBlockUtils.hasWidgetWithSettingName(layoutService, site.getPortalLayout(), name);
    return inSiteLayout ? null : pageReference;
  }

  private PageKey toPageKey(String pageReference) {
    try {
      return StringUtils.isBlank(pageReference) ? null : PageKey.parse(pageReference);
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Override
  public boolean canView(String name, Identity identity) {
    return linkService.hasAccessPermission(name, identity);
  }

  private String getTitle(Map<String, String> header, String language) {
    String title = null;
    if (header != null && !header.isEmpty()) {
      title = StringUtils.firstNonBlank(header.get(language),
                                        header.get(Locale.ENGLISH.getLanguage()),
                                        header.values().stream().filter(StringUtils::isNotBlank).findFirst().orElse(null));
    }
    // A header given at the application's creation may be a label key of the
    // Links bundle, which the application itself translates the same way
    ResourceBundle bundle = getBundle(language);
    if (StringUtils.isBlank(title)) {
      return bundle != null && bundle.containsKey(DEFAULT_LIST_NAME_KEY) ? bundle.getString(DEFAULT_LIST_NAME_KEY) : null;
    } else {
      return bundle != null && bundle.containsKey(title) ? bundle.getString(title) : title;
    }
  }

  private ResourceBundle getBundle(String language) {
    Locale locale = StringUtils.isBlank(language) ? Locale.ENGLISH : Locale.forLanguageTag(language);
    return resourceBundleService.getResourceBundle(LINKS_BUNDLE_NAME, locale);
  }

}
