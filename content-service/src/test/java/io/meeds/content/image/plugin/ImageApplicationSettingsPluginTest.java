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
package io.meeds.content.image.plugin;

import static io.meeds.content.image.plugin.ImageAttachmentPlugin.OBJECT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.FileAttachmentObject;
import org.exoplatform.social.attachment.model.FileAttachmentResourceList;
import org.exoplatform.social.attachment.model.ObjectAttachmentDetail;
import org.exoplatform.social.attachment.model.ObjectAttachmentList;
import org.exoplatform.social.attachment.model.ObjectAttachmentOperationReport;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.ImageConsumption;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.layout.service.LayoutImageService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class ImageApplicationSettingsPluginTest {

  private static final String            USERNAME    = "mary";

  private static final String            LIVE_PAGE   = "portal::classic::home";

  private static final String            LIVE_NAME   = "Untitled-12-live";

  private static final String            UPLOAD_ID   = "upload-1";

  private static final long              IDENTITY_ID = 7L;

  @Mock
  private CMSService                     cmsService;

  @Mock
  private AttachmentService              attachmentService;

  @Mock
  private IdentityManager                identityManager;

  @Mock
  private UploadService                  uploadService;

  @Mock
  private UserACL                        userAcl;

  @Mock
  private LayoutImageService             layoutImageService;

  @Mock
  private ApplicationSettingsService     applicationSettingsService;

  @InjectMocks
  private ImageApplicationSettingsPlugin plugin;

  private Identity                       identity;

  private Map<String, List<ObjectAttachmentDetail>> attachments;

  @BeforeEach
  void setUp() throws Exception {
    identity = mock(Identity.class);
    attachments = new HashMap<>();
    lenient().when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    lenient().when(cmsService.hasEditPermission(identity, LIVE_PAGE, 0L)).thenReturn(true);
    lenient().when(cmsService.hasAccessPermission(identity, OBJECT_TYPE, LIVE_NAME)).thenReturn(true);
    lenient().when(layoutImageService.isConsumable(UPLOAD_ID, USERNAME)).thenReturn(true);
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                        mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(socialIdentity.getId()).thenReturn(String.valueOf(IDENTITY_ID));
    lenient().when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(socialIdentity);
    lenient().when(attachmentService.getAttachments(eq(OBJECT_TYPE), anyString()))
             .thenAnswer(invocation -> new ObjectAttachmentList(attachments.getOrDefault(invocation.getArgument(1),
                                                                                         new ArrayList<>()),
                                                                OBJECT_TYPE,
                                                                invocation.getArgument(1)));
    lenient().when(attachmentService.saveAttachments(any(FileAttachmentResourceList.class), eq(identity)))
             .thenAnswer(invocation -> {
               FileAttachmentResourceList list = invocation.getArgument(0);
               List<ObjectAttachmentDetail> stored = new ArrayList<>();
               list.getAttachedFiles().forEach(file -> stored.add(detail(file.getId(), file)));
               list.getUploadedFiles().forEach(file -> stored.add(detail("new-file", file)));
               attachments.put(list.getObjectId(), stored);
               return new ObjectAttachmentOperationReport();
             });
    lenient().when(attachmentService.getAttachmentFileIds(eq(OBJECT_TYPE), anyString()))
             .thenAnswer(invocation -> attachments.getOrDefault(invocation.getArgument(1), new ArrayList<>())
                                                  .stream()
                                                  .map(ObjectAttachmentDetail::getId)
                                                  .toList());
  }

  @Test
  void testInitRegistersOnlyWithTheLayoutServices() throws Exception {
    plugin.init();
    verify(applicationSettingsService).addPlugin(plugin);
    assertTrue(ImageApplicationSettingsPlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
  }

  @Test
  void testSchemaNamesTheDrawerOptions() {
    assertEquals("Image", plugin.getPortletName());
    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) plugin.getSettingsSchema().get("properties");
    assertEquals(List.of("image_upload_id", "image_url", "format", "alt_text", "link_url", "link_target"),
                 new ArrayList<>(properties.keySet()));
  }

  @Test
  void testReadWithoutSettingIsNull() throws Exception {
    assertNull(plugin.read(context(null, false)));
    assertNull(plugin.read(context(LIVE_NAME, false)));
  }

  @Test
  void testReadReturnsTheStoredImage() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", "alt", "square", "/portal/classic/news", "_blank");

    Map<String, Object> settings = plugin.read(context(LIVE_NAME, false));

    assertEquals("/portal/rest/v1/social/attachments/imagePortlet/" + LIVE_NAME + "/file-1", settings.get("image_url"));
    assertEquals("square", settings.get("format"));
    assertEquals("alt", settings.get("alt_text"));
    assertEquals("/portal/classic/news", settings.get("link_url"));
    assertEquals("new_tab", settings.get("link_target"));
  }

  @Test
  void testReadRefusesASettingTheUserCannotAccess() {
    storeLiveSetting();
    when(cmsService.hasAccessPermission(identity, OBJECT_TYPE, LIVE_NAME)).thenReturn(false);
    ApplicationSettingsContext context = context(LIVE_NAME, false);
    assertThrows(IllegalAccessException.class, () -> plugin.read(context));
  }

  @Test
  void testValidateRefusesANonEditor() {
    when(cmsService.hasEditPermission(identity, LIVE_PAGE, 0L)).thenReturn(false);
    ApplicationSettingsContext context = context(null, true);
    Map<String, Object> settings = Map.of("image_upload_id", UPLOAD_ID);
    assertThrows(IllegalAccessException.class, () -> plugin.validate(context, settings));
  }

  @Test
  void testValidateRefusesAnUploadTheCallerCannotConsume() {
    when(layoutImageService.isConsumable(UPLOAD_ID, USERNAME)).thenReturn(false);
    ApplicationSettingsContext context = context(null, true);
    Map<String, Object> settings = Map.of("image_upload_id", UPLOAD_ID);
    ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class, () -> plugin.validate(context, settings));
    assertEquals(ImageConsumption.UPLOAD_NOT_FOUND, exception.getMessage());
  }

  @Test
  void testValidateRefusesValuesTheDrawerDoesNotOffer() {
    ApplicationSettingsContext context = context(null, true);
    assertThrows(IllegalArgumentException.class,
                 () -> plugin.validate(context, Map.of("image_upload_id", UPLOAD_ID, "format", "wide")));
    assertThrows(IllegalArgumentException.class,
                 () -> plugin.validate(context, Map.of("image_upload_id", UPLOAD_ID, "link_target", "_blank")));
    assertThrows(IllegalArgumentException.class,
                 () -> plugin.validate(context, Map.of("image_upload_id", UPLOAD_ID, "link_url", "javascript:alert(1)")));
    assertThrows(IllegalArgumentException.class,
                 () -> plugin.validate(context, Map.of("image_upload_id", UPLOAD_ID, "alt_text", "a".repeat(1001))));
    assertThrows(IllegalArgumentException.class,
                 () -> plugin.validate(context,
                                       Map.of("image_upload_id", UPLOAD_ID, "alt_text", "</script><img src=x onerror=alert(1)>")));
    assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, Map.of("image_path", "/a.png")));
    assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, Map.of("image_upload_id", 12)));
  }

  @Test
  void testValidateRefusesPropertiesWithoutAnImage() {
    ApplicationSettingsContext context = context(null, true);
    Map<String, Object> settings = Map.of("format", "square");
    assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
  }

  @Test
  void testValidateRefusesPropertiesWithARemovedImage() {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", null, "landscape", null, null);
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    Map<String, Object> settings = Map.of("image_upload_id", "", "format", "square");
    assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
  }

  @Test
  void testValidateKeepsOnlyTheStoredImageUrl() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", null, "landscape", null, null);
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    plugin.validate(context,
                    Map.of("image_url", "/portal/rest/v1/social/attachments/imagePortlet/" + LIVE_NAME + "/file-1"));
    Map<String, Object> settings = Map.of("image_url", "https://example.com/a.png");
    assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    verify(attachmentService, never()).saveAttachments(any(), any());
  }

  @Test
  void testApplyOnADraftCopiesTheLiveImageAndLeavesItUnchanged() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", "kept alt", "portrait", "/portal/classic/news", null);
    doAnswer(invocation -> {
      String copyName = invocation.getArgument(3);
      attachments.put(copyName, new ArrayList<>(List.of(attachments.get(LIVE_NAME).getFirst().clone())));
      return null;
    }).when(attachmentService).copyAttachments(eq(OBJECT_TYPE), eq(LIVE_NAME), eq(OBJECT_TYPE), anyString(), eq(null), eq(IDENTITY_ID));
    ApplicationSettingsContext context = context(LIVE_NAME, true);

    plugin.apply(context, Map.of("image_upload_id", UPLOAD_ID, "format", "square"));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(cmsService).saveSettingName(eq(OBJECT_TYPE), name.capture(), eq(LIVE_PAGE), eq(0L), eq(IDENTITY_ID));
    assertNotEquals(LIVE_NAME, name.getValue());
    verify(applicationSettingsService).markOwner(context, Map.of("name", name.getValue()));
    ArgumentCaptor<FileAttachmentResourceList> list = ArgumentCaptor.forClass(FileAttachmentResourceList.class);
    verify(attachmentService).saveAttachments(list.capture(), eq(identity));
    assertEquals(name.getValue(), list.getValue().getObjectId());
    assertEquals(OBJECT_TYPE, list.getValue().getObjectType());
    assertEquals(IDENTITY_ID, list.getValue().getUserIdentityId());
    assertTrue(list.getValue().getAttachedFiles().isEmpty());
    assertEquals(List.of(new FileAttachmentObject(null, UPLOAD_ID, "kept alt", "square", "/portal/classic/news", null)),
                 list.getValue().getUploadedFiles());
    assertEquals("file-1", attachments.get(LIVE_NAME).getFirst().getId());
    verify(uploadService).removeUploadResource(UPLOAD_ID);
  }

  @Test
  void testApplyOnANewWindowCreatesItsSettingWithTheCustomFormat() throws Exception {
    ApplicationSettingsContext context = context(null, true);

    plugin.apply(context, Map.of("image_upload_id", UPLOAD_ID, "alt_text", "Team", "link_target", "new_tab"));

    verify(cmsService).saveSettingName(eq(OBJECT_TYPE), anyString(), eq(LIVE_PAGE), eq(0L), eq(IDENTITY_ID));
    verify(attachmentService, never()).copyAttachments(any(), any(), any(), any(), any(), anyLong());
    ArgumentCaptor<FileAttachmentResourceList> list = ArgumentCaptor.forClass(FileAttachmentResourceList.class);
    verify(attachmentService).saveAttachments(list.capture(), eq(identity));
    assertEquals(List.of(new FileAttachmentObject(null, UPLOAD_ID, "Team", "custom", null, "_blank")),
                 list.getValue().getUploadedFiles());
  }

  @Test
  void testApplyOnAnOwnedWindowUpdatesTheStoredImageInPlace() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", "old", "portrait", "/portal/classic/news", "_blank");
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    when(applicationSettingsService.isOwner(context)).thenReturn(true);

    plugin.apply(context, Map.of("alt_text", "", "link_target", "same_tab"));

    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    ArgumentCaptor<FileAttachmentResourceList> list = ArgumentCaptor.forClass(FileAttachmentResourceList.class);
    verify(attachmentService).saveAttachments(list.capture(), eq(identity));
    assertEquals(LIVE_NAME, list.getValue().getObjectId());
    assertTrue(list.getValue().getUploadedFiles().isEmpty());
    assertEquals(List.of(new FileAttachmentObject("file-1", null, null, "portrait", "/portal/classic/news", null)),
                 list.getValue().getAttachedFiles());
  }

  @Test
  void testApplyOnAWindowOwnedOnAnotherPageCopies() throws Exception {
    storeLiveSetting();
    when(cmsService.getSetting(OBJECT_TYPE, LIVE_NAME)).thenReturn(new CMSSetting(OBJECT_TYPE, LIVE_NAME, "portal::classic::other", 0L));
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    lenient().when(applicationSettingsService.isOwner(context)).thenReturn(true);

    plugin.apply(context, Map.of("image_upload_id", UPLOAD_ID));

    verify(cmsService).saveSettingName(eq(OBJECT_TYPE), anyString(), eq(LIVE_PAGE), eq(0L), eq(IDENTITY_ID));
  }

  @Test
  void testApplyRemovesTheImage() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", null, "landscape", null, null);
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    when(applicationSettingsService.isOwner(context)).thenReturn(true);

    plugin.apply(context, Map.of("image_upload_id", ""));

    ArgumentCaptor<FileAttachmentResourceList> list = ArgumentCaptor.forClass(FileAttachmentResourceList.class);
    verify(attachmentService).saveAttachments(list.capture(), eq(identity));
    assertTrue(list.getValue().getUploadedFiles().isEmpty());
    assertTrue(list.getValue().getAttachedFiles().isEmpty());
  }

  @Test
  void testApplyWithoutChangeWritesNoAttachment() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", null, "landscape", null, null);
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    when(applicationSettingsService.isOwner(context)).thenReturn(true);

    plugin.apply(context, Map.of());

    verify(attachmentService, never()).saveAttachments(any(), any());
  }

  @Test
  void testApplyReleasesTheUploadWhenTheSaveFails() throws Exception {
    ApplicationSettingsContext context = context(null, true);
    when(attachmentService.saveAttachments(any(FileAttachmentResourceList.class), eq(identity))).thenThrow(new IllegalStateException("boom"));
    Map<String, Object> settings = Map.of("image_upload_id", UPLOAD_ID);

    assertThrows(IllegalStateException.class, () -> plugin.apply(context, settings));

    verify(uploadService).removeUploadResource(UPLOAD_ID);
  }

  @Test
  void testApplyRefusesAnUploadTheAttachmentServiceDidNotSave() throws Exception {
    ApplicationSettingsContext context = context(null, true);
    ObjectAttachmentOperationReport report = new ObjectAttachmentOperationReport();
    report.addError(UPLOAD_ID, "attachment.uploadIdNotFound");
    when(attachmentService.saveAttachments(any(FileAttachmentResourceList.class), eq(identity))).thenReturn(report);
    Map<String, Object> settings = Map.of("image_upload_id", UPLOAD_ID);

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> plugin.apply(context, settings));

    assertEquals(LayoutImageService.IMAGE_NOT_SAVED, exception.getMessage());
    verify(uploadService).removeUploadResource(UPLOAD_ID);
  }

  @Test
  void testApplyRefusedBeforeAnyWriteStillReleasesTheUpload() throws Exception {
    when(cmsService.hasEditPermission(identity, LIVE_PAGE, 0L)).thenReturn(false);
    ApplicationSettingsContext context = context(null, true);
    Map<String, Object> settings = Map.of("image_upload_id", UPLOAD_ID);

    assertThrows(IllegalAccessException.class, () -> plugin.apply(context, settings));

    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(uploadService).removeUploadResource(UPLOAD_ID);
  }

  @Test
  void testReadWithoutImageReportsTheFormatANewImageTakes() throws Exception {
    storeLiveSetting();

    assertEquals("custom", plugin.read(context(LIVE_NAME, false)).get("format"));
  }

  @Test
  void testApplyKeepsAStoredAltTextSentBackUnchanged() throws Exception {
    storeLiveSetting();
    storeImage(LIVE_NAME, "file-1", "Revenue <10%", "landscape", null, null);
    ApplicationSettingsContext context = context(LIVE_NAME, true);
    when(applicationSettingsService.isOwner(context)).thenReturn(true);

    plugin.apply(context, Map.of("alt_text", "Revenue <10%", "link_url", "/portal/classic/news"));

    ArgumentCaptor<FileAttachmentResourceList> list = ArgumentCaptor.forClass(FileAttachmentResourceList.class);
    verify(attachmentService).saveAttachments(list.capture(), eq(identity));
    assertEquals(List.of(new FileAttachmentObject("file-1", null, "Revenue <10%", "landscape", "/portal/classic/news", null)),
                 list.getValue().getAttachedFiles());
    Map<String, Object> changed = Map.of("alt_text", "Revenue <20%");
    assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, changed));
  }

  @Test
  void testValidateLeavesTheUploadUsable() throws Exception {
    ApplicationSettingsContext context = context(null, true);

    plugin.validate(context, Map.of("image_upload_id", UPLOAD_ID, "format", "square"));

    verify(uploadService, never()).removeUploadResource(anyString());
    verify(attachmentService, never()).saveAttachments(any(), any());
  }

  private void storeLiveSetting() {
    lenient().when(cmsService.getSetting(OBJECT_TYPE, LIVE_NAME)).thenReturn(new CMSSetting(OBJECT_TYPE, LIVE_NAME, LIVE_PAGE, 0L));
  }

  private void storeImage(String name, String fileId, String altText, String format, String linkUrl, String linkTarget) {
    ObjectAttachmentDetail image = new ObjectAttachmentDetail(fileId, "image.png", "image/png", 10, 0, USERNAME);
    image.setAltText(altText);
    image.setFormat(format);
    image.setLinkUrl(linkUrl);
    image.setLinkTarget(linkTarget);
    attachments.put(name, new ArrayList<>(List.of(image)));
  }

  private ApplicationSettingsContext context(String name, boolean draft) {
    Map<String, String> preferences = new HashMap<>();
    if (name != null) {
      preferences.put("name", name);
    }
    return new ApplicationSettingsContext(12L,
                                          "content/Image",
                                          preferences,
                                          false,
                                          draft,
                                          null,
                                          null,
                                          LIVE_PAGE,
                                          0L,
                                          USERNAME);
  }

  private static ObjectAttachmentDetail detail(String id, FileAttachmentObject file) {
    ObjectAttachmentDetail detail = new ObjectAttachmentDetail(id, "image.png", "image/png", 10, 0, USERNAME);
    detail.setAltText(file.getAltText());
    detail.setFormat(file.getFormat());
    detail.setLinkUrl(file.getLinkUrl());
    detail.setLinkTarget(file.getLinkTarget());
    return detail;
  }

}
