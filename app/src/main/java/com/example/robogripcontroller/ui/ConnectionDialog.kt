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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceStrong),
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

                if (uiState.selectedDevice != null) {
                    Text(
                        text = "Selected: ${uiState.selectedDevice!!.name}",
                        color = AppColors.TextMuted,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Chưa chọn thiết bị",
                        color = AppColors.TextDim,
                        fontSize = 13.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { bluetoothController.refreshPairedDevices() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.SurfaceSoft,
                            contentColor = AppColors.TextMain
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Refresh")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (uiState.status == ConnectionStatus.CONNECTED) {
                                bluetoothController.disconnect()
                            } else {
                                uiState.selectedDevice?.let {
                                    bluetoothController.connect(it)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = AppColors.Background
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (uiState.status == ConnectionStatus.CONNECTED) {
                                "Disconnect"
                            } else {
                                "Connect"
                            },
                            fontWeight = FontWeight.Black
                        )
                    }
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
                            text = "Không tìm thấy thiết bị đã pair. Hãy pair ESP32 trong Bluetooth Settings trước.",
                            color = AppColors.TextDim,
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
                                shape = RoundedCornerShape(16.dp)
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
                        color = AppColors.Danger,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.SurfaceSoft,
                        contentColor = AppColors.TextMain
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}