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
package org.sonatype.nexus.ci.github

import com.sonatype.nexus.api.common.ProxyConfig
import com.sonatype.nexus.api.common.ServerConfig

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder

class GitHubClient
{
  private final ServerConfig serverConfig
  private final ProxyConfig proxyConfig
  private final String userAgent

  GitHubClient(final ServerConfig serverConfig, final ProxyConfig proxyConfig) {
    this.serverConfig = serverConfig
    this.proxyConfig = proxyConfig
    def properties = new Properties()
    this.getClass().getResource('/com/sonatype/insight/client.properties').withInputStream {
      properties.load(it)
    }
    userAgent = properties.getProperty('userAgent')
  }

  GitHubUser getUser() {
    return new GitHubUser(get('/user'))
  }

  void updateStatus(String repository,
                    String commitHash,
                    String description,
                    String context,
                    GitHubStatus status,
                    String targetUrl)
  {
    def statusPath =  "/repos/${repository}/statuses/${commitHash}"
    post(statusPath, [
        state: status.value,
        'target_url': targetUrl,
        description: description,
        context: context
    ])
  }

  private Map get(String path) {
    def http = new HTTPBuilder(serverConfig.address)
    return (Map)http.get(path: path,
        contentType: ContentType.JSON,
        headers: [
            'User-Agent': userAgent,
            'Authorization': 'Basic ' +
                (serverConfig.authentication.username + ":" + serverConfig.authentication.password).bytes.encodeBase64()
        ])
  }

  private void post(String path, Map json) {
    def http = new HTTPBuilder(serverConfig.address)
    http.post(path: path,
        contentType: ContentType.JSON,
        requestContentType: ContentType.JSON,
        body: json,
        headers: [
            'User-Agent'   : userAgent,
            'Authorization': 'Basic ' +
                (serverConfig.authentication.username + ":" + serverConfig.authentication.password).bytes.encodeBase64()
        ])
  }
}
