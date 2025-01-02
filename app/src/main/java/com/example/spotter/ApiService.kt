package com.example.spotter

import org.bson.types.ObjectId
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    /*
    @GET("api/getIcon")
    fun getUserIcon(@Query("id") id: String): Call<ServerResponse>
    */

    @GET("/events/")
    fun getAllEvents(): Call<List<GET_ALL_EVENTS_MODEL>>

    @GET("/users/show/{id}")
    fun getUser(@Path("id") id: ObjectId): Call<User>

    @GET("/events/follow/{eventId}")
    fun subscribeToEvent(@Path("eventId") eventId: ObjectId): Call<ServerResponse>

    @POST("/events/")
    fun createEvent(@Body e : CREATE_EVENT_MODEL) : Call<ServerResponse> // set correct queries

    @POST("/users/login")
    fun login(@Body user: LOGIN_MODEL) : Call<User>

    @POST("/users/register")
    fun register(@Body user : REGISTER_MODEL) : Call<User>

}
