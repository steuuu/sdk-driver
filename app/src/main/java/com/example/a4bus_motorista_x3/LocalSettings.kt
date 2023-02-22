package com.example.a4bus_motorista_x3

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("ApplySharedPref")
class LocalSettings(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(
            context.getString(R.string.shared_preference_key),
            Context.MODE_PRIVATE
        )

    fun saveVehicleId(vehicleId: String?) {
        sharedPrefs.edit().putString(KEY_VEHICLE_ID, vehicleId).commit()
    }

    fun getVehicleId(): String =
        sharedPrefs.getString(KEY_VEHICLE_ID, DEFAULT_VEHICLE_ID) ?: DEFAULT_VEHICLE_ID

    fun saveIsSimulationEnabled(isSimulationEnabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_IS_SIMULATION_ENABLED, isSimulationEnabled).commit()
    }

    fun getIsSimulationEnabled(): Boolean =
        sharedPrefs.getBoolean(KEY_IS_SIMULATION_ENABLED, DEFAULT_IS_SIMULATION_ENABLED)

    private companion object {
        const val KEY_VEHICLE_ID = "VEHICLE_ID"
        const val DEFAULT_VEHICLE_ID = "Vehicle_1"

        const val KEY_IS_SIMULATION_ENABLED = "IS_SIMULATION_ENABLED"
        const val DEFAULT_IS_SIMULATION_ENABLED = true
    }
}
