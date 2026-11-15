package org.samvid.orchit.audio

import com.google.gson.JsonParser

data class AudioClipSettings(val volume: Float = 1f, val fadeInUs: Long = 0, val fadeOutUs: Long = 0, val speed: Float = 1f)
data class AudioPackageAsset(val id: String, val path: String, val license: String)
data class AudioAssetPackage(val formatVersion: Int, val id: String, val name: String, val assets: List<AudioPackageAsset>)
class AudioFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object AudioClipSettingsValidator {
    fun validate(settings: AudioClipSettings): AudioClipSettings {
        if (!settings.volume.isFinite() || settings.volume !in 0f..1f) throw AudioFormatException("volume must be finite and between 0 and 1")
        if (settings.fadeInUs < 0 || settings.fadeOutUs < 0) throw AudioFormatException("fade durations must not be negative")
        if (!settings.speed.isFinite() || settings.speed !in 0.25f..4f) throw AudioFormatException("speed must be finite and between 0.25 and 4")
        return settings
    }
}

/** Strict v1 manifest: formatVersion, id, name, and assets[{id,path,license}]. */
object AudioAssetPackageLoader {
    private val openLicenses = setOf("CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0", "MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "OFL-1.1", "0BSD")
    fun load(json: String): AudioAssetPackage = try {
        val root = JsonParser.parseString(json).asJsonObject
        root.only("formatVersion", "id", "name", "assets")
        root.need("formatVersion", "id", "name", "assets")
        if (root.get("formatVersion").strictLong("formatVersion") != 1L) fail("only audio package formatVersion 1 is supported")
        val id = root.get("id").asString.nonBlank("id")
        val name = root.get("name").asString.nonBlank("name")
        val entries = root.getAsJsonArray("assets").mapIndexed { index, value ->
            val context = "assets[$index]"
            val asset = value.asJsonObject
            asset.only("id", "path", "license")
            asset.need("id", "path", "license")
            val assetId = asset.get("id").asString.nonBlank("$context.id")
            val path = asset.get("path").asString.nonBlank("$context.path")
            if (path.startsWith('/') || path.split('/').any { it == ".." }) fail("$context.path must be a local package path")
            val license = asset.get("license").asString.nonBlank("$context.license")
            if (license !in openLicenses) fail("$context.license '$license' is not an allowed copyright-free or open-source license")
            AudioPackageAsset(assetId, path, license)
        }
        if (entries.map { it.id }.toSet().size != entries.size) fail("asset ids must be unique")
        AudioAssetPackage(1, id, name, entries)
    } catch (exception: AudioFormatException) {
        throw exception
    } catch (exception: Exception) {
        throw AudioFormatException("Invalid audio package manifest: ${exception.message}", exception)
    }

    private fun String.nonBlank(context: String) = trim().takeIf { it.isNotEmpty() } ?: fail("$context must not be blank")
    private fun com.google.gson.JsonElement.strictLong(context: String): Long {
        if (!isJsonPrimitive || !asJsonPrimitive.isNumber) fail("$context must be an integer")
        return try { asJsonPrimitive.asBigDecimal.stripTrailingZeros().longValueExact() } catch (_: Exception) { fail("$context must be an integer") }
    }
    private fun fail(message: String): Nothing = throw AudioFormatException(message)
    private fun com.google.gson.JsonObject.need(vararg keys: String) { keys.forEach { if (!has(it)) fail("missing required field '$it'") } }
    private fun com.google.gson.JsonObject.only(vararg keys: String) { (keySet() - keys.toSet()).takeIf { it.isNotEmpty() }?.let { fail("unknown fields: ${it.joinToString()}") } }
}

class AudioPackageRegistry {
    private val packages = LinkedHashMap<String, AudioAssetPackage>()
    fun ids(): List<String> = packages.keys.toList()
    fun find(id: String): AudioAssetPackage? = packages[id]
    fun register(value: AudioAssetPackage): AudioAssetPackage {
        if (packages.containsKey(value.id)) throw AudioFormatException("Duplicate audio package id '${value.id}'")
        packages[value.id] = value
        return value
    }
    fun load(json: String): AudioAssetPackage = register(AudioAssetPackageLoader.load(json))
}
