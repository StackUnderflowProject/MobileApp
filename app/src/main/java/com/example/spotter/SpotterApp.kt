package com.example.spotter

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SpotterApp : Application() {
    private val viewModelStore = ViewModelStore()
    val eventsViewModel: EventViewModel by lazy {
        ViewModelProvider(ViewModelStore(), ViewModelProvider.NewInstanceFactory())[EventViewModel::class.java]
    }

    var user : User? = null

    override fun onCreate() {
        super.onCreate()
        eventsViewModel.getAllEvents()
    }

    override fun onTerminate() {
        super.onTerminate()
        viewModelStore.clear()
    }

    fun storeUser(context: Context, user: User) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val editor = sharedPreferences.edit()
        editor.putString("user_data", user.toJson())
        editor.apply()
    }

    fun getUser(context: Context): User? {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val userJsonString = sharedPreferences.getString("user_data", null) ?: return null
        return User.toObject(userJsonString)
    }
}