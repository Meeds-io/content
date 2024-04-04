package io.meeds.news.model;

import org.exoplatform.social.metadata.model.MetadataObject;

public class NewsPageVersionObject extends MetadataObject {

  public NewsPageVersionObject() {
  }

  public NewsPageVersionObject(String objectType, String objectId, String parentObjectId) {
    super(objectType, objectId, parentObjectId);
  }
}
