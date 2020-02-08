package com.example.potholedatacollection.home.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.potholedatacollection.R
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * TODO : Add location wherever sheet is updated. Sheet is basically database. Abhi phone me hi excel sheet bana denge
 * */

@Suppress("DEPRECATION")
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
    lateinit var geocoder: Geocoder
    var accelerometerData = ArrayList<String>()


    var ax : Double = 0.0
    var ay : Double = 0.0
    var az : Double = 0.0

    var prevax : Double = 0.0
    var prevay : Double = 0.0
    var prevaz : Double = 0.0
    var gx : Double = 0.0
    var gy : Double = 0.0
    var gz : Double = 0.0

    var speed : Double = 0.0
    var time : Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLocationRequest = LocationRequest()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
        startLocationService()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)


        btn_start.setOnClickListener{
            startNewTrip()
        }

        btn_pothole.setOnClickListener{

            potHoleCounter++

            txt_cnt_pothole.text = "Total Potholes Reported: "+potHoleCounter

            val ts = Timestamp(System.currentTimeMillis()).time
            Log.e("log","TImestamp : "+ts)


            var type = "Pothole"

            var data : String = ts.toString() + "," + accelerometerData[7] + "," + accelerometerData[8] + "," +type + ","

            for (i in 10..14){

                if(i==14)
                    data +=accelerometerData[i]+"\n"
                else
                    data += accelerometerData[i] + ","
            }


            writeFileExternalStorage(data, "ConfirmPothole.csv")

            var data2 : String = ""

            for(x in accelerometerData){

                data2 += x + ","
            }

            data2 += "1\n"

            writeFileExternalStorage(data2,"FirstSheet.csv")


            }

        }

    fun writeFileExternalStorage(data : String,  fileName : String) {

    var fullPath = Environment.getExternalStorageDirectory().absolutePath
        var myDir = File(fullPath+"/Documents")

        if (!myDir.exists()) {
            myDir.mkdirs()
        }
    try
    {

        var file =  File(myDir, fileName)
        if(file.exists()) {


            try {
                var file_writer =  OutputStreamWriter( FileOutputStream(file,true))
                var buffered_writer =  BufferedWriter(file_writer)
                buffered_writer.write(data)
                buffered_writer.close()
            } catch (e : IOException ) {
                e.printStackTrace()
            }
        }
        else {
            Log.e("loggg", file.toString() + " " + data)

            file.createNewFile()

            var fOut = FileOutputStream(file,true)
            fOut.write(data.toByteArray())
            fOut.flush()
            fOut.close()
        }
    }
    catch (e : Exception )
    {
        Log.e("External File Storage Error ",e.message)
    }

    }

    fun startNewTrip() {
        Log.e("log","Starting new trip")

        if (isTripOn) {
            isTripOn = false
            btn_start.text = "START"
            text_intro.text = "Start the trip to store the data"
            TripDesc.text = "Trip is OFF!"

            txt_gyroscope.text = "Gyroscope :"
            txt_accelerometer.text = "Accelerometer :"
            txt_speed.text = "Speed :"
            txt_location.text ="Location :"

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
        btn_start.text = "STOP"
        TripDesc.text = "Trip is ON!"
        text_intro.text = "Stop the trip to save the data"
        btn_pothole.isEnabled = true
        countDownTimer = object : CountDownTimer(5000000, 1000) {
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
        val curTime = System.currentTimeMillis()
        if (curTime - time > 200) {
            val diff = curTime - time
            if (location.hasSpeed())
                speed = location.speed.toDouble()
            else
                speed = getDistance(latitude,longitude,location.latitude,location.longitude)/diff
        }
        latitude = location.latitude
        longitude = location.longitude
        Log.e("log","Location change : "+location.latitude+" "+location.longitude)

    }

//    /**
// * Gets distance in meters, coordinates in RADIAN
// */
    fun getDistance(lat1:Double, lon1:Double, lat2:Double, lon2:Double): Double {
        val R = 6371000.00
        var dLat : Double = lat2-lat1
        var dLon : Double = lon2 - lon1
        var a : Double = sin(dLat/2)*sin(dLat/2)+cos(lat1)*cos(lat2)*sin(dLon/2)*sin(dLon/2)
        var c = 2 * atan2(sqrt(a), sqrt(1-a))
        return 2*R* atan2(sqrt(a), sqrt(1-a))
    }
//private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
//    double R = 6371000; // for haversine use R = 6372.8 km instead of 6371 km
//    double dLat = lat2 - lat1;
//    double dLon = lon2 - lon1;
//    double a = ;
//    //double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//    return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//    // simplify haversine:
//    //return 2 * R * 1000 * Math.asin(Math.sqrt(a));
//}


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
             ax = p0.values[0].toDouble()
             ay = p0.values[1].toDouble()
             az = p0.values[2].toDouble()

            var tax = String.format("%.6f", ax).toDouble().toString()
            var tay = String.format("%.6f", ay).toDouble().toString()
            var taz = String.format("%.6f", az).toDouble().toString()

             txt_accelerometer.text = "Accelerometer : ["+ tax+" , "+tay+" , "+taz+"]"
        }

        if (mySensor?.type == Sensor.TYPE_GYROSCOPE) {
             gx = p0.values[0].toDouble()
             gy = p0.values[1].toDouble()
             gz = p0.values[2].toDouble()

            var tax = String.format("%.6f", gx).toDouble().toString()
            var tay = String.format("%.6f", gy).toDouble().toString()
            var taz = String.format("%.6f", gz).toDouble().toString()
             txt_gyroscope.text = "Gyroscope : ["+tax+" , "+tay+" , "+taz+"]"

        }

        if (tripTimer%6 == 0) {
            // To send data in interval of 5 seconds
            // Add data to sheet - calculate speed here by maintaining
            // prev x,y,z and then calculating it on an interval of 5 seconds

            val rnds = (0..20).random()

            val ts : String = SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Date())

            txt_speed.text="Speed : "+speed.toString()
            prevax = ax
            prevay = ay
            prevaz = az
            var addresses : List<Address>

            geocoder = Geocoder(this, Locale.getDefault())

            addresses = geocoder.getFromLocation(latitude, longitude, 1)

            accelerometerData.clear()

            if (addresses.isNotEmpty()) {
                accelerometerData?.add(ts)
                accelerometerData?.add(ax.toString())
                accelerometerData?.add(ay.toString())
                accelerometerData?.add(az.toString())
                accelerometerData?.add(gx.toString())
                accelerometerData?.add(gy.toString())
                accelerometerData?.add(gz.toString())
                accelerometerData?.add(latitude.toString())
                accelerometerData?.add(longitude.toString())
                accelerometerData?.add(speed.toString())
                accelerometerData?.add(addresses.get(0).featureName)
                accelerometerData?.add(addresses.get(0).locality)
                accelerometerData?.add(addresses.get(0).adminArea)
                accelerometerData?.add(addresses.get(0).postalCode)
                accelerometerData?.add(addresses.get(0).countryName)
            }


            var data : String = ""

            for(x in accelerometerData){

                data += x + ","
            }

            txt_location.text = "Location : "+addresses.get(0).getAddressLine(0)

            data += "0\n"

            if(rnds == 10){
                Log.e("Accelerometer ",accelerometerData.toString())
                writeFileExternalStorage(data,"FirstSheet.csv")
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
