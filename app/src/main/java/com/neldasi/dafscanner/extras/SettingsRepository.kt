package com.neldasi.dafscanner.extras

import android.content.Context
import androidx.core.content.edit

object SettingsRepository {

    val defaultAllowedTypes = setOf(
        "2245293", "2245295", "2261325", "2150001", "2342199", "2342201", "2012566",
    )

    fun loadAllowedTypes(context: Context): Set<String> {
        val prefs = ScanStorage.prefs(context)
        return prefs.getStringSet(ScanStorage.Keys.ALLOWED_TYPES, defaultAllowedTypes) ?: defaultAllowedTypes
    }
    
    fun shouldKeepScreenOn(context: Context): Boolean {
        val prefs = ScanStorage.prefs(context)
        return prefs.getBoolean(ScanStorage.Keys.SCREEN_ALWAYS_ON, true)
    }

    fun getTheme(context: Context): String {
        val prefs = ScanStorage.prefs(context)
        return prefs.getString(ScanStorage.Keys.APP_THEME, "DAF") ?: "DAF"
    }

    fun setTheme(context: Context, theme: String) {
        val prefs = ScanStorage.prefs(context)
        prefs.edit { putString(ScanStorage.Keys.APP_THEME, theme) }
    }

    fun getFontSizeScale(context: Context): Float {
        val prefs = ScanStorage.prefs(context)
        return prefs.getFloat(ScanStorage.Keys.FONT_SIZE_SCALE, 1.0f)
    }

    fun setFontSizeScale(context: Context, scale: Float) {
        val prefs = ScanStorage.prefs(context)
        prefs.edit { putFloat(ScanStorage.Keys.FONT_SIZE_SCALE, scale) }
    }

    fun saveAllowedTypes(context: Context, types: Collection<String>) {
        val prefs = ScanStorage.prefs(context)
        prefs.edit { putStringSet(ScanStorage.Keys.ALLOWED_TYPES, types.toSet()) }
    }
}
