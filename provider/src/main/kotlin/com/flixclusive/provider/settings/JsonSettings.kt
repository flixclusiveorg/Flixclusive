package com.flixclusive.provider.settings

import com.flixclusive.core.util.log.errorLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal

/**
 *
 * Utility class that acts as settings storage. Uses JSON.
 *
 * @see [SettingsUtilsJSON](https://github.com/Aliucord/Aliucord/blob/main/Aliucord/src/main/java/com/aliucord/SettingsUtilsJSON.kt)
 *
 * */
@Suppress("unused", "KDocUnresolvedReference", "SpellCheckingInspection")
class JsonSettings(
    fileDirectory: String,
    fileName: String,
) {
    private val settingsFile = "$fileDirectory/$fileName.json"

    val gson = Gson()
    val cache: MutableMap<String, Any> = HashMap()
    val settings: JSONObject by lazy {
        val file = File(settingsFile)
        if (file.exists()) {
            val read = file.readText()
            if (read != "") return@lazy JSONObject(read)
        }
        JSONObject()
    }

    init {
        val dir = File(fileDirectory)
        if (!dir.exists() && !dir.mkdirs()) throw RuntimeException("Failed to create settings dir")
    }

    /**
     * Saves settings changes to file
     * */
    private fun writeData() {
        if (settings.length() > 0) {
            val file = File(settingsFile)
            try {
                file.writeText(settings.toString(4))
            } catch (e: Throwable) {
                errorLog("Failed to save settings: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Resets all settings
     * @return true if successful, else false
     */
    fun resetFile() = File(settingsFile).delete()

    /**
     * Toggles Boolean and returns it
     * @param key Key of the value
     * @param defVal Default Value if setting doesn't exist
     * @return Toggled boolean
     */
    fun toggleBool(key: String, defVal: Boolean): Boolean {
        getBool(key, !defVal).also {
            setBool(key, !it)
            return !it
        }
    }

    /**
     * Removes Item from settings
     * @param key Key of the value
     * @return True if removed, else false
     */
    @Synchronized
    fun remove(key: String): Boolean {
        val bool = settings.remove(key) != null
        writeData()
        return bool
    }

    /**
     * Gets All Keys from settings
     * @return List of all keys
     */
    fun getAllKeys(): List<String> {
        val iterator: Iterator<String> = settings.keys()
        val copy: MutableList<String> = ArrayList()
        while (iterator.hasNext()) copy.add(iterator.next())
        return copy
    }

    /**
     * Check if Key exists in settings
     * @param key Key of the value
     * @return True if found, else false
     */
    fun exists(key: String): Boolean = settings.has(key)

    /**
     * Get a boolean from the preferences
     * @param key Key of the value
     * @param defaultValue Default value
     * @return Value if found, else the defaultValue
     */
    fun getBool(key: String, defaultValue: Boolean) = if (settings.has(key)) settings.getBoolean(key) else defaultValue

    /**
     * Set a boolean item
     * @param key Key of the item
     * @param value Value
     */
    fun setBool(key: String, value: Boolean) = putObject(key, value)

    /**
     * Get an int from the preferences
     * @param key Key of the value
     * @param defaultValue Default value
     * @return Value if found, else the defaultValue
     */
    fun getInt(key: String, defaultValue: Int) =
        if (settings.has(key)) settings.getInt(key) else defaultValue

    @Synchronized
    private fun putObject(key: String, value: Any?) {
        settings.put(key, value)
        writeData()
    }

    /**
     * Set an int item
     * @param key Key of the item
     * @param value Value
     */
    fun setInt(key: String, value: Int) = putObject(key, value)

    /**
     * Get a float from the preferences
     * @param key Key of the value
     * @param defaultValue Default value
     * @return Value if found, else the defaultValue
     */
    fun getFloat(key: String, defaultValue: Float) =
        if (settings.has(key)) BigDecimal.valueOf(settings.getDouble(key)).toFloat() else defaultValue

    /**
     * Set a float item
     * @param key Key of the item
     * @param value Value
     */
    fun setFloat(key: String, value: Float) = putObject(key, value)

    /**
     * Get a long from the preferences
     * @param key Key of the value
     * @param defaultValue Default value
     * @return Value if found, else the defaultValue
     */
    fun getLong(key: String, defaultValue: Long) = if (settings.has(key)) settings.getLong(key) else defaultValue

    /**
     * Set a long item
     * @param key Key of the item
     * @param value Value
     */
    fun setLong(key: String, value: Long) = putObject(key, value)

    /**
     * Get a [String] from the preferences
     * @param key Key of the value
     * @param defaultValue Default value
     * @return Value if found, else the defaultValue
     */
    fun getString(key: String, defaultValue: String?) =
        if (settings.has(key)) settings.getString(key) else defaultValue

    /**
     * Set a [String] item
     * @param key Key of the item
     * @param value Value
     */
    fun setString(key: String, value: String?) = putObject(key, value)

    /**
     * Get a [JSONObject] item
     * @param key Key of the item
     * @param defaultValue Default value
     * @return Value if found, else the defaultValue
     */
    fun getJSONObject(key: String, defaultValue: JSONObject?) =
        if (settings.has(key)) settings.getJSONObject(key) else defaultValue

    /**
     * Set a [JSONObject] item
     * @param key Key of the item
     * @param value Value
     */
    fun setJSONObject(key: String, value: JSONObject) = putObject(key, value)

    /**
     * Get an [Object] from the preferences
     * @param key Key of the value
     * @param defaultValue Default value
     *
     * @return Value if found, else the defaultValue
     */
    inline fun <reified T> getObject(
        key: String,
        defaultValue: T? = null
    ): T? {
        val cached = cache[key]
        if (cached != null) try {
            return cached as T
        } catch (ignored: Throwable) { }

        val t: T? = when {
            settings.has(key) -> gson.fromJson(
                /* json = */ settings.getString(key),
                /* typeOfT = */ object : TypeToken<T>() {}.type
            )
            else -> null
        }
        return t ?: defaultValue
    }

    /**
     * Set an [Object] item
     * @param key Key of the item
     * @param value Value
     */
    fun setObject(key: String, value: Any) {
        cache[key] = value
        val stringJson = gson.toJson(value)
        putObject(key, if (stringJson.startsWith("{")) JSONObject(stringJson) else JSONArray(stringJson))
    }
}