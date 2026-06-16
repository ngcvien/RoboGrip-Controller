package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopBar(
                status = uiState.status,
                onConnectionClick = { showConnectionDialog = true }
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ControlPanel(
                    modifier = Modifier.weight(1.18f),
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
                    },
                    onStop = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sendNormalCommand(
                            command = RobotCommand.stop(),
                            saveToHistory = true,
                            recordable = true
                        )
                    }
                )

                RightDashboard(
                    modifier = Modifier.weight(0.82f),
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

@Composable
private fun TopBar(
    status: ConnectionStatus,
    onConnectionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .border(1.dp, AppColors.Border, RoundedCornerShape(24.dp))
            .background(AppColors.Surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp),
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
                text = "Robot drive & gripper dashboard",
                color = AppColors.TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusChip(
                text = if (status == ConnectionStatus.CONNECTED) "CONNECTED" else "DISCONNECTED",
                active = status == ConnectionStatus.CONNECTED
            )
            DashboardButton(
                text = "CONNECTION",
                onClick = onConnectionClick,
                primary = false,
                modifier = Modifier.height(46.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    active: Boolean
) {
    val borderColor = if (active) AppColors.Primary else AppColors.Border
    val textColor = if (active) AppColors.Primary else AppColors.TextMuted

    Box(
        modifier = Modifier
            .height(38.dp)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .background(AppColors.Background, RoundedCornerShape(50))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
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
    onGyroSensitivityChange: (Float) -> Unit,
    onStop: () -> Unit
) {
    ControlCard(
        modifier = modifier.fillMaxHeight(),
        contentPadding = 18
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ModeSwitch(
                mode = mode,
                onModeChange = onModeChange
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (mode == ControlMode.JOYSTICK) {
                        JoystickModePanel(onJoystickMove = onJoystickMove)
                    } else {
                        GyroDrivePanel(
                            gyroState = gyroState,
                            forwardValue = gyroForwardValue,
                            onForwardChange = onGyroForwardChange,
                            onCalibrate = onGyroCalibrate,
                            onSensitivityChange = onGyroSensitivityChange
                        )
                    }
                }

                EmergencyStopButton(
                    modifier = Modifier
                        .width(152.dp)
                        .fillMaxHeight(),
                    onStop = onStop
                )
            }
        }
    }
}

@Composable
private fun JoystickModePanel(
    onJoystickMove: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        JoystickPad(
            size = 190.dp,
            onMove = onJoystickMove
        )
        Text(
            text = "Joystick Mode",
            color = AppColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ModeSwitch(
    mode: ControlMode,
    onModeChange: (ControlMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, AppColors.Border, RoundedCornerShape(20.dp))
            .background(AppColors.Background, RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentedModeButton(
            text = "JOYSTICK",
            selected = mode == ControlMode.JOYSTICK,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ControlMode.JOYSTICK) }
        )
        SegmentedModeButton(
            text = "GYRO",
            selected = mode == ControlMode.GYRO,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ControlMode.GYRO) }
        )
    }
}

@Composable
private fun SegmentedModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppColors.Primary else Color.Transparent,
            contentColor = if (selected) AppColors.Background else AppColors.TextMuted
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EmergencyStopButton(
    modifier: Modifier,
    onStop: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Danger,
            contentColor = AppColors.TextMain
        ),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "STOP",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "EMERGENCY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(1.dp, AppColors.Border, RoundedCornerShape(24.dp))
            .background(AppColors.Surface, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpeedPanel(
                speed = speed,
                onSpeedChange = onSpeedChange
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

            CommandHistoryPanel(commandHistory = commandHistory)

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = AppColors.Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SpeedPanel(
    speed: Int,
    onSpeedChange: (Int) -> Unit
) {
    ControlCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Speed",
                    color = AppColors.TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Drive output: $speed",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$speed",
                color = AppColors.Primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        Slider(
            value = speed.toFloat(),
            onValueChange = { onSpeedChange(it.roundToInt()) },
            valueRange = 80f..255f,
            colors = dashboardSliderColors()
        )
    }
}

@Composable
private fun CommandHistoryPanel(commandHistory: List<String>) {
    ControlCard {
        Text(
            text = "Command History",
            color = AppColors.TextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp, max = 150.dp)
                .border(1.dp, AppColors.Border, RoundedCornerShape(18.dp))
                .background(AppColors.Background, RoundedCornerShape(18.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val latestCommands = commandHistory.takeLast(6)

            if (latestCommands.isEmpty()) {
                Text(
                    text = "No commands yet",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
            } else {
                latestCommands.forEach { command ->
                    Text(
                        text = "> $command",
                        color = AppColors.Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ControlCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 12,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, RoundedCornerShape(22.dp))
            .background(AppColors.Background, RoundedCornerShape(22.dp))
            .padding(contentPadding.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun DashboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    danger: Boolean = false,
    enabled: Boolean = true
) {
    val containerColor = when {
        danger -> AppColors.Danger
        primary -> AppColors.Primary
        else -> AppColors.SurfaceSoft
    }
    val contentColor = if (primary && !danger) AppColors.Background else AppColors.TextMain

    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AppColors.SurfaceSoft,
            disabledContentColor = AppColors.TextMuted
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp
        )
    }
}

@Composable
fun dashboardSliderColors() = SliderDefaults.colors(
    thumbColor = AppColors.Primary,
    activeTrackColor = AppColors.Primary,
    inactiveTrackColor = AppColors.SurfaceSoft,
    activeTickColor = AppColors.Primary,
    inactiveTickColor = AppColors.Border
)
