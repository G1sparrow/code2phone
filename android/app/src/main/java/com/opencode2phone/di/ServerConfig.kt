package com.opencode2phone.di

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("server_config", Context.MODE_PRIVATE)

    var host: String
        get() = prefs.getString("host", "100.100.100.100") ?: "100.100.100.100"
        set(value) = prefs.edit().putString("host", value).apply()

    var port: Int
        get() = prefs.getInt("port", 3001)
        set(value) = prefs.edit().putInt("port", value).apply()
}
