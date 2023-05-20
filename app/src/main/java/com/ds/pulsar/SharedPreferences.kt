package com.ds.pulsar

import androidx.core.content.edit
import kotlin.reflect.KProperty

class PreferenceDelegate<T>(val name: String, val default: T) {
    operator fun getValue(thisRef: HeartRateStream, property: KProperty<*>) : T {
        val res = when (default) {
            is Long -> preferences.getLong(name, default)
            is String -> preferences.getString(name, default)
            else -> throw IllegalArgumentException("This type cannot be saved into Preferences")
        }
        @Suppress("UNCHECKED_CAST")
        return res as T
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = preferences.edit {
        when (value){
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            else -> throw IllegalArgumentException("This type cannot be saved into Preferences")
        }
    }
}
