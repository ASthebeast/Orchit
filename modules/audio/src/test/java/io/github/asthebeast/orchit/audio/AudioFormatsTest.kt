package io.github.asthebeast.orchit.audio

import org.junit.Assert.*
import org.junit.Test

class AudioFormatsTest {
    private val valid = """{"formatVersion":1,"id":"io.github.asthebeast.orchit.audio.sample","name":"Sample","assets":[{"id":"tone","path":"audio/tone.wav","license":"CC0-1.0"}]}"""

    @Test fun loaderAcceptsSampleAndRegistryIndexesIt() {
        val value = AudioAssetPackageLoader.load(valid)
        assertEquals("CC0-1.0", value.assets.single().license)
        val registry = AudioPackageRegistry()
        registry.load(valid)
        assertEquals(listOf(value.id), registry.ids())
        assertEquals(value, registry.find(value.id))
    }

    @Test fun loaderRejectsMissingOrUnapprovedLicense() {
        assertThrows(AudioFormatException::class.java) { AudioAssetPackageLoader.load(valid.replace(",\"license\":\"CC0-1.0\"", "")) }
        assertThrows(AudioFormatException::class.java) { AudioAssetPackageLoader.load(valid.replace("CC0-1.0", "All-Rights-Reserved")) }
    }

    @Test fun settingsValidatorAcceptsAndRejectsValues() {
        assertEquals(AudioClipSettings(), AudioClipSettingsValidator.validate(AudioClipSettings()))
        assertThrows(AudioFormatException::class.java) { AudioClipSettingsValidator.validate(AudioClipSettings(volume = 1.2f)) }
        assertThrows(AudioFormatException::class.java) { AudioClipSettingsValidator.validate(AudioClipSettings(fadeOutUs = -1)) }
        assertThrows(AudioFormatException::class.java) { AudioClipSettingsValidator.validate(AudioClipSettings(speed = 0f)) }
    }
}
