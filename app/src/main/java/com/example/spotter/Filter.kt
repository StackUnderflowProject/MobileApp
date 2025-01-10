package com.example.spotter

import android.util.Log
import org.osmdroid.util.GeoPoint
import java.sql.Time
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class Category(val category: String) {
    ALL("All"),
    FOOTBALL("Football"),
    HANDBALL("Handball"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): Category {
            val category : Category? = entries.find { it.category.equals(value, ignoreCase = true) }
            if (category != null) return category
            else return ALL
        }
    }
}

enum class TimeInterval(val time: String) {
    UPCOMING("Upcoming"),
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    ALL("All");

    companion object {
        fun fromString(value: String): TimeInterval {
            val timeInterval: TimeInterval? = entries.find { it.time.equals(value, ignoreCase = true) }
            return timeInterval ?: ALL
        }
    }
}

class Filter(
    var category: Category = Category.ALL,
    var distance: Int = 0,
    var time: TimeInterval = TimeInterval.ALL
) {
     fun isEventOk(startPoint: GeoPoint, event: Event) : Boolean {
         // check category
         if (category != Category.ALL) {
             when (event.activity.lowercase()) {
                 "football", "futsal", "nogomet" -> if (category != Category.FOOTBALL) return false
                 "handball", "rokomet" -> if (category != Category.HANDBALL) return false
                 else -> if (category != Category.OTHER) return false
             }
         }
         // check time
         if (time != TimeInterval.ALL) {
             val startTime : LocalDate = LocalDate.now()
             var endTime : LocalDate? = null
             when (time) {
                 TimeInterval.TODAY -> endTime = LocalDate.now().plusDays(1)
                 TimeInterval.WEEK -> endTime = LocalDate.now().plusWeeks(1)
                 TimeInterval.MONTH -> endTime = LocalDate.now().plusMonths(1)
                 else -> endTime = null
             }
             if (endTime != null) {
                 if (event.date.isBefore(startTime) || event.date.isAfter(endTime)) return false
             } else {
                 if (event.date.isBefore(startTime)) return false
             }
         }
         // check location
         if (distance != 0) {
             val earthRadius = 6371000.0 // Radius of the Earth in meters

             val lat1 = Math.toRadians(startPoint.latitude)
             val lon1 = Math.toRadians(startPoint.longitude)
             val lat2 = Math.toRadians(event.location.coordinates[1])
             val lon2 = Math.toRadians(event.location.coordinates[0])

             val dLat = lat2 - lat1
             val dLon = lon2 - lon1

             // Haversine formula
             val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
             val c = 2 * atan2(sqrt(a), sqrt(1 - a))

             val eventDistance = earthRadius * c / 1000 // in km
             if (eventDistance > distance) return false
         }
         return true
     }

    fun isDefault(): Boolean {
        return category == Category.ALL && distance == 0 && time == TimeInterval.ALL
    }

    fun restoreDefaults() {
        category = Category.ALL
        distance = 0
        time = TimeInterval.ALL
    }
}