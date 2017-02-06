/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.nexus.ci.iq

import javax.annotation.Nullable

import com.sonatype.nexus.ci.config.NxiqConfiguration
import com.sonatype.nexus.ci.util.FormUtil
import com.sonatype.nexus.ci.util.IqUtil

import hudson.Extension
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

class IqPolicyEvaluatorWorkflowStep
    extends AbstractStepImpl
    implements IqPolicyEvaluator
{

  @DataBoundConstructor
  IqPolicyEvaluatorWorkflowStep(final String iqStage,
                                final String iqApplication,
                                final List<ScanPattern> iqScanPatterns,
                                final Boolean failBuildOnNetworkError,
                                final String jobCredentialsId)
  {
    this.jobCredentialsId = !NxiqConfiguration.isPkiAuthentication ? jobCredentialsId : null
    this.failBuildOnNetworkError = failBuildOnNetworkError
    this.iqScanPatterns = iqScanPatterns
    this.iqApplication = iqApplication
    this.iqStage = iqStage
  }

  @Extension
  static final class DescriptorImpl
      extends AbstractStepDescriptorImpl
      implements IqPolicyEvaluatorDescriptor
  {
    DescriptorImpl() {
      super(PolicyEvaluatorExecution.class)
    }

    @Override
    String getFunctionName() {
      Messages.IqPolicyEvaluation_FunctionName()
    }

    @Override
    String getDisplayName() {
      Messages.IqPolicyEvaluation_DisplayName()
    }

    @Override
    FormValidation doCheckIqStage(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqStageItems(@QueryParameter @Nullable String jobCredentialsId) {
      IqUtil.doFillIqStageItems(jobCredentialsId)
    }

    @Override
    FormValidation doCheckIqApplication(@QueryParameter String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillIqApplicationItems(@QueryParameter @Nullable String jobCredentialsId) {
      IqUtil.doFillIqApplicationItems(jobCredentialsId)
    }

    @Override
    FormValidation doCheckScanPattern(@QueryParameter final String scanPattern) {
      FormValidation.ok()
    }

    @Override
    FormValidation doCheckFailBuildOnNetworkError(@QueryParameter final String value) {
      FormValidation.validateRequired(value)
    }

    @Override
    ListBoxModel doFillJobCredentialsIdItems() {
      FormUtil.buildCredentialsItems(NxiqConfiguration.serverUrl.toString(), NxiqConfiguration.credentialsId)
    }
  }
}
