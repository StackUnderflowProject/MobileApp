package com.example.spotter

import android.util.Log
import org.bson.types.ObjectId
import java.time.ZoneId
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.abs

data class Event(
    var name : String,
    var description : String,
    var activity : String = "nogomet",
    var date : LocalDate,
    var time : String,
    var location : LOCATION,
    var _id : ObjectId = ObjectId(),
    var host : ObjectId = _id,
    var followers : MutableList<ObjectId> = mutableListOf<ObjectId>(),
    var hostObj : User? = null,
    val score: String? = "",
    val predicted_count: Int? = -1,
    val __v: Int? = 0,
    val image: String? = "",
    var notifyOn : Boolean = false
) : Comparable<Event> {

    override fun compareTo(other: Event): Int {
        return this.time.compareTo(other.time)
    }
    override fun toString(): String {
        return "Event(name='$name', description='$description', activity='$activity', date='$date', " +
                "time='$time', location='$location', _id='$_id', host='$host', followers=$followers, " +
                "hostObj=$hostObj, score=$score, predicted_count=$predicted_count, __v=$__v, image='$image', " +
                "notifyOn=$notifyOn)"
    }

    fun isHappeningToday() : Boolean {
        val timeString = time
        val timeParts = timeString.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val timeDiff = date.atTime(LocalTime.of(hours, minutes)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
        return timeDiff > 0 && timeDiff < (3600000) || timeDiff < 0 && timeDiff > (-5 * 3600000)
    }
}

fun convertToLocalDate(isoDate: String): LocalDate {
    val zonedDateTime = ZonedDateTime.parse(isoDate)
    val localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault())
    return localDateTime.toLocalDate()
}