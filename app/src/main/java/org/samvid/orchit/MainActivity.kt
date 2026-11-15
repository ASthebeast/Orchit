package org.samvid.orchit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.samvid.orchit.ui.OrchitApp
import org.samvid.orchit.ui.OrchitDependencies
import org.samvid.orchit.engine.LocalProjectStorage
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
