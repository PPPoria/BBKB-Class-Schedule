package com.poria.base.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.poria.base.BaseApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object DSHelper {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_sense_preferences")
    private val ds by lazy { BaseApp.app.dataStore }

    suspend fun setBoolean(key: String, value: Boolean) {
        ds.edit { it[booleanPreferencesKey(key)] = value }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean> {
        return ds.data.map {
            it[booleanPreferencesKey(key)]?: defaultValue
        }
    }

    suspend fun setLong(key: String, value: Long) {
        ds.edit { it[longPreferencesKey(key)] = value }
    }

    fun getLong(key: String, defaultValue: Long = 0): Flow<Long> {
        return ds.data.map {
            it[longPreferencesKey(key)]?: defaultValue
        }
    }

    suspend fun setInt(key: String, value: Int) {
        ds.edit { it[intPreferencesKey(key)] = value }
    }

    fun getInt(key: String, defaultValue: Int = 0): Flow<Int> {
        return ds.data.map {
            it[intPreferencesKey(key)]?: defaultValue
        }
    }

    suspend fun setFloat(key: String, value: Float) {
        ds.edit { it[floatPreferencesKey(key)] = value }
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Flow<Float> {
        return ds.data.map {
            it[floatPreferencesKey(key)] ?: defaultValue
        }
    }

    suspend fun setDouble(key: String, value: Double) {
        ds.edit { it[floatPreferencesKey(key)] = value.toFloat() }
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Flow<Double> {
        return ds.data.map {
            it[floatPreferencesKey(key)]?.toDouble() ?: defaultValue
        }
    }

    suspend fun setString(key: String, value: String) {
        ds.edit { it[stringPreferencesKey(key)] = value }
    }

    fun getString(key: String, defaultValue: String = ""): Flow<String> {
        return ds.data.map {
            it[stringPreferencesKey(key)] ?: defaultValue
        }
    }
}