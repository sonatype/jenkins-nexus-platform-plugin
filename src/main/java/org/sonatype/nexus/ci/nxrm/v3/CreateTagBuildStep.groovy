package org.sonatype.nexus.ci.nxrm.v3

import java.lang.reflect.Type

import javax.annotation.Nonnull

import com.sonatype.nexus.api.exception.RepositoryManagerException

import org.sonatype.nexus.ci.config.NxrmVersion
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.NxrmUtil

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
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
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

import static com.sonatype.nexus.api.common.NexusStringUtils.isBlank
import static hudson.model.Result.FAILURE
import static hudson.util.FormValidation.error
import static hudson.util.FormValidation.ok
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_DisplayName
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_TagAttributesJson
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_TagAttributesPath
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagAttributesJson
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagNameRequired
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

class CreateTagBuildStep
    extends Builder
    implements SimpleBuildStep
{
  private static final Gson GSON = new Gson()

  private static final Type ATTRIBUTE_TYPE = new TypeToken<Map<String, Object>>() {}.getType()

  final String nexusInstanceId

  final String tagName

  String tagAttributesPath

  String tagAttributesJson

  @DataBoundConstructor
  CreateTagBuildStep(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = nexusInstanceId
    this.tagName = tagName
  }

  @DataBoundSetter
  void setTagAttributesPath(final String tagAttributesPath) {
    this.tagAttributesPath = tagAttributesPath
  }

  @DataBoundSetter
  void setTagAttributesJson(final String tagAttributesJson) {
    this.tagAttributesJson = tagAttributesJson
  }

  @Override
  void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
               @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    def log = listener.getLogger()
    def env = run.getEnvironment(listener)
    def client
    def tagAttributes = (tagAttributesPath || tagAttributesJson) ? [:] : null

    try {
      client = nexus3Client(nexusInstanceId)
    }
    catch (RepositoryManagerException e) {
      failBuildAndThrow(run, log, e.message, e)
      return
    }


    if (tagAttributesPath) {
      try {
        FilePath attributesPath = new FilePath(workspace, env.expand(tagAttributesPath))
        Map<String, Object> readAttributes = GSON.
            fromJson(new JsonReader(new InputStreamReader(attributesPath.read())), ATTRIBUTE_TYPE)
        tagAttributes << readAttributes
      }
      catch (Exception e) {
        failBuildAndThrow(run, log, CreateTag_Error_TagAttributesPath(), e)
      }
    }

    if (tagAttributesJson) {
      try {
        Map<String, Object> parsedAttributes = GSON.fromJson(tagAttributesJson, ATTRIBUTE_TYPE)
        tagAttributes << parsedAttributes
      }
      catch (Exception e) {
        failBuildAndThrow(run, log, CreateTag_Error_TagAttributesJson(), e)
        return
      }
    }

    try {
      client.createTag(tagName, tagAttributes)
    }
    catch (RepositoryManagerException e) {
      failBuild(run, log, e.responseMessage.orElse(e.message))
    }
  }

  private void failBuildAndThrow(Run run, PrintStream log, String reason, Throwable throwable) {
    failBuild(run, log, reason)
    throw throwable
  }

  private void failBuild(Run run, PrintStream log, String reason) {
    log.println("Failing build due to: ${reason}")
    run.setResult(FAILURE)
  }

  @Extension
  static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    String getDisplayName() {
      CreateTag_DisplayName()
    }

    @Override
    boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      true
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      FormUtil.validateNotEmpty(tagName, CreateTag_Validation_TagNameRequired())
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      NxrmUtil.doCheckNexusInstanceId(value)
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3)
    }

    FormValidation doCheckTagAttributesJson(@QueryParameter String tagAttributesJson) {
      if (!isBlank(tagAttributesJson)) {
        try {
          GSON.fromJson(tagAttributesJson, ATTRIBUTE_TYPE)
        }
        catch (Exception e) {
          return error(CreateTag_Validation_TagAttributesJson())
        }
      }
      return ok()
    }
  }
}
