/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

@SpringBootTest(classes = {
  ImageAttachmentPlugin.class,
})
@RunWith(SpringRunner.class)
public class ImageAttachmentPluginTest {

  private static final String   CMS_SETTING_NAME = "cmsSettingName";

  @MockBean
  private CMSService            cmsService;

  @MockBean
  private IdentityManager       identityManager;

  @MockBean
  private SpaceService          spaceService;

  @MockBean
  private AttachmentService     attachmentService;

  @Autowired
  private ImageAttachmentPlugin imageAttachmentPlugin;

  @Test
  public void testHasAccessPermission() throws ObjectNotFoundException {
    org.exoplatform.services.security.Identity userIdentity = Mockito.mock(org.exoplatform.services.security.Identity.class);
    when(cmsService.hasAccessPermission(userIdentity, ImageAttachmentPlugin.OBJECT_TYPE, CMS_SETTING_NAME)).thenReturn(false);
    assertFalse(imageAttachmentPlugin.hasAccessPermission(userIdentity, CMS_SETTING_NAME));
    when(cmsService.hasAccessPermission(userIdentity, ImageAttachmentPlugin.OBJECT_TYPE, CMS_SETTING_NAME)).thenReturn(true);
    assertTrue(imageAttachmentPlugin.hasAccessPermission(userIdentity, CMS_SETTING_NAME));
  }

  @Test
  public void testHasEditPermission() throws ObjectNotFoundException {
    org.exoplatform.services.security.Identity userIdentity = Mockito.mock(org.exoplatform.services.security.Identity.class);
    when(cmsService.hasEditPermission(userIdentity, ImageAttachmentPlugin.OBJECT_TYPE, CMS_SETTING_NAME)).thenReturn(false);
    assertFalse(imageAttachmentPlugin.hasEditPermission(userIdentity, CMS_SETTING_NAME));
    when(cmsService.hasEditPermission(userIdentity, ImageAttachmentPlugin.OBJECT_TYPE, CMS_SETTING_NAME)).thenReturn(true);
    assertTrue(imageAttachmentPlugin.hasEditPermission(userIdentity, CMS_SETTING_NAME));
  }

  @Test
  public void testGetSpaceId() throws ObjectNotFoundException {
    CMSSetting setting = Mockito.mock(CMSSetting.class);
    when(setting.getSpaceId()).thenReturn(2l);
    when(cmsService.getSetting(ImageAttachmentPlugin.OBJECT_TYPE, CMS_SETTING_NAME)).thenReturn(setting);
    assertEquals(setting.getSpaceId(), imageAttachmentPlugin.getSpaceId(CMS_SETTING_NAME));
  }

  @Test
  public void testGetAudienceId() throws ObjectNotFoundException {
    CMSSetting setting = Mockito.mock(CMSSetting.class);
    Space space = Mockito.mock(Space.class);
    Identity spaceIdentity = Mockito.mock(Identity.class);
    when(space.getPrettyName()).thenReturn("spacePrettyName");
    when(setting.getSpaceId()).thenReturn(2l);
    when(spaceIdentity.getId()).thenReturn("5");
    when(spaceService.getSpaceById(String.valueOf(setting.getSpaceId()))).thenReturn(space);
    when(identityManager.getOrCreateSpaceIdentity(space.getPrettyName())).thenReturn(spaceIdentity);
    when(cmsService.getSetting(ImageAttachmentPlugin.OBJECT_TYPE, CMS_SETTING_NAME)).thenReturn(setting);
    assertEquals(Long.parseLong(spaceIdentity.getId()), imageAttachmentPlugin.getAudienceId(CMS_SETTING_NAME));
  }

}
