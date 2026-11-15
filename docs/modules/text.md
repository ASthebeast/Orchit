# Text module

## Owns

Text overlays, bundled font references, and text animation presets.

## Exposes

Kotlin contracts and v1 package models in `org.samvid.orchit.text`. `TextStyleValidator` checks font family, positive finite size, and unsigned 32-bit ARGB color. `TextAnimationPackageLoader` strictly validates the existing `docs/formats/schemas/text-animation.schema.json` shape, including timing, keyframes, and optional local font assets with required licenses. `TextPackageRegistry` indexes loaded presets and exposes their licensed font assets through `TextCatalog`.

## Must not touch

Visual effect processing, audio processing, timeline persistence, UI screens, or network access.

## Dependencies

The loader tests read `docs/formats/samples/text-animation.json`; Gson handles JSON parsing and JUnit runs loader/validator tests. No text rendering or UI behavior is implemented in this slice.
