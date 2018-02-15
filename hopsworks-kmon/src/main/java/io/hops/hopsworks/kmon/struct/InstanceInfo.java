/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import java.io.Serializable;

public class InstanceInfo implements Serializable {

  private String name;
  private String host;
  private String cluster;
  private String service;
  private String group;
  private Status status;
  private String health;

  public InstanceInfo(String cluster, String group, String service, String host,
          Status status, String health) {

    this.name = service + " @" + host;
    this.host = host;
    this.cluster = cluster;
    this.group = group;
    this.service = service;
    this.status = status;
    this.health = health;
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public Status getStatus() {
    return status;
  }

  public String getHealth() {
    return health;
  }

  public String getService() {
    return service;
  }

  public String getGroup() {
    return group;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

}
