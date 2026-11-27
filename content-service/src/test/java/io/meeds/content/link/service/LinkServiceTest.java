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
package io.meeds.content.link.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.mock.MockUploadService;
import org.exoplatform.upload.UploadService;

import io.meeds.content.AbstractSpringConfigurationTest;
import io.meeds.content.link.constant.LinkAlignType;
import io.meeds.content.link.constant.LinkDisplayType;
import io.meeds.content.link.model.Link;
import io.meeds.content.link.model.LinkData;
import io.meeds.content.link.model.LinkSetting;
import io.meeds.content.link.model.LinkWithIconAttachment;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.hidden.service.HiddenApplicationService;

import lombok.SneakyThrows;

@SuppressWarnings("deprecation")
public class LinkServiceTest extends AbstractSpringConfigurationTest { // NOSONAR

  private static final String SEE_MORE_NAME        = "SeeMore";

  private static final String HEADER_NAME          = "header";

  private static final String USERS_GROUP          = "*:/platform/users";

  private static final String ADMINISTRATORS_GROUP = "*:/platform/administrators";

  private static final String USERNAME             = "testuser";

  private static final String LINK_SETTING_NAME    = "linkServiceSettingName";

  private static final String MIME_TYPE            = "image/png";

  private static final String FILE_NAME            = "cover.png";

  private static final String UPLOAD_ID            = "1234";

  private static final long   SPACE_ID             = 3l;

  private LayoutService       layoutService;

  private IdentityRegistry    identityRegistry;

  private MockUploadService   uploadService;

  private FileService         fileService;

  private LocaleConfigService localeConfigService;

  @Autowired
  private HiddenApplicationService hiddenApplicationService;

  @Before
  @Override
  public void setUp() {
    super.setUp();
    layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    uploadService = (MockUploadService) getContainer().getComponentInstanceOfType(UploadService.class);
    fileService = getContainer().getComponentInstanceOfType(FileService.class);
    localeConfigService = getContainer().getComponentInstanceOfType(LocaleConfigService.class);
  }

  @Test
  public void testInitLinkSetting() {
    String pageReference = createPage("testInitLinkSetting", UserACL.EVERYONE, ADMINISTRATORS_GROUP);

    assertThrows(IllegalArgumentException.class, () -> linkService.initLinkSetting(null, pageReference, 0l));
    assertThrows(IllegalArgumentException.class, () -> linkService.initLinkSetting(LINK_SETTING_NAME, null, 0l));
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    LinkSetting linkSetting = linkService.getLinkSettingByName(LINK_SETTING_NAME);
    assertNotNull(linkSetting);
    assertEquals(LINK_SETTING_NAME, linkSetting.getName());
    assertEquals(pageReference, linkSetting.getPageReference());
    assertEquals(SPACE_ID, linkSetting.getSpaceId());
  }

