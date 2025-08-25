/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.content.link.rest.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.meeds.content.link.constant.LinkAlignType;
import io.meeds.content.link.constant.LinkDisplayType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LinkSettingRestEntity {

  private long                 id;

  private String               name;

  private Map<String, String>  header;

  private LinkDisplayType      type;

  @JsonProperty("vAlign")
  private LinkAlignType        vAlign;

  @JsonProperty("hAlign")
  private LinkAlignType        hAlign;

  private boolean              largeIcon;

  private int                  iconSize;

  private boolean              showName;

  private boolean              showIcon;

  private boolean              showDescription;

  private String               seeMore;

  private List<LinkRestEntity> links;

}
