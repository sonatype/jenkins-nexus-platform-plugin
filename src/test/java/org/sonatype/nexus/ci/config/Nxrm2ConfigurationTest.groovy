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
package org.sonatype.nexus.ci.config

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v2.RepositoryManagerV2Client

import org.sonatype.nexus.ci.config.Nxrm2Configuration.DescriptorImpl
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class Nxrm2ConfigurationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'it tests valid server credentials'() {
    when:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def client = Mock(RepositoryManagerV2Client.class)
      client.getRepositoryList() >> repositories
      RepositoryManagerClientUtil.newRepositoryManagerClient(serverUrl, credentialsId) >> client
      RepositoryManagerClientUtil.nexus2Client(serverUrl, credentialsId) >> client
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(Nxrm2Configuration.class)

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId)

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus Repository Manager 2.x connection succeeded (1 hosted release Maven 2 repositories)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      repositories << [
        [
            [
                id: 'maven-releases',
                name: 'Maven Releases',
                format: 'maven2',
                repositoryType: 'hosted',
                repositoryPolicy: 'Release'
            ],
            [
                id: 'maven1-releases',
                name: 'Maven 1 Releases',
                format: 'maven1',
                repositoryType: 'hosted',
                repositoryPolicy: 'Release'
            ],
            [
                id: 'maven-snapshots',
                name: 'Maven Snapshots',
                format: 'maven2',
                repositoryType: 'hosted',
                repositoryPolicy: 'Snapshot'
            ],
            [
                id: 'maven-proxy',
                name: 'Maven Proxy',
                format: 'maven2',
                repositoryType: 'proxy',
                repositoryPolicy: 'Release'
            ]
        ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      def client = Mock(RepositoryManagerV2Client.class)
      client.getRepositoryList() >> { throw new RepositoryManagerException("something went wrong") }
      RepositoryManagerClientUtil.newRepositoryManagerClient(serverUrl, credentialsId) >> client
      RepositoryManagerClientUtil.nexus2Client(serverUrl, credentialsId) >> client
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(Nxrm2Configuration.class)

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId)


    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus Repository Manager 2.x connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
  }
}
