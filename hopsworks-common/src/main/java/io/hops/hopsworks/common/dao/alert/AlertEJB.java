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

package io.hops.hopsworks.common.dao.alert;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.alert.Alert.Provider;
import io.hops.hopsworks.common.dao.alert.Alert.Severity;

/**
 * See:
 * https://abhirockzz.wordpress.com/2015/02/19/valid-cdi-scoped-for-session-ejb-beans/
 */
@Stateless
public class AlertEJB {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public AlertEJB() {
  }

  public List<Alert> find(Date fromDate, Date toDate) {

    TypedQuery<Alert> query = em.createNamedQuery("Alerts.findAll", Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate);
    return query.getResultList();
  }

  public List<Alert> find(Date fromDate, Date toDate, Severity severity) {
    TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Severity",
            Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate)
            .setParameter("severity", severity.toString());
    return query.getResultList();
  }

  public List<Alert> find(Date fromDate, Date toDate, Provider provider) {
    TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Provider",
            Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate)
            .setParameter("provider", provider.toString());
    return query.getResultList();
  }

  public List<Alert> find(Date fromDate, Date toDate, Provider provider,
          Severity severity) {
    TypedQuery<Alert> query = em.createNamedQuery(
            "Alerts.findBy-Provider-Severity", Alert.class)
            .setParameter("fromdate", fromDate).setParameter("todate", toDate)
            .setParameter("provider", provider.toString()).setParameter(
            "severity", severity.toString());
    return query.getResultList();
  }

  public void persistAlert(Alert alert) {
    em.persist(alert);
  }

  public void removeAlert(Alert alert) {
    em.remove(em.merge(alert));
  }

  public void removeAllAlerts() {
    em.createNamedQuery("Alerts.removeAll").executeUpdate();
  }
}
