package org.sonatype.nexus.ci.nxrm.v3;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;

import org.sonatype.nexus.ci.config.NxrmVersion;
import org.sonatype.nexus.ci.util.NxrmUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import static com.sonatype.nexus.api.common.NexusStringUtils.isBlank;
import static com.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static hudson.model.Result.FAILURE;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_DisplayName;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_TagAttributesJson;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Error_TagAttributesPath;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagAttributesJson;
import static org.sonatype.nexus.ci.nxrm.Messages.CreateTag_Validation_TagNameRequired;
import static org.sonatype.nexus.ci.util.FormUtil.validateNotEmpty;
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client;

public class CreateTagBuilder
    extends Builder
    implements SimpleBuildStep
{
  static final Gson GSON = new Gson();

  static final Type ATTRIBUTE_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

  private final String nexusInstanceId;

  private final String tagName;

  private String tagAttributesPath;

  private String tagAttributesJson;

  @DataBoundConstructor
  public CreateTagBuilder(final String nexusInstanceId, final String tagName) {
    this.nexusInstanceId = nexusInstanceId;
    this.tagName = tagName;
  }

  @DataBoundSetter
  public void setTagAttributesPath(final String tagAttributesPath) {
    this.tagAttributesPath = tagAttributesPath;
  }

  @DataBoundSetter
  public void setTagAttributesJson(final String tagAttributesJson) {
    this.tagAttributesJson = tagAttributesJson;
  }

  @Override
  public void perform(@Nonnull final Run run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher,
                      @Nonnull final TaskListener listener) throws InterruptedException, IOException
  {
    RepositoryManagerV3Client client = null;
    Map<String, Object> tagAttributes = (isNotBlank(tagAttributesPath) ||
        isNotBlank(tagAttributesJson)) ? new HashMap<>() : null;
    EnvVars env = run.getEnvironment(listener);

    try {
      client = nexus3Client(nexusInstanceId);
    }
    catch (RepositoryManagerException e) {
      e.getResponseMessage();
      failBuildAndThrow(run, listener, e.getResponseMessage().orElse(e.getMessage()), new IOException(e));
    }

    if (isNotBlank(tagAttributesPath)) {
      try {
        tagAttributes.putAll(parseAttributes(workspace, tagAttributesPath, env));
      }
      catch (Exception e) {
        failBuildAndThrow(run, listener, CreateTag_Error_TagAttributesPath(), new IOException(e));
      }
    }

    if (isNotBlank(tagAttributesJson)) {
      try {
        tagAttributes.putAll(parseAttributes(tagAttributesJson));
      }
      catch (Exception e) {
        failBuildAndThrow(run, listener, CreateTag_Error_TagAttributesJson(), new IOException(e));
      }
    }

    try {
      client.createTag(tagName, tagAttributes);
    }
    catch (RepositoryManagerException e) {
      failBuildAndThrow(run, listener, e.getResponseMessage().orElse(e.getMessage()), new IOException(e));
    }
  }

  private Map<String, Object> parseAttributes(final FilePath workspace, final String filePath, final EnvVars env)
      throws IOException, InterruptedException
  {
    FilePath attributesPath = new FilePath(workspace, env.expand(filePath));
    return parseAttributes(attributesPath.readToString());
  }

  private Map<String, Object> parseAttributes(String json) {
    return GSON.fromJson(json, ATTRIBUTE_TYPE);
  }

  private void failBuild(Run run, TaskListener listener, String reason) {
    listener.getLogger().println("Failing build due to: " + reason);
    run.setResult(FAILURE);
  }

  private void failBuildAndThrow(Run run, TaskListener listener, String reason, IOException ioException)
      throws IOException
  {
    failBuild(run, listener, reason);
    throw ioException;
  }

  @Extension
  @Symbol("createTag")
  public static final class DescriptorImpl
      extends BuildStepDescriptor<Builder>
  {
    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return CreateTag_DisplayName();
    }

    FormValidation doCheckTagName(@QueryParameter String tagName) {
      return validateNotEmpty(tagName, CreateTag_Validation_TagNameRequired());
    }

    FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
      return NxrmUtil.doCheckNexusInstanceId(value);
    }

    ListBoxModel doFillNexusInstanceIdItems() {
      return NxrmUtil.doFillNexusInstanceIdItems(NxrmVersion.NEXUS_3);
    }

    FormValidation doCheckTagAttributesJson(@QueryParameter String tagAttributesJson) {
      if (!isBlank(tagAttributesJson)) {
        try {
          return GSON.fromJson(tagAttributesJson, ATTRIBUTE_TYPE);
        }
        catch (Exception e) {
          return error(CreateTag_Validation_TagAttributesJson());
        }
      }

      return ok();
    }
  }
}
