package io.hops.hopsworks.common.dao.user.security.audit;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.account_audit")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AccountAudit.findAll",
          query = "SELECT a FROM AccountAudit a"),
  @NamedQuery(name = "AccountAudit.findByLogId",
          query = "SELECT a FROM AccountAudit a WHERE a.logId = :logId"),
  @NamedQuery(name = "AccountAudit.findByInitiator",
          query = "SELECT a FROM AccountAudit a WHERE a.initiator = :initiator"),
  @NamedQuery(name = "AccountAudit.findByTarget",
          query = "SELECT r FROM ServiceAudit r WHERE r.target = :target"),
  @NamedQuery(name = "AccountAudit.findByAction",
          query = "SELECT a FROM AccountAudit a WHERE a.action = :action"),
  @NamedQuery(name = "AccountAudit.findByTime",
          query = "SELECT a FROM AccountAudit a WHERE a.actionTimestamp = :actionTimestamp"),
  @NamedQuery(name = "AccountAudit.findByMessage",
          query = "SELECT a FROM AccountAudit a WHERE a.message = :message"),
  @NamedQuery(name = "AccountAudit.findByOutcome",
          query = "SELECT a FROM AccountAudit a WHERE a.outcome = :outcome"),
  @NamedQuery(name = "AccountAudit.findByIp",
          query = "SELECT a FROM AccountAudit a WHERE a.ip = :ip"),
  @NamedQuery(name = "AccountAudit.findByUserAgent",
      query = "SELECT a FROM AccountAudit a WHERE a.userAgent = :userAgent")})
public class AccountAudit implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "log_id")
  private Integer logId;
  @Size(max = 45)
  @Column(name = "action")
  private String action;
  @Column(name = "action_timestamp")
  @Temporal(TemporalType.TIMESTAMP)
  private Date actionTimestamp;
  @Size(max = 100)
  @Column(name = "message")
  private String message;
  @Size(max = 45)
  @Column(name = "outcome")
  private String outcome;
  @Size(max = 45)
  @Column(name = "ip")
  private String ip;
  @Size(max = 255)
  @Column(name = "useragent")
  private String userAgent;
  @JoinColumn(name = "initiator",
      referencedColumnName = "uid")
  @ManyToOne
  private Users initiator;

  @JoinColumn(name = "target",
      referencedColumnName = "uid")
  @ManyToOne
  private Users target;

  public AccountAudit() {
  }

  public AccountAudit(String action, Date actionTimestamp, String message, String outcome, String ip,
                      String userAgent, Users target, Users initiator) {
    this.action = action;
    this.actionTimestamp = actionTimestamp;
    this.message = message;
    this.outcome = outcome;
    this.ip = ip;
    this.userAgent = userAgent;
    this.initiator = initiator;
    this.target = target;
  }

  public AccountAudit(Integer logId) { this.logId = logId; }

  public Integer getLogId() { return logId; }

  public void setLogId(Integer logId) { this.logId = logId; }

  public String getAction() { return action; }

  public void setAction(String action) { this.action = action; }

  public Date getActionTimestamp() { return actionTimestamp; }

  public void setActionTimestamp(Date time) { this.actionTimestamp = time; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public String getOutcome() { return outcome; }

  public void setOutcome(String outcome) { this.outcome = outcome; }

  public String getIp() { return ip; }

  public void setIp(String ip) { this.ip = ip; }

  public String getUserAgent() { return userAgent; }

  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

  public Users getInitiator() { return initiator; }

  public void setInitiator(Users initiator) { this.initiator = initiator; }

  public Users getTarget() { return target; }

  public void setTarget(Users target) { this.target = target; }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (logId != null ? logId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof AccountAudit)) {
      return false;
    }
    AccountAudit other = (AccountAudit) object;
    if ((this.logId == null && other.logId != null) || (this.logId != null
        && !this.logId.equals(other.logId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "AccountAudit{" +
        "logId=" + logId +
        ", action='" + action + '\'' +
        ", time=" + actionTimestamp +
        ", message='" + message + '\'' +
        ", outcome='" + outcome + '\'' +
        ", ip='" + ip + '\'' +
        ", userAgent='" + userAgent + '\'' +
        ", initiator=" + initiator +
        ", target=" + target +
        '}';
  }
}
