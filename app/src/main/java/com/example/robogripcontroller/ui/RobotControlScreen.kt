package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robogripcontroller.bluetooth.BluetoothController
import com.example.robogripcontroller.bluetooth.ConnectionStatus
import com.example.robogripcontroller.protocol.RobotCommand
import com.example.robogripcontroller.sensor.GyroSteeringController
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
enum class ControlMode {
    JOYSTICK,
    GYRO
}

@Composable
fun RobotControlScreen(
    bluetoothController: BluetoothController,
    gyroController: GyroSteeringController
) {
    val uiState by bluetoothController.uiState.collectAsState()
    val roll by gyroController.rollDegrees.collectAsState()
    val haptic = LocalHapticFeedback.current

    var mode by remember { mutableStateOf(ControlMode.JOYSTICK) }
    var speed by remember { mutableIntStateOf(180) }
    var armSpeed by remember { mutableIntStateOf(180) }

    var joystickForward by remember { mutableIntStateOf(0) }
    var joystickTurn by remember { mutableIntStateOf(0) }
    var gyroForward by remember { mutableIntStateOf(0) }

    val gyroMaxTurn = if (gyroForward == 0) 127 else 80
    val gyroTurn = gyroController.calculateTurn(maxTurn = gyroMaxTurn)

    val driveForward = if (mode == ControlMode.JOYSTICK) joystickForward else gyroForward
    val driveTurn = if (mode == ControlMode.JOYSTICK) joystickTurn else gyroTurn
    val driveCommand by rememberUpdatedState(
        RobotCommand.drive(driveForward, driveTurn, speed)
    )
    val isConnected = uiState.status == ConnectionStatus.CONNECTED

    var showConnectionDialog by remember { mutableStateOf(false) }

    if (showConnectionDialog) {
        ConnectionDialog(
            bluetoothController = bluetoothController,
            onDismiss = { showConnectionDialog = false }
        )
    }

    LaunchedEffect(isConnected) {
        while (true) {
            if (isConnected) {
                bluetoothController.send(driveCommand, saveToHistory = false)
            }
            delay(50L)
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF07090D)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF18202D), Color(0xFF07090D)),
                            radius = 900f
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderBar(
                        status = uiState.status,
                        speed = speed,
                        mode = mode,
                        onConnectionClick = { showConnectionDialog = true },
                        onStop = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            bluetoothController.send(RobotCommand.stop(), saveToHistory = true)
                        }
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ControlPanel(
                            modifier = Modifier.weight(1.1f),
                            mode = mode,
                            onModeChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mode = it
                            },
                            onJoystickMove = { forward, turn ->
                                joystickForward = forward
                                joystickTurn = turn
                            },
                            gyroRoll = roll,
                            gyroTurn = gyroTurn,
                            onGyroForwardChange = { pressed ->
                                gyroForward = if (pressed) 100 else if (gyroForward == 100) 0 else gyroForward
                            },
                            onGyroBackwardChange = { pressed ->
                                gyroForward = if (pressed) -100 else if (gyroForward == -100) 0 else gyroForward
                            },
                            onGyroCalibrate = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                gyroController.calibrate()
                            }
                        )

                        RightDashboard(
                            modifier = Modifier.weight(0.9f),
                            speed = speed,
                            onSpeedChange = { speed = it },
                            armSpeed = armSpeed,
                            onArmSpeedChange = { armSpeed = it },
                            onArmCommand = { axis1, axis2 ->
                                bluetoothController.send(
                                    RobotCommand.arm(axis1, axis2),
                                    saveToHistory = true
                                )
                            },
                            commandHistory = uiState.commandHistory,
                            errorMessage = uiState.errorMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    status: ConnectionStatus,
    speed: Int,
    mode: ControlMode,
    onConnectionClick: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .border(1.dp, Color(0xFF273040), RoundedCornerShape(22.dp))
            .background(Color(0xCC10141D), RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "RoboGrip Controller",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Tank drive + 2-axis gripper | Android Kotlin",
                color = Color(0xFF9AA4B5),
                fontSize = 12.sp
            )

            
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StatusChip("MODE: ${mode.name}", Color(0xFF2F80ED))
            StatusChip("SPEED: $speed", Color(0xFFFFC857))
            StatusChip(status.name, if (status == ConnectionStatus.CONNECTED) Color(0xFF2ECC71) else Color(0xFFE74C3C))
            ConnectionButton(
                isConnected = status == ConnectionStatus.CONNECTED,
                onClick = onConnectionClick
            )
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("STOP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlPanel(
    modifier: Modifier,
    mode: ControlMode,
    onModeChange: (ControlMode) -> Unit,
    onJoystickMove: (Int, Int) -> Unit,
    gyroRoll: Float,
    gyroTurn: Int,
    onGyroForwardChange: (Boolean) -> Unit,
    onGyroBackwardChange: (Boolean) -> Unit,
    onGyroCalibrate: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0D1119)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeButton(
                    text = "JOYSTICK",
                    selected = mode == ControlMode.JOYSTICK,
                    onClick = { onModeChange(ControlMode.JOYSTICK) }
                )
                ModeButton(
                    text = "GYRO DRIVE",
                    selected = mode == ControlMode.GYRO,
                    onClick = { onModeChange(ControlMode.GYRO) }
                )
            }

            if (mode == ControlMode.JOYSTICK) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    JoystickPad(onMove = onJoystickMove)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Joystick Mode", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Y: tới/lùi", color = Color(0xFFAAB4C3), fontSize = 15.sp)
                        Text("X: rẽ trái/phải", color = Color(0xFFAAB4C3), fontSize = 15.sp)
                        Text("Thả joystick = dừng", color = Color(0xFFFFC857), fontSize = 15.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Gyro Drive Mode", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Tới/lùi bằng nút lớn, rẽ bằng độ nghiêng điện thoại", color = Color(0xFFAAB4C3))
                        }
                        Button(
                            onClick = onGyroCalibrate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263247)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("CALIBRATE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HoldButton(
                            text = "TỚI",
                            primaryColor = Color(0xFF2ECC71),
                            onHoldChanged = onGyroForwardChange
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ROLL", color = Color(0xFF9AA4B5), fontSize = 12.sp)
                            Text("${gyroRoll.roundToInt()}°", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                            Text("TURN = $gyroTurn", color = Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        HoldButton(
                            text = "LÙI",
                            primaryColor = Color(0xFFE74C3C),
                            onHoldChanged = onGyroBackwardChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFFFC857) else Color(0xFF1B2230)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.White,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun RightDashboard(
    modifier: Modifier,
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    armSpeed: Int,
    onArmSpeedChange: (Int) -> Unit,
    onArmCommand: (axis1: Int, axis2: Int) -> Unit,
    commandHistory: List<String>,
    errorMessage: String?
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Drive Settings",
                color = AppColors.TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Speed: $speed",
                color = AppColors.TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = speed.toFloat(),
                onValueChange = { onSpeedChange(it.roundToInt()) },
                valueRange = 80f..255f
            )

            ArmControlPanel(
                armSpeed = armSpeed,
                onArmSpeedChange = onArmSpeedChange,
                onArmCommand = onArmCommand
            )

            Text(
                text = "Command History",
                color = AppColors.TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(18.dp))
                    .background(AppColors.Background, RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (commandHistory.isEmpty()) {
                    Text(
                        text = "Chưa có lệnh",
                        color = AppColors.TextDim,
                        fontSize = 13.sp
                    )
                } else {
                    commandHistory.take(8).forEach { command ->
                        Text(
                            text = command,
                            color = AppColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = AppColors.Danger,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ConnectionButton(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isConnected) {
                AppColors.Primary
            } else {
                AppColors.SurfaceSoft
            },
            contentColor = if (isConnected) {
                AppColors.Background
            } else {
                AppColors.TextMain
            }
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = if (isConnected) "CONNECTED" else "CONNECTION",
            fontWeight = FontWeight.Black
        )
    }
}
