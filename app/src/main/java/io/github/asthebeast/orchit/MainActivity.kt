package io.github.asthebeast.orchit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.asthebeast.orchit.ui.OrchitApp
import io.github.asthebeast.orchit.ui.OrchitDependencies
import io.github.asthebeast.orchit.engine.LocalProjectStorage
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dependencies = OrchitDependencies(LocalProjectStorage(File(filesDir, "projects")))
        setContent {
            OrchitApp(dependencies)
        }
    }
}
