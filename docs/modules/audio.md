# Audio module

## Owns

Audio tracks, sound effects, volume, fades, and beat synchronization.

## Exposes

Kotlin contracts and v1 package models in `org.samvid.orchit.audio`. `AudioClipSettingsValidator` accepts finite volume in 0–1, nonnegative fades, and finite speed in 0.25–4. `AudioAssetPackageLoader` strictly reads `{formatVersion,id,name,assets}` manifests; each asset requires a local path and a license from its copyright-free/open-source allowlist (CC0, CC-BY/SA, MIT, Apache-2.0, BSD, ISC, OFL, or 0BSD). Missing, proprietary, or unknown licenses are rejected. `AudioPackageRegistry` indexes validated packages in memory.

## Must not touch

Visual effect processing, timeline/project persistence, text overlays, UI screens, or network access.

## Dependencies

The module uses Gson for strict local manifest parsing and JUnit for loader/validator tests. No audio playback or rendering is implemented in this slice.
