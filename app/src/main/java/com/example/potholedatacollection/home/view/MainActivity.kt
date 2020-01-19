package com.example.potholedatacollection.home.view

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import com.example.potholedatacollection.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        if (isTripOn) {
            isTripOn = false
            btn_start.text = "Start"
            disableSensors()
            disablePotholeCounter()
            return
        }
        tripTimer = 0
        potHoleCounter = 0
        isTripOn = true
        btn_start.text = "Trip On"
        btn_pothole.isEnabled = true
        object : CountDownTimer(50000, 1000) {
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
    }


    fun enableSensors() {
        /**We need sensor readings every 5th second */
        sensorManager.registerListener(this,sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun disableSensors() {
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
            val y = p0.values[0]
            val z = p0.values[0]

            if (tripTimer%5 == 0) {
                // To send data in interval of 5 seconds
                // Add data to sheet - calculate speed here by maintaining
                // prev x,y,z and then calculating it on an interval of 5 seconds
                Log.e("log","Sensor1 data : "+x+" "+y+" "+z)
            }
        }

        if (mySensor?.type == Sensor.TYPE_GYROSCOPE) {
            val x = p0.values[0]
            val y = p0.values[0]
            val z = p0.values[0]
            if (tripTimer%5 ==0) {
                // Add this data directly
                Log.e("log","Sensor2 data : "+x+" "+y+" "+z)
            }
        }
    }
    override fun onPause() {
        super.onPause()
        disableSensors()
    }

    override fun onResume() {
        super.onResume()
        enableSensors()
    }
}
