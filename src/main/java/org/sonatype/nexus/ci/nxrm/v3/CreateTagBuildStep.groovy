package org.sonatype.nexus.ci.nxrm.v3

import java.lang.reflect.Type

import javax.annotation.Nonnull
import javax.annotation.Nullable

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

class CreateTagBuildStep
    extends Builder
    implements SimpleBuildStep
{
  private static final Type ATTRIBUTE_TYPE = new TypeToken<Map<String, Object>>() {}.getType()

  String nexusInstanceId

  String tagName

  String tagAttributesJson

  @DataBoundConstructor
  CreateTagBuildStep(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    def log = listener.getLogger()
    def client
    def tagAttributes

    try {
      client = RepositoryManagerClientUtil.nexus3Client(nexusInstanceId)
    }
    catch (RepositoryManagerException e) {
      failBuild(run, log, e.message, e)
    }

    try {
      tagAttributes = new Gson().fromJson(tagAttributesJson, ATTRIBUTE_TYPE)
    }
    catch (Exception e) {
      failBuild(run, log, 'invalid tag attributes', e)
    }

    try {
      client.createTag(tagName, tagAttributes)
    }
    catch (RepositoryManagerException e) {
      failBuild(run, log, e.responseMessage.orElse(e.message))
    }
  }

  private void failBuild(Run run, PrintStream log, String reason, @Nullable Exception illegalArgCause) {
    log.println("Failing build due to: ${reason}")
    run.setResult(FAILURE)
    if (illegalArgCause) {
      throw new IllegalArgumentException(reason, illegalArgCause)
    }
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      "Create Tag (Nexus Repository Manager 3.x)"
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      FormUtil.validateNotEmpty(tagName)
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3)
    }

    FormValidation doCheckNexusRepositoryId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusRepositoryId(value)
    }

    ListBoxModel doFillNexusRepositoryIdItems(@QueryParameter String nexusInstanceId) {
      NxrmUtil.doFillNexusRepositoryIdItems(nexusInstanceId)
    }
  }
}
