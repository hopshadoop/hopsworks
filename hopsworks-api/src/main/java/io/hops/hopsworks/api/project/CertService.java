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

package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * <p>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertService {

  private final static Logger LOGGER = Logger.getLogger(CertService.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectController projectController;

  private Project project;

  public CertService setProject(Project project) {
    this.project = project;
    return this;
  }

  @GET
  @Path("/certpw")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getCertPw(@QueryParam("keyStore") String keyStore,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    //Find user
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(userEmail);
    try {
      CertPwDTO respDTO = projectController.getProjectSpecificCertPw(user, project.getName(), keyStore);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(respDTO).build();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Could not retrieve certificate passwords for user:" + user.getUsername(), ex);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).build();
    }
  }

}
