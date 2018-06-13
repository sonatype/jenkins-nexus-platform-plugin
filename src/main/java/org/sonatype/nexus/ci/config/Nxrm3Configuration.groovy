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

import static hudson.util.FormValidation.Kind.OK
import static hudson.util.FormValidation.error
import static hudson.util.FormValidation.ok
import static org.sonatype.nexus.ci.config.NxrmConfiguration.NxrmDescriptor
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3
import static org.sonatype.nexus.ci.util.Nxrm3Util.getApplicableRepositories

class Nxrm3Configuration
    extends NxrmConfiguration
{
  @SuppressWarnings('ParameterCount')
  @DataBoundConstructor
  Nxrm3Configuration(final String id,
                     final String internalId,
                     final String displayName,
                     final String serverUrl,
                     final String credentialsId)
  {
    super(id, internalId, displayName, serverUrl, credentialsId)
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

    @Override
    FormValidation doVerifyCredentials(@QueryParameter String serverUrl, @QueryParameter String credentialsId)
        throws IOException
    {
      try {
        def repositories = getApplicableRepositories(serverUrl, credentialsId, 'maven2')
        ok("Nexus Repository Manager 3.x connection succeeded (${repositories.size()} hosted maven2 repositories)")
      }
      catch (RepositoryManagerException e) {
        error(e, 'Nexus Repository Manager 3.x connection failed')
      }
    }

    @Override
    FormValidation doCheckServerUrl(@QueryParameter String value) {
      def validation = super.doCheckServerUrl(value)

      if (validation.kind != OK) {
        return validation
      }

      // check nexus version, warn if < 3.13.0 PRO
      try {
        def response = new XmlSlurper().parseText(new URL("${value}/service/rest/wonderland/status").text)
        def edition = response.edition.text()
        def version = response.version.text()
        def (major, minor) = version.tokenize('.').take(2).collect { it as int }

        if (!edition.equalsIgnoreCase('pro') || major < 3 || minor < 13) {
          return FormValidation.warning(
              "NXRM ${edition} ${version} found. Some operations require a Professional Edition Nexus Repository " +
                  "Manager server of version 3.13.0 or newer; use of an incompatible server will result in failed " +
                  "builds")
        }
      }
      catch (Exception e) {
        return FormValidation.warning(
            'Unable to determine Nexus Repository Manager version. Certain operations may not be compatible with your' +
                ' server which could result in failed builds.')
      }

      ok()
    }
  }
}
