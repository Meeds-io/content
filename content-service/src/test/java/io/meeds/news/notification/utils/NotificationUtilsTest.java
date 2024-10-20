/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.news.notification.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.social.core.space.model.Space;

@RunWith(MockitoJUnitRunner.class)
public class NotificationUtilsTest {

  private static final MockedStatic<CommonsUtils>    COMMONS_UTILS    = mockStatic(CommonsUtils.class);

  private static final MockedStatic<PortalContainer> PORTAL_CONTAINER = mockStatic(PortalContainer.class);

  private static final MockedStatic<PropertyManager> PROPERTY_MANAGER = mockStatic(PropertyManager.class);

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
    PORTAL_CONTAINER.close();
    PROPERTY_MANAGER.close();
  }

  @Test
  public void shouldGetTheSpaceUrlWhenTheUserIsNotMember() {
    // Given
    Space space1 = new Space();
    space1.setId("3");
    space1.setDisplayName("space1");
    space1.setPrettyName("space1");
    space1.setGroupId("space1");
    PORTAL_CONTAINER.when(() -> PortalContainer.getCurrentPortalContainerName()).thenReturn("portal");
    PROPERTY_MANAGER.when(() -> PropertyManager.getProperty("gatein.email.domain.url")).thenReturn("http://localhost:8080");

    // When
    String activityUrl = NotificationUtils.getNotificationActivityLink(space1, "13", false);

    assertEquals("http://localhost:8080/portal/s/" + space1.getId(), activityUrl);
  }

  @Test
  public void shouldGetTheSpaceUrlWhenTheUserIsNotMemberAndAfterUpdatingSpaceName() {
    // Given
    Space space = new Space();
    space.setId("4");
    space.setDisplayName("Space1");
    space.setPrettyName(space.getDisplayName());
    space.setGroupId("space1");

    PORTAL_CONTAINER.when(() -> PortalContainer.getCurrentPortalContainerName()).thenReturn("portal");
    PROPERTY_MANAGER.when(() -> PropertyManager.getProperty("gatein.email.domain.url")).thenReturn("http://localhost:8080");

    // When
    String activityUrl = NotificationUtils.getNotificationActivityLink(space, "13", false);

    assertEquals("http://localhost:8080/portal/s/" + space.getId(), activityUrl);

    Space updatedSpace = space;
    updatedSpace.setDisplayName("Space One");
    updatedSpace.setPrettyName(updatedSpace.getDisplayName());

    activityUrl = NotificationUtils.getNotificationActivityLink(updatedSpace, "13", false);
    assertEquals("http://localhost:8080/portal/s/" + updatedSpace.getId(), activityUrl);
  }
}
