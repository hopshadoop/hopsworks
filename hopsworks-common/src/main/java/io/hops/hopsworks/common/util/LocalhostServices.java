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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalhostServices {

  public static String createUserAccount(String username, String projectName,
          List<String> sshKeys) throws IOException {

    String user = getUsernameInProject(username, projectName);
    String home = Settings.HOPS_USERS_HOMEDIR + user;
    if (new File(home).exists()) {
      throw new IOException("Home directory already exists: " + home);
    }
    StringBuilder publicKeysAsString = new StringBuilder();
    for (String key : sshKeys) {
      publicKeysAsString.append(key).append(System.lineSeparator());
    }
    List<String> commands = new ArrayList<>();
    commands.add("/bin/bash");
    commands.add("-c");
    // Need to enclose public keys in quotes here.
    commands.add("sudo /srv/mkuser.sh " + user + " \"" + publicKeysAsString.
            toString() + "\"");

    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands, false);
    String stdout = "", stderr = "";
    try {
      int result = commandExecutor.executeCommand();
      // get the stdout and stderr from the command that was run
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result != 0) {
        throw new IOException("Could not create user: " + home + " - " + stderr);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new IOException("Interrupted. Could not create user: " + home
              + " - " + stderr);
    }

    return stdout;
  }

  public static String deleteUserAccount(String username, String projectName)
          throws IOException {
    // Run using a bash script the following with sudo '/usr/sbin/deluser johnny'

    String user = getUsernameInProject(username, projectName);
    String home = Settings.HOPS_USERS_HOMEDIR + user;

    if (new File(home).exists() == false) {
      throw new IOException("Home directory does not exist: " + home);
    }
    List<String> commands = new ArrayList<String>();
    commands.add("/bin/bash");
    commands.add("-c");
    commands.add("sudo /usr/sbin/deluser " + user);

    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands, false);
    String stdout = "", stderr = "";
    try {
      int result = commandExecutor.executeCommand();
      // get the stdout and stderr from the command that was run
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result != 0) {
        throw new IOException("Could not delete user " + home + " - " + stderr);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new IOException("Interrupted. Could not delete user: " + home
              + " - " + stderr);
    }
    return stdout;
  }

  @Deprecated
  public static String getUsernameInProject(String username, String projectName) {

    if (username.contains("@")) {
      throw new IllegalArgumentException("Email sent in - should be username");
    }

    return projectName + Settings.HOPS_USERNAME_SEPARATOR + username;
  }

  public static String unzipHdfsFile(String hdfsFile, String localFolder,
          String domainsDir) throws IOException {

    List<String> commands = new ArrayList<>();
    commands.add(domainsDir + "/bin/" + Settings.UNZIP_FILES_SCRIPTNAME);
    commands.add(hdfsFile);
    commands.add(localFolder);

    AsyncSystemCommandExecutor commandExecutor = new AsyncSystemCommandExecutor(
            commands);
    String stdout = "", stderr = "";
    try {
      int result = commandExecutor.executeCommand();
      // get the stdout and stderr from the command that was run
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result != 0) {
        throw new IOException(stderr);
      }
    } catch (InterruptedException e) {
      throw new IOException("Interrupted. Could not generate the certificates: "
              + stderr);
    }
    return stdout;
  }
  
  //Dela Certificates
  public static void generateHopsSiteKeystore(Settings settings, String userKeyPwd) throws IOException {
    List<String> commands = new ArrayList<>();
    commands.add("/usr/bin/sudo");
    commands.add(settings.getHopsSiteCaScript());
    commands.add(userKeyPwd);
    
    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands, false);
    String stdout = "", stderr = "";
    try {
      int result = commandExecutor.executeCommand();
      // get the stdout and stderr from the command that was run
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result != 0) {
        throw new IOException("stdout:" + stdout + "\nstderr:" + stderr);
      }
    } catch (InterruptedException e) {
      throw new IOException("Interrupted. Could not generate the certificates: " + stderr);
    }
  }
  //Dela Certificates end
}
