package com.example.spotter

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.bson.types.ObjectId
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


    fun addItem(e: Event, user: User?, callback: (Boolean) -> Unit) {
        val body = CREATE_EVENT_MODEL(e.name, e.description, e.activity, e.date.toString(), e.time, e.location, e.host.toString())

        RetrofitInstance.api.createEvent("Bearer: " + user?.token, body).enqueue(object : Callback<Event> {
            override fun onResponse(
                call: Call<Event>,
                response: Response<Event>
            ) {
                if (response.isSuccessful) {
                    val receivedEvent = response.body()
                    val currentList = events.value.orEmpty().toMutableList()
                    getHostObj(receivedEvent!!.host) {user ->
                        if (user != null) receivedEvent.hostObj = user
                        Log.i("Output", "$user")
                        currentList.add(receivedEvent)
                        action = 1
                        index = currentList.size - 1
                        events.value = currentList
                        callback(true)
                    }
                } else {
                    Log.i("Output", "createEvent(), Error: ${response.code()}")
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Event>, t: Throwable) {
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
                            getHostObj(receivedEvent.host) {user ->
                                if (user != null) receivedEvent.hostObj = user
                            }
                        }
                        val updatedList = currentList.toMutableList().apply {this[i] = receivedEvent}
                        events.value = updatedList
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

    private fun getHostObj(host: ObjectId, callback: (User?) -> Unit) {
        RetrofitInstance.api.getUser(host).enqueue(object : Callback<User> {
            override fun onResponse(
                call: Call<User>,
                response: Response<User>
            ) {
                if (response.isSuccessful) {
                    callback(response.body())
                    Log.i("Output", "Updated")
                } else {
                    Log.i("Output", "Error: ${response.code()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.i("Output", "Failed ${t.message}")
                callback(null)
            }
        })
    }
}

fun getPredictedCount(user: User?, event: Event, image: MultipartBody.Part, callback: (Int) -> Unit) {
    RetrofitInstance2.api.getPredictedCount("Bearer: " + user?.token,  RequestBody.create("text/plain".toMediaTypeOrNull(), "dense"), image).enqueue(object : Callback<PREDICT_IMG_OUTPUT> {
        override fun onResponse(
            call: Call<PREDICT_IMG_OUTPUT>,
            response: Response<PREDICT_IMG_OUTPUT>
        ) {
            if (response.isSuccessful) {
                Log.i("Output", "+ " + response.body()?.toString())
                val receivedCount = response.body()
                callback(receivedCount!!.predicted_count)
            } else {
                Log.i("Output", "Error: ${response.code()}")
                callback(-1)
            }
        }

        override fun onFailure(call: Call<PREDICT_IMG_OUTPUT>, t: Throwable) {
            Log.i("Output", "Failed ${t.message}")
            callback(-1)
        }
    })
}

fun uploadImgResults(user: User?, event: Event, image: MultipartBody.Part, predictedCount: Int, callback: (Event?) -> Unit) {
    RetrofitInstance.api.uploadEventImg("Bearer: " + user?.token, event._id, image).enqueue(object : Callback<ServerResponse> {
        override fun onResponse(
            call: Call<ServerResponse>,
            response: Response<ServerResponse>
        ) {
            if (response.isSuccessful) {
                Log.i("Output", "+ " + response.body()?.toString())
                RetrofitInstance.api.uploadPredictedCount("Bearer: " + user?.token, event._id, PREDICT_IMG_OUTPUT(predictedCount)).enqueue(object : Callback<Event> {
                    override fun onResponse(
                        call: Call<Event>,
                        response: Response<Event>
                    ) {
                        if (response.isSuccessful) {
                            Log.i("Output", "+ " + response.body()?.toString())
                            val receivedEvent = response.body()
                            callback(receivedEvent)
                        } else {
                            Log.i("Output", "Error: ${response.code()}")
                            callback(null)
                        }
                    }

                    override fun onFailure(call: Call<Event>, t: Throwable) {
                        Log.i("Output", "Failed ${t.message}")
                        callback(null)
                    }
                })
            } else {
                Log.i("Output", "Error: ${response.code()}")
                callback(null)
            }
        }

        override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
            Log.i("Output", "Failed ${t.message}")
            callback(null)
        }
    })
}


