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
package org.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.internal.InternalIqClient
import com.sonatype.nexus.api.iq.internal.InternalIqClientBuilder

import org.sonatype.nexus.ci.config.NxiqConfiguration
import org.sonatype.nexus.ci.util.ServerConfigurationUtil

import jenkins.model.Jenkins
import org.slf4j.Logger

class IqClientFactory
{
  static InternalIqClient getIqClient(IqClientFactoryConfiguration conf = new IqClientFactoryConfiguration()) {
    def serverUrl = conf.serverUrl ?: NxiqConfiguration.serverUrl
    def context = conf.context ?: Jenkins.instance
    def credentialsId = conf.credentialsId ?: NxiqConfiguration.credentialsId
    def credentials = ServerConfigurationUtil.findCredentials(serverUrl, credentialsId, context)
    def serverConfig = ServerConfigurationUtil.getServerConfig(serverUrl, credentials)
    def proxyConfig = ServerConfigurationUtil.getProxyConfig(serverUrl)
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withServerConfig(serverConfig)
        .withProxyConfig(proxyConfig)
        .withLogger(conf.log)
        .build()
  }

  static InternalIqClient getIqLocalClient(Logger log, String instanceId) {
    return (InternalIqClient) InternalIqClientBuilder.create()
        .withInstanceId(instanceId)
        .withLogger(log)
        .build()
  }
}
