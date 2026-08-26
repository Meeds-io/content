/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.content.utils.ContentUtils;
import io.meeds.social.activity.plugin.ActivityCategoryPlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.plugin.CategoryPlugin;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryPluginService;

import jakarta.annotation.PostConstruct;

/**
 * Registers the Content List app as an aggregate, tree-restriction-only
 * Category objectType ("content"), so that its categories filter only ever
 * lists categories actually linked to a still-existing News article or Note -
 * not the whole org-wide category tree, not a category whose only links are
 * metadata left behind by content that was since deleted, and not a category
 * that only happens to be linked to some unrelated Activity (e.g. a plain
 * status update, via the generic cross-app category-on-activity feature).
 * Category links on posted content are recorded under "news"/"notes" (or
 * "activity" once posted to a feed, see
 * {@link io.meeds.content.service.ContentService#resolveCategoryLinkedIds}),
 * so this plugin unions all three, resolving each Activity-typed link back to
 * the News/Note it actually represents (or discarding it, if it represents
 * neither) via {@link CategoryPluginService#getObject(CategoryObject)}.
 */
@Component
public class ContentCategoryPlugin implements CategoryPlugin {

  private static final Log      LOG                      = ExoLogger.getLogger(ContentCategoryPlugin.class);

  // Upper bound on how many linked objects of a candidate category are
  // inspected to find one that still genuinely exists.
  private static final int      LINKED_OBJECTS_FETCH_CAP = 20;

  public static final String    OBJECT_TYPE              = "content";

  @Autowired
  private PortalContainer       container;

  @Autowired
  private CategoryLinkService   categoryLinkService;

  @Autowired
  private CategoryPluginService categoryPluginService;

  @Autowired
  private NoteService           noteService;

  @Autowired
  private IdentityRegistry      identityRegistry;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(CategoryPluginService.class).addPlugin(this);
  }

  @Override
  public String getType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canAccess(String objectId, String username) {
    // A raw id alone can't be resolved back to a News article or a Note
    // (this aggregate type has no objects of its own): per-object access
    // isn't meaningful here, only getCategoryIds() (tree restriction) is.
    return false;
  }

  @Override
  public boolean canEdit(String objectId, String username) {
    return false;
  }

  @Override
  public List<Long> getCategoryIds(long spaceId, String username) {
    List<String> queryTypes = Arrays.asList(ContentUtils.CONTENT_TYPE_NEWS,
                                            ContentUtils.CONTENT_TYPE_NOTES,
                                            ActivityCategoryPlugin.OBJECT_TYPE);
    Set<Long> candidateIds = new HashSet<>();
    candidateIds.addAll(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NEWS));
    candidateIds.addAll(categoryLinkService.getLinkedIds(ContentUtils.CONTENT_TYPE_NOTES));
    candidateIds.addAll(categoryLinkService.getLinkedIds(ActivityCategoryPlugin.OBJECT_TYPE));

    Identity identity = resolveIdentity(username);
    Map<String, Boolean> visibilityByLinkedObject = new HashMap<>();
    List<Long> categoryIds = new ArrayList<>();
    for (Long categoryId : candidateIds) {
      List<CategoryObject> linkedObjects = categoryLinkService.getLinkedObjects(categoryId,
                                                                                queryTypes,
                                                                                0,
                                                                                LINKED_OBJECTS_FETCH_CAP);
      if (linkedObjects.stream()
                       .anyMatch(object -> visibilityByLinkedObject.computeIfAbsent(object.getType() + ":" + object.getId(),
                                                                                    key -> stillExistsAndVisible(object, identity)))) {
        categoryIds.add(categoryId);
      } else if (linkedObjects.size() >= LINKED_OBJECTS_FETCH_CAP) {
        LOG.debug("Category {} has at least {} linked objects, none of the first {} still exist/are visible - "
            + "the fetch cap may be hiding a genuinely-visible one further down",
                 categoryId,
                 LINKED_OBJECTS_FETCH_CAP,
                 LINKED_OBJECTS_FETCH_CAP);
      }
    }
    return categoryIds;
  }

  private Identity resolveIdentity(String username) {
    Identity identity = identityRegistry.getIdentity(username);
    if (identity != null) {
      return identity;
    }
    // IdentityRegistry is an in-memory, per-node cache: a cluster node that
    // didn't handle the login, or an evicted entry, can miss even for a
    // genuinely authenticated user - fall back to the request's own
    // identity (set once per request, unaffected by that cache) before
    // giving up.
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null ? null : conversationState.getIdentity();
  }

  private boolean stillExistsAndVisible(CategoryObject object, Identity identity) {
    if (identity == null) {
      // Neither IdentityRegistry nor ConversationState could resolve who's
      // asking (e.g. a background/system call) - NoteService dereferences
      // the identity unguarded, so bail out rather than risk an NPE here.
      LOG.debug("No identity available to resolve visibility of {}:{} - treating it as not visible",
               object.getType(),
               object.getId());
      return false;
    }
    try {
      // An Activity-typed link is generic (any app can tag an activity with a
      // category, e.g. a plain status update) - it only counts as "content"
      // once resolved to what it actually represents (news.getMetadataObject
      // returns a "news"/"notes" object for a posted article/note, or leaves
      // it as "activity" for anything unrelated to this app).
      CategoryObject resolved = StringUtils.equals(object.getType(), ActivityCategoryPlugin.OBJECT_TYPE)
          ? categoryPluginService.getObject(object) : object;
      if (StringUtils.equals(resolved.getType(), ContentUtils.CONTENT_TYPE_NEWS)
          || StringUtils.equals(resolved.getType(), ContentUtils.CONTENT_TYPE_NOTES)) {
        // A News article is itself a wiki Page, so this single ACL-checked
        // lookup both confirms existence and scopes the result to what the
        // requesting user can actually see, without paying for a News
        // article's full build (space/metadata/targets...) just to test
        // "does this still exist".
        return noteService.getNoteById(resolved.getId(), identity) != null;
      }
      return false;
    } catch (Exception e) {
      // A dangling/orphaned link (e.g. deleted content) or a link the user
      // isn't allowed to see must not surface its category - treat any
      // resolution failure as "no longer exists", but keep a trace so a
      // filter that looks empty for the wrong reason is diagnosable.
      LOG.debug("Unable to resolve visibility of {}:{} for user '{}'", object.getType(), object.getId(), identity.getUserId(), e);
      return false;
    }
  }

}
