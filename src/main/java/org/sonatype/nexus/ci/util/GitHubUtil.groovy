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
package org.sonatype.nexus.ci.util

import org.sonatype.nexus.ci.config.Messages
import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.github.GitHubClient
import org.sonatype.nexus.ci.github.GitHubClientConfiguration
import org.sonatype.nexus.ci.github.GitHubClientFactory

import groovyx.net.http.HttpResponseException
import hudson.model.ModelObject
import hudson.util.FormValidation

class GitHubUtil
{
  static FormValidation verifyJobCredentials(final String jobCredentialsId, final ModelObject context) {
    return verifyJobCredentials(NxiqConfiguration.serverUrl.toString(), jobCredentialsId, context)
  }

  static FormValidation verifyJobCredentials(final String serverUrl,
                                             final String jobCredentialsId,
                                             final ModelObject context) {
    try {
      GitHubClient gitHubClient = GitHubClientFactory.getGitHubClient(
          new GitHubClientConfiguration(credentialsId: jobCredentialsId, context: context,
              serverUrl: URI.create(serverUrl)))
      def user = gitHubClient.getUser()

      return FormValidation.ok(Messages.GitHubConfiguration_ConnectionSucceeded(user.login))
    }
    catch (HttpResponseException e) {
      return FormValidation.error(e, Messages.GitHubConfiguration_ConnectionFailed())
    }
  }
}
