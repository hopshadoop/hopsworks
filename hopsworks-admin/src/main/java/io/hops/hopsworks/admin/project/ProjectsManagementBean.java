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
package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "projectsmanagement")
@ViewScoped
public class ProjectsManagementBean implements Serializable {
  private static final long serialVersionUID = -1L;

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;

  private List<ProjectQuotas> projectsQuotas = null;
  private List<ProjectQuotas> filteredProjectsQuotas = null;
  private ProjectQuotas projectQuotasSelected = null;
  private String projectNameForceCleanup;
  private List<PaymentType> paymentTypes = null;
  private Map<String, Object> dialogOptions;

  private static Logger logger = Logger.getLogger(ProjectsManagementBean.class.toString());

  @PostConstruct
  public void init() {
    List<Project> projects = projectFacade.findAll();
    projectsQuotas = new ArrayList<>();
    for (Project project : projects) {
      projectsQuotas.add(new ProjectQuotas(project, projectController.getQuotasInternal(project)));
    }

    paymentTypes = new ArrayList<>();
    for (PaymentType paymentType : PaymentType.values()) {
      paymentTypes.add(paymentType);
    }

    dialogOptions = new HashMap<>(3);
    dialogOptions.put("resizable", false);
    dialogOptions.put("draggable", false);
    dialogOptions.put("modal", true);
  }

  public List<ProjectQuotas> getProjectsQuotas() { return projectsQuotas; }

  public void setProjectQuotas(List<ProjectQuotas> projectsQuotas) {
    this.projectsQuotas = projectsQuotas;
  }

  public List<ProjectQuotas> getFilteredProjectsQuotas () { return filteredProjectsQuotas; }

  public void setFilteredProjectsQuotas(List<ProjectQuotas> filteredProjectsQuotas) {
    this.filteredProjectsQuotas = filteredProjectsQuotas;
  }

  public List<PaymentType> getPaymentTypes() { return paymentTypes; }

  public ProjectQuotas getProjectQuotasSelected() {
    return projectQuotasSelected;
  }

  public void setProjectQuotasSelected(ProjectQuotas projectQuotasSelected) {
    this.projectQuotasSelected = projectQuotasSelected;
  }

  public void onRowEdit(RowEditEvent event) {
    ProjectQuotas pQuotas = (ProjectQuotas) event.getObject();
    try {
      projectController.adminProjectUpdate(pQuotas.getProject(), pQuotas.getNormalizedQuotas());
    } catch (AppException e) {
      logger.log(Level.SEVERE, "Error updating the quota for the project: " + pQuotas.getName(),
          e);
      MessagesController.addErrorMessage("Error updating the project, " +
            "check the logs for more information");
    }

    // Update the timestamp in the table
    Date timestamp = new Date();
    pQuotas.setLastQuotaUpdate(timestamp);
    int rowIndex = ((DataTable)event.getSource()).getRowIndex();
    RequestContext.getCurrentInstance().update("projectmodifyForm:projectsTable:" + rowIndex
        + ":lastQuotaUpdateColumn");

    logger.log(Level.INFO, "Quotas successfully updated for project: " + pQuotas.getName());
  }

  public void deleteProject() {
    logger.log(Level.INFO, "Deleting project: " + projectQuotasSelected.getName());
    Cookie[] cookies = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
        .getCookies();
    try {
      String sessionId = "";
      for (Cookie cookie : cookies) {
        if (cookie.getName().equalsIgnoreCase("SESSION")) {
          sessionId = cookie.getValue();
          break;
        }
      }

      projectController.removeProject(projectQuotasSelected.getOwner(),
          projectQuotasSelected.getId(), sessionId);

      projectsQuotas.remove(projectQuotasSelected);
      projectQuotasSelected = null;
      MessagesController.addInfoMessage("Project deleted!");
    } catch (AppException ex) {
      logger.log(Level.SEVERE, "Failed to delete project " + projectQuotasSelected.getName(), ex);
      MessagesController.addErrorMessage("Deletion failed", "Failed deleting project "
          + projectQuotasSelected.getName());
    }
  }

  public void dialogForceCleanup() {
    RequestContext.getCurrentInstance().openDialog("projectForceRemoveDialog", dialogOptions, null);
  }

  public String getProjectNameForceCleanup() {
    return projectNameForceCleanup;
  }

  public void setProjectNameForceCleanup(String projectNameForceCleanup) {
    this.projectNameForceCleanup = projectNameForceCleanup;
  }

  public void selectedProjectForceCleanup() {
    RequestContext.getCurrentInstance().closeDialog(projectNameForceCleanup);
  }
  
  public void onProjectForceCleanupChosen(SelectEvent event) {
    String projectName = (String) event.getObject();
    logger.log(Level.INFO, "Project force cleanup: " + projectName);
    String userEmail = FacesContext.getCurrentInstance().getExternalContext()
        .getUserPrincipal().getName();
    MessagesController.addInfoMessage("Force project deletion", "Check Inbox for status");
    projectController.forceCleanup(projectName, userEmail, getSessionId());
  }
  
  private String getSessionId() {
    Cookie[] cookies = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
        .getCookies();
    String sessionId = "";
    for (Cookie cookie : cookies) {
      if (cookie.getName().equalsIgnoreCase("SESSION")) {
        sessionId = cookie.getValue();
        break;
      }
    }
    return sessionId;
  }

  public void onRowCancel(RowEditEvent event) { }

}
