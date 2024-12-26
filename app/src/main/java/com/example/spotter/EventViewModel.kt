package com.example.spotter

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventViewModel : ViewModel() {
    private val events = MutableLiveData<MutableList<Event>>(mutableListOf())
    // to optimize UI adapters
    var action : Int = 0 // 0 -> change || 1 -> insert || 2 -> delete || 3 -> update
    var index : Int = 0 // index in case I am removing/updating an event from list

    val currentEvents : LiveData<MutableList<Event>> get() = events

    fun getAllEvents() {
        RetrofitInstance.api.getAllEvents().enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                if (response.isSuccessful) {
                    val message = response.body()?.message
                    Log.i("Output", message ?: "no message")
                    //action.value = 0
                    //events.value = message.events
                } else {
                    Log.i("Output", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                Log.i("Output", "Failed ${t.message}")
            }
        })
    }

    fun addItem(e: Event, callback: (Boolean) -> Unit) {
        RetrofitInstance.api.createEvent(e).enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                if (response.isSuccessful) {
                    val message = response.body()?.message
                    Log.i("Output", message ?: "no message")
                    val currentList = events.value.orEmpty().toMutableList()
                    currentList.add(0, e)
                    action = 1
                    events.value = currentList
                    callback(true)
                } else {
                    Log.i("Output", "Error: ${response.code()}")
                    callback(false)
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                Log.i("Output", "Failed ${t.message}")
                callback(false)
            }
        })
    }

    fun removeItem(e: Event) {
        val currentList = events.value.orEmpty().toMutableList()
        val i = currentList.indexOfFirst { it.uuid == e.uuid }
        if (i != -1) {
            index = i
            currentList.removeAt(i)
        }
        action = 2
        events.value = currentList
    }

    fun updateItem(e: Event) {
        val currentList = events.value.orEmpty().toMutableList()
        val i = currentList.indexOfFirst { it.uuid == e.uuid }
        if (i != -1) {
            index = i
            currentList[i] = e
        }
        action = 3
        events.value = currentList
    }
}

