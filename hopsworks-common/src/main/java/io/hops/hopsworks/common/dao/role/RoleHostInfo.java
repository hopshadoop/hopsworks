/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.role;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;

public class RoleHostInfo {

  private Roles role;
  private Hosts host;

  public RoleHostInfo(Roles role, Hosts host) {
    this.role = role;
    this.host = host;
  }

  public Roles getRole() {
    return role;
  }

  public Hosts getHost() {
    return host;
  }

  public Health getHealth() {
    if (role.getHealth() == Health.Good) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public Status getStatus() {
    return role.getStatus();
  }
}
