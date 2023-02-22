package com.example.a4bus_motorista_x3

import com.google.gson.annotations.SerializedName

class GetTripResponse(
    @SerializedName("trip") val tripModel: TripModel? = null,
)
