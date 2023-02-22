package com.example.a4bus_motorista_x3

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.example.a4bus_motorista_x3.TripUtils.EXCLUSIVE_TRIP_TYPE
import com.example.a4bus_motorista_x3.TripUtils.SHARED_TRIP_TYPE


class VehicleDialogFragment
private constructor(
    private val localSettings: LocalSettings,
    private val vehicleSettings: VehicleSettings,
    private val listener: OnDialogResultListener,
) : DialogFragment() {
    interface OnDialogResultListener {
        fun onResult(vehicleSettings: VehicleSettings)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.vehicle_info_dialog, null)

        val editText = view.findViewById<EditText>(R.id.vehicle_id)
        editText.setText(localSettings.getVehicleId())

        val vehicleCapacitySpinner = view.findViewById<Spinner>(R.id.vehicle_capacity_spinner)

        vehicleCapacitySpinner.adapter =
            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.vehicle_capacities_array,
                android.R.layout.simple_spinner_dropdown_item,
            )

        vehicleCapacitySpinner.setSelection(
            getVehicleCapacityIndex(requireContext().resources, vehicleSettings.maximumCapacity),
            true
        )

        val backToBackEnabledCheckbox = view.findViewById<CheckBox>(R.id.back_to_back_checkbox)
        backToBackEnabledCheckbox.isChecked = vehicleSettings.backToBackEnabled

        val sharedTripTypeCheckbox = view.findViewById<CheckBox>(R.id.trip_type_shared)
        sharedTripTypeCheckbox.isChecked =
            vehicleSettings.supportedTripTypes.any { it == SHARED_TRIP_TYPE }

        val exclusiveTripTypeCheckbox = view.findViewById<CheckBox>(R.id.trip_type_exclusive)
        exclusiveTripTypeCheckbox.isChecked =
            vehicleSettings.supportedTripTypes.any { it == EXCLUSIVE_TRIP_TYPE }

        val simulationEnabledCheckbox =
            view.findViewById<CheckBox>(R.id.simulate_locations_checkbox)
        simulationEnabledCheckbox.isChecked = localSettings.getIsSimulationEnabled()

        return AlertDialog.Builder(activity)
            .setView(view)
            .setPositiveButton(R.string.save_label) { dialog: DialogInterface?, id: Int ->
                val newVehicleId = editText.text.toString()

                if (! TextUtils.isEmpty(newVehicleId)) {
                    val supportedTripTypes = buildList {
                        if (sharedTripTypeCheckbox.isChecked) add(SHARED_TRIP_TYPE)
                        if (exclusiveTripTypeCheckbox.isChecked) add(EXCLUSIVE_TRIP_TYPE)
                    }

                    listener.onResult(
                        VehicleSettings(
                            vehicleId = newVehicleId,
                            maximumCapacity = vehicleCapacitySpinner.selectedItem !!.toString()
                                .toInt(),
                            backToBackEnabled = backToBackEnabledCheckbox.isChecked,
                            supportedTripTypes = supportedTripTypes
                        )
                    )

                    localSettings.saveIsSimulationEnabled(simulationEnabledCheckbox.isChecked)
                }
            }
            .setNegativeButton(R.string.cancel_label) { dialog: DialogInterface?, id: Int -> }
            .create()
    }

    companion object {
        private val TAG = VehicleDialogFragment::class.java.name

        fun newInstance(
            localSettings: LocalSettings,
            vehicleSettings: VehicleSettings,
            listener: OnDialogResultListener
        ): VehicleDialogFragment {
            return VehicleDialogFragment(localSettings, vehicleSettings, listener)
        }

        private fun getVehicleCapacityIndex(resources: Resources, maximumCapacity: Int): Int =
            resources.getStringArray(R.array.vehicle_capacities_array)
                .indexOf(maximumCapacity.toString())
    }
}
