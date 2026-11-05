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

import static io.meeds.content.utils.ApplicationSettingsUtils.enumSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.notAccepted;
import static io.meeds.content.utils.ApplicationSettingsUtils.objectSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.requireKnownOptions;
import static io.meeds.content.utils.ApplicationSettingsUtils.siteWithoutPage;
import static io.meeds.content.utils.ApplicationSettingsUtils.stringSchema;
import static io.meeds.content.utils.ApplicationSettingsUtils.toText;
import static io.meeds.content.utils.ApplicationSettingsUtils.toUrl;
import static io.meeds.content.utils.ApplicationSettingsUtils.toValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.FileAttachmentObject;
import org.exoplatform.social.attachment.model.FileAttachmentResourceList;
import org.exoplatform.social.attachment.model.ObjectAttachmentDetail;
import org.exoplatform.social.attachment.model.ObjectAttachmentList;
import org.exoplatform.social.attachment.model.ObjectAttachmentOperationReport;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.ImageConsumption;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.layout.service.LayoutImageService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

import jakarta.annotation.PostConstruct;

/**
 * Configures an Image window as its settings drawer does. The image and its
 * format, alternative text and link are an attachment of the CMS setting the
 * window's {@code name} preference names, which a draft window shares with
 * the live one: the first write on a window that does not own its setting
 * creates a copy for it, keyed on the live page, the site's home page for a
 * window of a site container, with the stored image. A window copied with no
 * setting name carries the image's bytes in its {@code data.init} preference
 * instead, which the portlet imports at the first render: the first write
 * imports them into the setting it creates, as the render imports that
 * preference (a descriptor's {@code image-path} is the render's alone).
 */
@Component
@Profile("layout")
public class ImageApplicationSettingsPlugin implements ApplicationSettingsPlugin {

  public static final String        PORTLET_NAME        = "Image";

  public static final String        NAME_PREFERENCE     = "name";

  public static final String        DATA_INIT_PREFERENCE = "data.init";

  private static final Log          LOG                 = ExoLogger.getLogger(ImageApplicationSettingsPlugin.class);

  public static final String        IMAGE_UPLOAD_ID     = "image_upload_id";

  public static final String        IMAGE_URL           = "image_url";

  public static final String        FORMAT              = "format";

  public static final String        ALT_TEXT            = "alt_text";

  public static final String        LINK_URL            = "link_url";

  public static final String        LINK_TARGET         = "link_target";

  public static final String        NEW_TAB             = "new_tab";

  public static final String        SAME_TAB            = "same_tab";

  public static final String        BLANK_TARGET        = "_blank";

  public static final String        DEFAULT_FORMAT      = "landscape";

  public static final String        CUSTOM_FORMAT       = "custom";

  public static final int           MAX_ALT_TEXT_LENGTH = 1000;

  public static final String        IMAGE_URL_FORMAT    = "/portal/rest/v1/social/attachments/%s/%s/%s";

  public static final List<String>  FORMATS             = Arrays.asList(DEFAULT_FORMAT, "portrait", "square", CUSTOM_FORMAT);

  public static final List<String>  LINK_TARGETS        = Arrays.asList(NEW_TAB, SAME_TAB);

  private static final String       SETTING_NAME_PREFIX = "Untitled";

  private static final List<String> OPTIONS             = Arrays.asList(IMAGE_UPLOAD_ID,
                                                                        IMAGE_URL,
                                                                        FORMAT,
                                                                        ALT_TEXT,
                                                                        LINK_URL,
                                                                        LINK_TARGET);

  private static final List<String> IMAGE_PROPERTIES    = Arrays.asList(FORMAT, ALT_TEXT, LINK_URL, LINK_TARGET);

  @Autowired
  private CMSService                 cmsService;

  @Autowired
  private AttachmentService          attachmentService;

  @Autowired
  private IdentityManager            identityManager;

  @Autowired
  private UploadService              uploadService;

  @Autowired
  private UserACL                    userAcl;

  @Autowired(required = false)
  private LayoutImageService         layoutImageService;

  @Autowired(required = false)
  private ApplicationSettingsService applicationSettingsService;

  @PostConstruct
  public void init() {
    if (applicationSettingsService != null && layoutImageService != null) {
      applicationSettingsService.addPlugin(this);
    }
  }

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public boolean isConfigurableInSiteContainer() {
    return true;
  }

