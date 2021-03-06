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

package io.hops.hopsworks.common.util;

import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.exception.AppException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;
import org.apache.commons.lang.StringEscapeUtils;

@Stateless
public class WebCommunication {

  private static final Logger logger = Logger.getLogger(WebCommunication.class.
          getName());

  private static boolean DISABLE_CERTIFICATE_VALIDATION = true;
  private static String PROTOCOL = "https";
  private static int PORT = 8090;
  private static String NOT_AVAILABLE = "Not available.";
  private final ConcurrentLinkedQueue<Client> inUseClientPool =
      new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Client> availableClientPool =
      new ConcurrentLinkedQueue<>();
  @EJB
  private Settings settings;

  public WebCommunication() {
  }

  @PreDestroy
  private void cleanUp() {
    for (Client client : availableClientPool) {
      client.close();
      client = null;
    }
    for (Client client : inUseClientPool) {
      client.close();
      client = null;
    }
  }
  
  public Response getWebResponse(String url, String agentPassword) {
    Response response;
    try {
      response = getWebResource(url, agentPassword);
      return response;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   *
   * @param operation start | stop | restart
   * @param hostAddress
   * @param agentPassword
   * @param cluster
   * @param service
   * @param service
   * @return
   */
  public String serviceOp(String operation, String hostAddress,
      String agentPassword, String cluster, String group, String service) throws AppException {
    String url = createUrl(operation, hostAddress, cluster, group, service);
    return fetchContent(url, agentPassword);
  }

  @Asynchronous
  public Future<String> asyncServiceOp(String operation, String hostAddress,
      String agentPassword, String cluster, String group, String service) throws AppException {
    String url = createUrl(operation, hostAddress, cluster, group, service);
    return new AsyncResult<String>(fetchContent(url, agentPassword));
  }
  
  public String getConfig(String hostAddress, String agentPassword,
      String cluster, String group, String service) throws AppException {
    String url = createUrl("config", hostAddress, cluster, group, service);
    return fetchContent(url, agentPassword);
  }

  public String getServiceLog(String hostAddress, String agentPassword,
      String cluster, String group, String service, int lines) throws AppException {
    String url = createUrl("log", hostAddress, cluster, group, service, String.
            valueOf(lines));
    return fetchLog(url, agentPassword);
  }

  public String getGroupLog(String hostAddress, String agentPassword,
      String cluster, String group, int lines) throws AppException {
    String url = createUrl("log", hostAddress, cluster, group, String.valueOf(
            lines));
    return fetchLog(url, agentPassword);
  }

  public String getAgentLog(String hostAddress, String agentPassword, int lines) throws AppException {
    String url = createUrl("agentlog", hostAddress, String.valueOf(lines));
    return fetchLog(url, agentPassword);
  }

  public List<NodesTableItem> getNdbinfoNodesTable(String hostAddress,
      String agentPassword) throws AppException {
    List<NodesTableItem> resultList = new ArrayList<NodesTableItem>();

    String url = createUrl("mysql", hostAddress, "ndbinfo", "nodes");
    String jsonString = fetchContent(url, agentPassword);
    InputStream stream = new ByteArrayInputStream(jsonString.getBytes(          StandardCharsets.UTF_8));
    try {
      JsonArray json = Json.createReader(stream).readArray();
      if (json.get(0).equals("Error")) {
        resultList.add(new NodesTableItem(null, json.getString(1), null, null,
                null));
        return resultList;
      }
      for (int i = 0; i < json.size(); i++) {
        JsonArray node = json.getJsonArray(i);
        Integer nodeId = node.getInt(0);
        Long uptime = node.getJsonNumber(1).longValue();
        String status = node.getString(2);
        Integer startPhase = node.getInt(3);
        Integer configGeneration = node.getInt(4);
        resultList.add(new NodesTableItem(nodeId, status, uptime, startPhase,
                configGeneration));
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Exception: {0}", ex);
      resultList.add(new NodesTableItem(null, "Error", null, null, null));
    }
    return resultList;
  }

  public String executeRun(String hostAddress, String agentPassword,
          String cluster, String group, String service, String command,
          String[] params) throws Exception {
    return execute("execute/run", hostAddress, agentPassword, cluster, group,
            service, command, params);
  }

  public String executeStart(String hostAddress, String agentPassword,
          String cluster, String group, String service, String command,
          String[] params) throws Exception {
    return execute("execute/start", hostAddress, agentPassword, cluster, group,
            service, command, params);
  }

  public String executeContinue(String hostAddress, String agentPassword,
          String cluster, String group, String service, String command,
          String[] params) throws Exception {
    return execute("execute/continue", hostAddress, agentPassword, cluster,
            group, service, command, params);
  }

  private String execute(String path, String hostAddress, String agentPassword,
          String cluster, String group, String service, String command,
          String[] params) throws Exception {
    String url = createUrl(path, hostAddress, cluster, group, service, command);
    String optionsAndParams = "";
    for (String param : params) {
      optionsAndParams += optionsAndParams.isEmpty() ? param : " " + param;
    }
    Response response = postWebResource(url, agentPassword,
            optionsAndParams);
    int code = response.getStatus();
    Family res = Response.Status.Family.familyOf(code);
    if (res == Response.Status.Family.SUCCESSFUL) {
      String responseString = response.readEntity(String.class);
      if (path.equalsIgnoreCase("execute/continue")) {
        JsonObject json = Json.createReader(response.readEntity(Reader.class)).
                readObject();
        responseString = json.getString("before");
      }
      return FormatUtils.stdoutToHtml(responseString);
    }
    throw new RuntimeException("Did not succeed to execute command.");
  }

  public Response doCommand(String hostAddress, String agentPassword,
          String cluster, String group, String service, String command) throws
          Exception {
    String url = createUrl("do", hostAddress, agentPassword, cluster, group,
            service, command);
    return getWebResource(url, agentPassword);
  }

  private String createUrl(String context, String hostAddress, String... args) {
    String template = "%s://%s:%s/%s";
    String url = String.format(template, PROTOCOL, hostAddress, PORT, context);
    for (int i = 0; i < args.length; i++) {
      url += "/" + args[i];
    }
    return url;
  }

  private String fetchContent(String url, String agentPassword) throws AppException {
    String content = NOT_AVAILABLE;
    try {
      Response response = getWebResource(url, agentPassword);
      int code = response.getStatus();
      Family res = Response.Status.Family.familyOf(code);
      if (res == Response.Status.Family.SUCCESSFUL) {
        content = response.readEntity(String.class);
      } else {
        throw new AppException(response.getStatus(), response.getStatusInfo().getReasonPhrase());
      }
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      logger.log(Level.SEVERE, null, e);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getCause().getMessage());
    }
    return content;
  }

  private String fetchLog(String url, String agentPassword) throws AppException {
    String log = fetchContent(url, agentPassword);
//        log = log.replaceAll("\n", "<br>");
    log = FormatUtils.stdoutToHtml(log);
    return log;
  }

  private Response getWebResource(String url, String agentPassword) throws NoSuchAlgorithmException,
      KeyManagementException {
    return getWebResource(url, agentPassword, null);
  }

  private Response getWebResource(String url, String agentPassword, Map<String, String> args) throws
      NoSuchAlgorithmException, KeyManagementException {

    Client client = getClient();
    WebTarget webResource = client.target(url);

    webResource = webResource.queryParam("username", Settings.AGENT_EMAIL);
    webResource = webResource.queryParam("password", agentPassword);
    if (args != null) {
      for (String key : args.keySet()) {
        webResource = webResource.queryParam(key, args.get(key));
      }
    }
    logger.log(Level.INFO,
            "WebCommunication: Requesting url: {0} with password {1}",
            new Object[]{url, agentPassword});
    
    Response response = webResource.request()
            .header("Accept-Encoding", "gzip,deflate")
            .get(Response.class);
    discardClient(client);
    logger.log(Level.INFO, "WebCommunication: Requesting url: {0}", url);
    return response;
  }
  
  private Client getClient() throws NoSuchAlgorithmException, KeyManagementException {
    Client client = availableClientPool.poll();
    if (null == client) {
      client = createClient();
    }
    
    inUseClientPool.offer(client);
    return client;
  }
  
  private void discardClient(Client client) {
    inUseClientPool.remove(client);
    availableClientPool.offer(client);
  }
  
  // This method should never be invoked but from the getClient() method
  private Client createClient() throws NoSuchAlgorithmException,
      KeyManagementException {
    if (DISABLE_CERTIFICATE_VALIDATION) {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
            
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
          }};
      
      // Ignore differences between given hostname and certificate hostname
      HostnameVerifier hv = new HostnameVerifier() {
        public boolean verify(String hostAddress, SSLSession session) {
          return true;
        }
      };
      
      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("TLSv1.2");
      sc.init(null, trustAllCerts, new SecureRandom());
      ClientBuilder clientBuilder = ClientBuilder.newBuilder()
          .hostnameVerifier(hv)
          .sslContext(sc);
      return clientBuilder.build();
    } else {
      return ClientBuilder.newClient();
    }
  }
  
