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
package org.sonatype.nexus.ci.github;

class GitHubNotifier
{
  final GitHubClient gitHubClient
  final String repository
  final String commitHash

  final static String CONTEXT = 'continuous-integration/lifecycle'
  final static String BEGIN_DESCRIPTION = 'Nexus Lifecycle Analysis Running'
  final static String SUCCESS_DESCRIPTION = 'Nexus Lifecycle Analysis Succeeded'
  final static String FAILURE_DESCRIPTION = 'Nexus Lifecycle Analysis Failed'

  GitHubNotifier(final GitHubClient gitHubClient, final String repository, final String commitHash) {
    this.gitHubClient = gitHubClient
    this.repository = repository
    this.commitHash = commitHash
  }

  void onAnalysisBegun() {
    gitHubClient.updateStatus(repository, commitHash, BEGIN_DESCRIPTION, CONTEXT, GitHubStatus.PENDING, null)
  }

  void onAnalysisSuccess(String reportUrl) {
    gitHubClient.updateStatus(repository, commitHash, SUCCESS_DESCRIPTION, CONTEXT, GitHubStatus.SUCCESS, reportUrl)
  }

  void onAnalysisFailure(String reportUrl) {
    gitHubClient.updateStatus(repository, commitHash, FAILURE_DESCRIPTION, CONTEXT, GitHubStatus.FAILURE, reportUrl)
  }
}
