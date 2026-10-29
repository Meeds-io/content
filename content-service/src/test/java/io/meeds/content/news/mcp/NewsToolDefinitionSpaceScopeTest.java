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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import io.meeds.mcp.server.constant.Origin;
import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.model.SpaceScopeDeclaration;
import io.meeds.mcp.server.model.SpaceScopeRefusedException;
import io.meeds.mcp.server.plugin.SpaceScopeResolver;
import io.meeds.mcp.server.service.McpToolSpaceScopeService;
import io.meeds.mcp.server.util.McpToolUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the {@code space_scope} declarations the content add-on ships, parsed
 * with the MCP server's own parser: which news tools a call limited to one
 * space may use, on which arguments, and which stay refused.
 */
class NewsToolDefinitionSpaceScopeTest {

  /** The reads declared by the article they reach. */
  private static final Set<String>         ARTICLE_READS  = Set.of("get_news_by_id");

  /** The writes declared by the article they change. */
  private static final Set<String>         ARTICLE_WRITES = Set.of("update_news",
                                                                   "delete_news",
                                                                   "set_news_illustration",
                                                                   "remove_news_illustration",
                                                                   "publish_news_in_activity_stream");

  /** The reads declared by the space they list. */
  private static final Set<String>         SPACE_READS    = Set.of("get_news_list", "search_news");

  /** The writes declared by the space they create in. */
  private static final Set<String>         SPACE_WRITES   = Set.of("create_news");

  /**
   * The tools left undeclared, so refused under a space scope: they reach the
   * platform's publication targets.
   */
  private static final Set<String>         REFUSED        = Set.of("list_news_targets",
                                                                   "publish_news",
                                                                   "schedule_news",
                                                                   "unpublish_news");

  private static final JsonMapper          MAPPER         = JsonMapper.builder().build();

  /**
   * Exactly nine news tools declare a space scope, each well formed for what
   * it is, a read or a write, every write keeps its approval, no declaration
   * carries a false-only argument, and every accepted argument is a property
   * of the tool's schema. A tool added, or one that stops declaring, fails
   * here.
   * Mutants: a declaration dropped or added, an approval dropped.
   */
  @Test
  void declaredToolsAreTheExpectedOnes() {
    Map<String, SimpleToolDefinition> tools = shippedTools();
    Map<String, SimpleToolDefinition> declared = tools.values()
                                                      .stream()
                                                      .filter(tool -> tool.getSpaceScope() != null)
                                                      .collect(Collectors.toMap(SimpleToolDefinition::getName,
                                                                                Function.identity()));

    assertEquals(union(ARTICLE_READS, ARTICLE_WRITES, SPACE_READS, SPACE_WRITES), declared.keySet());
    declared.forEach((name, tool) -> {
      boolean write = ARTICLE_WRITES.contains(name) || SPACE_WRITES.contains(name);
      SpaceScopeDeclaration scope = tool.getSpaceScope();
      assertNotNull(tool.getAnnotations(), name);
      assertEquals(!write, McpToolSpaceScopeService.isReadTool(tool), name);
      assertTrue(scope.isWellFormedFor(write), name);
      assertNull(scope.getIdFormat(), name);
      assertTrue(scope.getFalseOnlyArgs() == null || scope.getFalseOnlyArgs().isEmpty(), name);
      if (write) {
        assertTrue(tool.isRequireApproval(), name);
      }
      JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
      allowed(tool).forEach(argument -> assertTrue(properties.has(argument), name + " takes " + argument));
    });
    REFUSED.forEach(name -> {
      assertTrue(tools.containsKey(name), name);
      assertNull(tools.get(name).getSpaceScope(), name);
    });
    assertEquals(tools.keySet(), union(declared.keySet(), REFUSED));
  }

