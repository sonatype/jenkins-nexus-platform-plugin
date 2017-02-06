/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.nexus.ci.iq

import com.sonatype.nexus.api.iq.Action
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation
import com.sonatype.nexus.api.iq.PolicyFact

class PolicyFailureMessageFormatter
{
  private final Map<String, List<PolicyFact>> groupedActions

  private final ApplicationPolicyEvaluation evaluation

  PolicyFailureMessageFormatter(ApplicationPolicyEvaluation evaluation) {
    this.evaluation = evaluation

    groupedActions = [Action.ID_FAIL, Action.ID_WARN]
        .collectEntries { [it, findFactsFor(it) ] }
  }

  List<PolicyFact> findFactsFor(final String actionTypeId) {
    evaluation.policyAlerts
        .findAll { it.actions.find { it.actionTypeId == actionTypeId } }
        .collect { it.trigger }
  }

  String getMessage() {
    def failures = groupedActions.get(Action.ID_FAIL).
        collect { Messages.PolicyFailureMessageFormatter_PolicyFailing(it) }
    def warnings = groupedActions.get(Action.ID_WARN).
        collect { Messages.PolicyFailureMessageFormatter_PolicyWarning(it) }
    def summary = [Messages.PolicyFailureMessageFormatter_EvaluationReport(evaluation.applicationCompositionReportUrl),
                   Messages.PolicyFailureMessageFormatter_EvaluationSummary(evaluation.criticalComponentCount,
                       evaluation.severeComponentCount, evaluation.moderateComponentCount)]
    return ([(failures + warnings).join('\n\n')] + summary).join('\n')
  }

  boolean hasWarnings() {
    groupedActions.get(Action.ID_WARN)
  }

  boolean hasFailures() {
    groupedActions.get(Action.ID_FAIL)
  }
}
