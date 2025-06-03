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
package io.meeds.content.news.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class CleanDuplicatedEsIndexesUpgradeTest {

  private CleanDuplicatedEsIndexesUpgrade cleanDuplicatedEsIndexesUpgrade;

  @Mock
  private ElasticSearchingClient          elasticSearchingClient;

  @Mock
  private IndexingService                 indexingService;

  @Mock
  private NewsService                     newsService;

  @Mock
  private SettingService                  settingService;

  InitParams                              initParams = new InitParams();

  @Before
  public void setUp() {
    ValueParam productGroupIdValueParam = new ValueParam();
    productGroupIdValueParam.setName("product.group.id");
    productGroupIdValueParam.setValue("org.exoplatform.platform");
    initParams.addParameter(productGroupIdValueParam);
    this.cleanDuplicatedEsIndexesUpgrade = new CleanDuplicatedEsIndexesUpgrade(initParams,
                                                                               newsService,
                                                                               elasticSearchingClient,
                                                                               indexingService,
                                                                               settingService);
  }

  @Test
  public void testProcessUpgrade() throws Exception {

    String queryResponse = "{\n" + "  \"took\": 5,\n" + "  \"timed_out\": false,\n" + "  \"_shards\": {\n" + "    \"total\": 1,\n"
        + "    \"successful\": 1,\n" + "    \"skipped\": 0,\n" + "    \"failed\": 0\n" + "  },\n" + "  \"hits\": {\n"
        + "    \"total\": {\n" + "      \"value\": 5,\n" + "      \"relation\": \"eq\"\n" + "    },\n"
        + "    \"max_score\": 1.0,\n" + "    \"hits\": [\n" + "      {\n" + "        \"_index\": \"notes_v1\",\n"
        + "        \"_type\": \"_doc\",\n" + "        \"_id\": \"1\",\n" + "        \"_score\": 1.0,\n"
        + "        \"_source\": {\n" + "          \"title\": \"Title 1\"\n" + "        }\n" + "      },\n" + "      {\n"
        + "        \"_index\": \"notes_v1\",\n" + "        \"_type\": \"_doc\",\n" + "        \"_id\": \"1-en\",\n"
        + "        \"_score\": 1.0,\n" + "        \"_source\": {\n" + "          \"title\": \"Title 1 EN\"\n" + "        }\n"
        + "      },\n" + "      {\n" + "        \"_index\": \"notes_v1\",\n" + "        \"_type\": \"_doc\",\n"
        + "        \"_id\": \"4\",\n" + "        \"_score\": 1.0,\n" + "        \"_source\": {\n"
        + "          \"title\": \"Title 4\"\n" + "        }\n" + "      }\n" + "      {\n" + "        \"_index\": \"notes_v1\",\n"
        + "        \"_type\": \"_doc\",\n" + "        \"_id\": \"4-fr\",\n" + "        \"_score\": 1.0,\n"
        + "        \"_source\": {\n" + "          \"title\": \"Title 4 fr\"\n" + "        }\n" + "      }\n" + "      {\n"
        + "        \"_index\": \"notes_v1\",\n" + "        \"_type\": \"_doc\",\n" + "        \"_id\": \"5\",\n"
        + "        \"_score\": 1.0,\n" + "        \"_source\": {\n" + "          \"title\": \"Title 5\"\n" + "        }\n"
        + "      }\n" + "    ]\n" + "  }\n" + "}";

    when(elasticSearchingClient.sendRequest(anyString(), anyString())).thenReturn(queryResponse);
    News article1 = mock(News.class);
    News article2 = mock(News.class);
    when(newsService.buildArticle(anyString())).thenReturn(article1)
                                               .thenReturn(article1)
                                               .thenReturn(article2)
                                               .thenReturn(article2)
                                               .thenReturn(null);
    when(article1.isReferred()).thenReturn(true);
    when(article2.isReferred()).thenReturn(false);
    when(article2.isFromExternalPage()).thenReturn(false);

    cleanDuplicatedEsIndexesUpgrade.processUpgrade("oldVersion", "newVersion");

    // Verify the result
    assertEquals(2, cleanDuplicatedEsIndexesUpgrade.getCleanedDuplicatedNewsEsIndexesCount());

    verify(indexingService, times(2)).unindex(anyString(), anyString());

  }
}
