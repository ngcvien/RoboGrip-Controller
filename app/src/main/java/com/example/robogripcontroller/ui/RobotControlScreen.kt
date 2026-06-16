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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.robogripcontroller.macro.RecordedCommand
import com.example.robogripcontroller.protocol.RobotCommand
import com.example.robogripcontroller.sensor.GyroSteeringController
import com.example.robogripcontroller.sensor.GyroSteeringState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val gyroState by gyroController.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    var mode by remember { mutableStateOf(ControlMode.JOYSTICK) }
    var speed by remember { mutableIntStateOf(180) }
    var armSpeed by remember { mutableIntStateOf(180) }

    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var isReplaying by remember { mutableStateOf(false) }
    var lastRecordTimeMs by remember { mutableLongStateOf(0L) }
    var lastRecordedCommand by remember { mutableStateOf("") }

    val recordedCommands = remember { mutableStateListOf<RecordedCommand>() }

    var joystickForward by remember { mutableIntStateOf(0) }
    var joystickTurn by remember { mutableIntStateOf(0) }
    var gyroForwardValue by remember { mutableIntStateOf(0) }

    val driveCommand by rememberUpdatedState(
        RobotCommand.drive(joystickForward, joystickTurn, speed)
    )
    val isConnected = uiState.status == ConnectionStatus.CONNECTED

    var showConnectionDialog by remember { mutableStateOf(false) }

    var currentLiftAxis by remember { mutableIntStateOf(0) }
    var currentGripAxis by remember { mutableIntStateOf(0) }
    var isGripHolding by remember { mutableStateOf(false) }

    if (showConnectionDialog) {
        ConnectionDialog(
            bluetoothController = bluetoothController,
            onDismiss = { showConnectionDialog = false }
        )
    }

    DisposableEffect(mode) {
        if (mode == ControlMode.GYRO) {
            gyroController.start()
        } else {
            gyroController.stop()
        }

        onDispose {
            gyroController.stop()
        }
    }


    fun normalizeCommand(command: String): String {
        return command.trim()
    }

    fun recordMacroCommand(command: String) {
        if (!isRecording || isReplaying) return

        val cleanCommand = normalizeCommand(command)
        if (cleanCommand.isBlank()) return

        val now = System.currentTimeMillis()

        // Tránh ghi quá dày cùng một lệnh, làm replay bị nặng và trễ
        if (cleanCommand == lastRecordedCommand && now - lastRecordTimeMs < 120) {
            return
        }

        val delayMs = if (recordedCommands.isEmpty()) {
            0L
        } else {
            (now - lastRecordTimeMs).coerceIn(0L, 1000L)
        }

        recordedCommands.add(
            RecordedCommand(
                delayMs = delayMs,
                command = cleanCommand
            )
        )

        lastRecordTimeMs = now
        lastRecordedCommand = cleanCommand
    }

    fun sendNormalCommand(
        command: String,
        saveToHistory: Boolean = false,
        recordable: Boolean = true
    ) {
        bluetoothController.send(
            command = command,
            saveToHistory = saveToHistory
        )

        if (recordable) {
            recordMacroCommand(command)
        }
    }
    fun sendArmState(
        liftAxis: Int = currentLiftAxis,
        gripAxis: Int = currentGripAxis,
        saveToHistory: Boolean = true,
        recordable: Boolean = true
    ) {
        sendNormalCommand(
            command = RobotCommand.arm(liftAxis, gripAxis),
            saveToHistory = saveToHistory,
            recordable = recordable
        )
    }

    fun sendRealtimeCommand(
        command: String,
        recordable: Boolean = true
    ) {
        bluetoothController.sendRealtime(command)

        if (recordable) {
            recordMacroCommand(command)
        }
    }

    fun startRecording() {
        if (isReplaying) return

        recordedCommands.clear()
        lastRecordTimeMs = System.currentTimeMillis()
        lastRecordedCommand = ""
        isRecording = true
    }

    fun stopRecording() {
        isRecording = false

        sendNormalCommand(
            command = RobotCommand.stop(),
            saveToHistory = true,
            recordable = false
        )
    }

    fun clearMacro() {
        if (isRecording || isReplaying) return

        recordedCommands.clear()
        lastRecordedCommand = ""
        lastRecordTimeMs = 0L
    }

    fun replayMacro() {
        if (isRecording || isReplaying || recordedCommands.isEmpty()) return

        val snapshot = recordedCommands.toList()

        scope.launch {
            isReplaying = true

            // Dừng robot trước khi replay để tránh dính lệnh cũ
            bluetoothController.send(
                RobotCommand.stop(),
                saveToHistory = true
            )

            delay(250)

            snapshot.forEach { item ->
                delay(item.delayMs)

                bluetoothController.send(
                    command = item.command,
                    saveToHistory = false
                )
            }

            delay(150)

            bluetoothController.send(
                RobotCommand.stop(),
                saveToHistory = true
            )

            isReplaying = false
        }
    }

    LaunchedEffect(isConnected, mode) {
        while (true) {
            if (mode != ControlMode.JOYSTICK) {
                return@LaunchedEffect
            }
            if (isConnected) {
                sendRealtimeCommand(driveCommand)
            }
            delay(50L)
        }
    }

    LaunchedEffect(
        mode,
        gyroForwardValue,
        gyroState.turn,
        speed,
        uiState.status
    ) {
        if (uiState.status != ConnectionStatus.CONNECTED) {
            return@LaunchedEffect
        }

        if (mode != ControlMode.GYRO) {
            return@LaunchedEffect
        }

        val isIdle = gyroForwardValue == 0 && gyroState.turn == 0

        if (isIdle) {
            sendNormalCommand(
                command = RobotCommand.drive(0, 0, speed),
                saveToHistory = false,
                recordable = true
            )
            return@LaunchedEffect
        }

        while (true) {
            val limitedTurn = if (gyroForwardValue == 0) {
                gyroState.turn
            } else {
                gyroState.turn.coerceIn(-80, 80)
            }

            sendRealtimeCommand(
                command = RobotCommand.drive(
                    forward = gyroForwardValue,
                    turn = limitedTurn,
                    speed = speed
                ),
                recordable = true
            )

            delay(80)
        }
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.Background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppColors.BackgroundTop, AppColors.Background),
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
//                            bluetoothController.send(RobotCommand.stop(), saveToHistory = true)
                        sendNormalCommand(
                            command = RobotCommand.stop(),
                            saveToHistory = true,
                            recordable = true
                        )
                    }

                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ControlPanel(
                        modifier = Modifier.weight(1.1f),
                        mode = mode,
                        onModeChange = { newMode ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            mode = newMode
                            gyroForwardValue = 0
                            joystickForward = 0
                            joystickTurn = 0

                            if (newMode == ControlMode.GYRO) {
                                gyroController.calibrate()
                            }

                            sendNormalCommand(
                                command = RobotCommand.drive(0, 0, speed),
                                saveToHistory = false,
                                recordable = false
                            )
                        },
                        onJoystickMove = { forward, turn ->
                            joystickForward = forward
                            joystickTurn = turn
                        },
                        gyroState = gyroState,
                        gyroForwardValue = gyroForwardValue,
                        onGyroForwardChange = { value ->
                            gyroForwardValue = value

                            if (value == 0 && gyroState.turn == 0) {
                                sendNormalCommand(
                                    command = RobotCommand.drive(0, 0, speed),
                                    saveToHistory = false,
                                    recordable = true
                                )
                            }
                        },
                        onGyroCalibrate = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            gyroController.calibrate()
                        },
                        onGyroSensitivityChange = { value ->
                            gyroController.setSensitivity(value)
                        }
                    )

                    RightDashboard(
                        modifier = Modifier.weight(0.9f),
                        speed = speed,
                        onSpeedChange = { speed = it },
                        armSpeed = armSpeed,
                        onArmSpeedChange = { armSpeed = it },
                        isGripHolding = isGripHolding,

                        onLiftChange = { lift ->
                            currentLiftAxis = lift

                            sendArmState(
                                liftAxis = currentLiftAxis,
                                gripAxis = currentGripAxis,
                                saveToHistory = true,
                                recordable = true
                            )
                        },

                        onGripHold = {
                            isGripHolding = true
                            currentGripAxis = -armSpeed

                            sendArmState(
                                liftAxis = currentLiftAxis,
                                gripAxis = currentGripAxis,
                                saveToHistory = true,
                                recordable = true
                            )
                        },

                        onGripOpenChanged = { pressed ->
                            if (pressed) {
                                isGripHolding = false
                                currentGripAxis = armSpeed
                            } else {
                                currentGripAxis = 0
                            }

                            sendArmState(
                                liftAxis = currentLiftAxis,
                                gripAxis = currentGripAxis,
                                saveToHistory = true,
                                recordable = true
                            )
                        },

                        onStopArm = {
                            isGripHolding = false
                            currentLiftAxis = 0
                            currentGripAxis = 0

                            sendArmState(
                                liftAxis = 0,
                                gripAxis = 0,
                                saveToHistory = true,
                                recordable = true
                            )
                        },
                        isRecording = isRecording,
                        isReplaying = isReplaying,
                        recordedCommands = recordedCommands,
                        onStartRecord = { startRecording() },
                        onStopRecord = { stopRecording() },
                        onReplayMacro = { replayMacro() },
                        onClearMacro = { clearMacro() },
                        commandHistory = uiState.commandHistory,
                        errorMessage = uiState.errorMessage
                    )
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
            .border(1.dp, AppColors.Border, RoundedCornerShape(22.dp))
            .background(AppColors.Surface, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "RoboGrip Controller",
                color = AppColors.TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Tank drive + 2-axis gripper | Android Kotlin",
                color = AppColors.TextMuted,
                fontSize = 12.sp
            )


        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StatusChip("MODE: ${mode.name}", AppColors.Info)
            StatusChip("SPEED: $speed", AppColors.Primary)
            ConnectionButton(
                isConnected = status == ConnectionStatus.CONNECTED,
                onClick = onConnectionClick
            )
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("STOP", color = AppColors.TextMain, fontWeight = FontWeight.Black, fontSize = 18.sp)
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
        Text(text = text, color = AppColors.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlPanel(
    modifier: Modifier,
    mode: ControlMode,
    onModeChange: (ControlMode) -> Unit,
    onJoystickMove: (Int, Int) -> Unit,
    gyroState: GyroSteeringState,
    gyroForwardValue: Int,
    onGyroForwardChange: (Int) -> Unit,
    onGyroCalibrate: () -> Unit,
    onGyroSensitivityChange: (Float) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
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
                        Text(
                            "Joystick Mode",
                            color = AppColors.TextMain,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Y: tới/lùi", color = AppColors.TextMuted, fontSize = 15.sp)
                        Text("X: rẽ trái/phải", color = AppColors.TextMuted, fontSize = 15.sp)
                        Text("Thả joystick = dừng", color = AppColors.Primary, fontSize = 15.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    GyroDrivePanel(
                        gyroState = gyroState,
                        forwardValue = gyroForwardValue,
                        onForwardChange = onGyroForwardChange,
                        onCalibrate = onGyroCalibrate,
                        onSensitivityChange = onGyroSensitivityChange
                    )
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
            containerColor = if (selected) AppColors.Primary else AppColors.SurfaceSoft
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.Background else AppColors.TextMain,
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
    isGripHolding: Boolean,
    onLiftChange: (Int) -> Unit,
    onGripHold: () -> Unit,
    onGripOpenChanged: (Boolean) -> Unit,
    onStopArm: () -> Unit,
    isRecording: Boolean,
    isReplaying: Boolean,
    recordedCommands: List<RecordedCommand>,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onReplayMacro: () -> Unit,
    onClearMacro: () -> Unit,
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
                isGripHolding = isGripHolding,
                onArmSpeedChange = onArmSpeedChange,
                onLiftChange = onLiftChange,
                onGripHold = onGripHold,
                onGripOpenChanged = onGripOpenChanged,
                onStopArm = onStopArm
            )

            MacroPanel(
                isRecording = isRecording,
                isReplaying = isReplaying,
                recordedCommands = recordedCommands,
                onStartRecord = onStartRecord,
                onStopRecord = onStopRecord,
                onReplay = onReplayMacro,
                onClear = onClearMacro
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
