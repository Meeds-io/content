/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.news.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

@RunWith(MockitoJUnitRunner.class)
public class NewsUtilsTest {

  private static final MockedStatic<CommonsUtils> COMMONS_UTILS = mockStatic(CommonsUtils.class);

  @Mock
  private SpaceService                            spaceService;

  @Mock
  private Space                                   space;

  @Mock
  private Identity                                userAclIdentity;

  @Mock
  private Identity                                adminAclIdentity;

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
  }

  @Test
  public void testCanPublishNews() {
    when(userAclIdentity.getUserId()).thenReturn("user");
    when(space.getId()).thenReturn("2");

    assertFalse(NewsUtils.canPublishNews(null, null));
    COMMONS_UTILS.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);
    assertFalse(NewsUtils.canPublishNews(space.getId(), null));
    assertFalse(NewsUtils.canPublishNews(space.getId(), userAclIdentity));

    when(spaceService.getSpaceById(space.getId())).thenReturn(space);

    assertFalse(NewsUtils.canPublishNews(space.getId(), null));
    assertFalse(NewsUtils.canPublishNews(space.getId(), userAclIdentity));

    when(adminAclIdentity.isMemberOf(NewsUtils.PLATFORM_WEB_CONTRIBUTORS_GROUP, NewsUtils.PUBLISHER_MEMBERSHIP_NAME)).thenReturn(true);
    assertTrue(NewsUtils.canPublishNews(space.getId(), adminAclIdentity));
    assertFalse(NewsUtils.canPublishNews(space.getId(), userAclIdentity));

    lenient().when(spaceService.isMember(space, userAclIdentity.getUserId())).thenReturn(true);
    assertFalse(NewsUtils.canPublishNews(space.getId(), userAclIdentity));
    lenient().when(spaceService.isMember(space, userAclIdentity.getUserId())).thenReturn(false);

    when(spaceService.isPublisher(space, userAclIdentity.getUserId())).thenReturn(true);
    assertTrue(NewsUtils.canPublishNews(space.getId(), userAclIdentity));
    when(spaceService.isPublisher(space, userAclIdentity.getUserId())).thenReturn(false);

    when(spaceService.isManager(space, userAclIdentity.getUserId())).thenReturn(true);
    assertTrue(NewsUtils.canPublishNews(space.getId(), userAclIdentity));
    when(spaceService.isManager(space, userAclIdentity.getUserId())).thenReturn(false);

    when(spaceService.isSuperManager(userAclIdentity.getUserId())).thenReturn(true);
    assertTrue(NewsUtils.canPublishNews(space.getId(), userAclIdentity));
  }

}
