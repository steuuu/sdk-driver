package com.example.a4bus_motorista_x3

import com.google.gson.annotations.SerializedName

class VehicleResponse(
    @SerializedName("name") val name: String? = null,
    @SerializedName("vehicleState") val vehicleState: String? = null,
    @SerializedName("currentTripsIds") val currentTripsIds: List<String> = listOf()
)
