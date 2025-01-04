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
    val location : LOCATION,
    val host : ObjectId
) {}

class GET_ALL_EVENTS_MODEL(
    val location: LOCATION,
    val _id: ObjectId,
    val name: String,
    val description: String,
    val activity: String,
    val date: String,
    val time: String,
    val host: User,
    val followers: MutableList<ObjectId>,
    val score: String? = "",
    val hostObj: User?,
    val predicted_count: Int? = 0,
    val __v: Int? = 0,
    val image: String? = ""
) {
    fun toEvent() : Event {
        return Event(
            name,
            description,
            activity,
            convertToLocalDate(date),
            time,
            LOCATION("point", listOf(location.coordinates[1], location.coordinates[0])),
            _id,
            host._id,
            followers,
            hostObj = host
        )
    }
}

data class LOCATION(
    val type: String,
    val coordinates: List<Double>
)