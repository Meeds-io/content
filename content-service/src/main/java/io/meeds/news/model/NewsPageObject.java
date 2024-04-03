package io.meeds.news.model;

import org.exoplatform.social.metadata.model.MetadataObject;

public class NewsPageObject extends MetadataObject {

  public NewsPageObject() {
  }

  public NewsPageObject(String objectType, String objectId, String parentObjectId) {
    super(objectType, objectId, parentObjectId);
  }
}
