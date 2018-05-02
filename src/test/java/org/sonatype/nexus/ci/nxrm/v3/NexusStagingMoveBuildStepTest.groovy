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
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import hudson.model.Run
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class NexusStagingMoveBuildStepTest
//    extends Specification
{

//  @Override
//  NexusPublisherDescriptor getDescriptor() {
//    return (NexusPublish erDescriptor) jenkins.getInstance().getDescriptor(NexusPublisherBuildStep.class)
//  }
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  RepositoryManagerV3Client nxrmClient = Mock()

//  def 'it successfully completes a move operation based on a tag'() {
  //    setup:
  //      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
  //      def packageList = buildPackageList()
  //      def nexusPublisher = new NexusPublisherBuildStep(nxrm2Configuration.id, 'maven-releases', packageList)
  //
  //      def project = jenkins.createFreeStyleProject()
  //      project.getBuildersList().add(nexusPublisher)
  //
  //      GroovyMock(RepositoryManagerClientUtil.class, global: true)
  //      RepositoryManagerClientUtil.nexus2Client(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient
  //
  //    when:
  //      Run build = project.scheduleBuild2(0).get()
  //
  //    then:
  //      jenkins.assertBuildStatus(Result.FAILURE, build)
  //
  //    and:
  //      String log = jenkins.getLog(build)
  //      log =~ /test.jar does not exist/
  //      log =~ /Failing build due to missing expected files for Nexus Repository Manager Publisher/
  //  }

  def 'it fails build when uploads to Nexus Repository Manager fails'() {
    setup:
      def destinationRepository = 'maven-releases'
      def nexusInstanceId = "localInstance"
      def tagName = "foo"
      def nxrm3Configuration = saveGlobalConfigurationWithNxrm3Configuration()
      def nexusStagingMove = new NexusStagingMoveBuildStep(nexusInstanceId, tagName, destinationRepository)

      def project = jenkins.createFreeStyleProject()
      project.getBuildersList().add(nexusStagingMove)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(nxrm3Configuration.serverUrl) >> nxrmClient

      nxrmClient.move(destinationRepository, tagName)

    when:
      Run build = project.scheduleBuild2(0).get()

    then:
      jenkins.assertBuildStatus(Result.SUCCESS, build)

//    and:
//      String log = jenkins.getLog(build)
//      log =~ /Upload of test.jar failed/
//      log =~ /Failing build due to failure to upload file to Nexus Repository Manager Publisher/
  }

//  def 'it uploads a Maven package to Nexus Repository Manager'() {
//    setup:
//      def repositoryId = 'maven-releases'
//      def nxrm2Configuration = saveGlobalConfigurationWithNxrm2Configuration()
//      def packageList = buildPackageList()
//      def nexusPublisher = new NexusPublisherBuildStep(nxrm2Configuration.id, repositoryId, packageList)
//
//      def project = jenkins.createFreeStyleProject()
//      def workspace = temp.newFolder()
//      def testJar = new File(workspace, 'test.jar')
//      testJar.createNewFile()
//      project.setCustomWorkspace(workspace.getAbsolutePath())
//      project.getBuildersList().add(nexusPublisher)
//
//      GroovyMock(RepositoryManagerClientUtil.class, global: true)
//      RepositoryManagerClientUtil.nexus2Client(nxrm2Configuration.serverUrl, nxrm2Configuration.credentialsId) >> nxrmClient
//
//    when:
//      Run build = project.scheduleBuild2(0).get()
//
//    then:
//      jenkins.assertBuildStatus(Result.SUCCESS, build)
//  }

//  private List<Package> buildPackageList() {
//    def mavenCoordinate = new MavenCoordinate('groupId', 'artifactId', 'version', 'pom')
//    def mavenAsset = new MavenAsset('test.jar', 'classifier', 'extension')
//    def mavenPackage = new MavenPackage(mavenCoordinate, [mavenAsset])
//    def packageList = new ArrayList<Package>()
//    packageList.add(mavenPackage)
//
//    return packageList
//  }

  protected Nxrm3Configuration saveGlobalConfigurationWithNxrm3Configuration() {
    def configurationList = new ArrayList<NxrmConfiguration>()
    def nxrm3Configuration = new Nxrm3Configuration('id', 'internalId', 'displayName', 'http://foo.com', 'credentialsId')
    configurationList.push(nxrm3Configuration)

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration.class)
    globalConfiguration.nxrmConfigs = configurationList
    globalConfiguration.save()

    return nxrm3Configuration
  }
}
