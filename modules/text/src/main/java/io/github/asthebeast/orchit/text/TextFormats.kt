package io.github.asthebeast.orchit.text

import com.google.gson.JsonElement
import com.google.gson.JsonParser

data class TextStyle(val fontFamily: String, val fontSizeSp: Float, val colorArgb: Long, val bold: Boolean = false, val italic: Boolean = false)
data class TextAnimationKeyframe(val property: String, val timeUs: Long, val value: JsonElement, val easing: String)
data class TextPackageAsset(val path: String, val kind: String, val license: String)
data class TextAnimationPackage(val formatVersion: Int, val id: String, val name: String, val durationUs: Long, val delayUs: Long, val keyframes: List<TextAnimationKeyframe>, val assets: List<TextPackageAsset>)
class TextFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object TextStyleValidator {
    fun validate(style: TextStyle): TextStyle {
        if (style.fontFamily.isBlank()) throw TextFormatException("fontFamily must not be blank")
        if (!style.fontSizeSp.isFinite() || style.fontSizeSp <= 0f) throw TextFormatException("fontSizeSp must be finite and positive")
        if (style.colorArgb !in 0L..0xffffffffL) throw TextFormatException("colorArgb must be an unsigned 32-bit value")
        return style
    }
}

/** Strict loader for the existing v1 text animation schema. */
object TextAnimationPackageLoader {
    fun load(json: String): TextAnimationPackage = try {
        val root = JsonParser.parseString(json).asJsonObject
        root.only("formatVersion", "id", "name", "timing", "keyframes", "assets")
        root.need("formatVersion", "id", "name", "timing", "keyframes")
        if (root.get("formatVersion").strictLong("formatVersion") != 1L) fail("only text animation formatVersion 1 is supported")
        val timing = root.getAsJsonObject("timing")
        timing.only("durationUs", "delayUs"); timing.need("durationUs", "delayUs")
        val duration = timing.get("durationUs").strictLong("timing.durationUs").nonNegative("timing.durationUs")
        val delay = timing.get("delayUs").strictLong("timing.delayUs").nonNegative("timing.delayUs")
        val keyframes = root.getAsJsonArray("keyframes").mapIndexed { index, element ->
            val frame = element.asJsonObject
            frame.only("property", "timeUs", "value", "easing"); frame.need("property", "timeUs", "value", "easing")
            val property = frame.get("property").asString.nonBlank("keyframes[$index].property")
            val time = frame.get("timeUs").strictLong("keyframes[$index].timeUs").nonNegative("keyframes[$index].timeUs")
            if (time > duration) fail("keyframes[$index].timeUs must not exceed durationUs")
            val easing = frame.get("easing").asString.nonBlank("keyframes[$index].easing")
            TextAnimationKeyframe(property, time, frame.get("value"), easing)
        }
        val assets = root.get("assets")?.asJsonArray?.mapIndexed { index, element ->
            val asset = element.asJsonObject
            asset.only("path", "kind", "license"); asset.need("path", "kind", "license")
            val path = asset.get("path").asString.nonBlank("assets[$index].path")
            if (path.startsWith('/') || path.split('/').any { it == ".." }) fail("assets[$index].path must be a local path")
            val kind = asset.get("kind").asString
            if (kind != "font") fail("assets[$index].kind must be font")
            TextPackageAsset(path, kind, asset.get("license").asString.nonBlank("assets[$index].license"))
        } ?: emptyList()
        TextAnimationPackage(1, root.get("id").asString.nonBlank("id"), root.get("name").asString.nonBlank("name"), duration, delay, keyframes, assets)
    } catch (exception: TextFormatException) {
        throw exception
    } catch (exception: Exception) {
        throw TextFormatException("Invalid text animation package: ${exception.message}", exception)
    }

    private fun Long.nonNegative(context: String) = takeIf { it >= 0 } ?: fail("$context must not be negative")
    private fun String.nonBlank(context: String) = trim().takeIf { it.isNotEmpty() } ?: fail("$context must not be blank")
    private fun JsonElement.strictLong(context: String): Long {
        if (!isJsonPrimitive || !asJsonPrimitive.isNumber) fail("$context must be an integer")
        return try { asJsonPrimitive.asBigDecimal.stripTrailingZeros().longValueExact() } catch (_: Exception) { fail("$context must be an integer") }
    }
    private fun fail(message: String): Nothing = throw TextFormatException(message)
    private fun com.google.gson.JsonObject.need(vararg keys: String) { keys.forEach { if (!has(it)) fail("missing required field '$it'") } }
    private fun com.google.gson.JsonObject.only(vararg keys: String) { (keySet() - keys.toSet()).takeIf { it.isNotEmpty() }?.let { fail("unknown fields: ${it.joinToString()}") } }
}

class TextPackageRegistry : TextCatalog {
    private val packages = LinkedHashMap<String, TextAnimationPackage>()
    override fun presetIds(): List<String> = packages.keys.toList()
    override fun fontAssets(): List<FontAsset> = packages.values.flatMap { value -> value.assets.map { FontAsset(it.path, it.license) } }.distinct()
    fun find(id: String): TextAnimationPackage? = packages[id]
    fun register(value: TextAnimationPackage): TextAnimationPackage {
        if (packages.containsKey(value.id)) throw TextFormatException("Duplicate text package id '${value.id}'")
        packages[value.id] = value
        return value
    }
    fun load(json: String): TextAnimationPackage = register(TextAnimationPackageLoader.load(json))
}