  @Override
  public Map<String, Object> getSettingsSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(IMAGE_UPLOAD_ID,
                   stringSchema("The upload_id upload_layout_image returned, replacing the image shown; the id is consumed and released by the call that writes this application, whether the write succeeds or fails, and stays usable when the call is refused before that write; an empty text removes the image, and is then given with no other setting but image_url"));
    properties.put(IMAGE_URL,
                   stringSchema("The url of the image shown, read only: the stored url sent back unchanged is kept"));
    properties.put(FORMAT,
                   enumSchema(FORMATS,
                              "The box the image is shown in: custom (the default whenever image_upload_id is given without format) keeps the image's own ratio; landscape (8:1), portrait (3:4) and square (1:1) stretch the image to that ratio, so send an image already cropped to it"));
    properties.put(ALT_TEXT,
                   stringSchema("The alternative text read by screen readers, at most 1000 characters, without '<' unless sent back unchanged; an empty text removes it"));
    properties.put(LINK_URL,
                   stringSchema("The link opened on a click: an http(s) URL or a path of the platform; an empty text removes it"));
    properties.put(LINK_TARGET, enumSchema(LINK_TARGETS, "Where the link opens: same_tab (default) or new_tab"));
    return objectSchema(properties);
  }

  @Override
  public Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException {
    CMSSetting setting = getReadableSource(context, getIdentity(context));
    if (setting == null) {
      return null; // NOSONAR
    }
    ObjectAttachmentDetail image = getImage(setting.getName());
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put(IMAGE_URL, image == null ? "" : getImageUrl(setting.getName(), image));
    settings.put(FORMAT, image == null ? CUSTOM_FORMAT : StringUtils.defaultIfBlank(image.getFormat(), DEFAULT_FORMAT));
    settings.put(ALT_TEXT, image == null ? "" : StringUtils.defaultString(image.getAltText()));
    settings.put(LINK_URL, image == null ? "" : StringUtils.defaultString(image.getLinkUrl()));
    settings.put(LINK_TARGET, image != null && BLANK_TARGET.equals(image.getLinkTarget()) ? NEW_TAB : SAME_TAB);
    return settings;
  }

  @Override
  public void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                         ObjectNotFoundException {
    Identity identity = requireEditor(context);
    CMSSetting source = getReadableSource(context, identity);
    ObjectAttachmentDetail image = source == null ? null : getImage(source.getName());
    String uploadId = getUploadId(settings);
    requireImageUrl(settings, source, image);
    mergeImage(image, settings, uploadId);
    if (StringUtils.isNotBlank(uploadId) && !layoutImageService.isConsumable(uploadId, context.getUsername())) {
      throw new ObjectNotFoundException(ImageConsumption.UPLOAD_NOT_FOUND);
    }
  }

  @Override
  public void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    String uploadId = settings.get(IMAGE_UPLOAD_ID) instanceof String id ? StringUtils.trimToNull(id) : null;
    try {
      validate(context, settings);
      Identity identity = requireEditor(context);
      CMSSetting source = getReadableSource(context, identity);
      long identityId = Long.parseLong(identityManager.getOrCreateUserIdentity(context.getUsername()).getId());
      String name;
      if (source != null && applicationSettingsService.isOwner(context)
          && StringUtils.equals(source.getPageReference(), context.getPageReference())) {
        name = source.getName();
      } else {
        name = createSetting(context, identityId);
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put(NAME_PREFERENCE, name);
        if (source != null) {
          attachmentService.copyAttachments(ImageAttachmentPlugin.OBJECT_TYPE,
                                            source.getName(),
                                            ImageAttachmentPlugin.OBJECT_TYPE,
                                            name,
                                            null,
                                            identityId);
        } else if (StringUtils.isNotBlank(context.getPreferences().get(DATA_INIT_PREFERENCE))) {
          importInitialImage(name, context.getPreferences().get(DATA_INIT_PREFERENCE), identityId);
          keys.put(DATA_INIT_PREFERENCE, null);
        }
        applicationSettingsService.markOwner(context, keys);
      }
      saveImage(name, getImage(name), settings, getUploadId(settings), identityId, identity);
    } finally {
      uploadService.removeUploadResource(uploadId);
    }
  }

  private void saveImage(String name,
                         ObjectAttachmentDetail image,
                         Map<String, Object> settings,
                         String uploadId,
                         long identityId,
                         Identity identity) throws IllegalAccessException, ObjectNotFoundException {
    FileAttachmentObject merged = mergeImage(image, settings, uploadId);
    if (merged == null && (image == null || !"".equals(uploadId))) {
      return;
    }
    FileAttachmentResourceList list = new FileAttachmentResourceList();
    list.setObjectType(ImageAttachmentPlugin.OBJECT_TYPE);
    list.setObjectId(name);
    list.setUserIdentityId(identityId);
    list.setUploadedFiles(new ArrayList<>());
    list.setAttachedFiles(new ArrayList<>());
    if (merged != null) {
      if (merged.getUploadId() == null) {
        list.getAttachedFiles().add(merged);
      } else {
        list.getUploadedFiles().add(merged);
      }
    }
    ObjectAttachmentOperationReport report = attachmentService.saveAttachments(list, identity);
    if (StringUtils.isNotEmpty(uploadId)
        && (report == null || report.getErrorByUploadId().containsKey(uploadId)
            || CollectionUtils.isEmpty(attachmentService.getAttachmentFileIds(ImageAttachmentPlugin.OBJECT_TYPE, name)))) {
      throw new IllegalStateException(LayoutImageService.IMAGE_NOT_SAVED);
    }
  }

  /**
   * @return the attachment to save: the given upload with the stored
   *         properties merged with the given ones, the stored image with the
   *         given properties when no upload is given, null when the image is
   *         removed or nothing changes
   */
  private FileAttachmentObject mergeImage(ObjectAttachmentDetail image, Map<String, Object> settings, String uploadId) {
    requireKnownOptions(settings, OPTIONS);
    boolean propertiesGiven = IMAGE_PROPERTIES.stream().anyMatch(settings::containsKey);
    if ("".equals(uploadId)) {
      if (propertiesGiven) {
        throw notAccepted(IMAGE_UPLOAD_ID);
      }
      return null;
    }
    if (uploadId == null && !propertiesGiven) {
      return null;
    }
    if (uploadId == null && image == null) {
      throw notAccepted(IMAGE_UPLOAD_ID);
    }
    FileAttachmentObject merged = new FileAttachmentObject(uploadId == null ? image.getId() : null,
                                                           uploadId,
                                                           image == null ? null : image.getAltText(),
                                                           image == null || uploadId != null ? CUSTOM_FORMAT
                                                                         : StringUtils.defaultIfBlank(image.getFormat(),
                                                                                                      DEFAULT_FORMAT),
                                                           image == null ? null : image.getLinkUrl(),
                                                           image == null ? null : image.getLinkTarget());
    if (settings.containsKey(FORMAT)) {
      merged.setFormat(toValue(settings.get(FORMAT), FORMAT, FORMATS));
    }
    if (settings.containsKey(ALT_TEXT)) {
      String altText = toText(settings.get(ALT_TEXT), ALT_TEXT, MAX_ALT_TEXT_LENGTH);
      if (altText.contains("<") && !altText.equals(merged.getAltText())) {
        throw notAccepted(ALT_TEXT);
      }
      merged.setAltText(StringUtils.trimToNull(altText));
    }
    if (settings.containsKey(LINK_URL)) {
      Object linkUrl = settings.get(LINK_URL);
      if ("".equals(linkUrl)) {
        merged.setLinkUrl(null);
      } else if (!linkUrl.equals(merged.getLinkUrl())) {
        merged.setLinkUrl(toUrl(linkUrl, LINK_URL));
      }
    }
    if (settings.containsKey(LINK_TARGET)) {
      merged.setLinkTarget(NEW_TAB.equals(toValue(settings.get(LINK_TARGET), LINK_TARGET, LINK_TARGETS)) ? BLANK_TARGET : null);
    }
    return merged;
  }

  private String getUploadId(Map<String, Object> settings) {
    if (!settings.containsKey(IMAGE_UPLOAD_ID)) {
      return null;
    }
    if (!(settings.get(IMAGE_UPLOAD_ID) instanceof String uploadId)) {
      throw notAccepted(IMAGE_UPLOAD_ID);
    }
    return uploadId.trim();
  }

  private void requireImageUrl(Map<String, Object> settings, CMSSetting source, ObjectAttachmentDetail image) {
    if (settings.containsKey(IMAGE_URL)) {
      String storedUrl = image == null ? "" : getImageUrl(source.getName(), image);
      if (!storedUrl.equals(settings.get(IMAGE_URL))) {
        throw notAccepted(IMAGE_URL);
      }
    }
  }

  private Identity requireEditor(ApplicationSettingsContext context) throws IllegalAccessException {
    if (context.isSiteContainer() && StringUtils.isBlank(context.getPageReference())) {
      throw siteWithoutPage();
    }
    Identity identity = getIdentity(context);
    if (identity == null || StringUtils.isBlank(context.getPageReference())
        || !cmsService.hasEditPermission(identity, context.getPageReference(), context.getSpaceId())) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    return identity;
  }

  /**
   * @return the setting the window's {@code name} preference designates,
   *         null when it designates none
   * @throws IllegalAccessException when the user may not read it: the
   *                                  preference is client-writable
   */
  private CMSSetting getReadableSource(ApplicationSettingsContext context, Identity identity) throws IllegalAccessException {
    String name = context.getPreferences() == null ? null : context.getPreferences().get(NAME_PREFERENCE);
    if (StringUtils.isBlank(name)) {
      return null;
    }
    CMSSetting setting = cmsService.getSetting(ImageAttachmentPlugin.OBJECT_TYPE, name);
    if (setting == null) {
      return null;
    }
    if (identity == null || !cmsService.hasAccessPermission(identity, ImageAttachmentPlugin.OBJECT_TYPE, name)) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    return setting;
  }

  private ObjectAttachmentDetail getImage(String name) {
    ObjectAttachmentList attachments = attachmentService.getAttachments(ImageAttachmentPlugin.OBJECT_TYPE, name);
    if (attachments == null || CollectionUtils.isEmpty(attachments.getAttachments())) {
      return null;
    }
    return attachments.getAttachments().getFirst();
  }

  private String getImageUrl(String name, ObjectAttachmentDetail image) {
    return String.format(IMAGE_URL_FORMAT, ImageAttachmentPlugin.OBJECT_TYPE, name, image.getId());
  }

  /**
   * Imports the image a copied window carries in its {@code data.init}
   * preference into the setting created for it, as the portlet's first
   * render imports that preference
   */
  private void importInitialImage(String name, String imageContent, long identityId) {
    File tempFile = null;
    try {
      tempFile = File.createTempFile("image", ".png");
      Files.write(tempFile.toPath(), Base64.decodeBase64(imageContent));
      UploadResource uploadResource = new UploadResource("ImageSettings" + UUID.randomUUID());
      uploadResource.setFileName(tempFile.getName());
      uploadResource.setMimeType("image/png");
      uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
      uploadResource.setStoreLocation(tempFile.getAbsolutePath());
      attachmentService.saveAttachment(new UploadedAttachmentDetail(uploadResource),
                                       ImageAttachmentPlugin.OBJECT_TYPE,
                                       name,
                                       null,
                                       identityId);
    } catch (IOException | ObjectAlreadyExistsException | ObjectNotFoundException e) {
      throw new IllegalStateException(LayoutImageService.IMAGE_NOT_SAVED, e);
    } finally {
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile.toPath());
        } catch (IOException e) {
          LOG.warn("Temporary file {} of an imported image not deleted", tempFile, e);
        }
      }
    }
  }

  private String createSetting(ApplicationSettingsContext context, long identityId) {
    String name;
    do {
      name = SETTING_NAME_PREFIX + "-" + context.getApplicationId() + "-" + UUID.randomUUID();
    } while (cmsService.isSettingNameExists(ImageAttachmentPlugin.OBJECT_TYPE, name));
    try {
      cmsService.saveSettingName(ImageAttachmentPlugin.OBJECT_TYPE,
                                 name,
                                 context.getPageReference(),
                                 context.getSpaceId(),
                                 identityId);
    } catch (ObjectAlreadyExistsException e) {
      throw new IllegalStateException(LayoutImageService.IMAGE_NOT_SAVED, e);
    }
    return name;
  }

  private Identity getIdentity(ApplicationSettingsContext context) {
    return userAcl.getUserIdentity(context.getUsername());
  }

}
