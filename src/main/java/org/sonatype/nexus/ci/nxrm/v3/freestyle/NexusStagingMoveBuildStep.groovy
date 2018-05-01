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

import org.sonatype.nexus.ci.nxrm.NexusStaging
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


import static org.sonatype.nexus.ci.nxrm.ComponentStaging.getComponentStaging

class NexusStagingMoveBuildStep
    extends Builder
    implements NexusStaging, SimpleBuildStep
{
  final String nexusInstanceId

  final String tagName

  final String destinationRepository

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
