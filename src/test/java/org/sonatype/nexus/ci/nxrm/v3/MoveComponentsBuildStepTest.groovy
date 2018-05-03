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
package org.sonatype.nexus.ci.nxrm.v3

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.nxrm.v3.MoveComponentsBuildStep.DescriptorImpl
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import hudson.model.Run

import static hudson.model.Result.SUCCESS

class MoveComponentsBuildStepTest
    extends MoveComponentTestSupport
{

  def 'it successfully completes a move operation based on a tag'() {
    setup:
      RepositoryManagerV3Client nxrmClient = Mock(RepositoryManagerV3Client.class)
      def destinationRepository = 'maven-releases'
      def nexusInstanceId = "localInstance"
      def tagName = "foo"
      def nexusStagingMove = new MoveComponentsBuildStep(nexusInstanceId, tagName, destinationRepository)

      def project = jenkinsRule.createFreeStyleProject()
      project.getBuildersList().add(nexusStagingMove)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(nexusInstanceId) >> nxrmClient

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(SUCCESS, build)
  }

  def 'it fails to complete a move operation based on a tag'() {
    setup:
      RepositoryManagerV3Client nxrmClient = Mock(RepositoryManagerV3Client.class)
      def destinationRepository = 'maven-releases'
      def nexusInstanceId = "localInstance"
      def tagName = "foo"
      def nexusStagingMove = new MoveComponentsBuildStep(nexusInstanceId, tagName, destinationRepository)

      def project = jenkinsRule.createFreeStyleProject()
      project.getBuildersList().add(nexusStagingMove)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(nexusInstanceId) >> nxrmClient
      nxrmClient.move(_, _) >> {throw new RepositoryManagerException("Move failed") }

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)

    and:
      jenkinsRule.assertLogContains("Move failed", build)
      jenkinsRule.assertLogContains("Build step 'Nexus Repository Move Components' changed build result to FAILURE", build)
  }

  def 'it fails attempting to get an nxrm3 client'() {
    setup:
      def destinationRepository = 'maven-releases'
      def nexusInstanceId = "localInstance"
      def tagName = "foo"
      def nexusStagingMove = new MoveComponentsBuildStep(nexusInstanceId, tagName, destinationRepository)

      def project = jenkinsRule.createFreeStyleProject()
      project.getBuildersList().add(nexusStagingMove)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(nexusInstanceId) >> { throw new RepositoryManagerException("Getting client failed") }

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(Result.FAILURE, build)

    and:
      jenkinsRule.assertLogContains("Getting client failed", build)
      jenkinsRule.assertLogContains("Build step 'Nexus Repository Move Components' changed build result to FAILURE", build)
  }

  DescriptorImpl getDescriptor() {
    return (DescriptorImpl) jenkinsRule.getInstance().getDescriptor(MoveComponentsBuildStep.class)
  }
}
