/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.content;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.cache.CacheService;

import io.meeds.content.dao.LinkDAO;
import io.meeds.content.dao.LinkSettingDAO;
import io.meeds.content.service.LinkService;
import io.meeds.content.storage.cache.CachedLinkStorage;
import io.meeds.kernel.test.AbstractSpringTest;
import io.meeds.spring.AvailableIntegration;

@SpringBootApplication(scanBasePackages = {
  "io.meeds.content",
  AvailableIntegration.KERNEL_TEST_MODULE,
  AvailableIntegration.JPA_MODULE,
  AvailableIntegration.LIQUIBASE_MODULE,
  AvailableIntegration.WEB_MODULE,
})
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application-common.properties")
@PropertySource("classpath:content.properties")
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/exo.social.component.core-local-root-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.social.component.core-local-configuration.xml"),
})
@RunWith(SpringRunner.class)
public abstract class AbstractSpringConfigurationTest extends AbstractSpringTest {

  @Autowired
  protected LinkService    linkService;

  @Autowired
  protected LinkDAO        linkDAO;

  @Autowired
  protected LinkSettingDAO linkSettingDAO;

  @Before
  public void setUp() {
    begin();
  }

  @After
  public void tearDown() {
    getContainer().getComponentInstanceOfType(CacheService.class).getCacheInstance(CachedLinkStorage.CACHE_NAME).clearCache();
    restartTransaction();
    linkDAO.deleteAll();
    restartTransaction();
    linkSettingDAO.deleteAll();
    end();
  }

}
