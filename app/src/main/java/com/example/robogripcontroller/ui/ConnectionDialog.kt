package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.robogripcontroller.bluetooth.BluetoothController
import com.example.robogripcontroller.bluetooth.ConnectionStatus

@Composable
fun ConnectionDialog(
    bluetoothController: BluetoothController,
    onDismiss: () -> Unit
) {
    val uiState by bluetoothController.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(min = 420.dp, max = 560.dp)
                .border(1.dp, AppColors.Border, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Bluetooth Connection",
                    color = AppColors.TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Status: ${uiState.status.name}",
                    color = if (uiState.status == ConnectionStatus.CONNECTED) {
                        AppColors.Primary
                    } else {
                        AppColors.TextMuted
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (uiState.selectedDevice != null) {
                        "Selected: ${uiState.selectedDevice!!.name}"
                    } else {
                        "No device selected"
                    },
                    color = AppColors.TextMuted,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardButton(
                        text = "REFRESH",
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        primary = false,
                        onClick = { bluetoothController.refreshPairedDevices() }
                    )

                    DashboardButton(
                        text = if (uiState.status == ConnectionStatus.CONNECTED) {
                            "DISCONNECT"
                        } else {
                            "CONNECT"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        onClick = {
                            if (uiState.status == ConnectionStatus.CONNECTED) {
                                bluetoothController.disconnect()
                            } else {
                                uiState.selectedDevice?.let {
                                    bluetoothController.connect(it)
                                }
                            }
                        }
                    )
                }

                Text(
                    text = "Paired devices",
                    color = AppColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 230.dp)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(20.dp))
                        .background(AppColors.Background, RoundedCornerShape(20.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.pairedDevices.isEmpty()) {
                        Text(
                            text = "No paired devices found. Pair the ESP32 in Android Bluetooth Settings first.",
                            color = AppColors.TextMuted,
                            fontSize = 13.sp
                        )
                    } else {
                        uiState.pairedDevices.forEach { device ->
                            val selected = uiState.selectedDevice?.address == device.address

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { bluetoothController.selectDevice(device) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) {
                                        AppColors.Primary
                                    } else {
                                        AppColors.SurfaceSoft
                                    },
                                    contentColor = if (selected) {
                                        AppColors.Background
                                    } else {
                                        AppColors.TextMain
                                    }
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    text = device.name,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = AppColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                DashboardButton(
                    text = "CLOSE",
                    onClick = onDismiss,
                    primary = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }
        }
    }
}
