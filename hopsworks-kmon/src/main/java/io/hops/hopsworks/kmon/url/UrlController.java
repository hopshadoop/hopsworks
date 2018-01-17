package io.hops.hopsworks.kmon.url;

import java.util.logging.Logger;
import javax.faces.application.NavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class UrlController {

  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.status}")
  private String status;
  @ManagedProperty("#{param.target}")
  private String target;
  private static final Logger logger = Logger.getLogger(UrlController.class.
          getName());

  public UrlController() {
    logger.info("UrlController");
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String host() {
    return "host?faces-redirect=true&hostname=" + hostname;
  }

  public String clustersStatus(){
    return "clusters?faces-redirect=true";
  }
  
  public String clusterStatus() {
    return "cluster-status?faces-redirect=true&cluster=" + cluster;
  }

  public String clusterStatus(String cluster) {
    return "cluster-status?faces-redirect=true&cluster=" + cluster;
  }
  
  public String groupInstance() {
    return "services-instances-status?faces-redirect=true&hostname="
            + hostname + "&cluster=" + cluster + "&group=" + group;
  }

  public String clusterActionHistory() {
    return "cluster-actionhistory?faces-redirect=true&cluster=" + cluster;
  }

  public String groupStatus() {
    return "group-status?faces-redirect=true&cluster=" + cluster + "&group=" + group;
  }

  public String groupInstances() {
    String url = "group-instances?faces-redirect=true";
    if (hostname != null) {
      url += "&hostname=" + hostname;
    }
    if (cluster != null) {
      url += "&cluster=" + cluster;
    }
    if (group != null) {
      url += "&group=" + group;
    }
    if (service != null) {
      url += "&service=" + service;
    }
    if (status != null) {
      url += "&status=" + status;
    }
    return url;
  }

  public String groupActionHistory() {
    return "group-actionhistory?faces-redirect=true&cluster=" + cluster + "&group=" + group;
  }

  public String groupTerminal() {
    return "group-terminal?faces-redirect=true&cluster=" + cluster + "&group=" + group;
  }

  public String serviceAudit() {
    return "service-status?faces-redirect=true&hostname=" + hostname + "&cluster="
            + cluster + "&group=" + group + "&service=" + service;
  }

  public String serviceActionHistory() {
    return "service-actionhistory?faces-redirect=true&hostname=" + hostname
            + "&cluster=" + cluster + "&group=" + group + "&service=" + service;
  }

  public void redirectToEditGraphs() {
    String outcome = "edit-graphs?faces-redirect=true";
    if (target != null) {
      outcome += "&target=" + target;
    }
    FacesContext context = FacesContext.getCurrentInstance();
    NavigationHandler navigationHandler = context.getApplication().
            getNavigationHandler();
    navigationHandler.handleNavigation(context, null, outcome);
  }
}
