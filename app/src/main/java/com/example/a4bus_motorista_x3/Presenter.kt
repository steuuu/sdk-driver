package com.example.a4bus_motorista_x3

interface Presenter {
    fun showTripId(tripId: String)

    fun showMatchedTripIds(tripIds: List<String>)

    fun showTripStatus(status: TripStatus)

    fun enableActionButton(enabled: Boolean)
}
