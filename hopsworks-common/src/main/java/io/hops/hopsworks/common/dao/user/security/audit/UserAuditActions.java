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

package io.hops.hopsworks.common.dao.user.security.audit;

public enum UserAuditActions {

  // for user authentication
  LOGIN("LOGIN"),
  // for user authentication
  LOGOUT("LOGOUT"),

  UNAUTHORIZED("UNAUTHORIZED ACCESS"),
  // get all the logs
  ALL("ALL"),

  SUCCESS("SUCCESS"),

  FAILED("FAILED"),

  ABORTED("ABORTED");

  private final String value;

  private UserAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static UserAuditActions getLoginsAuditActions(String text) {
    if (text != null) {
      for (UserAuditActions b : UserAuditActions.values()) {
        if (text.equalsIgnoreCase(b.value)) {
          return b;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
