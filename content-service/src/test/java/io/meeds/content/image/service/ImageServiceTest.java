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
package io.meeds.content.image.service;

import static io.meeds.content.image.plugin.ImageAttachmentPlugin.OBJECT_TYPE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;

import io.meeds.social.cms.service.CMSService;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  private static final String PAGE        = "portal::classic::home";

  private static final long   IDENTITY_ID = 7L;

  @Mock
  private CMSService          cmsService;

  @Mock
  private AttachmentService   attachmentService;

  @InjectMocks
  private ImageService        imageService;

  @Test
  void testCreateImageSettingTakesAFreeNameKeyedOnThePage() throws Exception {
    when(cmsService.isSettingNameExists(eq(OBJECT_TYPE), anyString())).thenReturn(true, false);

    String name = imageService.createImageSetting(12L, PAGE, 3L, IDENTITY_ID);

    assertTrue(name.startsWith("Untitled-12-"));
    ArgumentCaptor<String> checked = ArgumentCaptor.forClass(String.class);
    verify(cmsService, times(2)).isSettingNameExists(eq(OBJECT_TYPE), checked.capture());
    assertEquals(name, checked.getAllValues().get(1));
    verify(cmsService).saveSettingName(OBJECT_TYPE, name, PAGE, 3L, IDENTITY_ID);
  }

  @Test
  void testCopyImageCopiesTheAttachment() {
    imageService.copyImage("source", "copy", IDENTITY_ID);

    verify(attachmentService).copyAttachments(OBJECT_TYPE, "source", OBJECT_TYPE, "copy", null, IDENTITY_ID);
  }

  @Test
  void testImportImageStoresTheDecodedContentAndDeletesItsTemporaryFile() throws Exception {
    List<byte[]> stored = new ArrayList<>();
    doAnswer(invocation -> {
      UploadedAttachmentDetail detail = invocation.getArgument(0);
      stored.add(Files.readAllBytes(new File(detail.getUploadedResource().getStoreLocation()).toPath()));
      return null;
    }).when(attachmentService).saveAttachment(any(), eq(OBJECT_TYPE), eq("name"), eq(null), eq(IDENTITY_ID));

    imageService.importImage("name", Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 }), IDENTITY_ID);

    ArgumentCaptor<UploadedAttachmentDetail> detail = ArgumentCaptor.forClass(UploadedAttachmentDetail.class);
    verify(attachmentService).saveAttachment(detail.capture(), eq(OBJECT_TYPE), eq("name"), eq(null), eq(IDENTITY_ID));
    UploadResource resource = detail.getValue().getUploadedResource();
    assertEquals("image/png", resource.getMimeType());
    assertEquals(UploadResource.UPLOADED_STATUS, resource.getStatus());
    assertArrayEquals(new byte[] { 1, 2, 3 }, stored.getFirst());
    assertFalse(new File(resource.getStoreLocation()).exists());
  }

  @Test
  void testImportImageDeletesItsTemporaryFileWhenTheSaveFails() throws Exception {
    doThrow(new IOException("disk")).when(attachmentService).saveAttachment(any(), any(), any(), any(), anyLong());
    String content = Base64.getEncoder().encodeToString(new byte[] { 1 });

    assertThrows(IOException.class, () -> imageService.importImage("name", content, IDENTITY_ID));

    ArgumentCaptor<UploadedAttachmentDetail> detail = ArgumentCaptor.forClass(UploadedAttachmentDetail.class);
    verify(attachmentService).saveAttachment(detail.capture(), any(), any(), any(), anyLong());
    assertFalse(new File(detail.getValue().getUploadedResource().getStoreLocation()).exists());
  }

  @Test
  void testImportImageFileStoresTheFileOfThePath() throws Exception {
    imageService.importImageFile("name", "/images/banner.png", IDENTITY_ID);

    ArgumentCaptor<UploadedAttachmentDetail> detail = ArgumentCaptor.forClass(UploadedAttachmentDetail.class);
    verify(attachmentService).saveAttachment(detail.capture(), eq(OBJECT_TYPE), eq("name"), eq(null), eq(IDENTITY_ID));
    UploadResource resource = detail.getValue().getUploadedResource();
    assertEquals("banner.png", resource.getFileName());
    assertEquals("/images/banner.png", resource.getStoreLocation());
    assertEquals("image/png", resource.getMimeType());
    assertEquals(UploadResource.UPLOADED_STATUS, resource.getStatus());
  }

}
