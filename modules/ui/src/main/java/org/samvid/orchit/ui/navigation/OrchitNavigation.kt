package org.samvid.orchit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.samvid.orchit.ui.screen.EditorScreen
import org.samvid.orchit.ui.screen.HomeScreen
import org.samvid.orchit.ui.screen.SettingsScreen
import org.samvid.orchit.ui.OrchitDependencies
import org.samvid.orchit.ui.StoredProjectListContract
import org.samvid.orchit.ui.EngineEditorUiContract
import org.samvid.orchit.engine.ProjectId
import org.samvid.orchit.engine.TimelineEditor
import org.samvid.orchit.engine.Project

enum class OrchitScreen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(route = "home", label = "Home", icon = Icons.Default.Home),
    Editor(route = "editor", label = "Editor", icon = Icons.Default.Edit),
    Settings(route = "settings", label = "Settings", icon = Icons.Default.Settings),
}

@Composable
fun OrchitNavHost(
    navController: NavHostController,
    windowSize: OrchitWindowSize,
    dependencies: OrchitDependencies? = null,
    modifier: Modifier = Modifier,
) {
    val projectList = rememberProjectList(dependencies)
    NavHost(
        navController = navController,
        startDestination = OrchitScreen.Home.route,
        modifier = modifier,
    ) {
        composable(OrchitScreen.Home.route) {
            HomeScreen(
                onProjectClick = {
                    navController.navigate(OrchitScreen.Editor.route) {
                        launchSingleTop = true
                    }
                },
                onNewProject = {
                    navController.navigate(OrchitScreen.Editor.route) {
                        launchSingleTop = true
                    }
                },
                projectListContract = projectList,
            )
        }
        composable(OrchitScreen.Editor.route) {
            val projectId = if (projectList == null) null else {
                val projectListState by projectList.state.collectAsStateWithLifecycle()
                projectListState.openedProjectId
            }
            val editor = androidx.compose.runtime.remember(projectId, dependencies) {
                if (dependencies == null) null else {
                    val project = if (projectId == null) null else dependencies.storage.load(ProjectId(projectId))
                    EngineEditorUiContract(TimelineEditor(project ?: Project(ProjectId("empty"), "Untitled Project", emptyList())))
                }
            }
            EditorScreen(windowSize = windowSize, editorContract = editor)
        }
        composable(OrchitScreen.Settings.route) {
            SettingsScreen()
        }
    }
}

@Composable
private fun rememberProjectList(dependencies: OrchitDependencies?): org.samvid.orchit.ui.ProjectListContract? =
    androidx.compose.runtime.remember(dependencies) { dependencies?.let { StoredProjectListContract(it.storage) } }
