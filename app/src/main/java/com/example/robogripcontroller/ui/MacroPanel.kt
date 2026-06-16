package com.example.robogripcontroller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robogripcontroller.macro.RecordedCommand

@Composable
fun MacroPanel(
    isRecording: Boolean,
    isReplaying: Boolean,
    recordedCommands: List<RecordedCommand>,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onReplay: () -> Unit,
    onClear: () -> Unit
) {
    ControlCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Macro",
                    color = AppColors.TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = when {
                        isRecording -> "Recording"
                        isReplaying -> "Replaying"
                        else -> "${recordedCommands.size} commands saved"
                    },
                    color = if (isRecording || isReplaying) AppColors.Primary else AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroButton(
                icon = if (isRecording) MacroIcon.Stop else MacroIcon.Record,
                contentDescription = if (isRecording) "Stop recording macro" else "Record macro",
                onClick = {
                    if (isRecording) {
                        onStopRecord()
                    } else {
                        onStartRecord()
                    }
                },
                enabled = !isReplaying,
                primary = true,
                modifier = Modifier.weight(1f)
            )

            MacroButton(
                icon = MacroIcon.Replay,
                contentDescription = "Replay macro",
                onClick = onReplay,
                enabled = !isRecording && !isReplaying && recordedCommands.isNotEmpty(),
                primary = false,
                modifier = Modifier.weight(1f)
            )

            MacroButton(
                icon = MacroIcon.Clear,
                contentDescription = "Clear macro",
                onClick = onClear,
                enabled = !isRecording && !isReplaying && recordedCommands.isNotEmpty(),
                primary = false,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp, max = 90.dp)
                .border(1.dp, AppColors.Border, RoundedCornerShape(18.dp))
                .background(AppColors.Surface, RoundedCornerShape(18.dp))
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (recordedCommands.isEmpty()) {
                Text(
                    text = "Press RECORD, then drive or move the arm.",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
            } else {
                recordedCommands.takeLast(3).forEach { item ->
                    Text(
                        text = "+${item.delayMs}ms  ${item.command}",
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
private fun MacroButton(
    icon: MacroIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true
) {
    val iconColor = when {
        !enabled -> AppColors.TextMuted
        primary -> AppColors.Background
        else -> AppColors.TextMain
    }

    Button(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) AppColors.Primary else AppColors.SurfaceSoft,
            contentColor = iconColor,
            disabledContainerColor = AppColors.SurfaceSoft,
            disabledContentColor = AppColors.TextMuted
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        MacroIconView(
            icon = icon,
            color = iconColor,
            contentDescription = contentDescription
        )
    }
}

private enum class MacroIcon {
    Record,
    Stop,
    Replay,
    Clear
}

@Composable
private fun MacroIconView(
    icon: MacroIcon,
    color: Color,
    contentDescription: String
) {
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        when (icon) {
            MacroIcon.Record -> {
                drawCircle(
                    color = color,
                    radius = width * 0.32f,
                    center = center
                )
            }

            MacroIcon.Stop -> {
                val side = width * 0.58f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            MacroIcon.Replay -> {
                val path = Path().apply {
                    moveTo(width * 0.34f, height * 0.24f)
                    lineTo(width * 0.34f, height * 0.76f)
                    lineTo(width * 0.76f, height * 0.50f)
                    close()
                }
                drawPath(path = path, color = color)
            }

            MacroIcon.Clear -> {
                val stroke = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.28f, height * 0.28f),
                    end = Offset(width * 0.72f, height * 0.72f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(width * 0.72f, height * 0.28f),
                    end = Offset(width * 0.28f, height * 0.72f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
