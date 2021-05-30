package com.rwRunTrackingApp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.observe
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private val mapsActivityViewModel: MapsActivityViewModel by viewModels {
        MapsActivityViewModelFactory(getTrackingRepository())
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val polylineOptions = PolylineOptions()
    private var lastKnownLocation: Location? = null

    private val KEY_SHARED_PREFERENCE = "com.rwRunTrackingApp.KEY_SHARED_PREFERENCE"
    private val KEY_INITIAL_STEP_COUNT = "com.rwRunTrackingApp.KEY_CURRENT_NUMBER_OF_STEP_COUNT"
    private val KEY_TOTAL_DISTANCE_TRAVELLED = "com.rwRunTrackingApp.KEY_TOTAL_DISTANCE_TRAVELLED"
    private val KEY_IS_TRACKING = "com.rwRunTrackingApp.KEY_IS_TRACKING"
    private var currentNumberOfStepCount = 0
    private var initialStepCount: Int
        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getInt(KEY_INITIAL_STEP_COUNT, -1)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putInt(KEY_INITIAL_STEP_COUNT, value).apply()
    private var totalDistanceTravelled: Float
        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getFloat(KEY_TOTAL_DISTANCE_TRAVELLED, 0f)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putFloat(KEY_TOTAL_DISTANCE_TRAVELLED, value).apply()
    private var isTracking: Boolean
        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getBoolean(KEY_IS_TRACKING, false)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_TRACKING, value).apply()

    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult ?: return

            locationResult.locations.forEach {
                Log.d("TAG", "New location got: (${it.latitude}, ${it.longitude})")
                if (lastKnownLocation == null) {
                    lastKnownLocation = it
                    return@forEach
                }
                totalDistanceTravelled = totalDistanceTravelled + it.distanceTo(lastKnownLocation)
                mapsActivityViewModel.insert(TrackingEntity(Calendar.getInstance().timeInMillis, it.latitude, it.longitude))
            }
            updateAllDisplayText()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up Fused Location Provider Client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up button click events
        startButton.setOnClickListener {
            isTracking = true
            // Clear previous local data
            initialStepCount = -1
            currentNumberOfStepCount = 0
            totalDistanceTravelled = 0f
            mMap.clear()

            updateButtonStatus()
            updateAllDisplayText()

            startButtonClicked()
        }
        endButton.setOnClickListener { endButtonClicked() }

        // Update layouts
        updateButtonStatus()
        updateAllDisplayText()

        mapsActivityViewModel.lastTrackingEntity.observe(this) {
            Log.d("TAG_MYRICK", "Last entity timestamp: ${it.timestamp}")
            addLocationToRoute(it)
        }
        if (isTracking) {
            startButtonClicked()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        runWithLocationPermissionChecking {
            mMap.isMyLocationEnabled = true
        }

        // Add a marker in Hong Kong and move the camera
        val latitude = 22.3193
        val longitude = 114.1694
        val hongKongLatLong = LatLng(latitude, longitude)

        val zoomLevel = 9.5f
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(hongKongLatLong, zoomLevel))
    }

    fun getTrackingApplicationInstance() = application as TrackingApplication
    fun getTrackingRepository() = getTrackingApplicationInstance().trackingRepository

    fun updateButtonStatus() {
        startButton.isEnabled = !isTracking
        endButton.isEnabled = isTracking
    }

    fun updateAllDisplayText() {
        numberOfStepTextView.text = "Step count: $currentNumberOfStepCount"
        totalDistanceTextView.text = String.format("Total distance: %.2fm", totalDistanceTravelled)

        val averagePace = if (currentNumberOfStepCount != 0) totalDistanceTravelled / currentNumberOfStepCount.toDouble() else 0.0
        averagePaceTextView.text = String.format("Average pace: %.2fm/ step", averagePace)
    }

    fun startButtonClicked() {
        RxPermissions(this).request(Manifest.permission.ACTIVITY_RECOGNITION)
            .subscribe { isGranted ->
                Log.d("TAG", "Is ACTIVITY_RECOGNITION permission granted: $isGranted")
                if (isGranted) {
                    setupStepCounterListener()
                }
            }
        setupLocationChangeListener()
    }

    fun endButtonClicked() {
        AlertDialog.Builder(this)
                .setTitle("Are you sure to stop tracking?")
                .setPositiveButton("Confirm") { dialog, which ->
                    isTracking = false
                    updateButtonStatus()
                    stopTracking()
                }.setNegativeButton("Cancel") { dialog, which ->
                }
                .create()
                .show()
    }

    fun stopTracking() {
        mapsActivityViewModel.deleteAllTrackingEntity()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun setupStepCounterListener() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCounterSensor ?: return
        sensorManager.registerListener(this@MapsActivity, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun runWithLocationPermissionChecking(callback: () -> Unit) {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe { isGranted ->
                    if (isGranted) {
                        callback()
                    } else {
                        Toast.makeText(this, "Please grant Location permission", Toast.LENGTH_LONG).show()
                    }
                }
    }
    @SuppressLint("MissingPermission")
    fun setupLocationChangeListener() {
        runWithLocationPermissionChecking {
            val locationRequest = LocationRequest()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 5000 // 5000ms (5s)

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun addLocationToRoute(trackingEntity: TrackingEntity) {
        mMap.clear()
        val newLatLngInstance = trackingEntity.asLatLng()
        polylineOptions.points.add(newLatLngInstance)
        mMap.addPolyline(polylineOptions)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("TAG", "onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        Log.d("TAG", "onSensorChanged")
        sensorEvent ?: return
        val firstSensorEvent = sensorEvent.values.firstOrNull() ?: return
        if (initialStepCount == -1) {
            initialStepCount = firstSensorEvent.toInt()
        }
        currentNumberOfStepCount = firstSensorEvent.toInt() - initialStepCount
        Log.d("TAG", "Step count: $currentNumberOfStepCount ")
        updateAllDisplayText()
    }
}