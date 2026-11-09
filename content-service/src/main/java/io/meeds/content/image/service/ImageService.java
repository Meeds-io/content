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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.upload.UploadResource;

import io.meeds.social.cms.service.CMSService;

/**
 * Creates the CMS setting of an Image window and stores its image: the image
 * is the attachment of the setting the window's {@code name} preference
 * names.
 */
@Service
public class ImageService {

  public static final String SETTING_NAME_PREFIX = "Untitled";

  public static final String IMAGE_MIME_TYPE     = "image/png";

  private static final Log   LOG                 = ExoLogger.getLogger(ImageService.class);

  @Autowired
  private CMSService         cmsService;

  @Autowired
  private AttachmentService  attachmentService;

  /**
   * Creates the CMS setting of an Image window, under a name no setting has,
   * keyed on the given page and space
   * 
   * @param  applicationId                the window's id
   * @param  pageReference                the page the setting is keyed on
   * @param  spaceId                      the space the setting is keyed on
   * @param  identityId                   the social identity id of the user
   *                                        creating it
   * @return                              the created setting's name
   * @throws ObjectAlreadyExistsException when the name was taken in the
   *                                        meantime
   */
  public String createImageSetting(long applicationId,
                                   String pageReference,
                                   long spaceId,
                                   long identityId) throws ObjectAlreadyExistsException {
    String name;
    do {
      name = SETTING_NAME_PREFIX + "-" + applicationId + "-" + UUID.randomUUID();
    } while (cmsService.isSettingNameExists(OBJECT_TYPE, name));
    cmsService.saveSettingName(OBJECT_TYPE, name, pageReference, spaceId, identityId);
    return name;
  }

  /**
   * Copies the image of a setting to another one
   * 
   * @param sourceSettingName the setting holding the image
   * @param settingName       the setting receiving its copy
   * @param identityId        the social identity id of the user copying it
   */
  public void copyImage(String sourceSettingName, String settingName, long identityId) {
    attachmentService.copyAttachments(OBJECT_TYPE, sourceSettingName, OBJECT_TYPE, settingName, null, identityId);
  }

  /**
   * Stores an image given as its Base64 content, as a window's
   * {@code data.init} preference carries it, as the image of a setting
   * 
   * @param  settingName                  the setting receiving the image
   * @param  imageContent                 the image's content, Base64 encoded
   * @param  identityId                   the social identity id of the user
   *                                        storing it
   * @throws IOException                  when the content cannot be written
   *                                        to a temporary file or stored
   * @throws ObjectAlreadyExistsException when the attachment exists
   * @throws ObjectNotFoundException      when the setting is not found
   */
  public void importImage(String settingName, String imageContent, long identityId) throws IOException,
                                                                                     ObjectAlreadyExistsException,
                                                                                     ObjectNotFoundException {
    File tempFile = File.createTempFile("image", ".png");
    try {
      Files.write(tempFile.toPath(), Base64.decodeBase64(imageContent));
      importImageFile(settingName, tempFile.getAbsolutePath(), identityId);
    } finally {
      try {
        Files.deleteIfExists(tempFile.toPath());
      } catch (IOException e) {
        LOG.warn("Temporary file {} of an imported image not deleted", tempFile, e);
      }
    }
  }

  /**
   * Stores the image file of the given path as the image of a setting
   * 
   * @param  settingName                  the setting receiving the image
   * @param  filePath                     the image file's path
   * @param  identityId                   the social identity id of the user
   *                                        storing it
   * @throws IOException                  when the file cannot be stored
   * @throws ObjectAlreadyExistsException when the attachment exists
   * @throws ObjectNotFoundException      when the setting is not found
   */
  public void importImageFile(String settingName, String filePath, long identityId) throws IOException,
                                                                                    ObjectAlreadyExistsException,
                                                                                    ObjectNotFoundException {
    UploadResource uploadResource = new UploadResource("ImageImport" + UUID.randomUUID());
    uploadResource.setFileName(new File(filePath).getName());
    uploadResource.setMimeType(IMAGE_MIME_TYPE);
    uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
    uploadResource.setStoreLocation(filePath);
    attachmentService.saveAttachment(new UploadedAttachmentDetail(uploadResource), OBJECT_TYPE, settingName, null, identityId);
  }

}
