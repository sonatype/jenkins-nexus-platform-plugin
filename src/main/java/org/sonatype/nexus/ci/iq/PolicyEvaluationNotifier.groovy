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
package org.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

import org.sonatype.nexus.ci.config.GitHubConfiguration
import org.sonatype.nexus.ci.github.GitHubClientConfiguration
import org.sonatype.nexus.ci.github.GitHubClientFactory
import org.sonatype.nexus.ci.github.GitHubNotifier

import hudson.EnvVars
import hudson.model.Run

class PolicyEvaluationNotifier
{
  private final Run run
  private final GitHubNotifier gitHubNotifier

  PolicyEvaluationNotifier(final Run run,
                           final EnvVars envVars,
                           final gitHubJobSpecificCredentialsId) {
    this.run = run

    if (GitHubConfiguration.gitHubConfig) {
      def gitCommit = envVars['GIT_COMMIT']
      def gitUrl = envVars['GIT_URL']
      def gitRepository = (gitUrl =~ /github.com[\/:](.*)/)[0][1]

      def gitHubClient = GitHubClientFactory.getGitHubClient(
          new GitHubClientConfiguration(credentialsId: gitHubJobSpecificCredentialsId, context: run.parent))
      gitHubNotifier = new GitHubNotifier(gitHubClient, gitRepository, gitCommit)
    } else {
      gitHubNotifier = null
    }
  }

  void notifyEvaluationBegun() {
    if (gitHubNotifier) {
      gitHubNotifier.onAnalysisBegun()
    }
  }

  void notifyEvaluationSuccess(ApplicationPolicyEvaluation policyEvaluation) {
    addHealthAction(policyEvaluation)
    if (gitHubNotifier) {
      gitHubNotifier.onAnalysisSuccess(policyEvaluation.applicationCompositionReportUrl)
    }

  }

  void notifyEvaluationFailure(ApplicationPolicyEvaluation policyEvaluation) {
    addHealthAction(policyEvaluation)
    if (gitHubNotifier) {
      gitHubNotifier.onAnalysisFailure(policyEvaluation.applicationCompositionReportUrl)
    }
  }

  private void addHealthAction(ApplicationPolicyEvaluation policyEvaluation) {
    def healthAction = new PolicyEvaluationHealthAction(run, policyEvaluation)
    run.addAction(healthAction)
  }
}
