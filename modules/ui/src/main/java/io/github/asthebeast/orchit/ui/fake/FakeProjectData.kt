package io.github.asthebeast.orchit.ui.fake

import io.github.asthebeast.orchit.ui.ProjectSummary

/** Provides sample projects for the in-memory home screen contract. */
object FakeProjectData {
    /** Initial project rows shown in the UI skeleton. */
    fun all(): List<ProjectSummary> = listOf(
        ProjectSummary(id = "proj-001", name = "My First Reel"),
        ProjectSummary(id = "proj-002", name = "Summer Highlights"),
    )
}
