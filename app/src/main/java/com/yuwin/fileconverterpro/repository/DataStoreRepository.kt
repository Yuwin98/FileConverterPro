package com.yuwin.fileconverterpro.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
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
    }


    suspend fun setDefaultQuality(value: Int) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.Quality] = value
        }
    }

    val defaultQuality: Flow<Int> = context.appDataStore.data
        .catch { exception ->
            if(exception is IOException) {
                Log.d("settings", exception.message.toString())
                emit(emptyPreferences())
            }else {
                throw exception
            }
        }.map { preferences ->
            val uiMode = preferences[PreferencesKeys.Quality] ?: 100
            uiMode
        }


}