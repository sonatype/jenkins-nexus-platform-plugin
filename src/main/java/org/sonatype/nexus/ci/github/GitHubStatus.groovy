package org.sonatype.nexus.ci.github

enum GitHubStatus {
  ERROR('error'), FAILURE('failure'), PENDING('pending'), SUCCESS('success')

  public final String value

  GitHubStatus(final String value) {
    this.value = value
  }
}
