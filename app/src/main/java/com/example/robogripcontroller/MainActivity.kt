package com.example.robogripcontroller

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.example.robogripcontroller.bluetooth.BluetoothController
import com.example.robogripcontroller.sensor.GyroSteeringController
import com.example.robogripcontroller.ui.RobotControlScreen
import com.example.robogripcontroller.ui.theme.RobogripcontrollerTheme



class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        bluetoothController?.refreshPairedDevices()
        bluetoothController?.autoReconnect()
    }

    private var bluetoothController: BluetoothController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val btController = BluetoothController(this, lifecycleScope)
        bluetoothController = btController

        requestBluetoothPermissionsIfNeeded()
        btController.refreshPairedDevices()
        btController.autoReconnect()

        setContent {
            val gyroController = remember { GyroSteeringController(this) }

            DisposableEffect(Unit) {
                gyroController.start()
                onDispose { gyroController.stop() }
            }

            RobogripcontrollerTheme {
                RobotControlScreen(
                    bluetoothController = btController,
                    gyroController = gyroController
                )
            }
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }
}
