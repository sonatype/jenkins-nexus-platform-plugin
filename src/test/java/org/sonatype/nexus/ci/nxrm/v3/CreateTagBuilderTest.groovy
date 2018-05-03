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

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.GlobalNexusConfiguration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.model.Result
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import spock.lang.Unroll

class CreateTagBuilderTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  @Rule
  public TemporaryFolder temp = new TemporaryFolder()

  RepositoryManagerV3Client nxrm3Client = Mock()

  def 'creates a tag using workspace'() {
    setup:
      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def attrFile = Paths.get(workspace.absolutePath, 'attrFile.json')
      Files.write(attrFile, '{"foo": "bar"}'.bytes, StandardOpenOption.CREATE_NEW) // create file in workspace
      def tagName = 'create-tag-test'
      def config = createNxrm3Config('nx3')
      def builder = new CreateTagBuilder('nx3', tagName)
      builder.setTagAttributesPath('attrFile.json') // uses relative path in workspace
      builder.setTagAttributesJson('{"baz": "qux"}')


      project.setCustomWorkspace(workspace.absolutePath)
      project.getBuildersList().add(builder)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.createTag(tagName, [foo: 'bar', baz: 'qux'])
      jenkins.assertBuildStatus(Result.SUCCESS, build)
  }

  @Unroll
  def 'tag attributes are optional - #description'(tagName, tagAttributeFile, tagAttributeJson, attributeMap,
                                                   description)
  {
    setup:
      def project = jenkins.createFreeStyleProject()
      def workspace = temp.newFolder()
      def config = createNxrm3Config('nx3')
      def builder = new CreateTagBuilder('nx3', tagName)

      if (tagAttributeFile) {
        def attrFile = Paths.get(workspace.absolutePath, 'attr-file.json')
        Files.write(attrFile, tagAttributeFile.bytes, StandardOpenOption.CREATE_NEW)
        builder.setTagAttributesPath('attr-file.json')
      }

      if (tagAttributeJson) {
        builder.setTagAttributesJson(tagAttributeJson)
      }

      project.setCustomWorkspace(workspace.absolutePath)
      project.getBuildersList().add(builder)

      GroovyMock(RepositoryManagerClientUtil.class, global: true)
      RepositoryManagerClientUtil.nexus3Client(config.serverUrl, config.credentialsId) >> nxrm3Client

    when:
      def build = project.scheduleBuild2(0).get()

    then:
      1 * nxrm3Client.createTag(tagName, attributeMap)
      jenkins.assertBuildStatus(Result.SUCCESS, build)

    where:
      tagName << ['tagFoo', 'tagBar', 'tagBaz']
      tagAttributeFile << [null, '{"baz": "qux"}', null]
      tagAttributeJson << ['{"foo": "bar"}', null, null]
      attributeMap << [[foo: 'bar'], [baz: 'qux'], null]
      description << ['no attribute file', 'no attribute json', 'no attributes']
  }

  def 'tag attribute json is optional'() {

  }

  def 'tag attribute json has priority over file attributes'() {

  }

  def 'descriptor validates json'() {

  }

  def 'fails build if client cannot be built'() {

  }

  def 'fails build on attribute file error'() {

  }

  def 'fails build on attribute json error'() {

  }

  def 'fails build on nxrm create tag error'() {

  }

  private Nxrm3Configuration createNxrm3Config(String id) {
    def configs = []
    def nxrm3Configuration = new Nxrm3Configuration(id, "internal${id}", 'displayName', 'http://foo.com',
        'credentialsId')
    configs << nxrm3Configuration

    def globalConfiguration = jenkins.getInstance().getDescriptorByType(GlobalNexusConfiguration)
    globalConfiguration.nxrmConfigs = configs
    globalConfiguration.save()

    nxrm3Configuration
  }
}