  /**
   * The tools that reach an article do it through {@code news_id} and the
   * {@value ArticleSpaceScopeResolver#NAME} resolver, with a decimal id,
   * the one news ids are.
   * Mutants: another argument, another resolver, or a hex32 id format.
   */
  @Test
  void articleToolsUseTheArticleResolver() {
    Map<String, SimpleToolDefinition> tools = shippedTools();

    union(ARTICLE_READS, ARTICLE_WRITES).forEach(name -> {
      SimpleToolDefinition tool = tools.get(name);
      SpaceScopeDeclaration scope = tool.getSpaceScope();
      assertTrue(scope.isObjectKind(), name);
      assertEquals("news_id", scope.getObjectArg(), name);
      assertEquals(ArticleSpaceScopeResolver.NAME, scope.getResolver(), name);
      assertFalse(scope.isHexId(), name);
      assertEquals("integer", MAPPER.readTree(tool.getInputSchema()).path("properties").path("news_id").path("type").stringValue(), name);
    });
    assertEquals(List.of("language"), allowed(tools.get("get_news_by_id")));
    assertEquals(List.of("title", "summary", "html_content", "language"), allowed(tools.get("update_news")));
    assertEquals(List.of(), allowed(tools.get("delete_news")));
    assertEquals(List.of("language"), allowed(tools.get("remove_news_illustration")));
    assertEquals(List.of(), allowed(tools.get("publish_news_in_activity_stream")));
  }

  /**
   * A cover is set in a limited call only from bytes the call carries: never
   * from a URL the server would fetch, nor from a file attached to another
   * object, which may be in another space.
   * Mutants: the URL or the attachment reference allowed.
   */
  @Test
  void coverIsSetFromTheCallBytesOnly() {
    SimpleToolDefinition tool = shippedTools().get("set_news_illustration");

    assertEquals(List.of("image_base64", "alt_text", "language"), allowed(tool));
    assertFalse(allowed(tool).contains("image_url"));
    assertFalse(allowed(tool).contains("attachment_object_type"));
    assertFalse(allowed(tool).contains("attachment_object_id"));
  }

  /**
   * The list and the search read the call's space when they name none, take
   * it only through {@code target_space_id}, and may carry their paging and,
   * for the search, its term, nothing else.
   * Mutants: {@code when_absent} dropped, the space argument renamed, or an
   * argument added.
   */
  @Test
  void listingsUseTheScopeSpaceWhenAbsent() {
    Map<String, SimpleToolDefinition> tools = shippedTools();

    SPACE_READS.forEach(name -> {
      SpaceScopeDeclaration scope = tools.get(name).getSpaceScope();
      assertTrue(scope.isSpaceKind(), name);
      assertEquals("target_space_id", scope.getSpaceArg(), name);
      assertTrue(scope.isUseScopeWhenAbsent(), name);
    });
    assertEquals(List.of("offset", "limit"), allowed(tools.get("get_news_list")));
    assertEquals(List.of("query", "offset", "limit"), allowed(tools.get("search_news")));
  }

  /**
   * An article is created only in the call's space, named by
   * {@code target_space_id}, which is never filled in for the write, with its
   * text only.
   * Mutants: {@code when_absent} added, or another argument allowed.
   */
  @Test
  void creationStaysInTheSpace() {
    SimpleToolDefinition tool = shippedTools().get("create_news");
    SpaceScopeDeclaration scope = tool.getSpaceScope();

    assertTrue(scope.isSpaceKind());
    assertEquals("target_space_id", scope.getSpaceArg());
    assertFalse(scope.isUseScopeWhenAbsent());
    assertEquals(List.of("title", "summary", "html_content"), allowed(tool));
  }

