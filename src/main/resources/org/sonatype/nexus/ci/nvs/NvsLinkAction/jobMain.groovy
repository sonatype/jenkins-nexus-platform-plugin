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
package org.sonatype.nexus.ci.nvs.NvsLinkAction

def t = namespace(lib.JenkinsTagLib)

def nvsLinkUi = {
  div() {
    span('Sonatype is building an application scanner for Jenkins.')
    br()
    a(href: "https://www.sonatype.com/nvsforjenkins", target: "_blank", "Learn more")
    span(" about what's coming to the Nexus Platform Plugin.")
  }
}

table() {
  t.summary(icon: '/plugin/nexus-jenkins-plugin/images/96x96/sonatype-logo.png', nvsLinkUi)
}

