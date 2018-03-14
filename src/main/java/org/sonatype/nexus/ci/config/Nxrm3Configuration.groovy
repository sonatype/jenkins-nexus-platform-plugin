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

import hudson.Extension
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

import static hudson.util.FormValidation.error
import static hudson.util.FormValidation.ok
import static org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.Nxrm3Util.getApplicableRepositories

class Nxrm3Configuration
    extends NxrmConfiguration
{
  boolean anonymousAccess

  @SuppressWarnings('ParameterCount')
  @DataBoundConstructor
  Nxrm3Configuration(final String id,
                     final String internalId,
                     final String displayName,
                     final String serverUrl,
                     final String credentialsId,
                     final boolean anonymousAccess)
  {
    super(id, internalId, displayName, serverUrl, credentialsId)
    this.anonymousAccess = anonymousAccess
  }

  @Override
  NxrmVersion getVersion() {
    NEXUS_3
  }

  @Extension
  static class DescriptorImpl
      extends NxrmDescriptor
  {
    DescriptorImpl() {
      super(Nxrm3Configuration)
    }

    @Override
    String getDisplayName() {
      return 'Nexus Repository Manager 3.x Server'
    }

    FormValidation doVerifyCredentials(
        @QueryParameter String serverUrl,
        @QueryParameter String credentialsId,
        @QueryParameter boolean anonymousAccess) throws IOException
    {
      try {
        def repositories = getApplicableRepositories(serverUrl, credentialsId, credentialsId ? anonymousAccess : true)

        return ok("Nexus Repository Manager 3.x connection succeeded (${repositories.size()} hosted " +
            'Maven 2 repositories)')
      }
      catch (RepositoryManagerException e) {
        return error(e, 'Nexus Repository Manager 3.x connection failed')
      }
    }
  }
}