  @Test
  public void testIsLinkTargetAvailable() {
    String homePage = createPage("availabilityHome", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    String settingsPage = createPage("availabilitySettings", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    String otherPage = createPage("availabilityOther", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    NavigationService navigationService = getContainer().getComponentInstanceOfType(NavigationService.class);
    SiteKey siteKey = SiteKey.portal("test");
    if (navigationService.loadNavigation(siteKey) == null) {
      navigationService.saveNavigation(new NavigationContext(siteKey, new NavigationState(0)));
    }
    NodeContext<NodeContext<?>> root = navigationService.loadNode(NodeModel.SELF_MODEL,
                                                                  navigationService.loadNavigation(siteKey),
                                                                  Scope.ALL,
                                                                  null);
    NodeContext<NodeContext<?>> home = root.add(null, "availabilityhome");
    home.setState(new NodeState.Builder().pageRef(PageKey.parse(homePage)).build());
    home.add(null, "settings").setState(new NodeState.Builder().pageRef(PageKey.parse(settingsPage)).build());
    home.add(null, "other").setState(new NodeState.Builder().pageRef(PageKey.parse(otherPage)).build());
    home.add(null, "addon")
        .setState(new NodeState.Builder().pageRef(new PageKey(SiteKey.portal("test"), "notImportedPage")).build());
    home.add(null, "category");
    navigationService.saveNode(root, null);

    LinkSetting linkSetting = linkService.initLinkSetting("linkAvailabilitySetting", homePage, 0);
    org.exoplatform.services.security.Identity administrator = registerAdministratorUser("availabilityAdmin");

    assertTrue(linkService.isLinkTargetAvailable(linkSetting, null, administrator));
    assertTrue(linkService.isLinkTargetAvailable(linkSetting, "https://meeds.io/test/availabilityhome/addon", administrator));
    assertTrue(linkService.isLinkTargetAvailable(linkSetting, "/portal/test", administrator));
    assertTrue(linkService.isLinkTargetAvailable(linkSetting, "/portal/notASite/availabilityhome/addon", administrator));
    assertTrue(linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/settings", administrator));
    assertTrue("A link to the links page itself is available",
               linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome", administrator));
    assertTrue(linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/settings#display", administrator));
    assertTrue(linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/settings?tab=1", administrator));
    assertTrue("A path parameter after a node that isn't the links page is kept",
               linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/other/123", administrator));

    assertFalse("A node whose page isn't imported is unavailable",
                linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/addon", administrator));
    assertFalse("A node without page is unavailable",
                linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/category", administrator));
    assertFalse("A path parameter after a node whose page isn't imported is unavailable",
                linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/addon/123", administrator));
    assertFalse("A path matching no node is unavailable",
                linkService.isLinkTargetAvailable(linkSetting, "/portal/test/missing/models", administrator));
    assertFalse("A path leading back to the links page is unavailable",
                linkService.isLinkTargetAvailable(linkSetting, "/portal/test/availabilityhome/ai/models", administrator));
  }

  @Test
  public void testIsLinkTargetAvailableRevealsNothingOfASiteTheReaderCannotAccess() {
    String homePage = createPage("restrictedAvailabilityHome", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    SiteKey siteKey = SiteKey.portal("restrictedAvailability");
    if (layoutService.getPortalConfig(siteKey) == null) {
      PortalConfig portal = new PortalConfig();
      portal.setType(siteKey.getTypeName());
      portal.setName(siteKey.getName());
      portal.setLocale("en");
      portal.setLabel("Restricted");
      portal.setAccessPermissions(new String[] { ADMINISTRATORS_GROUP });
      layoutService.create(portal);
    }
    NavigationService navigationService = getContainer().getComponentInstanceOfType(NavigationService.class);
    if (navigationService.loadNavigation(siteKey) == null) {
      navigationService.saveNavigation(new NavigationContext(siteKey, new NavigationState(0)));
    }
    NodeContext<NodeContext<?>> root = navigationService.loadNode(NodeModel.SELF_MODEL,
                                                                  navigationService.loadNavigation(siteKey),
                                                                  Scope.ALL,
                                                                  null);
    root.add(null, "addon").setState(new NodeState.Builder().pageRef(new PageKey(siteKey, "notImportedPage")).build());
    navigationService.saveNode(root, null);
    LinkSetting linkSetting = linkService.initLinkSetting("linkRestrictedAvailabilitySetting", homePage, 0);

    assertFalse("An administrator, who can access the site, learns that the page doesn't exist",
                linkService.isLinkTargetAvailable(linkSetting,
                                                  "/portal/restrictedAvailability/addon",
                                                  registerAdministratorUser("restrictedAvailabilityAdmin")));
    assertTrue("A user who can't access the site learns nothing of its nodes and pages",
               linkService.isLinkTargetAvailable(linkSetting,
                                                 "/portal/restrictedAvailability/addon",
                                                 registerInternalUser("restrictedAvailabilityUser")));
    assertTrue("A guest learns nothing of the site either",
               linkService.isLinkTargetAvailable(linkSetting, "/portal/restrictedAvailability/addon", null));

    String adminPage = createPage("restrictedAvailabilityAdminPage", ADMINISTRATORS_GROUP, ADMINISTRATORS_GROUP);
    SiteKey openSiteKey = SiteKey.portal("test");
    if (navigationService.loadNavigation(openSiteKey) == null) {
      navigationService.saveNavigation(new NavigationContext(openSiteKey, new NavigationState(0)));
    }
    NodeContext<NodeContext<?>> openRoot = navigationService.loadNode(NodeModel.SELF_MODEL,
                                                                      navigationService.loadNavigation(openSiteKey),
                                                                      Scope.ALL,
                                                                      null);
    openRoot.add(null, "restrictedadmin").setState(new NodeState.Builder().pageRef(PageKey.parse(adminPage)).build());
    navigationService.saveNode(openRoot, null);

    assertTrue("An administrator, who can open the page, sees the link",
               linkService.isLinkTargetAvailable(linkSetting,
                                                 "/portal/test/restrictedadmin",
                                                 registerAdministratorUser("restrictedAvailabilityAdmin")));
    assertFalse("A page the reader can't open counts as a page that doesn't exist",
                linkService.isLinkTargetAvailable(linkSetting,
                                                  "/portal/test/restrictedadmin",
                                                  registerInternalUser("restrictedAvailabilityUser")));
    assertEquals("A node the reader can't open and a node that doesn't exist give the same answer",
                 linkService.isLinkTargetAvailable(linkSetting,
                                                   "/portal/test/restrictedmissing",
                                                   registerInternalUser("restrictedAvailabilityUser")),
                 linkService.isLinkTargetAvailable(linkSetting,
                                                   "/portal/test/restrictedadmin",
                                                   registerInternalUser("restrictedAvailabilityUser")));
  }

  @Test
  @SneakyThrows
  public void testSaveLinkSettingPermissions() {
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, "testSaveLinkSettingPermissions1");
    assertNotNull(linkSetting);

    linkSetting.setLargeIcon(true);
    linkSetting.setIconSize(12);
    linkSetting.setShowName(true);
    linkSetting.setSeeMore("SeeMore1");
    linkSetting.setType(LinkDisplayType.COLUMN);
    linkSetting.setVAlign(LinkAlignType.CENTER);
    linkSetting.setHAlign(LinkAlignType.END);
    assertThrows(IllegalAccessException.class, () -> linkService.saveLinkSetting(linkSetting, null, null));
    assertThrows(IllegalAccessException.class,
                 () -> linkService.saveLinkSetting(linkSetting, null, registerInternalUser(USERNAME)));

    LinkSetting nameNotFoundLinkSetting = linkSetting.clone();
    nameNotFoundLinkSetting.setName("NotFound");
    assertThrows(ObjectNotFoundException.class,
                 () -> linkService.saveLinkSetting(nameNotFoundLinkSetting, null, registerAdministratorUser(USERNAME)));

    Thread.sleep(2); // NOSONAR wait for 10 milliseconds to have a different
                     // modification timestamp after saving the link setting
    LinkSetting updatedLinkSetting = linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser(USERNAME));
    assertNotNull(updatedLinkSetting);
    assertNotEquals(linkSetting.getLastModified(), updatedLinkSetting.getLastModified());
    linkSetting.setLastModified(updatedLinkSetting.getLastModified());
    assertEquals(linkSetting, updatedLinkSetting);

    linkSetting.setHeader(Collections.singletonMap("en", "header2"));
    linkSetting.setLargeIcon(false);
    linkSetting.setIconSize(12);
    linkSetting.setShowName(true);
    linkSetting.setSeeMore("SeeMore2");
    linkSetting.setType(LinkDisplayType.CARD);
    linkSetting.setVAlign(LinkAlignType.END);
    linkSetting.setHAlign(LinkAlignType.START);

    Thread.sleep(2); // NOSONAR wait for 10 milliseconds to have a different
                     // modification timestamp after saving the link setting
    updatedLinkSetting = linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser(USERNAME));
    assertNotNull(updatedLinkSetting);

    updatedLinkSetting = linkService.getLinkSettingByName(updatedLinkSetting.getName(), "en", true);
    assertNotEquals(linkSetting.getLastModified(), updatedLinkSetting.getLastModified());
    linkSetting.setLastModified(updatedLinkSetting.getLastModified());
    assertEquals(linkSetting, updatedLinkSetting);

    linkSetting.setShowName(false);
    Thread.sleep(2); // NOSONAR wait for 10 milliseconds to have a different
                     // modification timestamp after saving the link setting
    updatedLinkSetting = linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser(USERNAME));
    assertNotNull(updatedLinkSetting);

    updatedLinkSetting = linkService.getLinkSettingByName(updatedLinkSetting.getName(), "en", true);
    assertNotEquals(linkSetting.getLastModified(), updatedLinkSetting.getLastModified());
    linkSetting.setLastModified(updatedLinkSetting.getLastModified());
    assertEquals(linkSetting, updatedLinkSetting);
  }

  @Test
  @SneakyThrows
  public void testSaveLinks() {
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, "testSaveLinkSettingPermissions");
    assertNotNull(linkSetting);

    linkSetting.setHeader(Collections.singletonMap("en", HEADER_NAME));
    linkSetting.setLargeIcon(true);
    linkSetting.setIconSize(12);
    linkSetting.setShowName(true);
    linkSetting.setSeeMore(SEE_MORE_NAME);
    linkSetting.setType(LinkDisplayType.COLUMN);
    linkSetting.setVAlign(LinkAlignType.CENTER);
    linkSetting.setHAlign(LinkAlignType.START);
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser(USERNAME));

    List<Link> links = linkService.getLinks(LINK_SETTING_NAME, null, true);
    assertNotNull(links);
    assertEquals(0l, links.size());

    Link linkToSave1 = new Link(0,
                                Collections.singletonMap("en", "Website"),
                                Collections.singletonMap("en", "Website description"),
                                "https://localhost/",
                                "icon",
                                true,
                                5,
                                0);
    List<Link> linksToSave = Collections.singletonList(linkToSave1.clone());
    linkService.saveLinkSetting(linkSetting, linksToSave, registerAdministratorUser(USERNAME));
    links = linkService.getLinks(LINK_SETTING_NAME, null, true);
    assertNotNull(links);
    assertEquals(1, links.size());
    Link savedLink1 = links.get(0);
    assertTrue(savedLink1.getId() > 0);
    linksToSave = Collections.singletonList(linkToSave1.clone());
    linksToSave.get(0).setId(savedLink1.getId());
    assertEquals(linksToSave, links);

    LinkWithIconAttachment linkToSave2 = new LinkWithIconAttachment(0,
                                                                    Collections.singletonMap("en", "Website2"),
                                                                    Collections.singletonMap("en", "Website2 description"),
                                                                    "https://localhost2/",
                                                                    "icon2",
                                                                    true,
                                                                    4,
                                                                    54444l,
                                                                    UPLOAD_ID);

    savedLink1.setName(Collections.singletonMap("en", "name1"));
    savedLink1.setDescription(Collections.singletonMap("en", "description1"));
    savedLink1.setUrl("url1");
    savedLink1.setOrder(6);
    savedLink1.setSameTab(!savedLink1.isSameTab());
    linksToSave = Arrays.asList(savedLink1.clone(), linkToSave2.clone());
    uploadResource();
    linkService.saveLinkSetting(linkSetting, linksToSave, registerAdministratorUser(USERNAME));
    links = linkService.getLinks(LINK_SETTING_NAME, null, true);
    assertNotNull(links);
    assertEquals(2, links.size());
    assertEquals(links.get(1), savedLink1);
    Link savedLink2 = links.get(0);
    assertTrue(savedLink2.getId() > 0);
    assertTrue(savedLink2.getIconFileId() > 0);

    linksToSave = Arrays.asList(savedLink1.clone(), linkToSave2.toLink());
    assertNotEquals(linksToSave.get(1).getIconFileId(), savedLink2.getIconFileId());
    linksToSave.get(1).setId(savedLink2.getId());
    linksToSave.get(1).setIconFileId(savedLink2.getIconFileId());
    assertEquals(linksToSave.get(1), savedLink2);

    linkToSave2 = linkToSave2.clone();
    linkToSave2.setId(savedLink2.getId());
    linksToSave = Arrays.asList(linkToSave2.clone());
    uploadResource();

    linkService.saveLinkSetting(linkSetting, linksToSave, registerAdministratorUser(USERNAME));
    links = linkService.getLinks(LINK_SETTING_NAME, null, true);
    assertNotNull(links);
    assertEquals(1, links.size());
    assertEquals(savedLink2.getId(), links.get(0).getId());
    assertNotEquals(savedLink2.getIconFileId(), links.get(0).getIconFileId());

    FileItem file = fileService.getFile(savedLink2.getIconFileId());
    assertTrue(file == null || file.getFileInfo().isDeleted());

    file = fileService.getFile(links.get(0).getIconFileId());
    assertTrue(file != null && !file.getFileInfo().isDeleted());

    links.get(0).setIconFileId(savedLink2.getIconFileId());
    assertEquals(savedLink2, links.get(0));
    savedLink2 = links.get(0);

    linkService.saveLinkSetting(linkSetting, Collections.singletonList(linkToSave1), registerAdministratorUser(USERNAME));
    links = linkService.getLinks(LINK_SETTING_NAME);
    assertNotNull(links);
    assertEquals(1, links.size());

    file = fileService.getFile(savedLink2.getIconFileId());
    assertTrue(file == null || file.getFileInfo().isDeleted());
  }

  @Test
  @SneakyThrows
  public void testHideLinkSetting() {
    String settingName = "linkHideSetting";
    String otherUser = "otherHideUser";
    registerInternalUser(USERNAME);
    registerInternalUser(otherUser);
    assertThrows(ObjectNotFoundException.class,
                 () -> hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, "notExisting", USERNAME));

    LinkSetting linkSetting = initLinkSetting(settingName, "testHideLinkSetting");
    assertFalse("The option is off by default", linkService.getLinkSettingByName(settingName).isAllowHide());
    assertThrows("Hiding is refused while the option is off",
                 IllegalArgumentException.class,
                 () -> hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME));

    linkSetting.setAllowHide(true);
    linkSetting.setHeader(Collections.singletonMap("en", "Hidden header"));
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser("hideAdmin"));
    assertTrue("The option is stored", linkService.getLinkSettingByName(settingName).isAllowHide());

    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME);
    LinkSetting storedLinkSetting = linkService.getLinkSettingByName(settingName);
    assertTrue(linkService.isHidden(storedLinkSetting, USERNAME));
    assertFalse("Another user still sees the application", linkService.isHidden(storedLinkSetting, otherUser));
    List<HiddenApplication> hiddenApplications = hiddenApplicationService.getHiddenApplications(USERNAME, "en");
    assertEquals(Collections.singletonList(settingName), hiddenApplications.stream().map(HiddenApplication::getName).toList());
    assertEquals("A hidden application is listed with its header", "Hidden header", hiddenApplications.get(0).getTitle());
    assertEquals("testHideLinkSetting", hiddenApplications.get(0).getPageTitle());
    assertTrue(hiddenApplicationService.getHiddenApplications(otherUser, "en").isEmpty());

    storedLinkSetting.setAllowHide(false);
    linkService.saveLinkSetting(storedLinkSetting, null, registerAdministratorUser("hideAdmin"));
    storedLinkSetting = linkService.getLinkSettingByName(settingName);
    assertFalse("Turning the option off shows the application again", linkService.isHidden(storedLinkSetting, USERNAME));
    assertTrue(hiddenApplicationService.getHiddenApplications(USERNAME, "en").isEmpty());

    storedLinkSetting.setAllowHide(true);
    linkService.saveLinkSetting(storedLinkSetting, null, registerAdministratorUser("hideAdmin"));
    storedLinkSetting = linkService.getLinkSettingByName(settingName);
    assertFalse("Turned on again, the option starts fresh: the earlier choice is gone",
                linkService.isHidden(storedLinkSetting, USERNAME));

    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME);
    linkService.saveLinkSetting(storedLinkSetting, null, registerAdministratorUser("hideAdmin"));
    assertTrue("Saving the application with the option still on keeps the choice",
               linkService.isHidden(linkService.getLinkSettingByName(settingName), USERNAME));
    hiddenApplicationService.show(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME);
    assertFalse(linkService.isHidden(storedLinkSetting, USERNAME));
    assertTrue(hiddenApplicationService.getHiddenApplications(USERNAME, "en").isEmpty());
  }

  @Test
  @SneakyThrows
  public void testHideLinkSettingPermission() {
    String settingName = "linkHideRestrictedSetting";
    String pageReference = createPage("testHideLinkSettingPermission", ADMINISTRATORS_GROUP, ADMINISTRATORS_GROUP);
    addLinksWindow(pageReference, settingName);
    LinkSetting linkSetting = linkService.initLinkSetting(settingName, pageReference, 0);
    linkSetting.setAllowHide(true);
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser("hideAdmin"));
    registerInternalUser(USERNAME);

    assertThrows("A user who can't see the application can't hide it",
                 IllegalAccessException.class,
                 () -> hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME));
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, settingName, "hideAdmin");
    assertTrue(linkService.isHidden(linkService.getLinkSettingByName(settingName), "hideAdmin"));
    assertEquals(1, hiddenApplicationService.getHiddenApplications("hideAdmin", "en").size());

    layoutService.remove(PageKey.parse(pageReference));
    assertTrue("An application whose page was deleted can't be shown again, even by a content manager",
               hiddenApplicationService.getHiddenApplications("hideAdmin", "en").isEmpty());
  }

  @Test
  @SneakyThrows
  public void testGetLinkIconStream() {
    assertNull(linkService.getLinkIconStream(LINK_SETTING_NAME, 2325l));

    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, "testGetLinkIconStream1");
    assertNotNull(linkSetting);

    linkSetting.setHeader(Collections.singletonMap("en", HEADER_NAME));
    linkSetting.setLargeIcon(true);
    linkSetting.setIconSize(12);
    linkSetting.setShowName(true);
    linkSetting.setSeeMore(SEE_MORE_NAME);
    linkSetting.setType(LinkDisplayType.COLUMN);
    linkSetting.setVAlign(LinkAlignType.START);
    linkSetting.setHAlign(LinkAlignType.END);
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser(USERNAME));

    LinkWithIconAttachment linkToSave = new LinkWithIconAttachment(0,
                                                                   Collections.singletonMap("en", "Website2"),
                                                                   Collections.singletonMap("en", "Website description2"),
                                                                   "https://localhost2/",
                                                                   "icon2",
                                                                   true,
                                                                   4,
                                                                   54444l,
                                                                   UPLOAD_ID);

    List<Link> linksToSave = Arrays.asList(linkToSave.clone());
    uploadResource();
    linkService.saveLinkSetting(linkSetting, linksToSave, registerAdministratorUser(USERNAME));
    List<Link> links = linkService.getLinks(LINK_SETTING_NAME);
    assertNotNull(links);
    assertEquals(1, links.size());
    Link savedLink = links.get(0);
    assertTrue(savedLink.getId() > 0);
    assertTrue(savedLink.getIconFileId() > 0);
    FileItem file = fileService.getFile(savedLink.getIconFileId());
    assertTrue(file != null && file.getFileInfo() != null && !file.getFileInfo().isDeleted());

    InputStream inputStream = linkService.getLinkIconStream(LINK_SETTING_NAME, savedLink.getId());
    assertNotNull(inputStream);
    FileInfo fileInfo = file.getFileInfo(); // NOSONAR
    assertEquals(fileInfo.getSize(), inputStream.available());
  }

  @Test
  @SneakyThrows
  public void testCopyLinkSetting() {
    LinkSetting source = initLinkSetting(LINK_SETTING_NAME, "testCopyLinkSettingSource");
    source.setHeader(Collections.singletonMap("en", HEADER_NAME));
    source.setShowName(true);
    source.setType(LinkDisplayType.CARD);
    source.setVAlign(LinkAlignType.START);
    source.setHAlign(LinkAlignType.END);
    File icon = File.createTempFile("icon", "temp");
    java.nio.file.Files.write(icon.toPath(), new byte[] { 1, 2, 3 });
    uploadService.createUploadResource(UPLOAD_ID, icon.getPath(), FILE_NAME, MIME_TYPE);
    linkService.saveLinkSetting(source,
                                Arrays.asList(new LinkWithIconAttachment(0,
                                                                         Collections.singletonMap("en", "Website"),
                                                                         Collections.singletonMap("en", "Description"),
                                                                         "https://localhost/",
                                                                         null,
                                                                         true,
                                                                         0,
                                                                         0,
                                                                         UPLOAD_ID)),
                                registerAdministratorUser(USERNAME));
    Link sourceLink = linkService.getLinks(LINK_SETTING_NAME).get(0);
    assertTrue(sourceLink.getIconFileId() > 0);

    List<Link> copies = linkService.getLinkCopies(LINK_SETTING_NAME);
    assertEquals(1, copies.size());
    assertEquals(sourceLink.getId(), copies.get(0).getId());
    assertEquals(0, copies.get(0).getIconFileId());
    assertNotNull(((LinkWithIconAttachment) copies.get(0)).getInputStream());

    String copyPage = createPage("testCopyLinkSettingCopy", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    LinkSetting setting = linkService.getLinkSettingByName(LINK_SETTING_NAME, null, true).clone();
    setting.setShowDescription(true);
    linkService.copyLinkSetting("linkServiceCopyName", copyPage, SPACE_ID, setting, copies, registerAdministratorUser(USERNAME));

    LinkSetting copy = linkService.getLinkSettingByName("linkServiceCopyName", null, true);
    assertNotEquals(linkService.getLinkSettingByName(LINK_SETTING_NAME).getId(), copy.getId());
    assertEquals(copyPage, copy.getPageReference());
    assertEquals(SPACE_ID, copy.getSpaceId());
    assertEquals(LinkDisplayType.CARD, copy.getType());
    assertTrue(copy.isShowName());
    assertTrue(copy.isShowDescription());
    assertEquals(HEADER_NAME, copy.getHeader().get("en"));
    List<Link> copiedLinks = linkService.getLinks("linkServiceCopyName", null, true);
    assertEquals(1, copiedLinks.size());
    Link copiedLink = copiedLinks.get(0);
    assertNotEquals(sourceLink.getId(), copiedLink.getId());
    assertEquals("https://localhost/", copiedLink.getUrl());
    assertEquals("Website", copiedLink.getName().get("en"));
    assertTrue(copiedLink.getIconFileId() > 0);
    assertNotEquals(sourceLink.getIconFileId(), copiedLink.getIconFileId());
    assertEquals(sourceLink.getIconFileId(), linkService.getLinks(LINK_SETTING_NAME).get(0).getIconFileId());
    assertFalse(linkService.getLinkSettingByName(LINK_SETTING_NAME).isShowDescription());
  }

  @Test
  @SneakyThrows
  public void testGetLinkSettingPermissions() {
    assertNull(linkService.getLinkSettingByName(LINK_SETTING_NAME, null, null));

    String pageReference = createPage("testGetLinkSettingPermissions1", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertNotNull(linkService.getLinkSettingByName(LINK_SETTING_NAME, null, null));
    assertNotNull(linkService.getLinkSettingByName(LINK_SETTING_NAME, null, registerInternalUser(USERNAME)));

    pageReference = createPage("testGetLinkSettingPermissions1", USERS_GROUP, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertThrows(IllegalAccessException.class, () -> linkService.getLinkSettingByName(LINK_SETTING_NAME, null, null));
    assertNotNull(linkService.getLinkSettingByName(LINK_SETTING_NAME, null, registerInternalUser(USERNAME)));
  }

  @Test
  @SneakyThrows
  public void testHasAccessPermission() {
    assertFalse(linkService.hasAccessPermission(LINK_SETTING_NAME, null));

    String pageReference = createPage("testHasAccessPermission1", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertTrue(linkService.hasAccessPermission(LINK_SETTING_NAME, null));
    assertTrue(linkService.hasAccessPermission(LINK_SETTING_NAME, registerInternalUser(USERNAME)));

    pageReference = createPage("testHasAccessPermission2", USERS_GROUP, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertFalse(linkService.hasAccessPermission(LINK_SETTING_NAME, null));
    assertTrue(linkService.hasAccessPermission(LINK_SETTING_NAME, registerInternalUser(USERNAME)));

    pageReference = createPage("testHasAccessPermission3", ADMINISTRATORS_GROUP, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertFalse(linkService.hasAccessPermission(LINK_SETTING_NAME, null));
    assertFalse(linkService.hasAccessPermission(LINK_SETTING_NAME, registerInternalUser(USERNAME)));
    assertTrue(linkService.hasAccessPermission(LINK_SETTING_NAME, registerAdministratorUser(USERNAME)));
  }

  @Test
  @SneakyThrows
  public void testHasEditPermission() {
    assertFalse(linkService.hasEditPermission(LINK_SETTING_NAME, null));

    String pageReference = createPage("testHasEditPermission1", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertFalse(linkService.hasEditPermission(LINK_SETTING_NAME, null));
    assertFalse(linkService.hasEditPermission(LINK_SETTING_NAME, registerInternalUser(USERNAME)));
    assertTrue(linkService.hasEditPermission(LINK_SETTING_NAME, registerAdministratorUser(USERNAME)));
  }

  @Test
  @SneakyThrows
  public void testLinkHasEditPermission() {
    List<Link> links = linkService.getLinks(LINK_SETTING_NAME);
    assertNotNull(links);
    assertTrue(links.isEmpty());

    String pageReference = createPage("testLinkHasEditPermission1", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    linkService.initLinkSetting(LINK_SETTING_NAME, pageReference, SPACE_ID);

    assertFalse(linkService.hasEditPermission(LINK_SETTING_NAME, null));
    assertFalse(linkService.hasEditPermission(LINK_SETTING_NAME, registerInternalUser(USERNAME)));
    assertTrue(linkService.hasEditPermission(LINK_SETTING_NAME, registerAdministratorUser(USERNAME)));
  }

  @Test
  @SneakyThrows
  public void testGetLinkTranslations() {
    LinkSetting linkSetting = initLinkSetting(LINK_SETTING_NAME, "testSaveLinkSettingPermissions");
    assertNotNull(linkSetting);

    linkSetting.setHeader(Collections.singletonMap("en", HEADER_NAME));
    linkSetting.setLargeIcon(true);
    linkSetting.setIconSize(12);
    linkSetting.setShowName(true);
    linkSetting.setSeeMore(SEE_MORE_NAME);
    linkSetting.setType(LinkDisplayType.COLUMN);
    linkSetting.setVAlign(LinkAlignType.END);
    linkSetting.setHAlign(LinkAlignType.START);
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser(USERNAME));

    List<Link> links = linkService.getLinks(LINK_SETTING_NAME, null, true);
    assertNotNull(links);
    assertEquals(0l, links.size());

    Link linkToSave1 = new Link(0,
                                Collections.singletonMap("en", "Website"),
                                Collections.singletonMap("en", "Website description"),
                                "https://localhost/",
                                "icon",
                                true,
                                5,
                                0);
    List<Link> linksToSave = Collections.singletonList(linkToSave1.clone());
    linkService.saveLinkSetting(linkSetting, linksToSave, registerAdministratorUser(USERNAME));
    links = linkService.getLinks(LINK_SETTING_NAME, "en", true);
    assertNotNull(links);
    assertEquals(1, links.size());
    Map<String, String> name = links.get(0).getName();
    assertNotNull(name.get("en"));
    assertNull(name.get("fr"));
    localeConfigService.saveDefaultLocaleConfig("fr");
    links = linkService.getLinks(LINK_SETTING_NAME, "fr", true);
    assertNotNull(links);
    assertEquals(1, links.size());
    name = links.get(0).getName();
    assertNotNull(name.get("en"));
    assertNull(name.get("fr"));
  }

  @Test
  @SneakyThrows
  public void testCopyLinkSettingKeepsTheCopyHiddenForTheUsersWhoHidTheSource() {
    String sourceName = "linkCopyHiddenSource";
    String copyName = "linkCopyHiddenCopy";
    String otherUser = "otherCopyHideUser";
    registerInternalUser(USERNAME);
    registerInternalUser(otherUser);
    LinkSetting source = initLinkSetting(sourceName, "testCopyHiddenSource");
    source.setAllowHide(true);
    linkService.saveLinkSetting(source, null, registerAdministratorUser("copyHideAdmin"));
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, sourceName, USERNAME);

    String copyPage = createPage("testCopyHiddenCopy", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    LinkSetting setting = linkService.getLinkSettingByName(sourceName).clone();
    linkService.copyLinkSetting(copyName, copyPage, SPACE_ID, setting, new ArrayList<>(), registerAdministratorUser("copyHideAdmin"));

    LinkSetting copy = linkService.getLinkSettingByName(copyName);
    assertTrue("The window that takes the copy's name stays hidden for its user", linkService.isHidden(copy, USERNAME));
    assertFalse(linkService.isHidden(copy, otherUser));
    assertTrue("The source keeps its choices", linkService.isHidden(linkService.getLinkSettingByName(sourceName), USERNAME));
  }

  @Test
  @SneakyThrows
  public void testSaveLinkSettingKeepsTheOptionOnWhenClearingTheChoicesFails() {
    String settingName = "linkClearFailsSetting";
    LinkSetting linkSetting = initLinkSetting(settingName, "testClearFails");
    linkSetting.setAllowHide(true);
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser("clearFailsAdmin"));

    HiddenApplicationService service = linkService.hiddenApplicationService;
    HiddenApplicationService failing = mock(HiddenApplicationService.class);
    doThrow(new IllegalStateException("clear failed")).when(failing).clearHiddenChoices(LinkService.LINK_APPLICATION_TYPE, settingName);
    linkService.hiddenApplicationService = failing;
    try {
      LinkSetting off = linkService.getLinkSettingByName(settingName);
      off.setAllowHide(false);
      assertThrows(IllegalStateException.class,
                   () -> linkService.saveLinkSetting(off, null, registerAdministratorUser("clearFailsAdmin")));
    } finally {
      linkService.hiddenApplicationService = service;
    }
    assertTrue("A failed clear leaves the option on, so that saving it off again clears the remaining choices",
               linkService.getLinkSettingByName(settingName).isAllowHide());
  }

  @Test
  @SneakyThrows
  public void testSiteLayoutWindowIsHiddenListedWithNoPageAndShownAgain() {
    String settingName = "linkSiteLayoutHideSetting";
    registerInternalUser(USERNAME);
    String pageReference = createPage("testSiteLayoutHide", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    // The window is in the site's layout: the page its setting records doesn't hold it
    PortalConfig site = layoutService.getPortalConfig(PageKey.parse(pageReference).getSite());
    Container siteLayout = new Container();
    siteLayout.setChildren(new ArrayList<>(List.of(linksWindow(settingName))));
    site.setPortalLayout(siteLayout);
    layoutService.save(site);
    LinkSetting linkSetting = linkService.initLinkSetting(settingName, pageReference, SPACE_ID);
    linkSetting.setAllowHide(true);
    linkService.saveLinkSetting(linkSetting, null, registerAdministratorUser("siteLayoutAdmin"));

    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME);
    List<HiddenApplication> hiddenApplications = hiddenApplicationService.getHiddenApplications(USERNAME, "en");
    assertEquals("A hidden site-layout window is listed, so that its user can show it again",
                 Collections.singletonList(settingName),
                 hiddenApplications.stream().map(HiddenApplication::getName).toList());
    assertNull("It is listed with no page: it is on every page of its site", hiddenApplications.get(0).getPageReference());

    hiddenApplicationService.show(LinkService.LINK_APPLICATION_TYPE, settingName, USERNAME);
    assertFalse(linkService.isHidden(linkService.getLinkSettingByName(settingName), USERNAME));
    site = layoutService.getPortalConfig(PageKey.parse(pageReference).getSite());
    site.setPortalLayout(new Container());
    layoutService.save(site);
  }

  @Test
  @SneakyThrows
  public void testSaveLinkDataKeepsTheImportedListHiddenForTheUsersWhoHidItsSource() {
    String sourceName = "linkImportHiddenSource";
    String targetName = "linkImportHiddenTarget";
    org.exoplatform.services.security.Identity reader = registerInternalUser(USERNAME);
    LinkSetting source = initLinkSetting(sourceName, "testImportHiddenSource");
    source.setAllowHide(true);
    linkService.saveLinkSetting(source, null, registerAdministratorUser("importHideAdmin"));
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, sourceName, USERNAME);
    initLinkSetting(targetName, "testImportHiddenTarget");

    linkService.saveLinkData(targetName, draftExportOf(sourceName), reader);

    LinkSetting target = linkService.getLinkSettingByName(targetName);
    assertTrue("The imported list allows hiding", target.isAllowHide());
    assertTrue("The window that imports the data keeps it hidden for its user", linkService.isHidden(target, USERNAME));
  }

  @Test
  @SneakyThrows
  public void testSaveLinkDataCarriesNoChoiceFromASourceTheUserCannotRead() {
    String sourceName = "linkImportUnreadableSource";
    String targetName = "linkImportUnreadableTarget";
    String reader = "importUnreadableUser";
    registerInternalUser(USERNAME);
    org.exoplatform.services.security.Identity readerIdentity = registerInternalUser(reader);
    String sourcePage = createPage("testImportUnreadableSource", ADMINISTRATORS_GROUP, ADMINISTRATORS_GROUP);
    addLinksWindow(sourcePage, sourceName);
    LinkSetting source = linkService.initLinkSetting(sourceName, sourcePage, 0);
    source.setAllowHide(true);
    linkService.saveLinkSetting(source, null, registerAdministratorUser("importUnreadableAdmin"));
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, sourceName, "importUnreadableAdmin");
    initLinkSetting(targetName, "testImportUnreadableTarget");

    linkService.saveLinkData(targetName, draftExportOf(sourceName), readerIdentity);

    assertFalse("A source named by a preference that the user can't read carries nothing",
                linkService.isHidden(linkService.getLinkSettingByName(targetName), "importUnreadableAdmin"));
  }

  @Test
  @SneakyThrows
  public void testCopyLinkSettingCarriesNoChoiceFromASourceTheEditorCannotRead() {
    String sourceName = "linkCopyUnreadableSource";
    String copyName = "linkCopyUnreadableCopy";
    String admin = "copyUnreadableAdmin";
    org.exoplatform.services.security.Identity editor = registerInternalUser("copyUnreadableEditor");
    String sourcePage = createPage("testCopyUnreadableSource", ADMINISTRATORS_GROUP, ADMINISTRATORS_GROUP);
    addLinksWindow(sourcePage, sourceName);
    LinkSetting source = linkService.initLinkSetting(sourceName, sourcePage, 0);
    source.setAllowHide(true);
    linkService.saveLinkSetting(source, null, registerAdministratorUser(admin));
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, sourceName, admin);

    // The editor names, in the data of a window they may edit, a list on a page they cannot read
    String copyPage = createPage("testCopyUnreadableCopy", UserACL.EVERYONE, USERS_GROUP);
    LinkSetting setting = linkService.getLinkSettingByName(sourceName).clone();
    linkService.copyLinkSetting(copyName, copyPage, 0, setting, new ArrayList<>(), editor);

    LinkSetting copy = linkService.getLinkSettingByName(copyName);
    assertTrue(copy.isAllowHide());
    assertFalse("A source the editor can't read carries no choice", linkService.isHidden(copy, admin));
  }

  @Test
  @SneakyThrows
  public void testSaveLinkDataStoredBeforeTheSourceMarkerCarriesNoChoice() {
    String sourceName = "linkImportOldShapeSource";
    String targetName = "linkImportOldShapeTarget";
    org.exoplatform.services.security.Identity reader = registerInternalUser(USERNAME);
    LinkSetting source = initLinkSetting(sourceName, "testImportOldShapeSource");
    source.setAllowHide(true);
    linkService.saveLinkSetting(source, null, registerAdministratorUser("oldShapeAdmin"));
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, sourceName, USERNAME);
    initLinkSetting(targetName, "testImportOldShapeTarget");

    // A template saved before the marker stores its source's name in the setting only
    LinkData oldShape = linkService.getLinkDataByName(sourceName);
    assertEquals(sourceName, oldShape.getLinkSetting().getName());
    linkService.saveLinkData(targetName, oldShape, reader);

    LinkSetting target = linkService.getLinkSettingByName(targetName);
    assertTrue(target.isAllowHide());
    assertFalse("A list made from a template, however old, starts shown to all", linkService.isHidden(target, USERNAME));
  }

  @Test
  @SneakyThrows
  public void testACopyThatDoesNotAllowHidingCarriesNoChoice() {
    String sourceName = "linkNoHideCopySource";
    String copyName = "linkNoHideCopyCopy";
    String importName = "linkNoHideImportTarget";
    org.exoplatform.services.security.Identity user = registerInternalUser(USERNAME);
    org.exoplatform.services.security.Identity admin = registerAdministratorUser("noHideCopyAdmin");
    LinkSetting source = initLinkSetting(sourceName, "testNoHideCopySource");
    source.setAllowHide(true);
    linkService.saveLinkSetting(source, null, admin);
    hiddenApplicationService.hide(LinkService.LINK_APPLICATION_TYPE, sourceName, USERNAME);

    // Through the settings tool's copy, its option turned off
    String copyPage = createPage("testNoHideCopyCopy", UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    LinkSetting setting = linkService.getLinkSettingByName(sourceName).clone();
    setting.setAllowHide(false);
    linkService.copyLinkSetting(copyName, copyPage, SPACE_ID, setting, new ArrayList<>(), admin);

    // Through a site draft's import, its option turned off
    initLinkSetting(importName, "testNoHideImportTarget");
    LinkData draftExport = draftExportOf(sourceName);
    draftExport.getLinkSetting().setAllowHide(false);
    linkService.saveLinkData(importName, draftExport, user);

    // Social's own record of the choice, which the option does not mask
    assertFalse("A copy that does not allow hiding carries no choice",
                hiddenApplicationService.isHidden(LinkService.LINK_APPLICATION_TYPE, copyName, USERNAME));
    assertFalse("An import that does not allow hiding carries no choice",
                hiddenApplicationService.isHidden(LinkService.LINK_APPLICATION_TYPE, importName, USERNAME));
  }

  /**
   * @return the data a site draft's export writes for a window: its source
   *         named by the marker only
   */
  private LinkData draftExportOf(String sourceName) throws Exception {
    LinkData linkData = linkService.getLinkDataByName(sourceName);
    LinkSetting linkSetting = linkData.getLinkSetting().clone();
    linkSetting.setName(null);
    return new LinkData(linkSetting, linkData.getLinks(), sourceName);
  }

  private LinkSetting initLinkSetting(String linkSettingName, String pageName) {
    String pageReference = createPage(pageName, UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    addLinksWindow(pageReference, linkSettingName);
    return linkService.initLinkSetting(linkSettingName, pageReference, SPACE_ID);
  }

  /**
   * Puts on the page a Links window carrying the setting's name: social's
   * listing of hidden applications keeps a window only while its page holds it
   */
  private void addLinksWindow(String pageReference, String linkSettingName) {
    Page page = layoutService.getPage(pageReference);
    page.setChildren(new ArrayList<>(List.of(linksWindow(linkSettingName))));
    layoutService.save(page);
  }

  private Application linksWindow(String linkSettingName) {
    Application application = Application.createPortletApplication();
    application.setState(new TransientApplicationState("content/LinkPortlet",
                                                       new Portlet().putPreference(new Preference("name", linkSettingName, false))));
    return application;
  }

  private String createPage(String pageName, String accessPermission, String editPermission) {
    String siteType = "portal";
    String siteName = "test";
    if (layoutService.getPortalConfig(siteName) == null) {
      PortalConfig portal = new PortalConfig();
      portal.setType(siteType);
      portal.setName(siteName);
      portal.setLocale("en");
      portal.setLabel("Test");
      portal.setDescription("Test");
      portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
      layoutService.create(portal);
    }

    PageKey pageKey = new PageKey(siteType, siteName, pageName);
    PageState pageState = new PageState(pageName,
                                        null,
                                        false,
                                        null,
                                        Collections.singletonList(accessPermission),
                                        editPermission);
    layoutService.save(new PageContext(pageKey, pageState));
    return pageKey.format();
  }

  private void uploadResource() throws Exception {
    File tempFile = File.createTempFile("image", "temp");
    uploadService.createUploadResource(UPLOAD_ID, tempFile.getPath(), FILE_NAME, MIME_TYPE);
  }

  private org.exoplatform.services.security.Identity registerAdministratorUser(String user) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(user,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/administrators")));
    identityRegistry.register(identity);
    return identity;
  }

  private org.exoplatform.services.security.Identity registerInternalUser(String username) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(username,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/users")));
    identityRegistry.register(identity);
    return identity;
  }

}
