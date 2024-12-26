package com.example.spotter

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

interface ApiService {
    @GET("api/hello")
    fun getHello(): Call<ServerResponse>

    /*
    @GET("api/getIcon")
    fun getUserIcon(@Query("id") id: String): Call<ServerResponse>
    */

    @GET("api/events")
    fun getAllEvents(): Call<ServerResponse>

    @GET("api/getUser/{id}")
    fun getUser(@Path("id") id: UUID): Call<ServerResponse>

    @GET("api/subscribe/{eventId}")
    fun subscribeToEvent(@Path("eventId") eventId: UUID): Call<ServerResponse>

    @POST("api/event")
    fun createEvent(@Body e : Event) : Call<ServerResponse> // set correct queries

}
