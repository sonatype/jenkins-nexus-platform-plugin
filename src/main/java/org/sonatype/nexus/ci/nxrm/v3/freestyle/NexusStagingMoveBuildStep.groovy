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
package org.sonatype.nexus.ci.nxrm.v3.freestyle

import javax.annotation.Nonnull

import org.sonatype.nexus.ci.nxrm.ComponentStaging
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
import hudson.util.FormValidation.Kind
import hudson.util.ListBoxModel
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.getGlobalNexusConfiguration
import static org.sonatype.nexus.ci.util.FormUtil.validateUrl

class NexusStagingMoveBuildStep
    extends Builder
    implements SimpleBuildStep
{
  String nexusInstanceId

  String tagName

  String destinationRepository

  @DataBoundConstructor
  NexusStagingMoveBuildStep(final String nexusInstanceId, final String tagName, final String destinationRepository) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
    this.destinationRepository = destinationRepository
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    getComponentStaging(nexusInstanceId, run, listener).moveComponents(this)
  }

  ComponentStaging getComponentStaging(final String nexusInstanceId, final Run run, final TaskListener listener) {
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

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      'Nexus Staging Component Move'
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems()
    }

    FormValidation doCheckDestinationRepository(@QueryParameter String value) {
      NxrmUtil.doCheckNexusRepositoryId(value)
    }

    ListBoxModel doFillDestinationRepositoryItems(@QueryParameter String nexusInstanceId) {
      NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }
  }
}
