package com.example.spotter

import okhttp3.MultipartBody
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
    val date : String,
    val time : String,
    val location : LOCATION,
    val host : String
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
    var score: String? = "",
    val hostObj: User?,
    var predicted_count: Int? = -1,
    val __v: Int? = 0,
    var image: String? = ""
) {
    fun toEvent() : Event {
        if (predicted_count == null) predicted_count = -1
        if (image == null) image = ""
        if (score == null) score = ""
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
            hostObj = host,
            predicted_count = predicted_count,
            image = image,
            score = score
        )
    }
}

data class LOCATION(
    val type: String,
    val coordinates: List<Double>
)

data class PREDICT_IMG_OUTPUT(
    val predicted_count: Int
)