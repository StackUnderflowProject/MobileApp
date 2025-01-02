package com.example.spotter

import org.bson.types.ObjectId
import java.time.LocalDate

data class LOGIN_MODEL(
    val username : String,
    val password : String
) {}

data class REGISTER_MODEL(
    val username : String,
    val email : String,
    val password : String
) {}

data class CREATE_EVENT_MODEL(
    val name : String,
    val description : String,
    val activity : String,
    val date : LocalDate,
    val time : String,
    val location : Pair<Double, Double>,
    val host : String
) {}

class GET_ALL_EVENTS_MODEL(
    val location: LOCATION,
    val _id: String,
    val name: String,
    val description: String,
    val activity: String,
    val date: String,
    val time: String,
    val host: User,
    val followers: MutableList<String>,
    val score: String? = "",
    val hostObj: User?,
    val predicted_count: Int? = 0,
    val __v: Int? = 0,
    val image: String? = ""
) {
    fun toEvent() : Event {
        val followersObjects = mutableListOf<ObjectId>()
        followers.forEach { f -> followersObjects.add(ObjectId(f)) }
        return Event(
            name,
            description,
            activity,
            convertToLocalDate(date),
            time,
            Pair<Double, Double>(location.coordinates[1], location.coordinates[0]),
            ObjectId(_id),
            ObjectId(host._id),
            followersObjects,
            hostObj = host
        )
    }
}

data class LOCATION(
    val type: String,
    val coordinates: List<Double>
)