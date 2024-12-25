package com.example.spotter

import android.media.Image
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class User(
    val username : String,
    val email : String,
    val iconPath : String = "",
) {}

data class Event(
    var title : String,
    var description : String,
    var activity : String = "nogomet",
    var time : Long,
    var location : Pair<Double, Double>,
    var uuid : UUID = UUID.randomUUID(),
    var host : UUID = uuid,
    var hostObj : User? = null,
    var followers : MutableList<UUID> = mutableListOf<UUID>(),
    var notifyOn : Boolean = false
) : Comparable<Event>, Parcelable {

    init {
        RetrofitInstance.api.getUser(host).enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                if (response.isSuccessful) {
                    val message = response.body()?.message
                    Log.i("Output", message ?: "no message")
                    // notify item changed myb
                } else {
                    Log.i("Output", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                Log.i("Output", "Failed ${t.message}")
            }
        })
    }

    fun getLocalDate() : String {
        val dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        return dateTime.toLocalDateTime().toLocalDate().toString()
    }

    fun getLocalTime() : String {
        val dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        val hours = dateTime.toLocalDateTime().toLocalTime().hour
        val mins = dateTime.toLocalDateTime().toLocalTime().minute
        return "${hours}:${if (mins > 9) mins.toString() else "0$mins"}"
    }

    override fun compareTo(other: Event): Int {
        return this.time.compareTo(other.time)
    }
    override fun toString() : String {
        return "$uuid: $title"
    }

    // encode the object
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(activity)
        parcel.writeLong(time)
        parcel.writeDouble(location.first)
        parcel.writeDouble(location.second)
        parcel.writeLong(uuid.mostSignificantBits)
        parcel.writeLong(uuid.leastSignificantBits)
    }

    override fun describeContents(): Int = 0

    // decode to the object
    // I intend to only store my events in the making
    companion object CREATOR : Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event {
            return Event(parcel.readString() ?: "", parcel.readString() ?: "", parcel.readString() ?: "", parcel.readLong(), Pair<Double, Double>(parcel.readDouble(), parcel.readDouble()), UUID(parcel.readLong(), parcel.readLong()))
        }
        override fun newArray(size: Int): Array<Event?> = arrayOfNulls(size)
    }
}