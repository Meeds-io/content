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
package io.meeds.content.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The cases are what social's Autolinker 3.14.1 does with each value: the
 * accepted ones it links whole, the refused ones it links shortened or not
 * at all
 */
class ApplicationSettingsUtilsTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "https://meeds.io/a?b=c#d",
      "http://localhost:8080/portal",
      "https://meeds.io:443",
      "https://a-b.c-d.io/x",
      "https://1.2.3.4/x",
      "https://intranet",
      "https://meeds.io/x/",
      "https://meeds.io/a%20b",
      "https://meeds.io/(x)",
      "https://meeds.io/a(b)",
      "https://meeds.io/a[b]",
      "https://meeds.io/a?b=c&d=e#f",
      "https://meeds.io/?a..b",
      "https://a.io/~u",
      "https://xn--bcher-kva.example/x",
      "/portal/dw",
      "/",
      "./news",
  })
  void testToUrlAcceptsWhatAutolinkerLinksWhole(String url) {
    assertEquals(url, ApplicationSettingsUtils.toUrl(url, "url"));
  }

  /**
   * Targets as long as the option allows, a long path and a host of many
   * labels, are accepted whole
   */
  @Test
  void testToUrlAcceptsTargetsAsLongAsAllowed() {
    String path = "https://meeds.io/" + "a/".repeat(990) + "x";
    assertEquals(path, ApplicationSettingsUtils.toUrl(path, "url"));
    String host = "https://" + "a.".repeat(995) + "io";
    assertEquals(host, ApplicationSettingsUtils.toUrl(host, "url"));
    String relative = "/" + "(a)".repeat(666);
    assertEquals(relative, ApplicationSettingsUtils.toUrl(relative, "url"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "https://my_server.corp/x",
      "https://intranet.corp_lan/x",
      "https://a!x",
      "https://-x",
      "https://meeds.io/x\"y",
      "https://meeds.io/x<y",
      "https://meeds.io/x>y",
      "https://meeds.io/x\\y",
      "https://meeds.io/x`y",
      "https://meeds.io/x?a=\"b",
      "https://meeds.io/x.",
      "https://meeds.io/x,",
      "https://meeds.io/x)",
      "https://meeds.io/x?",
      "https://meeds.io/x!",
      "https://meeds.io/x:",
      "https://meeds.io/x;",
      "https://meeds.io/x]",
      "https://meeds.io/x}",
      "https://meeds.io/x^",
      "https://meeds.io/x\u2028y",
      "https://meeds.io/x\u3000y",
      "https://meeds.io/x\u2003y",
      "https://meeds.io/x\u00a0y",
      "https://meeds.io/caf\u00e9",
      "https://a..b/x",
      "https://meeds.io./x",
      "https://meeds.io-",
      "./news.",
      "https://meeds.io?x=1",
      "https://meeds.io#x",
      "https://meeds.io?q=a..b",
      "https://meeds.io/a b",
      "/a<b",
      "meeds.io",
      "intranet",
      "mailto:team@meeds.io",
      "tel:+33612345678",
      "ftp://meeds.io",
      "javascript:alert(1)",
      "",
  })
  void testToUrlRefusesWhatAutolinkerShortensOrSkips(String url) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ApplicationSettingsUtils.toUrl(url, "url"));
    assertEquals("value is not accepted for url", e.getMessage());
  }

}
