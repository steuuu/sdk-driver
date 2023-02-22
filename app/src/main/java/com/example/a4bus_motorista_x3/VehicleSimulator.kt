package com.example.a4bus_motorista_x3

import com.google.maps.model.LatLng


internal class VehicleSimulator(
    private val simulator: Simulator,
    private val localSettings: LocalSettings,
) {
    fun setLocation(location: Waypoint.Point): Unit =
        simulator.setUserLocation(LatLng(location.latitude, location.longitude))

    fun start(speedMultiplier: Float) {
        if (localSettings.getIsSimulationEnabled()) {
            simulator.simulateLocationsAlongExistingRoute(
                SimulationOptions().speedMultiplier(speedMultiplier)
            )
        }
    }

    fun pause() {
        simulator.pause()
    }

    fun unsetLocation() {
        simulator.unsetUserLocation()
    }
}
