package com.example.spotter

import org.json.JSONObject

class User(
    val _id : String,
    val username : String,
    val email : String,
    val __v : Int = 0,
    val image : String = "",
    var token : String = ""
) {
    fun toJson() : String {
        val userJson = JSONObject().apply {
            put("_id", _id)
            put("username", username)
            put("email", email)
            put("image", image)
            put("token", token)
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
                token = userJson.optString("token", "")
            )
        }
    }
}