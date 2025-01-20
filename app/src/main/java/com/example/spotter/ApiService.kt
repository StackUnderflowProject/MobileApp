package com.example.spotter

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.bson.types.ObjectId
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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
    fun subscribeToEvent(@Header("Authorization") token: String, @Path("eventId") eventId: ObjectId): Call<Event>

    @POST("/events/")
    fun createEvent(@Header("Authorization") token: String, @Body e : CREATE_EVENT_MODEL) : Call<Event> // set correct queries

    @DELETE("/events/{id}")
    fun deleteEvent(@Header("Authorization") token: String, @Path("id") eventId: ObjectId) : Call<ServerResponse>

    @PUT("/events/{id}")
    fun updateEvent(@Header("Authorization") token: String, @Path("id") eventId: ObjectId, @Body e : CREATE_EVENT_MODEL) : Call<Event>

    @POST("/users/login")
    fun login(@Body user: LOGIN_MODEL) : Call<User>

    @POST("/users/register")
    fun register(@Body user : REGISTER_MODEL) : Call<User>

    @Multipart
    @POST("/predict")
    fun getPredictedCount(@Header("Authorization") token: String,  @Part("density") density: RequestBody, @Part image: MultipartBody.Part) : Call<PREDICT_IMG_OUTPUT>

    @Multipart
    @POST("/events/{id}/image")
    fun uploadEventImg(@Header("Authorization") token: String, @Path("id") eventId: ObjectId, @Part image: MultipartBody.Part) : Call<ServerResponse>

    @PATCH("/events/{id}/predicted-count")
    fun uploadPredictedCount(@Header("Authorization") token: String, @Path("id") eventId: ObjectId, @Body input: PREDICT_IMG_OUTPUT) : Call<Event>

    @GET("footballMatch/filterByDateRange/{startDate}/{endDate}")
    suspend fun getFootballMatches(
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String
    ): List<Match>

    @PUT("footballMatch/{id}")
    fun updateFootballMatch(@Header("Authorization") token: String, @Path("id") id: String, @Body req: Match) : Call<Match>

    @PUT("handballMatch/{id}")
    fun updateHandballMatch(@Header("Authorization") token: String, @Path("id") id: String, @Body req: Match) : Call<Match>

    @GET("handballMatch/filterByDateRange/{startDate}/{endDate}")
    suspend fun getHandballMatches(
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String
    ): List<Match>

    @Multipart
    @PUT("/users/profilePicture")
    fun uploadProfilePicture(@Header("Authorization") token: String, @Part image: MultipartBody.Part) : Call<User>
}
