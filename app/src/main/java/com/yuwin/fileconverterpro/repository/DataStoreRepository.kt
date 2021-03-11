package com.yuwin.fileconverterpro.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


const val PREFERENCE_NAME = "app_preference"

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(PREFERENCE_NAME)

class DataStoreRepository(val context: Application) {

    private object PreferencesKeys {
        val Quality = intPreferencesKey("quality")
        val Opened = intPreferencesKey("opened")
        val Review = booleanPreferencesKey("review")
        val Format = intPreferencesKey("format")
        val IsGrid = booleanPreferencesKey("grid")
        val Storage = stringPreferencesKey("storage")
        val Premium = intPreferencesKey("premium")
    }

    suspend fun setPremium(value: Int) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.Premium] = value
        }
    }

    val isPremiumMember: Flow<Int> = context.appDataStore.data
        .catch { exception ->
            if(exception is IOException) {
                emit(emptyPreferences())
            }else {
                throw exception
            }
        }
        .map { preferences ->
            val isPremium = preferences[PreferencesKeys.Premium] ?: 0
            isPremium
        }


    suspend fun setDefaultQuality(value: Int) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.Quality] = value
        }
    }

    val defaultQuality: Flow<Int> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.d("settings", exception.message.toString())
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val uiMode = preferences[PreferencesKeys.Quality] ?: 100
            uiMode
        }

    suspend fun incrementOpenedTimes() {
        context.appDataStore.edit { settings ->
            val currentOpenedValue = settings[PreferencesKeys.Opened] ?: 0
            settings[PreferencesKeys.Opened] = currentOpenedValue + 1
        }
    }

    val openedTimes: Flow<Int> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw  exception
            }
        }
        .map { preferences ->
            val openedTime = preferences[PreferencesKeys.Opened] ?: 0
            openedTime
        }

    suspend fun setReviewPrompted(value: Boolean) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.Review] = value
        }
    }

    val reviewPrompted: Flow<Boolean> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val reviewPrompted = preferences[PreferencesKeys.Review] ?: false
            reviewPrompted
        }


    suspend fun setDefaultFormatType(value: Int) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.Format] = value
        }
    }

    val formatType: Flow<Int> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val defaultFormat = preferences[PreferencesKeys.Format] ?: 0
            defaultFormat
        }

    suspend fun setIsGrid(value: Boolean) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.IsGrid] = value
        }
    }

    val isGridEnabled: Flow<Boolean> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isGrid = preferences[PreferencesKeys.IsGrid] ?: false
            isGrid
        }

    suspend fun setStorageDirectory(path: String) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.Storage] = path
        }
    }

    val storageDirectory: Flow<String> = context.appDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw  exception
            }
        }
        .map { preferences ->
            val path = preferences[PreferencesKeys.Storage]
                ?: com.yuwin.fileconverterpro.Util.getExternalDir(context)
            path
        }

}