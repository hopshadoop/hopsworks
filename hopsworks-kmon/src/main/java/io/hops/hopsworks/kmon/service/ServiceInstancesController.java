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

package io.hops.hopsworks.kmon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.kmon.role.ServiceRoleMapper;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import io.hops.hopsworks.common.dao.role.RoleHostInfo;
import io.hops.hopsworks.kmon.struct.ServiceType;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.kmon.utils.FilterUtils;

@ManagedBean
@RequestScoped
public class ServiceInstancesController {

  @ManagedProperty("#{param.r}")
  private String role;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.s}")
  private String status;
  @EJB
  private RoleEJB roleEjb;
  private static final SelectItem[] statusOptions;
  private static final SelectItem[] healthOptions;
  private List<InstanceInfo> filteredInstances = new ArrayList<>();
  private static final Logger logger = Logger.getLogger(
          ServiceInstancesController.class.getName());
  private enum servicesWithMetrics {
    HDFS, YARN
  };
//   private CookieTools cookie = new CookieTools();

  static {
    statusOptions = FilterUtils.createFilterOptions(Status.values());
    healthOptions = FilterUtils.createFilterOptions(Health.values());
  }

  public ServiceInstancesController() {
    logger.info("ServiceInstancesController");
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public boolean isYarnService() {
    if (service != null) {
      try {
        if (ServiceType.valueOf(service).equals(ServiceType.YARN)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }

  public boolean isHDFSService() {
    if (service != null) {
      try {
        if (ServiceType.valueOf(service).equals(ServiceType.HDFS)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }
  
  public boolean getServiceWithMetrics() {
    if (service != null) {
      try {
        servicesWithMetrics.valueOf(service);
        return true;
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }
  
  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<InstanceInfo> getFilteredInstances() {
    return filteredInstances;
  }

  public void setFilteredInstances(List<InstanceInfo> filteredInstances) {
    this.filteredInstances = filteredInstances;
  }

  public SelectItem[] getStatusOptions() {
    return statusOptions;
  }

  public SelectItem[] getHealthOptions() {
    return healthOptions;
  }

  public SelectItem[] getRoleOptions() {
    try {
      return FilterUtils.createFilterOptions(ServiceRoleMapper.getRolesArray(
              ServiceType.valueOf(service)));
    } catch (Exception ex) {
      logger.log(Level.WARNING,
              "Service not found. Returning no option. Error message: {0}", ex.
              getMessage());
      return new SelectItem[]{};
    }
  }

  public List<InstanceInfo> getInstances() {
//      With prettyfaces, parameters (clusters, service, role) will not be null.
//      Without prettyfaces, parameters will be null when filter is changed, they
//      should be stored in cookie
    List<InstanceInfo> instances = new ArrayList<InstanceInfo>();
    List<RoleHostInfo> roleHostList = new ArrayList<RoleHostInfo>();
    if (cluster != null && role != null && service != null && status != null) {
      for (RoleHostInfo roleHostInfo : roleEjb.findRoleHost(cluster, service,
              role)) {
        if (roleHostInfo.getStatus() == Status.valueOf(status)) {
          roleHostList.add(roleHostInfo);
        }
      }
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);         
    } else if (cluster != null && service != null && role != null) {
      roleHostList = roleEjb.findRoleHost(cluster, service, role);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);    
    } else if (cluster != null && service != null) {
      roleHostList = roleEjb.findRoleHost(cluster, service);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);          
    } else if (cluster != null) {
      roleHostList = roleEjb.findRoleHost(cluster);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);             
    }
//      else {
//         roleHostList = roleEjb.findRoleHost(cookie.read("cluster"), cookie.read("service"));
//      }     
    for (RoleHostInfo r : roleHostList) {
      instances.add(new InstanceInfo(r.getRole().getCluster(), r.getRole().
              getService(), r.getRole().getRole(),
              r.getRole().getHost().getHostname(), r.getStatus(), r.getHealth().toString()));
    }
    filteredInstances.addAll(instances);
    return instances;
  }
  
  public boolean disableStart() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == Status.Stopped) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean disableStop() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == Status.Started) {
          return false;
        }
      }
    }
    return true;
  }
}
