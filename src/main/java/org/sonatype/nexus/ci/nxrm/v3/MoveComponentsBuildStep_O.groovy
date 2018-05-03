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
package org.sonatype.nexus.ci.nxrm.v3

import javax.annotation.Nonnull

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponentsBuildStep_DisplayName
import static org.sonatype.nexus.ci.nxrm.Messages.MoveComponentsBuildStep_Validation_TagNameRequired
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

class MoveComponentsBuildStep_O
    extends Builder
    implements SimpleBuildStep
{
  final String nexusInstanceId

  final String tagName

  final String destinationRepository

  @DataBoundConstructor
  MoveComponentsBuildStep_O(final String nexusInstanceId, final String tagName, final String destinationRepository) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
    this.destinationRepository = destinationRepository
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    def log = listener.getLogger()

    try {
      def client = nexus3Client(nexusInstanceId)
      client.move(destinationRepository, tagName)
      run.setResult(SUCCESS)
    }
    catch (RepositoryManagerException e) {
      log.println("Failing build due to: ${e.responseMessage.orElse(e.message)}")
      run.setResult(FAILURE)
    }
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      MoveComponentsBuildStep_DisplayName()
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
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
}
