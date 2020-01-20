package com.example.potholedatacollection.home.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.potholedatacollection.R
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
/**
 * TODO : Add location wherever sheet is updated. Sheet is basically database. Abhi phone me hi excel sheet bana denge
 * */

class MainActivity : AppCompatActivity(), SensorEventListener {

    var isTripOn : Boolean = false
    lateinit var sensorManager: SensorManager
    lateinit var sensorAccelerometer: Sensor
    lateinit var sensorGyroscope: Sensor
    var tripTimer: Int = 0
    var potHoleCounter: Int = 0
    private val REQUEST_PERMISSION_LOCATION = 10
    internal lateinit var mLocationRequest: LocationRequest
    var latitude:Double = 0.0
    var longitude:Double = 0.0
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    lateinit var countDownTimer:CountDownTimer



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLocationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)


        btn_start.setOnClickListener{
            startNewTrip()
        }

        btn_pothole.setOnClickListener{
            // Update pothole counter and redraw the textview.
            // Add pothole with locations to new pothole sheet.
            // RN just log it with location.
        }


    }

    fun startNewTrip() {
        Log.e("log","Starting new trip")

        if (isTripOn) {
            isTripOn = false
            btn_start.text = "Start"
            disableSensors()
            disablePotholeCounter()
            stopLocationService()
            countDownTimer.onFinish()
            countDownTimer.cancel()
            return
        }
        tripTimer = 0
        potHoleCounter = 0
        isTripOn = true
        btn_start.text = "Trip On"
        btn_pothole.isEnabled = true
        countDownTimer = object : CountDownTimer(50000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                txt_trip_duration.text = "Trip time : "+tripTimer+" seconds"
                tripTimer++
            }

            override fun onFinish() {
                txt_trip_duration.text = "Last trip finished in "+tripTimer+" seconds"
            }
        }.start()
        enableSensors()
        enablePotHoleCounter()
        startLocationService()
    }

    fun startLocationService() {
        Log.e("log","Starting Location services")
        if (checkPermissionForLocation(this)) {
            startLocationUpdates()
        }
    }

    fun stopLocationService() {
        Log.e("log","Stopping location service")
        stoplocationUpdates()
    }



    protected fun startLocationUpdates() {

        // Create the location request to start receiving updates

        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.fastestInterval = 500

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }
    private fun stoplocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined
        latitude = location.latitude
        longitude = location.longitude
        Log.e("log","Location change : "+location.latitude+" "+location.longitude)

        // You can now create a LatLng Object for use with maps
    }


    fun enableSensors() {
        /**We need sensor readings every 5th second */
        Log.e("log","Sensors enabled")

        sensorManager.registerListener(this,sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun disableSensors() {
        Log.e("log","Sensors disabled")

        sensorManager.unregisterListener(this, sensorAccelerometer)
        sensorManager.unregisterListener(this,sensorGyroscope)
    }

    fun enablePotHoleCounter() {
        /**We need to add a onclick on pothole buttons
         * to store them with latitude and longitude*/
        btn_pothole.isClickable = true
    }

    private fun disablePotholeCounter() {
        btn_pothole.isClickable = false
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not of any use
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        val mySensor = p0?.sensor

        if (mySensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = p0.values[0]
            val y = p0.values[1]
            val z = p0.values[2]

            if (tripTimer%5 == 0) {
                // To send data in interval of 5 seconds
                // Add data to sheet - calculate speed here by maintaining
                // prev x,y,z and then calculating it on an interval of 5 seconds


                Log.e("log","Sensor1 data : "+x+" "+y+" "+z+" "+latitude+" "+longitude)
            }
        }

        if (mySensor?.type == Sensor.TYPE_GYROSCOPE) {
            val x = p0.values[0]
            val y = p0.values[1]
            val z = p0.values[2]
            if (tripTimer%5 ==0) {
                // Add this data directly
                Log.e("log","Sensor2 data : "+x+" "+y+" "+z+" "+latitude+" "+longitude)
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun checkPermissionForLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
                true
            }else{
                // Show the permission request
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have to add startlocationUpdate() method later instead of Toast
                Toast.makeText(this,"Permission granted",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11)
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()


    }

}
