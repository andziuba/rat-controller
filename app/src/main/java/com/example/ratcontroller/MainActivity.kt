package com.example.ratcontroller

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.StrictMode
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.Socket

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private lateinit var gyroscopeTextView: TextView
    private lateinit var directionTextView: TextView
    private lateinit var layDownButton: Button

    private lateinit var socket: Socket
    private lateinit var out: PrintWriter

    private var lastSentTime = 0L
    private val sendInterval = 1000L // 1 sekunda

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicjalizacja
        gyroscopeTextView = findViewById(R.id.gyroscope_text)
        directionTextView = findViewById(R.id.direction_text)
        layDownButton = findViewById(R.id.lay_down_button)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        if (sensor == null) {
            gyroscopeTextView.text = "Game Rotation Vector sensor not available on this device."
        }

        // Zezwolenie na operacje sieciowe w głównym wątku (niezalecane w produkcji)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Połączenie z serwerem na początku
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket("192.168.34.27", 1100) // Podaj IP i port Raspberry Pi
                out = PrintWriter(socket.getOutputStream(), true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Obsługa kliknięcia przycisku "Lay Down"
        layDownButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                sendDirectionToServer("Rest")
            }
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

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                out.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

                // Wysłanie kierunku do Raspberry Pi z opóźnieniem
                if (System.currentTimeMillis() - lastSentTime >= sendInterval) {
                    lastSentTime = System.currentTimeMillis()
                    CoroutineScope(Dispatchers.IO).launch {
                        sendDirectionToServer(direction)
                    }
                }
            }
        }
    }

    private fun getDirectionFromOrientation(pitch: Float, roll: Float): String {
        val threshold = 15  // Próg czułości

        return when {
            pitch > threshold -> "Forward"
            pitch < -threshold -> "Backward"
            roll > threshold -> "Right"
            roll < -threshold -> "Left"
            else -> "Neutral"
        }
    }

    private fun sendDirectionToServer(direction: String) {
        try {
            out.println(direction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}