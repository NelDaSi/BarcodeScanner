package com.neldasi.dafscanner.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import com.neldasi.dafscanner.data.AppDatabase
import com.neldasi.dafscanner.extras.ScanStorage
import com.neldasi.dafscanner.extras.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _updateInfo = MutableStateFlow<UpdateManager.ReleaseInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(value = false)
    val isCheckingUpdates = _isCheckingUpdates.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage = _updateMessage.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            when (val result = UpdateManager.checkForUpdates(getApplication())) {
                is UpdateManager.UpdateResult.NewUpdate -> {
                    _updateInfo.value = result.info
                }
                is UpdateManager.UpdateResult.UpToDate -> {
                    _updateMessage.value = "App is up to date"
                }
                is UpdateManager.UpdateResult.Error -> {
                    _updateMessage.value = result.message
                }
            }
            _isCheckingUpdates.value = false
        }
    }

    fun clearUpdateMessage() {
        _updateMessage.value = null
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            
            // 1. Explicitly clear the database first
            try {
                AppDatabase.getDatabase(context).clearAllTables()
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error clearing database", e)
            }

            // 2. Synchronously clear all SharedPreferences using the KTX extension
            try {
                val prefs = ScanStorage.prefs(context)
                prefs.edit(commit = true) {
                    clear()
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error clearing SharedPreferences", e)
            }

            // 3. Delete files manually just in case
            try {
                context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error deleting files/cache", e)
            }

            // 4. Nuclear option: Ask the system to wipe the app data and kill the process
            withContext(Dispatchers.Main) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.clearApplicationUserData()
            }
        }
    }
}
