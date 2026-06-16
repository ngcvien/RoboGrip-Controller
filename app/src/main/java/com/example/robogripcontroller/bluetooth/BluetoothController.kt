package com.example.robogripcontroller.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

data class BtDeviceUi(
    val name: String,
    val address: String
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class BluetoothUiState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val selectedDevice: BtDeviceUi? = null,
    val pairedDevices: List<BtDeviceUi> = emptyList(),
    val errorMessage: String? = null,
    val commandHistory: List<String> = emptyList(),
    val autoReconnectEnabled: Boolean = true
)

class BluetoothController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("robogrip_bt", Context.MODE_PRIVATE)
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectJob: Job? = null

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private var lastHistoryCommand: String = ""
    private var lastHistoryTime: Long = 0L

    fun setAutoReconnect(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoReconnectEnabled = enabled)
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (bluetoothAdapter == null) {
            _uiState.value = _uiState.value.copy(
                status = ConnectionStatus.ERROR,
                errorMessage = "Thiết bị này không hỗ trợ Bluetooth"
            )
            return
        }

        if (!hasBluetoothConnectPermission()) {
            _uiState.value = _uiState.value.copy(
                status = ConnectionStatus.ERROR,
                errorMessage = "Chưa có quyền BLUETOOTH_CONNECT"
            )
            return
        }

        val devices = bluetoothAdapter.bondedDevices
            .map { device ->
                BtDeviceUi(
                    name = device.name ?: "Unknown device",
                    address = device.address
                )
            }
            .sortedBy { it.name }

        _uiState.value = _uiState.value.copy(
            pairedDevices = devices,
            errorMessage = null
        )
    }

    fun selectDevice(device: BtDeviceUi) {
        _uiState.value = _uiState.value.copy(selectedDevice = device)
    }

    fun autoReconnect() {
        if (!_uiState.value.autoReconnectEnabled) return
        val lastAddress = prefs.getString("last_device_address", null) ?: return
        val device = _uiState.value.pairedDevices.firstOrNull { it.address == lastAddress } ?: return
        selectDevice(device)
        connect(device)
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BtDeviceUi? = _uiState.value.selectedDevice) {
        val targetDevice = device ?: return

        if (bluetoothAdapter == null) return

        if (!hasBluetoothConnectPermission()) {
            _uiState.value = _uiState.value.copy(
                status = ConnectionStatus.ERROR,
                errorMessage = "Chưa có quyền Bluetooth"
            )
            return
        }

        connectJob?.cancel()

        connectJob = scope.launch {
            _uiState.value = _uiState.value.copy(
                status = ConnectionStatus.CONNECTING,
                selectedDevice = targetDevice,
                errorMessage = null
            )

            val result = withContext(Dispatchers.IO) {
                try {
                    bluetoothAdapter.cancelDiscovery()
                    closeCurrentSocket()

                    val remoteDevice: BluetoothDevice =
                        bluetoothAdapter.getRemoteDevice(targetDevice.address)

                    val newSocket =
                        remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)

                    newSocket.connect()

                    socket = newSocket
                    outputStream = newSocket.outputStream

                    prefs.edit()
                        .putString("last_device_address", targetDevice.address)
                        .apply()

                    null
                } catch (e: IOException) {
                    closeCurrentSocket()
                    e.message ?: "Không kết nối được Bluetooth"
                } catch (e: SecurityException) {
                    closeCurrentSocket()
                    e.message ?: "Thiếu quyền Bluetooth"
                }
            }

            _uiState.value = if (result == null) {
                _uiState.value.copy(
                    status = ConnectionStatus.CONNECTED,
                    errorMessage = null
                )
            } else {
                _uiState.value.copy(
                    status = ConnectionStatus.ERROR,
                    errorMessage = result
                )
            }
        }
    }
    fun disconnect() {
        scope.launch(Dispatchers.IO) {
            closeCurrentSocket()
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(status = ConnectionStatus.DISCONNECTED)
            }
        }
    }

    fun send(command: String, saveToHistory: Boolean = true) {
        val stream = outputStream ?: return
        scope.launch(Dispatchers.IO) {
            try {
                stream.write(command.toByteArray())
                stream.flush()
                if (saveToHistory) addHistory(command.trim())
            } catch (e: IOException) {
                closeCurrentSocket()
                _uiState.value = _uiState.value.copy(
                    status = ConnectionStatus.ERROR,
                    errorMessage = "Mất kết nối Bluetooth"
                )
                if (_uiState.value.autoReconnectEnabled) {
                    _uiState.value.selectedDevice?.let { connect(it) }
                }
            }
        }
    }

    private fun addHistory(command: String) {
        if (command.isBlank()) return
        val now = System.currentTimeMillis()
        val shouldAdd = command != lastHistoryCommand || now - lastHistoryTime > 700L
        if (!shouldAdd) return

        lastHistoryCommand = command
        lastHistoryTime = now

        val newHistory = buildList {
            add(command)
            addAll(_uiState.value.commandHistory)
        }.take(12)

        _uiState.value = _uiState.value.copy(commandHistory = newHistory)
    }

    private fun closeCurrentSocket() {
        try {
            outputStream?.close()
        } catch (_: IOException) {
        }
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        outputStream = null
        socket = null
    }
}
