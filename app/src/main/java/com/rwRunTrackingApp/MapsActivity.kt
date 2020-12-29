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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    lateinit var appDatabase: AppDatabase
    private lateinit var mMap: GoogleMap
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val polylineOptions = PolylineOptions()
    var lastKnownLocation: Location? = null

    val KEY_SHARED_PREFERENCE = "com.rwRunTrackingApp.KEY_SHARED_PREFERENCE"
    val KEY_INITIAL_STEP_COUNT = "com.rwRunTrackingApp.KEY_CURRENT_NUMBER_OF_STEP_COUNT"
    val KEY_TOTAL_DISTANCE_TRAVELLED = "com.rwRunTrackingApp.KEY_TOTAL_DISTANCE_TRAVELLED"
    val KEY_IS_TRACKING = "com.rwRunTrackingApp.KEY_IS_TRACKING"
    var currentNumberOfStepCount = 0
    var initialStepCount: Int
        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getInt(KEY_INITIAL_STEP_COUNT, -1)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putInt(KEY_INITIAL_STEP_COUNT, value).apply()
    var totalDistanceTravelled: Float
        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getFloat(KEY_TOTAL_DISTANCE_TRAVELLED, 0f)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putFloat(KEY_TOTAL_DISTANCE_TRAVELLED, value).apply()
    var isTracking: Boolean
        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getBoolean(KEY_IS_TRACKING, false)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_TRACKING, value).apply()

    val locationCallback = object: LocationCallback() {
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
                lifecycleScope.launch { // coroutine on Main
                    async(Dispatchers.IO) {
                        try {
                            appDatabase.trackingDao().insert(TrackingRecord(Calendar.getInstance().timeInMillis, it.latitude, it.longitude))
                            Log.d("TAG", "Data is added")
                        } catch (error: Exception) {
                            error.localizedMessage
                        }
                    }
                }
            }
            updateAllDisplayText()
            addLocationToRoute(locationResult.locations)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        appDatabase = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database-name").build()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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

        updateButtonStatus()
        updateAllDisplayText()

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
        lifecycleScope.launch { // coroutine on Main
            async(Dispatchers.IO) {
                try {
                    val originalTrackingRecordList = appDatabase.trackingDao().getAll()
                    Log.d("TAG", "Original number of data: ${originalTrackingRecordList.size}")

                    appDatabase.trackingDao().delete()

                    val newTrackingRecordList = appDatabase.trackingDao().getAll()
                    Log.d("TAG", "New Number of data: ${newTrackingRecordList.size}")
                } catch (error: Exception) {
                    error.localizedMessage
                }
            }
        }
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
    fun setupStepCounterListener() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCounterSensor?.let {
            sensorManager.registerListener(this@MapsActivity, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
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

    fun addLocationToRoute(locations: List<Location>) {
        mMap.clear()
        val originalLatLngList = polylineOptions.points
        val latLngList = locations.map {
            LatLng(it.latitude, it.longitude)
        }
        originalLatLngList.addAll(latLngList)
        mMap.addPolyline(polylineOptions)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("TAG", "onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        Log.d("TAG", "onSensorChanged")
        sensorEvent ?: return
        sensorEvent.values.firstOrNull()?.let {
            if (initialStepCount == -1) {
                initialStepCount = it.toInt()
            }
            currentNumberOfStepCount = it.toInt() - initialStepCount
            Log.d("TAG", "Step count: $currentNumberOfStepCount ")
            updateAllDisplayText()
        }
    }
}