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
package io.meeds.content.news.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.meeds.content.news.model.ArticleTarget;
import io.meeds.content.news.model.News;
import io.meeds.content.news.service.NewsService;

/**
 * Pins which space {@link ArticleSpaceScopeResolver} tells for an article, and
 * every case it fails closed on.
 */
@ExtendWith(MockitoExtension.class)
class ArticleSpaceScopeResolverTest {

  private static final String       ARTICLE_ID = "12";

  private static final String       SPACE_ID   = "33";

  @Mock
  private NewsService               newsService;

  @InjectMocks
  private ArticleSpaceScopeResolver resolver;

  /**
   * The resolver answers to the name the definitions give.
   * Mutant: another name.
   */
  @Test
  void nameIsArticle() {
    assertEquals("article", resolver.getName());
    assertEquals(ArticleSpaceScopeResolver.NAME, resolver.getName());
  }

  /**
   * A posted article of a space, neither published to a target nor shared,
   * belongs to that space, read without any user.
   */
  @Test
  void postedArticleOfASpaceBelongsToIt() {
    News article = article();
    when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(article);

    assertEquals(Long.valueOf(33L), resolver.spaceOf(ARTICLE_ID));
  }

  /**
   * An id that isn't a decimal number is never looked up.
   * Mutant: the numeric guard dropped.
   */
  @Test
  void nonNumericIdBelongsToNoSpace() {
    assertNull(resolver.spaceOf(null));
    assertNull(resolver.spaceOf(""));
    assertNull(resolver.spaceOf("12a"));
    assertNull(resolver.spaceOf("-12"));
    assertNull(resolver.spaceOf("0123456789abcdef0123456789abcdef"));
    verify(newsService, never()).getNewsArticleById(anyString());
  }

  /**
   * A missing article belongs to no space.
   * Mutant: the null guard dropped.
   */
  @Test
  void missingArticleBelongsToNoSpace() {
    when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(null);

    assertNull(resolver.spaceOf(ARTICLE_ID));
  }

  /**
   * A lookup that fails belongs to no space, and the failure doesn't escape.
   * Mutant: the catch narrowed or removed.
   */
  @Test
  void failingLookupBelongsToNoSpace() {
    when(newsService.getNewsArticleById(ARTICLE_ID)).thenThrow(new IllegalStateException("broken"));

    assertNull(resolver.spaceOf(ARTICLE_ID));
  }

  /**
   * A draft article, and a scheduled one, which only their author and the
   * space publishers see, belong to no space, nor does one with no state.
   * Mutant: the posted check dropped or inverted.
   */
  @Test
  void unpostedArticleBelongsToNoSpace() {
    for (String state : new String[] { NewsService.DRAFT, NewsService.STAGED, null }) {
      News article = article();
      when(article.getPublicationState()).thenReturn(state);
      when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(article);

      assertNull(resolver.spaceOf(ARTICLE_ID), state);
    }
  }

  /**
   * A deleted article belongs to no space.
   * Mutant: the deleted check dropped.
   */
  @Test
  void deletedArticleBelongsToNoSpace() {
    News article = article();
    when(article.isDeleted()).thenReturn(true);
    when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(article);

    assertNull(resolver.spaceOf(ARTICLE_ID));
  }

  /**
   * An article published to the platform, whether its flag or its targets
   * say so, belongs to no space.
   * Mutants: the published check or the targets check dropped.
   */
  @Test
  void publishedArticleBelongsToNoSpace() {
    News published = article();
    when(published.isPublished()).thenReturn(true);
    News targeted = article();
    when(targeted.getTargets()).thenReturn(List.of(mock(ArticleTarget.class)));
    when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(published, targeted);

    assertNull(resolver.spaceOf(ARTICLE_ID));
    assertNull(resolver.spaceOf(ARTICLE_ID));
  }

  /**
   * An article shared into another space belongs to no space.
   * Mutant: the shared check dropped.
   */
  @Test
  void sharedArticleBelongsToNoSpace() {
    News article = article();
    when(article.getSharedInSpacesList()).thenReturn(List.of("44"));
    when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(article);

    assertNull(resolver.spaceOf(ARTICLE_ID));
  }

  /**
   * An article outside any space (a personal or portal notebook) belongs to
   * no space, nor does one whose space id isn't a number.
   * Mutant: the space id check dropped.
   */
  @Test
  void articleOutsideASpaceBelongsToNoSpace() {
    for (String spaceId : new String[] { null, "", "x" }) {
      News article = article();
      when(article.getSpaceId()).thenReturn(spaceId);
      when(newsService.getNewsArticleById(ARTICLE_ID)).thenReturn(article);

      assertNull(resolver.spaceOf(ARTICLE_ID), spaceId);
    }
  }

  /**
   * @return a posted article of space {@value #SPACE_ID}, not deleted,
   *         published, targeted nor shared
   */
  private News article() {
    News article = mock(News.class);
    lenient().when(article.getId()).thenReturn(ARTICLE_ID);
    lenient().when(article.getSpaceId()).thenReturn(SPACE_ID);
    lenient().when(article.getPublicationState()).thenReturn(NewsService.POSTED);
    lenient().when(article.isDeleted()).thenReturn(false);
    lenient().when(article.isPublished()).thenReturn(false);
    lenient().when(article.getTargets()).thenReturn(List.of());
    lenient().when(article.getSharedInSpacesList()).thenReturn(null);
    return article;
  }

}
