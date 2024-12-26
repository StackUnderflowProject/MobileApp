package com.example.spotter

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SpotterApp : Application() {
    private val viewModelStore = ViewModelStore()
    val eventsViewModel: EventViewModel by lazy {
        ViewModelProvider(ViewModelStore(), ViewModelProvider.NewInstanceFactory())[EventViewModel::class.java]
    }

    override fun onCreate() {
        super.onCreate()
        eventsViewModel.getAllEvents()
    }

    override fun onTerminate() {
        super.onTerminate()
        viewModelStore.clear()
    }
}