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

package io.hops.hopsworks.common.dao.user;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.security.UserGroup;
import io.hops.hopsworks.common.dao.user.security.UserGroupPK;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.util.Settings;

@Stateless
public class UserFacade extends AbstractFacade<Users> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public UserFacade() {
    super(Users.class);
  }

  @Override
  public List<Users> findAll() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAll",
            Users.class);
    return query.getResultList();
  }

  public List<Users> findAllByName() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findAllByName",
            Users.class);
    return query.getResultList();
  }

  public List<Users> findAllUsers() {
    Query query = em.createNativeQuery("SELECT * FROM hopsworks.users",
            Users.class);
    return query.getResultList();
  }

  public List<Users> findAllMobileRequests() {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", UserAccountStatus.VERIFIED_ACCOUNT);
    query.setParameter("mode", UserAccountType.M_ACCOUNT_TYPE);
    List<Users> res = query.getResultList();
    
    query = em.createNamedQuery("Users.findByStatusAndMode", Users.class);
    query.setParameter("status", UserAccountStatus.NEW_MOBILE_ACCOUNT);
    query.setParameter("mode", UserAccountType.M_ACCOUNT_TYPE);
    
    res.addAll(query.getResultList());
    return res;
  }
  
  public Users findByUsername(String username) {
    try {
      return em.createNamedQuery("Users.findByUsername", Users.class).setParameter("username", username).
          getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<Users> filterUsersBasedOnProject(String name) {

    Query query = em.createNativeQuery(
            "SELECT * FROM hopsworks.users WHERE email NOT IN (SELECT team_member "
            + "FROM hopsworks.ProjectTeam WHERE name=?)",
            Users.class).setParameter(1, name);
    return query.getResultList();
  }

  public void persist(Users user) {
    em.persist(user);
  }

  public int lastUserID() {
    Query query = em.createNativeQuery("SELECT MAX(p.uid) FROM hopsworks.users p");
    Object obj = query.getSingleResult();

    if (obj == null) {
      return Settings.STARTING_USER;
    }
    return (Integer) obj;
  }

  @Override
  public Users update(Users user) {
    return em.merge(user);
  }

  public void removeByEmail(String email) {
    Users user = findByEmail(email);
    if (user != null) {
      em.remove(user);
    }
  }

  @Override
  public void remove(Users user) {
    if (user != null && user.getEmail() != null && em.contains(user)) {
      em.remove(user);
    }
  }

  /**
   * Get the user with the given email.
   * <p/>
   * @param email
   * @return The user with given email, or null if no such user exists.
   */
  public Users findByEmail(String email) {
    try {
      return em.createNamedQuery("Users.findByEmail", Users.class).setParameter(
              "email", email)
              .getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  public void detach(Users user) {
    em.detach(user);
  }

  /**
   * Get all users with STATUS = status.
   *
   * @param status
   * @return
   */
  public List<Users> findAllByStatus(UserAccountStatus status) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByStatus",
            Users.class);
    query.setParameter("status", status);
    return query.getResultList();
  }
  
  public List<Integer> findAllInGroup(int gid) {
    Query query = em.createNativeQuery(
        "SELECT u.uid FROM hopsworks.users u JOIN hopsworks.user_group g ON u.uid = g.uid Where g.gid = ?");
    query.setParameter(1, gid);
    return (List<Integer>) query.getResultList();
  }

  /**
   * Add a new group for a user.
   *
   * @param userMail 
   * @param gidNumber
   * @return
   */
  public void addGroup(String userMail, int gidNumber) {
    BbcGroup bbcGroup = em.find(BbcGroup.class, gidNumber);
    Users user = findByEmail(userMail);
    user.getBbcGroupCollection().add(bbcGroup);
    em.merge(user);
  }

  /**
   * Remove user's group based on userMail/gid.
   *
   * @param userMail
   * @param gid
   */
  public void removeGroup(String userMail, int gid) {
    Users user = findByEmail(userMail);
    UserGroup p = em.find(UserGroup.class, new UserGroup(
            new UserGroupPK(user.getUid(), gid)).getUserGroupPK());
    em.remove(p);
  }
  
  /**
   * Update a user status 
   *
   * @param userMail 
   * @param newStatus
   */
  public void updateStatus(String userMail, UserAccountStatus newStatus) {
    Users user = findByEmail(userMail);
    user.setStatus(newStatus);
    em.merge(user);
  }
  
}
