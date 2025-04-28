/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

package io.meeds.content.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.meeds.content.entity.LinkEntity;

public interface LinkDAO extends JpaRepository<LinkEntity, Long> {

  @Query("""
        SELECT l from SocLink l
        INNER JOIN l.setting s
        ON s.name = ?1
        ORDER BY l.order ASC
      """)
  public List<LinkEntity> getLinks(String name);

  @Query("""
        SELECT l.setting.id
        FROM SocLink l
        WHERE l.id = ?1
      """)
  public long getLinkSettingByLinkId(long linkId);

}