  private Response postWebResource(String url, String agentPassword,
          String body) throws Exception {
    return postWebResource(url, agentPassword, "", "", body);
  }

  private Response postWebResource(String url, String agentPassword,
          String channelUrl, String version, String body) throws Exception {
    Client client = getClient();
    WebTarget webResource = client.target(url);
    webResource.queryParam("username", Settings.AGENT_EMAIL);
    webResource.queryParam("password", agentPassword);

    Response response = webResource.request()
            .header("Accept-Encoding", "gzip,deflate")
            .post(Entity.entity(body, MediaType.TEXT_PLAIN), Response.class);
    discardClient(client);
    return response;
  }

  public Object anaconda(String hostAddress, String agentPassword, String op,
          String project, String arg) throws Exception {

    String path = "anaconda/" + settings.getAnacondaUser() + '/' + op.toLowerCase()
            + "/" + project;
    String template = "%s://%s:%s/%s";
    String url = String.format(template, PROTOCOL, hostAddress, PORT, path);
    Map<String, String> args = null;
    if (op.compareToIgnoreCase(PythonDepsFacade.CondaOp.CLONE.toString())
            == 0) {
      args = new HashMap<>();
      if (arg == null || arg.isEmpty()) {
        throw new RuntimeException(
                "You forgot the 'srcProject' argument for the conda "
                + "clone environment command for project " + project);
      }
      args.put("srcproj", arg);
    }
    Response response = getWebResource(url, agentPassword, args);
    int code = response.getStatus();
    Family res = Response.Status.Family.familyOf(code);
    if (res == Response.Status.Family.SUCCESSFUL) {
      return response.getEntity();
    }
    throw new RuntimeException("Error. Failed to execute anaconda command " + op
            + " on " + project + ". Result was: " + res);
  }

  public Object conda(String hostAddress, String agentPassword, String op,
          String project, String channel, String lib, String version) throws
          Exception {

    String template = "%s://%s:%s/%s";
    String channelEscaped = StringEscapeUtils.escapeJava(channel);
    String path = "conda/" + settings.getAdamUser() + '/' + op.toLowerCase()
            + "/" + project + "/" + lib;

    String url = String.format(template, PROTOCOL, hostAddress, PORT, path);

    Map<String, String> args = new HashMap<>();

    if (!channel.isEmpty()) {
      args.put("channelurl", channelEscaped);
    }
    if (!version.isEmpty()) {
      args.put("version", version);
    }

    Response response = getWebResource(url, agentPassword, args);
    int code = response.getStatus();
    Family res = Response.Status.Family.familyOf(code);
    if (res == Response.Status.Family.SUCCESSFUL) {
      return response.getEntity();
    }
    throw new RuntimeException("Error. Failed to execute conda command " + op
            + " on " + project + ". Result was: " + res);
  }

}
