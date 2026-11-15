package org.samvid.orchit.text

import org.junit.Assert.*
import org.junit.Test

class TextFormatsTest {
    private fun sample(): String = requireNotNull(javaClass.classLoader?.getResourceAsStream("text-animation.json"))
        .bufferedReader().use { it.readText() }

    @Test fun loaderReadsDocumentedV1SampleAndRegistry() {
        val value = TextAnimationPackageLoader.load(sample())
        assertEquals("org.samvid.orchit.text.fade", value.id)
        assertEquals("OFL-1.1", value.assets.single().license)
        val registry = TextPackageRegistry()
        registry.load(sample())
        assertEquals(listOf(value.id), registry.presetIds())
        assertEquals(FontAsset("fonts/sample.ttf", "OFL-1.1"), registry.fontAssets().single())
    }

    @Test fun loaderRejectsMissingLicenseAndBadValues() {
        assertThrows(TextFormatException::class.java) { TextAnimationPackageLoader.load(sample().replace(",\n      \"license\": \"OFL-1.1\"", "")) }
        assertThrows(TextFormatException::class.java) { TextAnimationPackageLoader.load(sample().replace("\"durationUs\": 500000", "\"durationUs\": -1")) }
        assertThrows(TextFormatException::class.java) { TextAnimationPackageLoader.load(sample().replace("\"formatVersion\": 1", "\"formatVersion\": 2")) }
    }

    @Test fun styleValidatorAcceptsAndRejectsValues() {
        assertEquals(TextStyle("Sans", 18f, 0xff00ff00), TextStyleValidator.validate(TextStyle("Sans", 18f, 0xff00ff00)))
        assertThrows(TextFormatException::class.java) { TextStyleValidator.validate(TextStyle("", 18f, 0)) }
        assertThrows(TextFormatException::class.java) { TextStyleValidator.validate(TextStyle("Sans", 0f, 0)) }
        assertThrows(TextFormatException::class.java) { TextStyleValidator.validate(TextStyle("Sans", 18f, -1)) }
    }
}
