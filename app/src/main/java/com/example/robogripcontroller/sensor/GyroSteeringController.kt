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
import kotlin.math.atan2
import kotlin.math.roundToInt

data class GyroSteeringState(
    val isAvailable: Boolean = false,
    val isRunning: Boolean = false,
    val rawAngle: Float = 0f,
    val zeroAngle: Float = 0f,
    val filteredAngle: Float = 0f,
    val turn: Int = 0,
    val deadZoneDeg: Float = 6f,
    val maxAngleDeg: Float = 45f,
    val sensitivity: Float = 1.0f
)

class GyroSteeringController(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Dùng accelerometer cho kiểu xoay vô lăng.
    // Không dùng roll/pitch vì cách cầm điện thoại là landscape + dựng màn hình.
    private val accelerometerSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _state = MutableStateFlow(
        GyroSteeringState(
            isAvailable = accelerometerSensor != null
        )
    )

    val state: StateFlow<GyroSteeringState> = _state.asStateFlow()

    private var lastRawAngle = 0f
    private var zeroAngle = 0f
    private var filteredAngle = 0f

    fun start() {
        val sensor = accelerometerSensor ?: return

        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        _state.value = _state.value.copy(
            isRunning = true,
            isAvailable = true
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)

        filteredAngle = 0f

        _state.value = _state.value.copy(
            isRunning = false,
            filteredAngle = 0f,
            turn = 0
        )
    }

    fun calibrate() {
        zeroAngle = lastRawAngle
        filteredAngle = 0f

        _state.value = _state.value.copy(
            zeroAngle = zeroAngle,
            filteredAngle = 0f,
            turn = 0
        )
    }

    fun setSensitivity(value: Float) {
        _state.value = _state.value.copy(
            sensitivity = value.coerceIn(0.4f, 2.0f)
        )
    }

    fun setDeadZone(value: Float) {
        _state.value = _state.value.copy(
            deadZoneDeg = value.coerceIn(2f, 15f)
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val angle = readSteeringWheelAngle(event)

        lastRawAngle = angle

        val centeredAngle = normalizeAngle(angle - zeroAngle)

        // Low-pass filter để góc vô lăng mượt hơn
        filteredAngle = filteredAngle * 0.82f + centeredAngle * 0.18f

        val turn = angleToTurn(
            angle = filteredAngle,
            deadZone = _state.value.deadZoneDeg,
            maxAngle = _state.value.maxAngleDeg,
            sensitivity = _state.value.sensitivity
        )

        _state.value = _state.value.copy(
            rawAngle = angle,
            zeroAngle = zeroAngle,
            filteredAngle = filteredAngle,
            turn = turn
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun readSteeringWheelAngle(event: SensorEvent): Float {
        val ax = event.values[0]
        val ay = event.values[1]

        /*
            Kiểu cầm:
            - Điện thoại nằm ngang landscape
            - Màn hình dựng lên gần vuông góc mặt đất
            - Xoay điện thoại như vô lăng

            Khi đó trọng lực nằm chủ yếu trên mặt phẳng X/Y của điện thoại.
            atan2(ax, -ay) cho ra góc xoay kiểu vô lăng.

            CALIBRATE sẽ lấy góc hiện tại làm 0, nên không cần quan tâm
            điện thoại đang xoay landscape trái hay landscape phải.
        */
        return Math.toDegrees(
            atan2(ax.toDouble(), (-ay).toDouble())
        ).toFloat()
    }

    private fun angleToTurn(
        angle: Float,
        deadZone: Float,
        maxAngle: Float,
        sensitivity: Float
    ): Int {
        if (abs(angle) < deadZone) return 0

        val limitedAngle = angle.coerceIn(-maxAngle, maxAngle)
        val normalized = limitedAngle / maxAngle

        return (normalized * 127f * sensitivity)
            .roundToInt()
            .coerceIn(-127, 127)
    }

    private fun normalizeAngle(angle: Float): Float {
        var result = angle

        while (result > 180f) result -= 360f
        while (result < -180f) result += 360f

        return result
    }
}