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

package io.hops.hopsworks.kmon.role;

import io.hops.hopsworks.kmon.struct.RoleType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import io.hops.hopsworks.kmon.struct.ServiceType;

public class ServiceRoleMapper {

  public static final Map<ServiceType, List<RoleType>> serviceRoleMap;
  public static final Map<RoleType, String> roleFullNames;

  static {
    serviceRoleMap = new EnumMap<ServiceType, List<RoleType>>(ServiceType.class);
    roleFullNames = new EnumMap<RoleType, String>(RoleType.class);

    serviceRoleMap.put(ServiceType.HDFS, Arrays.asList(RoleType.namenode,
            RoleType.datanode));
    serviceRoleMap.put(ServiceType.NDB, Arrays.asList(RoleType.ndbmtd,
            RoleType.mysqld, RoleType.ndb_mgmd));
    serviceRoleMap.put(ServiceType.YARN, Arrays.asList(RoleType.resourcemanager,
            RoleType.nodemanager));
    serviceRoleMap.put(ServiceType.MAP_REDUCE, Arrays.asList(RoleType.historyserver));
    serviceRoleMap.put(ServiceType.zookeeper, Arrays.asList(RoleType.zookeeper));
    serviceRoleMap.put(ServiceType.influxdb, Arrays.asList(RoleType.influxdb));
    serviceRoleMap.put(ServiceType.epipe, Arrays.asList(RoleType.epipe));
    serviceRoleMap.put(ServiceType.logstash, Arrays.asList(RoleType.logstash));
    serviceRoleMap.put(ServiceType.livy, Arrays.asList(RoleType.livy));
    serviceRoleMap.put(ServiceType.historyserver, Arrays.asList(RoleType.historyserver));
    serviceRoleMap.put(ServiceType.sparkhistoryserver, Arrays.asList(RoleType.sparkhistoryserver));
    serviceRoleMap.put(ServiceType.telegraf, Arrays.asList(RoleType.telegraf));
    serviceRoleMap.put(ServiceType.elasticsearch, Arrays.asList(RoleType.elasticsearch));
    serviceRoleMap.put(ServiceType.grafana, Arrays.asList(RoleType.grafana));
    serviceRoleMap.put(ServiceType.kafka, Arrays.asList(RoleType.kafka));
    serviceRoleMap.put(ServiceType.kibana, Arrays.asList(RoleType.kibana));
    serviceRoleMap.put(ServiceType.hive, Arrays.asList(RoleType.hiveserver2));
    serviceRoleMap.put(ServiceType.hive, Arrays.asList(RoleType.hivemetastore));
    serviceRoleMap.put(ServiceType.hive, Arrays.asList(RoleType.hivecleaner));
    serviceRoleMap.put(ServiceType.dela, Arrays.asList(RoleType.dela));
    
    roleFullNames.put(RoleType.namenode, "NameNode");
    roleFullNames.put(RoleType.datanode, "DataNode");
    roleFullNames.put(RoleType.ndbmtd, "MySQL Cluster NDB");
    roleFullNames.put(RoleType.mysqld, "MySQL Server");
    roleFullNames.put(RoleType.ndb_mgmd, "MGM Server");
    roleFullNames.put(RoleType.resourcemanager, "Resource Manager");
    roleFullNames.put(RoleType.nodemanager, "Node Manager");
    roleFullNames.put(RoleType.zookeeper, "Zookeeper");
    roleFullNames.put(RoleType.influxdb, "Influxdb");
    roleFullNames.put(RoleType.epipe, "Epipe");
    roleFullNames.put(RoleType.logstash, "Logstash");
    roleFullNames.put(RoleType.livy, "Livy");
    roleFullNames.put(RoleType.historyserver, "MapRed History Server");
    roleFullNames.put(RoleType.sparkhistoryserver, "Spark History Server");
    roleFullNames.put(RoleType.telegraf, "Telegraf");
    roleFullNames.put(RoleType.elasticsearch, "Elasticsearch");
    roleFullNames.put(RoleType.grafana, "Grafana");
    roleFullNames.put(RoleType.kafka, "Kafka");
    roleFullNames.put(RoleType.kibana, "Kibana");
    roleFullNames.put(RoleType.hiveserver2, "HiveServer2");
    roleFullNames.put(RoleType.hivemetastore, "HiveMetastore");
    roleFullNames.put(RoleType.hivecleaner, "HiveCleaner");
    roleFullNames.put(RoleType.dela, "Dela");
  }

  public static List<RoleType> getRoles(ServiceType serviceType) {
    return serviceRoleMap.get(serviceType);
  }

  public static List<RoleType> getRoles(String service) {
    return getRoles(ServiceType.valueOf(service));
  }

  public static String[] getRolesArray(ServiceType serviceType) {

    List<RoleType> rolesList = serviceRoleMap.get(serviceType);
    String[] rolesArray = new String[rolesList.size()];
    for (int i = 0; i < rolesList.size(); i++) {
      rolesArray[i] = rolesList.get(i).toString();
    }
    return rolesArray;
  }

  public static String getRoleFullName(RoleType role) {
    return roleFullNames.get(role);
  }
}
