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
package io.meeds.content.elasticsearch;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides the Elasticsearch the {@code *IT} classes execute the module's
 * hand-written queries against: how the query_string parser reads a searched
 * text, and how the index analyzers split it, are exactly what a mocked
 * {@code ElasticSearchingClient} cannot check ({@code backend-spring.md} §7).
 * <p>
 * Off in a plain build: the {@code *IT} classes run under the parent's
 * {@code run-its} profile only ({@code mvn verify -Prun-its}). One container
 * is started for the whole fork and left to Ryuk to reap. Setting
 * {@code -Des.url} points the suite at an already-running engine instead.
 * <b>The suite uses its own index names</b> ({@code it_news*}), so an engine
 * holding a platform's {@code news_*} indices can be used safely; it never
 * touches them.
 * <p>
 * Same shape as the {@code ai-rag} and {@code analytics} harnesses, on the
 * Testcontainers version the Spring Boot BOM manages.
 */
public abstract class AbstractElasticsearchIT {

  private static final String           IMAGE = System.getProperty("es.testcontainer.image",
                                                                   "docker.elastic.co/elasticsearch/elasticsearch:9.3.4");

  private static ElasticsearchContainer container;

  private static String                 url;

  private static RuntimeException       unavailable;

  protected static synchronized String elasticsearchUrl() {
    if (unavailable != null) {
      // Memoised on purpose: a machine with no Docker pays Testcontainers'
      // discovery timeout once per build rather than once per test method.
      throw unavailable;
    }
    if (url == null) {
      String configured = System.getProperty("es.url");
      if (StringUtils.isNotBlank(configured)) {
        url = configured;
      } else {
        container = new ElasticsearchContainer(DockerImageName.parse(IMAGE))
                                                                            // No credentials, like the engine the
                                                                            // platform runs beside it.
                                                                            .withEnv("xpack.security.enabled", "false")
                                                                            .withEnv("discovery.type", "single-node")
                                                                            // The clause limit is max(1024, heap MB
                                                                            // x 64 / search threads): this heap and
                                                                            // 40 search threads hold it at 1024, the
                                                                            // engine's floor, whatever the machine.
                                                                            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                                                                            .withEnv("thread_pool.search.size", "40")
                                                                            .withStartupTimeout(Duration.ofMinutes(5));
        try {
          container.start();
        } catch (RuntimeException e) {
          IllegalStateException failure =
                                        new IllegalStateException("Could not start " + IMAGE
                                            + ". These tests execute hand-written Elasticsearch queries and cannot"
                                            + " be faked; install Docker, or point the build at a running engine with"
                                            + " -Des.url.", e);
          if (!DockerClientFactory.instance().isDockerAvailable()) {
            // Only the deterministic failure is memoised; a start that failed
            // with Docker present is usually transient and worth retrying.
            unavailable = failure;
          }
          container = null;
          throw failure;
        }
        url = "http://" + container.getHttpHostAddress();
      }
    }
    return url;
  }

}
