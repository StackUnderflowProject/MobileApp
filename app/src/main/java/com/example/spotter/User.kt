package com.example.spotter

import org.bson.types.ObjectId
import org.json.JSONObject

class User(
    val _id : ObjectId,
    val username : String,
    val email : String,
    val __v : Int = 0,
    val image : String = "",
    var token : String = "",
    var loginTime : Long = 0
) {
    fun toJson() : String {
        val userJson = JSONObject().apply {
            put("_id", _id.toString())
            put("username", username)
            put("email", email)
            put("image", image)
            put("token", token)
            put("loginTime", loginTime)
        }
        return userJson.toString()
    }

    fun isLoginValid() : Boolean {
        return System.currentTimeMillis() - loginTime < 3550000 // a little less than a hour
    }

    companion object {
        fun toObject(userString : String) : User {
            val userJson = JSONObject(userString)
            return User(
                _id = ObjectId(userJson.getString("_id")),
                username = userJson.getString("username"),
                email = userJson.getString("email"),
                image = userJson.optString("image", ""),
                token = userJson.optString("token", ""),
                loginTime = userJson.optLong("loginTime", 0)
            )
        }
    }
}