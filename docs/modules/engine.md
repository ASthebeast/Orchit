# Engine module

## Owns

Project documents, timelines, clips, tracks, playback, and export orchestration.

## Exposes

Kotlin contracts in `io.github.asthebeast.orchit.engine`. `ProjectDocumentContract` is implemented by the schema-matching `ProjectDocument` JSON model, including project duration, local asset paths and licenses, effects, and keyframes. `Project.duration` reports the timeline end. `TimelineEditor` implements `TimelineOperations` and `UndoRedo` for clip and track editing with snapshot undo/redo. `LocalProjectStorage` stores one strict JSON document per project under an injected directory, with create/list/load/save/rename/delete operations. The Android app supplies its private files directory; tests can inject a temporary directory. Corrupt documents are skipped by listing and reported by direct load. `TimelineMediaMapping` and `TimelinePositionLogic` provide JVM-testable, trimmed video playlist mapping and flattened position/duration calculations. `Media3PlaybackController` builds an ExoPlayer playlist from the first video track, publishes position/duration as `StateFlow`, and supports playback controls and seeks.

## Must not touch

Compose UI, effect algorithms, audio DSP, text rendering details, networking, accounts, or remote storage.

## Dependencies

The UI consumes public contracts. The engine is the composition root and may wire effects, audio, and text contracts. Feature contracts do not depend on each other; all app behavior remains local and offline. Project JSON uses Gson inside this module; timeline and document APIs use no Android classes.
