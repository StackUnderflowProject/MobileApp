package com.example.spotter

import android.content.Context
import android.util.Log
import android.widget.Toast
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

    fun getAllEvents(context: Context) {
        RetrofitInstance.api.getAllEvents().enqueue(object : Callback<List<GET_ALL_EVENTS_MODEL>> {
            override fun onResponse(
                call: Call<List<GET_ALL_EVENTS_MODEL>>,
                response: Response<List<GET_ALL_EVENTS_MODEL>>
            ) {
                if (response.isSuccessful) {
                    val receivedEvents = response.body() ?: emptyList()
                    val evnts = mutableListOf<Event>()
                    receivedEvents.forEach { event ->
                        evnts.add(event.toEvent())
                        Log.i("MyEvents", event.name)
                    }
                    action = 0
                    events.value = evnts
                } else {
                    Log.i("Output", call.toString())
                    Log.i("Output", "getAllEvents(), Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<GET_ALL_EVENTS_MODEL>>, t: Throwable) {
                Log.i("Output", "Failed ${t.message}")
                Toast.makeText(context, "Failed to connect to server :(", Toast.LENGTH_LONG).show()
            }
        })
    }


    fun addItem(e: Event, callback: (Boolean) -> Unit) {
        val body = CREATE_EVENT_MODEL(e.name, e.description, e.activity, e.date, e.time, e.location, e.host)

        RetrofitInstance.api.createEvent(body).enqueue(object : Callback<ServerResponse> {
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
                    Log.i("Output", "createEvent(), Error: ${response.code()}")
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
        val i = currentList.indexOfFirst { it._id == e._id }
        if (i != -1) {
            index = i
            currentList.removeAt(i)
        }
        action = 2
        events.value = currentList
    }

    fun updateItem(e: Event) {
        val currentList = events.value.orEmpty().toMutableList()
        val i = currentList.indexOfFirst { it._id == e._id }
        if (i != -1) {
            index = i
            currentList[i] = e
        }
        action = 3
        events.value = currentList
    }

    fun followEvent(e: Event, user: User?) {
        RetrofitInstance.api.subscribeToEvent("Bearer: " + user?.token, e._id).enqueue(object : Callback<Event> {
            override fun onResponse(
                call: Call<Event>,
                response: Response<Event>
            ) {
                if (response.isSuccessful) {
                    Log.i("Output", "+ " + response.body()?.toString())
                    val receivedEvent = response.body() ?: return
                    action = 3
                    val currentList = events.value.orEmpty().toMutableList()
                    val i = currentList.indexOfFirst { it._id == e._id }
                    if (i != -1) {
                        index = i
                        if (receivedEvent.hostObj == null) {
                            RetrofitInstance.api.getUser(receivedEvent.host).enqueue(object : Callback<User> {
                                override fun onResponse(
                                    call: Call<User>,
                                    response: Response<User>
                                ) {
                                    if (response.isSuccessful) {
                                        receivedEvent.hostObj = response.body()
                                        val updatedList = currentList.toMutableList().apply {this[i] = receivedEvent}
                                        events.value = updatedList
                                        Log.i("Output", "Updated")
                                    } else {
                                        Log.i("Output", "Error: ${response.code()}")
                                    }
                                }

                                override fun onFailure(call: Call<User>, t: Throwable) {
                                    Log.i("Output", "Failed ${t.message}")
                                }
                            })
                        }
                    }
                } else {
                    Log.i("Output", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Event>, t: Throwable) {
                Log.i("Output", "Failed ${t.message}")
            }
        })
    }
}