  /**
   * The shipped declarations, run through the MCP server's own space scope
   * check for a run answering in space 33: an article of that space is read,
   * one the resolver doesn't tell that space for is refused, a list naming no
   * space gets the run's space, and a publication tool is refused.
   * Mutants: the resolver name or the space argument changed, a refused tool
   * declared.
   */
  @Test
  void shippedDeclarationsKeepAMentionRunInItsSpace() {
    Map<String, SimpleToolDefinition> tools = shippedTools();
    SpaceScopeResolver resolver = mock(SpaceScopeResolver.class);
    when(resolver.getName()).thenReturn(ArticleSpaceScopeResolver.NAME);
    when(resolver.spaceOf("12")).thenReturn(33L);
    when(resolver.spaceOf("13")).thenReturn(null);
    @SuppressWarnings("unchecked")
    ObjectProvider<SpaceScopeResolver> resolvers = mock(ObjectProvider.class);
    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.of(resolver));
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(resolvers);
    ActingIdentity mention = mock(ActingIdentity.class);
    when(mention.spaceScope()).thenReturn(33L);
    when(mention.origin()).thenReturn(Origin.MENTION);

    String read = "{\"news_id\":12,\"language\":\"fr\"}";
    assertEquals(read, service.enforce("get_news_by_id", tools.get("get_news_by_id"), read, mention));
    assertRefused(() -> service.enforce("get_news_by_id", tools.get("get_news_by_id"), "{\"news_id\":13}", mention));
    assertEquals(33L,
                 MAPPER.readTree(service.enforce("get_news_list", tools.get("get_news_list"), "{\"limit\":5}", mention))
                       .path("target_space_id")
                       .longValue());
    assertRefused(() -> service.enforce("search_news",
                                        tools.get("search_news"),
                                        "{\"query\":\"x\",\"target_space_id\":44}",
                                        mention));
    REFUSED.forEach(name -> assertRefused(() -> service.enforce(name, tools.get(name), "{\"news_id\":12}", mention)));
  }

  /**
   * The shipped cover declaration, run through the MCP server's own space
   * scope check for an approved proposal of a run answering in space 33: a
   * cover sent as bytes on an article of that space passes unchanged, a cover the server would fetch
   * from a URL, or take from a file attached to another object, is refused,
   * and so is a cover on an article the resolver doesn't tell that space for.
   * Mutants: {@code image_url} or the attachment reference allowed, the
   * resolver dropped.
   */
  @Test
  void aSpaceScopedCoverIsSentAsBytesNeverFetched() {
    SimpleToolDefinition tool = shippedTools().get("set_news_illustration");
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(articleResolverTelling(12L, 33L));
    ActingIdentity proposal = limitedTo(33L, Origin.PROPOSAL);

    String bytes = "{\"news_id\":12,\"image_base64\":\"iVBORw0KGgo=\",\"alt_text\":\"a cover\",\"language\":\"fr\"}";
    assertEquals(bytes, service.enforce("set_news_illustration", tool, bytes, proposal));
    assertRefused(() -> service.enforce("set_news_illustration",
                                        tool,
                                        "{\"news_id\":12,\"image_url\":\"http://169.254.169.254/latest/meta-data\"}",
                                        proposal));
    assertRefused(() -> service.enforce("set_news_illustration",
                                        tool,
                                        "{\"news_id\":12,\"attachment_object_type\":\"activity\",\"attachment_object_id\":\"5\"}",
                                        proposal));
    assertRefused(() -> service.enforce("set_news_illustration",
                                        tool,
                                        "{\"news_id\":12,\"image_base64\":\"iVBORw0KGgo=\",\"image_url\":\"http://example.org/a.png\"}",
                                        proposal));
    assertRefused(() -> service.enforce("set_news_illustration",
                                        tool,
                                        "{\"news_id\":13,\"image_base64\":\"iVBORw0KGgo=\"}",
                                        proposal));
  }

  /**
   * The shipped declaration of the activity stream post, run through the MCP
   * server's own space scope check for an approved proposal of a run answering
   * in space 33: the tool takes the article only, so it posts to the space of
   * the article, which the resolver tells is the run's space. An article of
   * another space or of no space is refused, and so is every argument that
   * could name a stream, a space or an audience. A mention (no approval) is
   * refused as for any write.
   * Mutants: the declaration dropped, the resolver changed, or an argument
   * allowed.
   */
  @Test
  void anArticleIsPostedOnlyToTheStreamOfItsOwnSpace() {
    SimpleToolDefinition tool = shippedTools().get("publish_news_in_activity_stream");
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(articleResolverTelling(12L, 33L));
    ActingIdentity proposal = limitedTo(33L, Origin.PROPOSAL);
    String name = "publish_news_in_activity_stream";

    assertTrue(tool.isRequireApproval());
    assertEquals("{\"news_id\":12}", service.enforce(name, tool, "{\"news_id\":12}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{\"news_id\":13}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{\"news_id\":12,\"space_id\":44}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{\"news_id\":12,\"target_space_id\":44}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{\"news_id\":12,\"targets\":[\"slider\"]}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{\"news_id\":12,\"audience\":\"all\"}", proposal));
    assertRefused(() -> service.enforce(name, tool, "{\"news_id\":12}", limitedTo(33L, Origin.MENTION)));
  }

  /**
   * @param articleId the one article the resolver knows
   * @param spaceId the space it tells for that article
   * @return the resolvers, holding an {@value ArticleSpaceScopeResolver#NAME}
   *         resolver telling no space for any other article
   */
  private static ObjectProvider<SpaceScopeResolver> articleResolverTelling(long articleId, long spaceId) {
    SpaceScopeResolver resolver = mock(SpaceScopeResolver.class);
    when(resolver.getName()).thenReturn(ArticleSpaceScopeResolver.NAME);
    when(resolver.spaceOf(String.valueOf(articleId))).thenReturn(spaceId);
    @SuppressWarnings("unchecked")
    ObjectProvider<SpaceScopeResolver> resolvers = mock(ObjectProvider.class);
    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.of(resolver));
    return resolvers;
  }

  /**
   * @param spaceId the space of the run
   * @param origin what the run answers: a mention, or the approved proposal
   *          a write runs from
   * @return the identity of a run limited to that space
   */
  private static ActingIdentity limitedTo(long spaceId, Origin origin) {
    ActingIdentity identity = mock(ActingIdentity.class);
    when(identity.spaceScope()).thenReturn(spaceId);
    when(identity.origin()).thenReturn(origin);
    return identity;
  }

  /**
   * @param call a call expected to be refused under the space scope
   */
  private static void assertRefused(Runnable call) {
    try {
      call.run();
    } catch (SpaceScopeRefusedException e) {
      return;
    }
    throw new AssertionError("The call wasn't refused");
  }

  /**
   * @param tool a tool declaring a space scope
   * @return its allowed arguments, empty when it names none
   */
  private static List<String> allowed(SimpleToolDefinition tool) {
    List<String> allowed = tool.getSpaceScope().getAllowedArgs();
    return allowed == null ? List.of() : allowed;
  }

  /**
   * @param sets the sets to join
   * @return every element of the sets
   */
  @SafeVarargs
  private static Set<String> union(Set<String>... sets) {
    return Arrays.stream(sets).flatMap(Set::stream).collect(Collectors.toSet());
  }

  /**
   * @return the shipped news tools, by name, as the MCP server parses them
   */
  private static Map<String, SimpleToolDefinition> shippedTools() {
    try {
      URL classes = NewsMcpTool.class.getProtectionDomain().getCodeSource().getLocation();
      URL url = new File(new File(classes.toURI()), "ai-tool-definitions.json").toURI().toURL();
      return McpToolUtils.parseToolDefinitions(url)
                         .stream()
                         .collect(Collectors.toMap(SimpleToolDefinition::getName, Function.identity()));
    } catch (Exception e) {
      throw new IllegalStateException("The news tool definitions couldn't be read", e);
    }
  }

}
