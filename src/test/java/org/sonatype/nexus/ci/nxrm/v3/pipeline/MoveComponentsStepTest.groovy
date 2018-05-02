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
package org.sonatype.nexus.ci.nxrm.v3.pipeline

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.nxrm.v3.MoveComponentTestSupport
import org.sonatype.nexus.ci.nxrm.v3.MoveComponentsBuildStep
import org.sonatype.nexus.ci.nxrm.v3.MoveComponentsBuildStep.DescriptorImpl
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Run
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS

class MoveComponentsStepTest
    extends MoveComponentTestSupport
{

  def 'it successfully completes a move operation based on a tag'() {
    setup:
      RepositoryManagerV3Client nxrmClient = Mock(RepositoryManagerV3Client.class)
      def destination = 'maven-releases'
      def instanceId = 'instanceId'
      def tagName = 'foo'

      WorkflowJob project = getWorkflowJob(instanceId, destination, tagName)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(instanceId) >> nxrmClient

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(SUCCESS, build)
  }

  def 'it fails completes a move operation based on a tag'() {
    setup:
      RepositoryManagerV3Client nxrmClient = Mock(RepositoryManagerV3Client.class)
      def destination = 'maven-releases'
      def instanceId = 'localInstance'
      def tagName = 'foo'

      WorkflowJob project = getWorkflowJob(instanceId, destination, tagName)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(instanceId) >> nxrmClient
      nxrmClient.move(_, _) >> { throw new RepositoryManagerException("Move failed") }

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkinsRule.assertBuildStatus(FAILURE, build)

    and:
      jenkinsRule.assertLogContains("Failing build due to: Move failed", build)
  }

  DescriptorImpl getDescriptor() {
    return (DescriptorImpl) jenkinsRule.getInstance().getDescriptor(MoveComponentsBuildStep.class)
  }

  WorkflowJob getWorkflowJob(String instanceId, String destination, String tagName) {
    WorkflowJob job = jenkinsRule.createProject(WorkflowJob.class, "nexusStagingMove")
    job.setDefinition(new CpsFlowDefinition("node {moveComponents destinationRepository: '" + destination +
        "', nexusInstanceId: '" + instanceId +"', tagName: '" + tagName + "'}"))
    return job
  }
}
