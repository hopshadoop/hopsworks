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

package io.hops.hopsworks.common.dao.jobhistory;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class YarnAppHeuristicResultFacade extends AbstractFacade<YarnAppHeuristicResult> {

  private static final Logger logger = Logger.getLogger(
          YarnAppHeuristicResultFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppHeuristicResultFacade() {
    super(YarnAppHeuristicResult.class);
  }

  public Integer searchByIdAndClass(String yarnAppResultId,
          String heuristicClass) {
    TypedQuery<YarnAppHeuristicResult> q = em.createNamedQuery(
            "YarnAppHeuristicResult.findByIdAndHeuristicClass",
            YarnAppHeuristicResult.class);
    q.setParameter("yarnAppResultId", yarnAppResultId);
    q.setParameter("heuristicClass", heuristicClass);

    YarnAppHeuristicResult result = q.getSingleResult();

    return result.getId();

  }

  public String searchForSeverity(String yarnAppResultId, String heuristicClass) {
    try {
      TypedQuery<YarnAppHeuristicResult> q = em.createNamedQuery(
              "YarnAppHeuristicResult.findByIdAndHeuristicClass",
              YarnAppHeuristicResult.class);
      q.setParameter("yarnAppResultId", yarnAppResultId);
      q.setParameter("heuristicClass", heuristicClass);

      YarnAppHeuristicResult result = q.getSingleResult();

      short severity = result.getSeverity();
      switch (severity) {
        case 0:
          return "NONE";
        case 1:
          return "LOW";
        case 2:
          return "MODERATE";
        case 3:
          return "SEVERE";
        case 4:
          return "CRITICAL";
        default:
          return "NONE";
      }
    } catch (NoResultException e) {
      return "UNDEFINED";
    }
  }

}
