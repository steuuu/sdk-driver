package com.example.a4bus_motorista_x3

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RestProvider {
    @GET("vehicle/{id}")
    suspend fun getVehicle(@Path("id") vehicle: String): VehicleModel

    @GET("token/driver/{vehicleId}")
    suspend fun getAuthToken(@Path("vehicleId") vehicleId: String): TokenResponse

    @POST("vehicle/new")
    suspend fun createVehicle(@Body body: VehicleSettings): VehicleModel

    @PUT("vehicle/{id}")
    suspend fun updateVehicle(@Path("id") id: String, @Body body: VehicleSettings): VehicleModel

    @PUT("trip/{id}")
    suspend fun updateTrip(@Path("id") id: String, @Body body: TripUpdateBody): TripModel
}
