/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.hops.hopsworks.admin.ndb;

import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.ndb.NdbBackup;
import io.hops.hopsworks.common.dao.ndb.NdbBackupFacade;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.event.RowEditEvent;

@ManagedBean(name = "ndbBackupBean")
@SessionScoped
public class NdbBackupBean implements Serializable {

  private static final Logger logger = Logger.getLogger(
          NdbBackupBean.class.getName());

  @EJB
  private NdbBackupFacade ndbBackupFacade;
  @EJB
  private Settings settings;

  private List<NdbBackup> filteredBackups = new ArrayList<NdbBackup>();
  public List<NdbBackup> allBackups = new ArrayList<>();

  public void setFilteredBackups(List<NdbBackup> filteredBackups) {
    this.filteredBackups = filteredBackups;
  }

  public List<NdbBackup> getFilteredBackups() {
    return filteredBackups;
  }

  public void setAllBackups(List<NdbBackup> allBackups) {
    this.allBackups = allBackups;
  }

  public List<NdbBackup> getAllBackups() {
    List<NdbBackup> all = ndbBackupFacade.findAll();
    if (all != null) {
      allBackups = all;
    }
    return allBackups;
  }

  public void onRowEdit(RowEditEvent event)
          throws IOException {

  }

  public void onRowCancel(RowEditEvent event) {
  }

  public String restore(Integer backupId) {

    String prog = settings.getNdbDir() + "/ndb/scripts/backup-restore.sh";
    int exitValue;

    String[] command = {prog, backupId.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);

    try {
      Process p = pb.start();
      p.waitFor();
      exitValue = p.exitValue();
    } catch (IOException | InterruptedException ex) {

      logger.log(Level.WARNING, "Problem starting a backup: {0}", ex.
              toString());
      //if the pid file exists but we can not test if it is alive then
      //we answer true, b/c pid files are deleted when a process is killed.
      return "RESTORE_FAILED";
    }
    return exitValue == 0 ? "RESTORE_OK" : "RESTORE_FAILED";
  }

  public String startBackup() {

    String prog = settings.getNdbDir() + "/ndb/scripts/backup-start.sh";
    int exitValue;

    NdbBackup backup = ndbBackupFacade.findHighestBackupId();
    Integer id = 1;
    if (backup != null) {
      id = backup.getBackupId() + 1;
    }
    String[] command = {prog, id.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      while ((line = br.readLine()) != null) {
        logger.info(line);
      }

      process.waitFor();
      exitValue = process.exitValue();
      if (exitValue == 0) {
        NdbBackup newBackup = new NdbBackup(id);
        ndbBackupFacade.persistBackup(backup);
      }
    } catch (IOException | InterruptedException ex) {

      logger.log(Level.WARNING, "Problem starting a backup: {0}", ex.
              toString());
      //if the pid file exists but we can not test if it is alive then
      //we answer true, b/c pid files are deleted when a process is killed.
      exitValue = -2;
    }
    if (exitValue == 0) {
      return "BACKUP_OK";
    } else {
      MessagesController.addSecurityErrorMessage("Backup failed: " + exitValue);
      return "BACKUP_FAILED";
    }
  }
}
