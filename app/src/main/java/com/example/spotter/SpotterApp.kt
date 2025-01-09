package com.example.spotter

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class SpotterApp : Application() {
    private val viewModelStore = ViewModelStore()
    val eventsViewModel: EventViewModel by lazy {
        ViewModelProvider(ViewModelStore(), ViewModelProvider.NewInstanceFactory())[EventViewModel::class.java]
    }

    var user : User? = null

    override fun onCreate() {
        super.onCreate()
        eventsViewModel.getAllEvents(this)
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

    fun scheduleNotification(event: Event) {
        val timeString = event.time
        val timeParts = timeString.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val delay = event.date.atTime(LocalTime.of(hours, minutes)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis() - (4 * 3600000) // 4 hours before the event
        if (delay < 0) return

        val workData = Data.Builder()
            .putString("name", event.name)
            .putString("description", "event you are subscribed to is happening in 4 hours")
            .putString("id", event._id.toString())
            .build()

        val workRequest = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
}