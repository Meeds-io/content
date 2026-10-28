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

import static io.meeds.layout.service.ApplicationSettingsService.VALUE_NOT_ACCEPTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.exoplatform.services.resources.LocaleConfigService;

/**
 * Reads and checks the values of an application settings object as the
 * layout tools pass them: JSON values, every refusal being
 * {@code value is not accepted for <option>}
 */
public class ApplicationSettingsUtils {

  /**
   * The targets the drawers' link check, social's Autolinker, turns into a
   * link: an http(s) URL with a host, a mailto address, a phone number, or a
   * path of the platform. A bare host is refused, Autolinker accepting it
   * only when its top-level domain is one it knows.
   */
  private static final Pattern URL_PATTERN       = Pattern.compile("^(?:https?://[^/?#]+.*"
      + "|mailto:[^@]+@[^@.]+(?:\\.[^@.]+)+"
      + "|tel:\\+?[0-9][0-9().-]{4,}"
      + "|\\.?/.*)$", Pattern.CASE_INSENSITIVE);

  private static final int         MAX_URL_LENGTH    = 2000;

  private ApplicationSettingsUtils() {
    // Utility class
  }

  public static IllegalArgumentException notAccepted(String option) {
    return new IllegalArgumentException(String.format(VALUE_NOT_ACCEPTED, option));
  }

  public static void requireKnownOptions(Map<String, Object> settings, Collection<String> options) {
    for (String option : settings.keySet()) {
      if (!options.contains(option)) {
        throw notAccepted(option);
      }
    }
  }

  public static boolean toBoolean(Object value, String option) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    throw notAccepted(option);
  }

  public static int toInt(Object value, String option, int min, int max) {
    if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())) {
      double doubleValue = number.doubleValue();
      if (doubleValue >= min && doubleValue <= max) {
        return (int) doubleValue;
      }
    }
    throw notAccepted(option);
  }

  public static String toText(Object value, String option, int maxLength) {
    if (value instanceof String text && text.length() <= maxLength) {
      return text;
    }
    throw notAccepted(option);
  }

  public static String toValue(Object value, String option, Collection<String> values) {
    if (value instanceof String text && values.contains(text)) {
      return text;
    }
    throw notAccepted(option);
  }

  public static <E extends Enum<E>> E toEnum(Object value, String option, Class<E> type) {
    if (value instanceof String text) {
      for (E constant : type.getEnumConstants()) {
        if (constant.name().equals(text)) {
          return constant;
        }
      }
    }
    throw notAccepted(option);
  }

  /**
   * @return the URL as given: an http(s) URL, a mailto or tel URL, or a path
   *         of the platform
   */
  public static String toUrl(Object value, String option) {
    String url = toText(value, option, MAX_URL_LENGTH);
    if (StringUtils.containsWhitespace(url) || !URL_PATTERN.matcher(url).matches()) {
      throw notAccepted(option);
    }
    return url;
  }

  /**
   * @return the ids of a list of numeric ids, in their order, each once
   */
  public static List<Long> toIds(Object value, String option) {
    if (!(value instanceof List<?> list)) {
      throw notAccepted(option);
    }
    List<Long> ids = new ArrayList<>();
    for (Object item : list) {
      long id = toId(item, option);
      if (ids.contains(id)) {
        throw notAccepted(option);
      }
      ids.add(id);
    }
    return ids;
  }

  public static long toId(Object value, String option) {
    if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())
        && number.longValue() > 0) {
      return number.longValue();
    } else if (value instanceof String text && NumberUtils.toLong(text) > 0) {
      return NumberUtils.toLong(text);
    }
    throw notAccepted(option);
  }

  /**
   * @param  value     a JSON object of language tag to text
   * @param  option    the option holding it
   * @param  maxLength the longest text accepted
   * @param  languages the language tags the platform offers
   * @return           the translations, an empty text dropped: an empty
   *                   text removes the translation of its language
   */
  public static Map<String, String> toTranslations(Object value,
                                                   String option,
                                                   int maxLength,
                                                   Collection<String> languages) {
    if (!(value instanceof Map<?, ?> map)) {
      throw notAccepted(option);
    }
    Map<String, String> translations = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String language) || !languages.contains(language)) {
        throw notAccepted(option);
      }
      String text = toText(entry.getValue(), option, maxLength);
      if (StringUtils.isNotBlank(text)) {
        translations.put(language, text);
      }
    }
    return translations;
  }

  public static Set<String> getLanguages(LocaleConfigService localeConfigService) {
    return localeConfigService.getLocalConfigs()
                              .stream()
                              .map(config -> config.getLocale().toLanguageTag())
                              .collect(Collectors.toSet());
  }

  public static Map<String, Object> objectSchema(Map<String, Object> properties) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("additionalProperties", false);
    return schema;
  }

  public static Map<String, Object> booleanSchema(String description) {
    return schema("boolean", description);
  }

  public static Map<String, Object> integerSchema(int min, int max, String description) {
    Map<String, Object> schema = schema("integer", description);
    schema.put("minimum", min);
    schema.put("maximum", max);
    return schema;
  }

  public static Map<String, Object> stringSchema(String description) {
    return schema("string", description);
  }

  public static Map<String, Object> enumSchema(Collection<String> values, String description) {
    Map<String, Object> schema = schema("string", description);
    schema.put("enum", new ArrayList<>(values));
    return schema;
  }

  public static Map<String, Object> idsSchema(String description) {
    Map<String, Object> schema = schema("array", description);
    schema.put("items", schema("integer", null));
    return schema;
  }

  public static Map<String, Object> arraySchema(Map<String, Object> items, String description) {
    Map<String, Object> schema = schema("array", description);
    schema.put("items", items);
    return schema;
  }

  public static Map<String, Object> translationsSchema(int maxLength, String description) {
    Map<String, Object> schema = schema("object", description);
    Map<String, Object> text = schema("string", null);
    text.put("maxLength", maxLength);
    schema.put("additionalProperties", text);
    return schema;
  }

  /**
   * @return the positive ids of a stored comma separated list, in order, an
   *         unreadable item ignored
   */
  public static List<Long> parseIds(String value) {
    List<Long> ids = new ArrayList<>();
    for (String id : StringUtils.split(StringUtils.defaultString(value), ',')) {
      long parsed = NumberUtils.toLong(id.trim());
      if (parsed > 0) {
        ids.add(parsed);
      }
    }
    return ids;
  }

  /**
   * @return the stored number, the default when it is absent or unreadable
   */
  public static int parseInt(String value, int defaultValue) {
    return NumberUtils.toInt(StringUtils.trim(value), defaultValue);
  }

  public static Map<Locale, String> toLocaleLabels(Map<String, String> translations) {
    Map<Locale, String> labels = new LinkedHashMap<>();
    translations.forEach((language, text) -> labels.put(Locale.forLanguageTag(language), text));
    return labels;
  }

  public static Map<String, String> toLanguageLabels(Map<Locale, String> labels) {
    Map<String, String> translations = new LinkedHashMap<>();
    if (labels != null) {
      labels.forEach((locale, text) -> {
        if (StringUtils.isNotBlank(text)) {
          translations.put(locale.toLanguageTag(), text);
        }
      });
    }
    return translations;
  }

  private static Map<String, Object> schema(String type, String description) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", type);
    if (description != null) {
      schema.put("description", description);
    }
    return schema;
  }

}
