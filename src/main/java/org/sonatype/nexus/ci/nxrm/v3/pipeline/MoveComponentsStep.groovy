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
package org.sonatype.nexus.ci.nxrm.v3.pipeline

import javax.annotation.Nonnull

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil

import com.google.common.collect.ImmutableSet
import hudson.Extension
import hudson.model.Run
import hudson.model.TaskListener
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepDescriptor
import org.jenkinsci.plugins.workflow.steps.StepExecution
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponentsBuildStep_DisplayName
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponentsBuildStep_FunctionName
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponentsBuildStep_Validation_TagNameRequired
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

class MoveComponentsStep
    extends Step

{
  final String nexusInstanceId

  final String tagName

  final String destinationRepository

  @DataBoundConstructor
  MoveComponentsStep(final String nexusInstanceId, final String tagName, final String destinationRepository) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
    this.destinationRepository = destinationRepository
  }

  @Override
  StepExecution start(final StepContext context) throws Exception {
    new NexusStagingMoveWorkflowStepExecution(tagName, destinationRepository, nexusInstanceId, context)
  }

  @Extension
  static final class DescriptorImpl
      extends StepDescriptor
  {
    @Override
    Set<? extends Class<?>> getRequiredContext() {
      return ImmutableSet.of(Run.class, TaskListener.class)
    }

    @Override
    String getFunctionName() {
      return MoveComponentsBuildStep_FunctionName()
    }

    @Override
    String getDisplayName() {
      return MoveComponentsBuildStep_DisplayName()
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3)
    }

    FormValidation doCheckDestinationRepository(@QueryParameter String value) {
      NxrmUtil.doCheckNexusRepositoryId(value)
    }

    ListBoxModel doFillDestinationRepositoryItems(@QueryParameter String nexusInstanceId) {
      NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      FormUtil.validateNotEmpty(tagName, MoveComponentsBuildStep_Validation_TagNameRequired())
    }
  }

  static class NexusStagingMoveWorkflowStepExecution
      extends StepExecution
  {

    private String tagName

    private String destinationRepository

    private String nexusInstanceId

    NexusStagingMoveWorkflowStepExecution(final String tagName, final String destinationRepository,
                                          final String nexusInstanceId, final StepContext stepContext) {
      super(stepContext)
      this.tagName = tagName
      this.destinationRepository = destinationRepository
      this.nexusInstanceId = nexusInstanceId
    }

    @Override
    boolean start() throws Exception {
      def log = context.get(TaskListener.class).getLogger()
      def run = context.get(Run.class)

      try {
        def client = nexus3Client(nexusInstanceId)
        context.onSuccess(client.move(destinationRepository, tagName))
        return true
      }
      catch (RepositoryManagerException e) {
        log.println("Failing build due to: ${e.responseMessage.orElse(e.message)}")
        run.setResult(FAILURE)
        context.onFailure(e)
        return false
      }
    }

    @Override
    void stop(@Nonnull final Throwable throwable) throws Exception {
      // noop (synchronous step)
    }
  }
}