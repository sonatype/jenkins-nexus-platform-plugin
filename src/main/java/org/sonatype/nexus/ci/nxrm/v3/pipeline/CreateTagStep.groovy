package org.sonatype.nexus.ci.nxrm.v3.pipeline

import java.lang.reflect.Type

import javax.annotation.Nonnull
import javax.annotation.Nullable

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil

import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank
import static hudson.model.Result.FAILURE
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_DisplayName
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_InvalidTagAttributes
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_FunctionName
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagNameRequired
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

class CreateTagStep
    extends Step
{
  final String nexusInstanceId

  final String tagName

  String tagAttributesJson

  @DataBoundConstructor
  CreateTagStep(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
  }

  @DataBoundSetter
  void setTagAttributesJson(final String tagAttributesJson) {
    this.tagAttributesJson = tagAttributesJson
  }

  @Override
  StepExecution start(final StepContext context) throws Exception {
    new CreateTagStepExecution(tagName, tagAttributesJson, nexusInstanceId, context)
  }

  @Extension
  static class DescriptorImpl
      extends StepDescriptor
  {
    @Override
    Set<? extends Class<?>> getRequiredContext() {
      return ImmutableSet.of(Run.class, TaskListener.class)
    }

    @Override
    String getFunctionName() {
      CreateTag_FunctionName()
    }

    @Override
    String getDisplayName() {
      CreateTag_DisplayName()
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3)
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      FormUtil.validateNotEmpty(tagName, CreateTag_Validation_TagNameRequired())
    }
  }

  static class CreateTagStepExecution
      extends StepExecution
  {
    private static final Type ATTRIBUTE_TYPE = new TypeToken<Map<String, Object>>() {}.getType()

    private String tagName

    private String tagAttributesJson

    private String nexusInstanceId

    CreateTagStepExecution(final String tagName, @Nullable final String tagAttributesJson, final String nexusInstanceId,
                           final StepContext context)
    {
      super(context)
      this.tagName = tagName
      this.tagAttributesJson = tagAttributesJson
      this.nexusInstanceId = nexusInstanceId
    }

    @Override
    boolean start() throws Exception {
      def log = context.get(TaskListener.class).getLogger()
      def run = context.get(Run.class)
      def client
      def tagAttributes

      try {
        client = nexus3Client(nexusInstanceId)
      }
      catch (RepositoryManagerException e) {
        return failBuild(run, log, e.message, new IllegalArgumentException(e))
      }

      try {
        tagAttributes = isNotBlank(tagAttributesJson) ? new Gson().fromJson(tagAttributesJson, ATTRIBUTE_TYPE) : null
      }
      catch (Exception e) {
        return failBuild(run, log, CreateTag_Error_InvalidTagAttributes(), new IllegalArgumentException(e))
      }

      try {
        context.onSuccess(client.createTag(tagName, tagAttributes))
        return true
      }
      catch (RepositoryManagerException e) {
        return failBuild(run, log, e.responseMessage.orElse(e.message), e)
      }
    }

    @Override
    void stop(@Nonnull final Throwable cause) throws Exception {
      // noop (synchronous step)
    }

    private boolean failBuild(Run run, PrintStream log, String reason, Throwable throwable) {
      log.println("Failing build due to: ${reason}")
      run.setResult(FAILURE)
      context.onFailure(throwable)
      return true
    }

  }
}
