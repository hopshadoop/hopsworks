/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.spark;

import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.elasticsearch.common.Strings;

/**
 * Orchestrates the execution of a Spark job: run job, update history object.
 * <p>
 */
public class SparkJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(SparkJob.class.getName());
  protected SparkYarnRunnerBuilder runnerbuilder;

  public SparkJob(Jobs job, AsynchronousJobExecutor services,
      Users user, final String hadoopDir,
      String jobUser, YarnJobsMonitor jobsMonitor, Settings settings) {
    super(job, services, user, jobUser, hadoopDir, jobsMonitor, settings);
    if (!(job.getJobConfig() instanceof SparkJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a SparkJobConfiguration object. Received: "
          + job.getJobConfig().getClass());
    }
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) {
    super.setupJob(dfso, yarnClient);
    SparkJobConfiguration jobconfig = (SparkJobConfiguration) jobs.getJobConfig();
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    //If runnerbuilder is not null, it has been instantiated by child class,
    //i.e. AdamJob
    if (runnerbuilder == null) {
      runnerbuilder = new SparkYarnRunnerBuilder(jobs);
      runnerbuilder.setJobName(jobconfig.getAppName());
      //Check if the user provided application arguments
      if (jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()) {
        String[] jobArgs = jobconfig.getArgs().trim().split(" ");
        runnerbuilder.addAllJobArgs(jobArgs);
      }
    }

    if(!Strings.isNullOrEmpty(jobconfig.getProperties())){
      runnerbuilder.setProperties(jobconfig.getProperties());
    }
    //Set spark runner options
    runnerbuilder.setExecutorCores(jobconfig.getExecutorCores());
    runnerbuilder.setExecutorMemory("" + jobconfig.getExecutorMemory() + "m");
    runnerbuilder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
    if (jobconfig.isDynamicExecutors()) {
      runnerbuilder.setDynamicExecutors(jobconfig.isDynamicExecutors());
      runnerbuilder.setNumberOfExecutorsMin(jobconfig.getSelectedMinExecutors());
      runnerbuilder.setNumberOfExecutorsMax(jobconfig.getSelectedMaxExecutors());
      runnerbuilder.setNumberOfExecutorsInit(jobconfig.
          getNumberOfExecutorsInit());
    }
    //Set Yarn running options
    runnerbuilder.setDriverMemoryMB(jobconfig.getAmMemory());
    runnerbuilder.setDriverCores(jobconfig.getAmVCores());
    runnerbuilder.setDriverQueue(jobconfig.getAmQueue());

    //Set TFSPARK params
    runnerbuilder.setNumOfGPUs(jobconfig.getNumOfGPUs());
    runnerbuilder.setNumOfPs(jobconfig.getNumOfPs());
    //Set Kafka params
    runnerbuilder.setServiceProps(serviceProps);
    runnerbuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources, i.e. Kafka certificates
    runnerbuilder.addExtraFiles(projectLocalResources);
    if (jobSystemProperties != null && !jobSystemProperties.isEmpty()) {
      for (Entry<String, String> jobSystemProperty : jobSystemProperties.
          entrySet()) {
        runnerbuilder.addSystemProperty(jobSystemProperty.getKey(),
            jobSystemProperty.getValue());
      }
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(jobs.getProject().getName())
        + Settings.SPARK_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(jobs.getProject().getName())
        + Settings.SPARK_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);

    try {
      runner = runnerbuilder.
          getYarnRunner(jobs.getProject().getName(),
              jobUser, services, services.getFileOperations(hdfsUser.getUserName()), yarnClient, settings);

    } catch (IOException e) {
      LOG.log(Level.WARNING,
          "Failed to create YarnRunner.", e);
      try {
        writeToLogs(e.getLocalizedMessage());
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Failed to write logs for failed application.", ex);
      }
      return false;
    }

    return true;
  }

  @Override
  protected void cleanup() {
    LOG.log(Level.INFO, "Job finished performing cleanup...");
    if (monitor != null) {
      monitor.close();
      monitor = null;
    }
  }

  @Override
  protected void stopJob(String appid) {
    super.stopJob(appid);
  }

}
