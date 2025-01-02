package com.example.spotter

import android.util.Log
import org.bson.types.ObjectId
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Date

class User(
    val _id : String,
    val username : String,
    val email : String,
    val __v : Int = 0,
    val image : String = "",
    var jwt : String = ""
) {
    fun toJson() : String {
        val userJson = JSONObject().apply {
            put("_id", _id)
            put("username", username)
            put("email", email)
            put("image", image)
            put("jwt", jwt)
        }
        return userJson.toString()
    }

    companion object {
        fun toObject(userString : String) : User {
            val userJson = JSONObject(userString)
            return User(
                _id = userJson.getString("_id"),
                username = userJson.getString("username"),
                email = userJson.getString("email"),
                image = userJson.optString("image", ""),
                jwt = userJson.optString("jwt", "")
            )
        }
    }
}

data class Event(
    var title : String,
    var description : String,
    var activity : String = "nogomet",
    var date : LocalDate,
    var time : String,
    var location : Pair<Double, Double>,
    var id : ObjectId = ObjectId(),
    var host : ObjectId = id,
    var followers : MutableList<ObjectId> = mutableListOf<ObjectId>(),
    var hostObj : User? = null,
    var notifyOn : Boolean = false
) : Comparable<Event> {

    init {
        if (hostObj == null) {
            RetrofitInstance.api.getUser(host).enqueue(object : Callback<User> {
                override fun onResponse(
                    call: Call<User>,
                    response: Response<User>
                ) {
                    if (response.isSuccessful) {
                        val message = response.body()
                        Log.i("Output", "ok")
                        // notify item changed myb
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


    override fun compareTo(other: Event): Int {
        return this.time.compareTo(other.time)
    }
    override fun toString() : String {
        return "$id: $title"
    }

}

fun convertToLocalDate(isoDate: String): LocalDate {
    val zonedDateTime = ZonedDateTime.parse(isoDate)
    val localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault())
    return localDateTime.toLocalDate()
}