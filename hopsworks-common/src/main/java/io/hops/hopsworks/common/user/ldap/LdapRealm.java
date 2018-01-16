package io.hops.hopsworks.common.user.ldap;

import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserDTO;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.login.LoginException;

@Singleton
public class LdapRealm {

  private final static Logger LOGGER = Logger.getLogger(LdapRealm.class.getName());
  private static final String[] DN_ONLY = {"dn"};
  private static final String SUBST_SUBJECT_NAME = "%s";
  private static final String SUBST_SUBJECT_DN = "%d";
  private static final String JNDICF_DEFAULT = "com.sun.jndi.ldap.LdapCtxFactory";

  @EJB
  private Settings settings;

  private String entryUUIDField;
  private String usernameField;
  private String givenNameField;
  private String surnameField;
  private String emailField;
  private String searchFilter;
  private String groupSearchFilter;
  private String groupTarget;
  private String baseDN;
  private String groupDN;
  private String dynamicGroupSearchFilter;
  private String dynamicGroupTarget;
  private String[] returningAttrs;
  private Hashtable ldapProperties;
  private LdapGroupMapper ldapGroupMapper;

  @Resource(name = "ldap/LdapResource")
  private DirContext dirContext;

  @PostConstruct
  public void init() {
    ldapProperties = getLdapBindProps();
    String attrBinary = settings.getLdapAttrBinary();
    entryUUIDField = (String) ldapProperties.get(attrBinary);
    if (entryUUIDField == null || entryUUIDField.isEmpty()) {
      throw new IllegalStateException("No UUID set for resource. Try setting " + attrBinary);
    }
    populateVars();
  }

  private void populateVars() {
    usernameField = settings.getLdapUserId();
    givenNameField = settings.getLdapUserGivenName();
    surnameField = settings.getLdapUserSurname();
    emailField = settings.getLdapUserMail();
    searchFilter = settings.getLdapUserSearchFilter();
    groupSearchFilter = settings.getLdapGroupSearchFilter();
    groupTarget = settings.getLdapGroupTarget();
    baseDN = settings.getLdapUserDN();
    groupDN = settings.getLdapGroupDN();
    dynamicGroupSearchFilter = settings.getLdapUserSearchFilter();
    dynamicGroupTarget = settings.getLdapDynGroupTarget();
    String[] attrs = {entryUUIDField, usernameField, givenNameField, surnameField, emailField};
    returningAttrs = attrs;
    ldapGroupMapper = new LdapGroupMapper(settings.getLdapGroupMapping());
  }

  public LdapUserDTO findAndBind(String username, String password) throws LoginException {
    StringBuffer sb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    String userid = sb.toString();
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    bindAsUser(userDN, password); // try login
    LdapUserDTO user = createLdapUser(userid);
    validateLdapUser(user);
    return user;
  }

  public void authenticateLdapUser(String username, String password) throws LoginException {
    StringBuffer sb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    String userid = sb.toString();
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    bindAsUser(userDN, password); // try login
  }

  public void authenticateLdapUser(LdapUser user, String password) throws LoginException {
    String userid = entryUUIDField + "=" + user.getEntryUuid();
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    bindAsUser(userDN, password); // try login
  }

  public List<String> getUserGroups(String username) {
    return ldapGroupMapper.getMappedGroups(getUserLdapGroups(username));
  }

  private List<String> getUserLdapGroups(String username) {
    StringBuffer sb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    String userid = sb.toString();
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new IllegalArgumentException("User not found.");
    }
    sb = new StringBuffer(groupSearchFilter);
    StringBuffer dynsb = new StringBuffer(dynamicGroupSearchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    substitute(sb, SUBST_SUBJECT_DN, userDN);
    substitute(dynsb, SUBST_SUBJECT_NAME, username);
    substitute(dynsb, SUBST_SUBJECT_DN, userDN);
    String srchFilter = sb.toString();
    String dynSearchFilter = dynsb.toString();
    List<String> groupsList = new ArrayList<>();
    groupsList.addAll(groupSearch(groupDN, srchFilter, groupTarget));
    groupsList.addAll(dynamicGroupSearch(groupDN, dynamicGroupTarget, dynSearchFilter, groupTarget));
    return groupsList;
  }

