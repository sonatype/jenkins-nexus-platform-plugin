/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.nxrm

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.v3.freestyle.NexusStagingMoveBuildStep
import org.sonatype.nexus.ci.nxrm.v3.pipeline.NexusStagingMoveExecution
import org.sonatype.nexus.ci.nxrm.v3.pipeline.NexusStagingMoveWorkflowStep

import groovy.transform.PackageScope
import hudson.EnvVars
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.util.FormValidation.Kind

import static com.sonatype.nexus.api.common.ArgumentUtils.checkArgument
import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.getGlobalNexusConfiguration
import static org.sonatype.nexus.ci.util.FormUtil.validateUrl
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

class ComponentStaging
{
  protected final NxrmConfiguration nxrmConfiguration

  protected final Run run

  protected final PrintStream logger

  protected final EnvVars envVars

  ComponentStaging(final NxrmConfiguration nxrmConfiguration, final Run run, final TaskListener taskListener)
  {
    this.nxrmConfiguration = nxrmConfiguration
    this.run = run
    this.logger = taskListener.getLogger()
    this.envVars = run.getEnvironment(taskListener)
  }

  void moveComponents(final NexusStagingMoveBuildStep stagingMoveBuildStep) {
    logger.println("Moving components associated with tag: ${stagingMoveBuildStep.tagName} " +
        "in Nexus Instance: ${stagingMoveBuildStep.nexusInstanceId} " +
        "to repository: ${stagingMoveBuildStep.destinationRepository}")

    try {
      getRepositoryManagerClient(nxrmConfiguration)
          .move(stagingMoveBuildStep.destinationRepository, stagingMoveBuildStep.tagName)
    }
    catch (RepositoryManagerException ex) {
      final String moveFailed = 'Move of components associated with tag: ' + "${stagingMoveBuildStep.tagName}" +
          'in repository ' + "${stagingMoveBuildStep.nexusInstanceId}" + 'to repository: ' +
          "${stagingMoveBuildStep.destinationRepository}" + ' failed'

      logger.println(moveFailed)
      logger.println('Failing build due to failure to move components to ' +
          "${stagingMoveBuildStep.destinationRepository}")
      run.setResult(Result.FAILURE)
      throw new IOException(moveFailed, ex)
    }
  }

  void moveComponents(final NexusStagingMoveExecution stagingMoveExecution) {
    final NexusStagingMoveWorkflowStep stagingMoveWorkflowStep = stagingMoveExecution.stagingMoveWorkflow;
    logger.println("Moving components associated with tag: ${stagingMoveWorkflowStep.tagName} " +
        "in Nexus Instance: ${stagingMoveWorkflowStep.nexusInstanceId} " +
        "to repository: ${stagingMoveWorkflowStep.destinationRepository}")


    try {
      //build the url
      getRepositoryManagerClient(nxrmConfiguration)
          .move(stagingMoveWorkflowStep.destinationRepository, stagingMoveWorkflowStep.tagName)
    }
    catch (RepositoryManagerException ex) {
      final String moveFailed = 'Move of components associated with tag: ' + "${stagingMoveWorkflowStep.tagName}" +
          'in repository ' + "${stagingMoveWorkflowStep.nexusInstanceId}" + 'to repository: ' +
          "${stagingMoveWorkflowStep.destinationRepository}" + ' failed'

      logger.println(moveFailed)
      logger.println('Failing build due to failure to move components to ' +
          "${stagingMoveWorkflowStep.destinationRepository}")
      run.setResult(FAILURE)
      throw new IOException(moveFailed, ex)
    }

  }

  static ComponentStaging getComponentStaging(final String nexusInstanceId, final Run run, final TaskListener listener) {
    def logger = listener.getLogger()
    def nxrmConfig = globalNexusConfiguration.nxrmConfigs.find { it.id == nexusInstanceId }

    if (!nxrmConfig) {
      failRun(run, logger, "Nexus Configuration ${nexusInstanceId} not found.")
    }

    if (validateUrl(nxrmConfig.serverUrl).kind == Kind.ERROR) {
      failRun(run, logger, "Nexus Server URL ${nxrmConfig.serverUrl} is invalid.")
    }

    return new ComponentStaging(nxrmConfig, run, listener)
  }

  private static void failRun(final Run run, final PrintStream logger, final String failMsg) {
    logger.println("Failing build due to: ${failMsg}")
    run.setResult(FAILURE)
    throw new IllegalArgumentException(failMsg)
  }

  @PackageScope
  RepositoryManagerV3Client getRepositoryManagerClient(final NxrmConfiguration nxrmConfiguration) {
    try {
      checkArgument(nxrmConfiguration.class == Nxrm3Configuration.class,
          'Nexus Repository Manager 3.x server is required')
      Nxrm3Configuration nxrm3Configuration = nxrmConfiguration as Nxrm3Configuration
      nexus3Client(nxrm3Configuration.serverUrl, nxrm3Configuration.credentialsId, nxrm3Configuration.anonymousAccess)
    }
    catch (Exception e) {
      logger.println('Error creating RepositoryManagerClient')
      logger.println('Failing build due to error creating RepositoryManagerClient')
      run.setResult(Result.FAILURE)
      throw e
    }
  }
}
