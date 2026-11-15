package org.samvid.orchit.engine

import java.io.File
import java.util.UUID

/** One strict JSON file per project under an injected app-private directory. */
class LocalProjectStorage(private val root: File) : ProjectRepository {
    init { root.mkdirs() }

    fun create(name: String): Project {
        require(name.isNotBlank())
        val project = Project(ProjectId(UUID.randomUUID().toString()), name, emptyList())
        save(project)
        return project
    }

    fun list(): List<Project> = root.listFiles { file -> file.isFile && file.extension == "json" }
        .orEmpty().mapNotNull { file -> runCatching { read(file) }.getOrNull() }.sortedBy { it.name.lowercase() }

    override fun load(id: ProjectId): Project? {
        val file = fileFor(id)
        if (!file.isFile) return null
        return read(file)
    }

    override fun save(project: Project) {
        val file = fileFor(project.id)
        val temp = File(root, "${project.id.value}.json.tmp")
        temp.writeText(ProjectJson.write(project.toDocument()), Charsets.UTF_8)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    fun rename(id: ProjectId, name: String): Project {
        require(name.isNotBlank())
        val updated = requireNotNull(load(id)) { "Unknown project: ${id.value}" }.copy(name = name)
        save(updated)
        return updated
    }

    fun delete(id: ProjectId): Boolean = fileFor(id).delete()

    private fun read(file: File): Project = ProjectJson.read(file.readText(Charsets.UTF_8)).toProject()
    private fun fileFor(id: ProjectId): File {
        require(id.value.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid project id" }
        return File(root, "${id.value}.json")
    }
}

private fun Project.toDocument() = ProjectDocument(1, id.value, name, duration.value, tracks.map { track ->
    ProjectTrackDocument(track.id, track.kind, track.clips.map { clip ->
        ProjectClipDocument(clip.id, ProjectAssetDocument(clip.media.uri, clip.media.license.ifBlank { "NOASSERTION" }), clip.start.value, clip.sourceIn.value, clip.duration.value, clip.effects)
    })
})

private fun ProjectDocument.toProject() = Project(ProjectId(projectId), name, tracks.map { track ->
    Track(track.id, track.kind, track.clips.map { clip ->
        Clip(clip.id, MediaReference(clip.asset.path, "application/octet-stream", clip.asset.license), TimelineTimeUs(clip.startUs), TimelineTimeUs(clip.sourceInUs), TimelineTimeUs(clip.durationUs), clip.effects)
    })
})
