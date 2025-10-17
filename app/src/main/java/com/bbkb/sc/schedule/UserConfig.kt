package com.bbkb.sc.schedule

import androidx.core.graphics.toColorInt
import com.bbkb.sc.datastore.StringKeys
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.poria.base.store.DSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class JsonDataDelegate<T>(
    private val key: String,
    private val defaultValue: T,
    private val typeToken: TypeToken<T>,
) {
    private var mutableLatest: T? = null

    operator fun getValue(thisRef: Any?, property: Any): T {
        val latest = mutableLatest ?: getOrDefault()
        return latest
    }

    operator fun setValue(thisRef: Any?, property: Any, value: T) {
        mutableLatest = value
        save(value)
    }

    private fun save(value: T) {
        val gson = GsonBuilder().serializeNulls().create()
        CoroutineScope(Dispatchers.IO).launch {
            DSManager.setString(
                key = key,
                value = gson.toJson(value)
            )
        }
    }

    private fun getOrDefault(): T {
        val gson = GsonBuilder().serializeNulls().create()
        gson.toJson(defaultValue)
        val json = runBlocking {
            DSManager.getString(
                key = key,
                defaultValue = ""
            ).first()
        }
        mutableLatest = if (json.isEmpty()) defaultValue
        else gson.fromJson(json, typeToken.type)
        return mutableLatest!!
    }
}

internal inline fun <reified T> jsonDelegate(
    key: String,
    defaultValue: T
): JsonDataDelegate<T> = JsonDataDelegate(key, defaultValue, object : TypeToken<T>() {})

data class TableConfig(
    val ignoreSaturday: Boolean = false,
    val ignoreSunday: Boolean = false,
    val ignoreEvening: Boolean = false,
    val nameFilter: String = "",
    val majorFilter: String = "",
) {
    companion object {
        var latest by jsonDelegate(StringKeys.TABLE_CONFIG, TableConfig())
    }
}

data class TableAttr(
    /* size */
    val xAxisDayOfWeekTextSizeScale: Float = 1.0f,
    val xAxisDateTextSizeScale: Float = 1.0f,
    val xAxisHeightScale: Float = 1.0f,
    val yAxisNodeNumberTextSizeScale: Float = 1.0f,
    val yAxisTimeTexSizeStScale: Float = 1.0f,
    val yAxisWidthScale: Float = 1.0f,
    val courseNameTextSizeScale: Float = 1.0f,
    val courseTeacherAndRoomTextSizeScale: Float = 1.0f,
    val tableHeightScale: Float = 1.0f,
    /* color */
    val tableBgImgMaskColor: Int = "#000000".toColorInt(),
    val tableBgImgMaskAlpha: Float = 0.5f,
    val courseColorHHueOffset: Int = 0,
    val courseColorSHueBase: Int = 60,
    val courseColorLHueBase: Int = 85,
) {
    companion object {
        var latest by jsonDelegate(StringKeys.TABLE_ATTR, TableAttr())
    }
}