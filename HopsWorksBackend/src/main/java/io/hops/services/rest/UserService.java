/*
 */
package io.hops.services.rest;

import io.hops.integration.GroupFacade;
import io.hops.integration.UserFacade;
import io.hops.model.Users;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author André & Ermias
 */
@Path("/user")
@RolesAllowed({"ADMIN", "USER"})
@Produces(MediaType.TEXT_PLAIN)
@Stateless
public class UserService {

    @EJB
    private UserFacade userBean;

    @EJB
    private GroupFacade groupBean;

    @GET
    @Path("profile")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response getUserProfile(@Context SecurityContext sc) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        if (user == null) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(json).build();
        }

        userBean.detach(user);
        user.setPassword("");
        //json.setStatus("OK");
        //json.setData(user);
        return getNoCacheResponseBuilder(Response.Status.OK).entity(user).build();
    }


    @POST
    @Path("updateProfile")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response updateProfile(@FormParam("firstName") String firstName,
                                  @FormParam("lastName") String lastName, 
                                  @FormParam("telephoneNum") String telephoneNum, 
                                  @Context SecurityContext sc, 
                                  @Context HttpServletRequest req) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        if (user == null) {
            json.setStatus("FAILED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }

        req.getServletContext().log("Updating..." + firstName + ", " + lastName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setTelephoneNum(telephoneNum);
        userBean.update(user);
        json.setStatus("OK");
        //we don't want to send the hashed password out in the json response
        userBean.detach(user);
        user.setPassword("");
        json.setData(user);

        return getNoCacheResponseBuilder(Response.Status.OK).entity(user).build();
    }

    @POST
    @Path("changeLoginCredentials")
    @Produces(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response changeLoginCredentials(@FormParam("oldPassword") String oldPassword,
                                           @FormParam("newPassword") String newPassword,
                                           @FormParam("confirmedPassword") String confirmedPassword,
                                           @Context SecurityContext sc,
                                           @Context HttpServletRequest req) {
        JsonResponse json = new JsonResponse();
        Users user = userBean.findByEmail(sc.getUserPrincipal().getName());

        if (user == null) {
            json.setStatus("NOT_MODIFIED");
            json.setErrorMsg("Operation failed. User not found");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
            json.setStatus("NOT_MODIFIED");
            json.setErrorMsg("Operation failed. password not correct");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (newPassword.length() == 0) {
            json.setStatus("NOT_MODIFIED");
            json.setErrorMsg("Operation failed. password can not be empty.");
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).entity(json).build();
        }
        if (!newPassword.equals(confirmedPassword)) {
            json.setStatus("304");
            json.setErrorMsg("Operation failed. passwords do not match.");
            req.getServletContext().log("Sending:----"+ json);
            return getNoCacheResponseBuilder(Response.Status.NOT_MODIFIED).type("text/plain").entity("Operation failed. passwords do not match.").build();
        }

        user.setPassword(newPassword);
        userBean.update(user);

        json.setStatus("OK");

        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }


    private Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }
}
