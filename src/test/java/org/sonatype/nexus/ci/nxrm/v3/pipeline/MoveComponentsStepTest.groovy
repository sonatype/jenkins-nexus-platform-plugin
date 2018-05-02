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

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.nxrm.v3.pipeline.MoveComponentsStep.DescriptorImpl
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Run
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS
import static org.hamcrest.MatcherAssert.assertThat

class MoveComponentsStepTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  DescriptorImpl getDescriptor() {
    return (DescriptorImpl) jenkins.getInstance().getDescriptor(MoveComponentsStep.class)
  }

  def 'it populates Nexus instances'() {
    setup:
      def nxrm3Configuration = saveGlobalConfigurationWithNxrm3Configuration()

    when: 'nexus instance items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillNexusInstanceIdItems()

    then: 'ListBox has the correct size'
      listBoxModel.size() == 2

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == nxrm3Configuration.displayName
      listBoxModel.get(1).value == nxrm3Configuration.id
  }

  def 'it populates the list of destination repositories'() {
    setup:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def nxrm3Configuration = saveGlobalConfigurationWithNxrm3Configuration()

      def client = Mock(RepositoryManagerV3Client.class)
      def repositories = [
          [
              name: 'Maven Releases',
              format: 'maven2',
              type: 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              name: 'Maven 1 Releases',
              format: 'maven1',
              type: 'hosted',
              repositoryPolicy: 'Release'
          ],
          [
              name: 'Maven Snapshots',
              format: 'maven2',
              type: 'hosted',
              repositoryPolicy: 'Snapshot'
          ],
          [
              name: 'Maven Proxy',
              format: 'maven2',
              type: 'proxy',
              repositoryPolicy: 'Release'
          ]
      ]
      client.getRepositories() >> repositories
      RepositoryManagerClientUtil.nexus3Client(nxrm3Configuration.serverUrl, nxrm3Configuration.credentialsId) >> client

    when: 'destination nexus repository items are filled'
      def descriptor = getDescriptor()
      def listBoxModel = descriptor.doFillDestinationRepositoryItems(nxrm3Configuration.id)

    then: 'ListBox has the correct size'
      //nxrm3 client looks for hosted maven repos when populating this list
      listBoxModel.size() == 3

    and: 'ListBox has empty item'
      listBoxModel.get(0).name == FormUtil.EMPTY_LIST_BOX_NAME
      listBoxModel.get(0).value == FormUtil.EMPTY_LIST_BOX_VALUE

    and: 'ListBox is populated'
      listBoxModel.get(1).name == repositories.get(0).name
      listBoxModel.get(1).value == repositories.get(0).name
  }

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
      jenkins.assertBuildStatus(SUCCESS, build)
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
      jenkins.assertBuildStatus(FAILURE, build)

    and:
      String log = jenkins.getLog(build)
      assertThat(log, log.contains("Failing build due to: Move failed"))
  }

  private WorkflowJob getWorkflowJob(String instanceId, String destination, String tagName) {
    WorkflowJob job = jenkins.createProject(WorkflowJob.class, "nexusStagingMove")
    job.setDefinition(new CpsFlowDefinition("node {moveComponents destinationRepository: '" + destination +
        "', nexusInstanceId: '" + instanceId +"', tagName: '" + tagName + "'}"))
    return job
  }

  private Nxrm3Configuration saveGlobalConfigurationWithNxrm3Configuration() {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm3Configuration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credentialsId')
    configurationList.push(nxrm3Configuration)

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration.class)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    return nxrm3Configuration
  }
}
