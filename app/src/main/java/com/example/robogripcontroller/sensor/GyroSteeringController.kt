package com.example.robogripcontroller.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

class GyroSteeringController(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _rollDegrees = MutableStateFlow(0f)
    val rollDegrees: StateFlow<Float> = _rollDegrees.asStateFlow()

    private var zeroRoll = 0f
    private var smoothedRoll = 0f
    private val alpha = 0.18f

    fun start() {
        rotationVectorSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    fun calibrate() {
        zeroRoll = _rollDegrees.value
    }

    fun calculateTurn(
        deadZoneDegrees: Float = 5f,
        maxAngleDegrees: Float = 30f,
        maxTurn: Int = 127
    ): Int {
        val delta = _rollDegrees.value - zeroRoll
        if (abs(delta) < deadZoneDegrees) return 0

        val limited = delta.coerceIn(-maxAngleDegrees, maxAngleDegrees)
        return ((limited / maxAngleDegrees) * maxTurn)
            .roundToInt()
            .coerceIn(-maxTurn, maxTurn)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        smoothedRoll = smoothedRoll + alpha * (roll - smoothedRoll)
        _rollDegrees.value = smoothedRoll
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
