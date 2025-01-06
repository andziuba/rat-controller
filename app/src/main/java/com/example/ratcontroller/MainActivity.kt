package com.example.ratcontroller

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private lateinit var gyroscopeTextView: TextView
    private lateinit var directionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja
        gyroscopeTextView = findViewById(R.id.gyroscope_text)
        directionTextView = findViewById(R.id.direction_text)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        if (sensor == null) {
            gyroscopeTextView.text = "Game Rotation Vector sensor not available on this device."
        }
    }

    override fun onResume() {
        super.onResume()
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)  // macierz rotacji
                val orientationAngles = FloatArray(3)  // kąty orientacji (yaw, pitch, roll)

                // Konwersja na macierz rotacji
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Otrzymanie kątu orientacji na podstawie macierzy rotacji
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                // Konwersja kątow z radianów na stopnie
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()  // pochylenie
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()  // przechylenie

                gyroscopeTextView.text = """
                    Pitch: ${String.format("%.1f", pitch)}
                    Roll: ${String.format("%.1f", roll)}
                """.trimIndent()

                // Określenie kierunku na podstawie kątów pochylenia i przechylenia
                val direction = getDirectionFromOrientation(pitch, roll)
                directionTextView.text = direction
            }
        }
    }

    private fun getDirectionFromOrientation(pitch: Float, roll: Float): String {
        val threshold = 15  // Próg czułości (do dostosowania)

        return when {
            pitch > threshold -> "Forward"
            pitch < -threshold -> "Backward"
            roll > threshold -> "Right"
            roll < -threshold -> "Left"
            else -> "Neutral"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}