  private String userDNSearch(String filter) {
    String distinguishedName = null;
    NamingEnumeration answer = null;

    SearchControls ctls = new SearchControls();
    ctls.setReturningAttributes(DN_ONLY);
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setCountLimit(1);

    try {
      answer = dirContext.search(baseDN, filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        CompositeName compDN = new CompositeName(res.getNameInNamespace());
        distinguishedName = compDN.get(0);
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Ldaprealm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldaprealm security exception: {0}", e.toString());
    } finally {
      if (answer != null) {
        try {
          answer.close();
        } catch (Exception ex) {

        }
      }
    }
    return distinguishedName;
  }

  private LdapUserDTO createLdapUser(String filter) {
    NamingEnumeration answer = null;
    LdapUserDTO ldapUserDTO = null;
    SearchControls ctls = new SearchControls();
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setReturningAttributes(returningAttrs);
    ctls.setCountLimit(1);
    try {
      answer = dirContext.search(baseDN, filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        Attributes attrs = res.getAttributes();
        ldapUserDTO = new LdapUserDTO(getUUIDAttribute(attrs, entryUUIDField), getAttribute(attrs, usernameField),
            getAttribute(attrs, givenNameField), getAttribute(attrs, surnameField), getAttrList(attrs, emailField));
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Ldaprealm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldaprealm security exception: {0}", e.toString());
    } finally {
      if (answer != null) {
        try {
          answer.close();
        } catch (Exception ex) {
        }
      }
    }
    return ldapUserDTO;
  }

  private boolean bindAsUser(String bindDN, String password) throws LoginException {
    boolean bindSuccessful = false;
    Hashtable p = getLdapBindProps();
    p.put(Context.INITIAL_CONTEXT_FACTORY, JNDICF_DEFAULT);
    p.put(Context.SECURITY_PRINCIPAL, bindDN);
    p.put(Context.SECURITY_CREDENTIALS, password);
    DirContext ctx = null;
    try {
      ctx = new InitialDirContext(p);
      bindSuccessful = true;
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error binding to directory as: {0}", bindDN);
      LOGGER.log(Level.INFO, "Exception from JNDI: {0}", e.toString());
      throw new LoginException(e.getMessage());
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (Exception e) {
        }
      }
    }
    return bindSuccessful;
  }

  private List groupSearch(String groupDN, String searchFilter, String groupTarget) {
    List groupList = new ArrayList();
    String[] targets = new String[]{groupTarget};
    try {
      SearchControls ctls = new SearchControls();
      ctls.setReturningAttributes(targets);
      ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      NamingEnumeration e = dirContext.search(groupDN, searchFilter.replaceAll(Matcher.quoteReplacement("\\"),
          Matcher.quoteReplacement("\\\\")), ctls);

      while (e.hasMore()) {
        SearchResult result = (SearchResult) e.next();
        Attribute grpAttr = result.getAttributes().get(groupTarget);
        for (int i = 0; i < grpAttr.size(); i++) {
          String s = (String) grpAttr.get(i);
          groupList.add(s);
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error in group search: {0}", searchFilter);
    }
    return groupList;
  }

  private List dynamicGroupSearch(String groupDN, String dynamicGroupTarget, String dynSearchFilter,
      String groupTarget) {
    List groupList = new ArrayList();
    String[] targets = new String[]{dynamicGroupTarget};
    try {
      SearchControls ctls = new SearchControls();
      ctls.setReturningAttributes(targets);
      ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      ctls.setReturningObjFlag(false);

      NamingEnumeration e = dirContext.search(groupDN, dynSearchFilter, ctls);
      while (e.hasMore()) {
        SearchResult result = (SearchResult) e.next();
        Attribute isMemberOf = result.getAttributes().get(dynamicGroupTarget);
        if (isMemberOf != null) {
          for (Enumeration values = isMemberOf.getAll(); values.hasMoreElements();) {
            String grpDN = (String) values.nextElement();
            LdapName dn = new LdapName(grpDN);
            for (Rdn rdn : dn.getRdns()) {
              if (rdn.getType().equalsIgnoreCase(groupTarget)) {
                groupList.add(rdn.getValue());
                break;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error in dynamic group search: {0}", dynSearchFilter);
    }

    return groupList;
  }

  private String getUUIDAttribute(Attributes attrs, String key) throws NamingException {
    Attribute attr = attrs.remove(key);
    byte[] guid = attr != null ? (byte[]) attr.get() : "".getBytes();
    return new String(guid);
  }

  private String getAttribute(Attributes attrs, String key) throws NamingException {
    Attribute attr = attrs.remove(key);
    return attr != null ? (String) attr.get() : "";
  }

  private List<String> getAttrList(Attributes attrs, String key) throws NamingException {
    List<String> vals = new ArrayList<>();
    Attribute attr = attrs.remove(key);
    if (attr == null) {
      return vals;
    }
    NamingEnumeration a = attr.getAll();
    while (a.hasMore()) {
      vals.add((String) a.next());
    }
    return vals;
  }

  private Hashtable getLdapBindProps() {
    Hashtable ldapProperties = new Hashtable();
    try {
      ldapProperties = (Hashtable) dirContext.getEnvironment().clone();
    } catch (NamingException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return ldapProperties;
  }

  private static void substitute(StringBuffer sb, String target, String value) {
    int i = sb.indexOf(target);
    while (i >= 0) {
      sb.replace(i, i + target.length(), value);
      i = sb.indexOf(target);
    }
  }

  private void validateLdapUser(LdapUserDTO user) throws LoginException {
    if (user.getEntryUUID() == null || user.getEntryUUID().isEmpty()) {
      LOGGER.log(Level.SEVERE, "Could not find UUID for Ldap user. Check LDAP configuration.");
      throw new LoginException("Could not find UUID for Ldap user.");
    }
    if (user.getEmail() == null || user.getEmail().isEmpty()) {
      LOGGER.log(Level.SEVERE, "Could not find email for Ldap user. Check LDAP configuration.");
      throw new LoginException("Could not find email for Ldap user.");
    }
    if (user.getGivenName() == null || user.getGivenName().isEmpty()) {
      LOGGER.log(Level.SEVERE, "Could not find givenName for Ldap user. Check LDAP configuration.");
      throw new LoginException("Could not find givenName for Ldap user.");
    }
    if (user.getSn() == null || user.getSn().isEmpty()) {
      LOGGER.log(Level.SEVERE, "Could not find surname for Ldap user. Check LDAP configuration.");
      throw new LoginException("Could not find surname for Ldap user.");
    }

  }

}
