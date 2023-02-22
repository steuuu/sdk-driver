package com.example.a4bus_motorista_x3

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import java.net.ConnectException
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), Presenter {
    private val executor = Executors.newCachedThreadPool()
    private lateinit var localSettings: LocalSettings
    private lateinit var navFragment: SupportNavigationFragment
    private lateinit var simulationStatusText: TextView
    private lateinit var tripIdText: TextView
    private lateinit var matchedTripIdsText: TextView
    private lateinit var textVehicleId: TextView
    private lateinit var actionButton: Button
    private lateinit var tripCard: CardView
    private lateinit var vehicleController: VehicleController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        localSettings = LocalSettings(this)

        simulationStatusText = findViewById(R.id.simulation_status)
        actionButton = findViewById(R.id.action_button)
        val editVehicleIdButton = findViewById<Button>(R.id.edit_button)
        editVehicleIdButton.setOnClickListener { onEditVehicleButtonClicked() }
        tripCard = findViewById(R.id.trip_card)
        tripIdText = findViewById(R.id.trip_id_label)
        matchedTripIdsText = findViewById(R.id.matched_trip_ids_label)
        textVehicleId = findViewById(R.id.menu_vehicle_id)
        textVehicleId.text = localSettings.getVehicleId()
        setupNavFragment()
        updateActionButton(TRIP_VIEW_INITIAL_STATE, null)
        actionButton.setOnClickListener { onActionButtonClicked() }
        showTripId(VehicleController.NO_TRIP_ID)
        showMatchedTripIds(emptyList())

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initializeSDKs()
        Log.i(TAG, "Driver SDK version: " + RidesharingDriverApi.getDriverSdkVersion())
        Log.i(TAG, "Navigation SDK version: " + NavigationApi.getNavSDKVersion())
    }

    private fun initializeSDKs() {
        NavigationApi.getNavigator(
            this,
            object : NavigatorListener {
                override fun onNavigatorReady(navigator: Navigator) {
                    vehicleController =
                        VehicleController(
                            navigator,
                            executor,
                            context = this@MainActivity,
                            coroutineScope = this@MainActivity.lifecycleScope,
                            providerService =
                            LocalProviderService(
                                LocalProviderService.createRestProvider(
                                    ProviderUtils.getProviderBaseUrl(application)
                                ),
                            ),
                            localSettings,
                        )
                    vehicleController.setPresenter(this@MainActivity)
                    initVehicleAndPollTrip()
                }

                override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                    logNavigationApiInitError(errorCode)
                }
            }
        )
    }

    private fun initVehicleAndPollTrip() {
        lifecycleScope.launch {
            try {
                vehicleController.initVehicleAndReporter(application)
            } catch (e: ConnectException) {
                Snackbar.make(
                    tripCard,
                    R.string.msg_provider_connection_error,
                    BaseTransientBottomBar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.button_retry) { initVehicleAndPollTrip() }
                    .show()
            }
        }
    }

    private fun setupNavFragment() {
        navFragment = SupportNavigationFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.nav_fragment_frame, navFragment, null)
            .setReorderingAllowed(true)
            .commit()
    }

    private fun onEditVehicleButtonClicked() {
        val fragment: VehicleDialogFragment =
            VehicleDialogFragment.newInstance(
                localSettings,
                vehicleController.vehicleSettings,
                object : VehicleDialogFragment.OnDialogResultListener {
                    override fun onResult(vehicleSettings: VehicleSettings) {
                        textVehicleId.text = vehicleSettings.vehicleId
                        localSettings.saveVehicleId(vehicleSettings.vehicleId)

                        lifecycleScope.launch {
                            vehicleController.updateVehicleSettings(application, vehicleSettings)
                        }
                    }
                }
            )
        fragment.show(supportFragmentManager, "VehicleInfoDialog")
    }

    private fun onActionButtonClicked() {
        vehicleController.processNextState()
    }

    @SuppressLint("MissingPermission")
    private fun updateCameraPerspective(isTilted: Boolean) {
        navFragment.getMapAsync { googleMap: GoogleMap ->
            googleMap.followMyLocation(
                if (isTilted) CameraPerspective.TILTED else CameraPerspective.TOP_DOWN_NORTH_UP
            )
        }
    }

    private fun updateActionButton(visibility: Int, @StringRes resourceId: Int?) {
        resourceId?.also { actionButton.setText(it) }
        actionButton.visibility = visibility
    }

    override fun showTripId(tripId: String) {
        if (tripId != VehicleController.NO_TRIP_ID) {
            tripIdText.text = resources.getString(R.string.trip_id_label, tripId)
            return
        }
        val noTripFoundText = resources.getString(R.string.status_unknown)
        tripIdText.text = resources.getString(R.string.trip_id_label, noTripFoundText)
    }

    override fun showMatchedTripIds(tripIds: List<String>) {
        val text =
            if (tripIds.isEmpty()) resources.getString(R.string.status_unknown)
            else tripIds.joinToString()

        matchedTripIdsText.text = resources.getString(R.string.matched_trip_ids_label, text)
    }

    override fun showTripStatus(status: TripStatus) {
        var resourceId = R.string.status_idle
        var buttonVisibility = View.VISIBLE
        var cardVisibility = View.VISIBLE

        var isCameraTilted = false
        when (status) {
            TripStatus.UNKNOWN_TRIP_STATUS -> {
                buttonVisibility = TRIP_VIEW_INITIAL_STATE
                cardVisibility = View.VISIBLE
                simulationStatusText.setText(resourceId)
            }
            TripStatus.NEW -> {
                simulationStatusText.setText(R.string.status_new)
                resourceId = R.string.button_start_trip
            }
            TripStatus.ENROUTE_TO_PICKUP -> {
                simulationStatusText.setText(R.string.status_enroute_to_pickup)
                resourceId = R.string.button_arrived_at_pickup
                isCameraTilted = true
            }
            TripStatus.ARRIVED_AT_PICKUP -> {
                simulationStatusText.setText(R.string.status_arrived_at_pickup)
                resourceId =
                    if (vehicleController.isNextCurrentTripWaypointIntermediate()) {
                        R.string.button_enroute_to_intermediate_stop
                    } else R.string.button_enroute_to_dropoff
                isCameraTilted = true
            }
            TripStatus.ENROUTE_TO_DROPOFF -> {
                simulationStatusText.setText(R.string.status_enroute_to_dropoff)
                resourceId = R.string.button_trip_complete
                isCameraTilted = true
            }
            TripStatus.ENROUTE_TO_INTERMEDIATE_DESTINATION -> {
                simulationStatusText.setText(R.string.status_enroute_to_intermediate_location)
                resourceId = R.string.button_arrived_at_intermediate_stop
                isCameraTilted = true
            }
            TripStatus.ARRIVED_AT_INTERMEDIATE_DESTINATION -> {
                simulationStatusText.setText(R.string.status_arrived_to_intermediate_location)
                resourceId =
                    if (vehicleController.isNextCurrentTripWaypointIntermediate()) {
                        R.string.button_enroute_to_intermediate_stop
                    } else R.string.button_enroute_to_dropoff
                isCameraTilted = true
            }
            TripStatus.COMPLETE -> {
                resourceId = R.string.status_complete
                buttonVisibility = View.GONE
                simulationStatusText.setText(resourceId)
            }
            TripStatus.CANCELED -> {
                resourceId = R.string.status_canceled
                buttonVisibility = View.GONE
                simulationStatusText.setText(resourceId)
            }
        }
        tripCard.visibility = cardVisibility
        updateActionButton(buttonVisibility, resourceId)
        updateCameraPerspective(isCameraTilted)
    }

    override fun enableActionButton(enabled: Boolean) {
        actionButton.isEnabled = enabled
    }

    override fun onDestroy() {
        vehicleController.cleanUp()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val TRIP_VIEW_INITIAL_STATE = View.GONE
        private fun logNavigationApiInitError(errorCode: Int) {
            when (errorCode) {
                NavigationApi.ErrorCode.NOT_AUTHORIZED ->
                    Log.w(
                        TAG,
                        "Observação: se esta mensagem for exibida, talvez seja necessário verificar se sua API_KEY é +" +
                                "especificado corretamente em AndroidManifest.xml e habilitado para +" +
                                "acesse a API de navegação."
                    )
                NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED ->
                    Log.w(
                        TAG,
                        "Erro ao carregar a API de navegação: o usuário não aceitou os termos de uso da navegação."
                    )
                else -> Log.w(TAG, "Erro ao carregar a API de navegação: $errorCode")
            }
        }
    }
}
