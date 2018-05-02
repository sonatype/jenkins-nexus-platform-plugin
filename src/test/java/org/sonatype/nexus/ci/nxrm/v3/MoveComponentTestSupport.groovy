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

import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.config.NxrmConfiguration
import org.sonatype.nexus.ci.util.FormUtil
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

abstract class MoveComponentTestSupport
    extends Specification
{
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule()

  abstract def getDescriptor()

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

  Nxrm3Configuration saveGlobalConfigurationWithNxrm3Configuration() {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm3Configuration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credentialsId')
    configurationList.push(nxrm3Configuration)

    def globalConfiguration = jenkinsRule.getInstance().getDescriptorByType(GlobalNexusConfiguration.class)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    return nxrm3Configuration
  }
}
