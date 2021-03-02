package com.yuwin.fileconverterpro.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore


const val PREFERENCE_NAME = "app_preference"


class DataStoreRepository(context: Context) {

    val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(PREFERENCE_NAME)


}