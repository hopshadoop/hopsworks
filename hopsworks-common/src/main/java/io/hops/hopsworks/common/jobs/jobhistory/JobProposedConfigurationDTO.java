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

package io.hops.hopsworks.common.jobs.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JobProposedConfigurationDTO implements Serializable {

  private String configType = "";
  private String message = "";
  private int amMemory;
  private int amVcores;
  private int numOfExecutors;
  private int executorCores;
  private int executorMemory;
  private String estimatedExecutionTime = "";

  public JobProposedConfigurationDTO() {
  }

  public JobProposedConfigurationDTO(String configType, String message,
          int amMemory, int amVcores, int numOfExecutors,
          int executorCores, int executorMemory) {
    this.configType = configType;
    this.message = message;
    this.amMemory = amMemory;
    this.amVcores = amVcores;
    this.numOfExecutors = numOfExecutors;
    this.executorCores = executorCores;
    this.executorMemory = executorMemory;
  }

  /**
   * @return the configType
   */
  public String getConfigType() {
    return configType;
  }

  /**
   * @param configType the configType to set
   */
  public void setConfigType(String configType) {
    this.configType = configType;
  }

  /**
   * @return the amMemory
   */
  public int getAmMemory() {
    return amMemory;
  }

  /**
   * @param amMemory the amMemory to set
   */
  public void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }

  /**
   * @return the amVcores
   */
  public int getAmVcores() {
    return amVcores;
  }

  /**
   * @param amVcores the amVcores to set
   */
  public void setAmVcores(int amVcores) {
    this.amVcores = amVcores;
  }

  /**
   * @return the numOfExecutors
   */
  public int getNumOfExecutors() {
    return numOfExecutors;
  }

  /**
   * @param numOfExecutors the numOfExecutors to set
   */
  public void setNumOfExecutors(int numOfExecutors) {
    this.numOfExecutors = numOfExecutors;
  }

  /**
   * @return the executorCores
   */
  public int getExecutorCores() {
    return executorCores;
  }

  /**
   * @param executorCores the executorCores to set
   */
  public void setExecutorCores(int executorCores) {
    this.executorCores = executorCores;
  }

  /**
   * @return the executorMemory
   */
  public int getExecutorMemory() {
    return executorMemory;
  }

  /**
   * @param executorMemory the executorMemory to set
   */
  public void setExecutorMemory(int executorMemory) {
    this.executorMemory = executorMemory;
  }

  /**
   * @return the estimatedExecutionTime
   */
  public String getEstimatedExecutionTime() {
    return estimatedExecutionTime;
  }

  /**
   * @param estimatedExecutionTime the estimatedExecutionTime to set
   */
  public void setEstimatedExecutionTime(String estimatedExecutionTime) {
    this.estimatedExecutionTime = estimatedExecutionTime;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }

}
