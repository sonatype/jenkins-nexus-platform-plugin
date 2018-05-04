package org.sonatype.nexus.ci.nxrm.v3.commons;

import org.sonatype.nexus.ci.util.NxrmUtil;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.QueryParameter;

import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_3;

public interface ConfigurableNexusInstance
{
  default FormValidation doCheckNexusInstanceId(@QueryParameter String value) {
    return NxrmUtil.doCheckNexusInstanceId(value);
  }

  default ListBoxModel doFillNexusInstanceIdItems() {
    return NxrmUtil.doFillNexusInstanceIdItems(NEXUS_3);
  }
}
