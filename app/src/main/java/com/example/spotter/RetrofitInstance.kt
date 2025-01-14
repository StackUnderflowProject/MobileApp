package com.example.spotter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bson.types.ObjectId
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId

object RetrofitInstance {
    private const val BASE_URL = "http://77.38.76.152:3000" // Replace with your server URL
//    private const val BASE_URL = "http://164.8.210.144:3000"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Options: NONE, BASIC, HEADERS, BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    class LocalDateDeserializer : JsonDeserializer<LocalDate> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LocalDate {
            val dateTimeString = json.asString
            val instant = Instant.parse(dateTimeString)
            return instant.atZone(ZoneId.of("UTC")).toLocalDate()
        }
    }

    class ObjectIdDeserializer : JsonDeserializer<ObjectId> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): ObjectId {
            return ObjectId(json.asString)
        }
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
        .registerTypeAdapter(ObjectId::class.java, ObjectIdDeserializer())
        .create()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}