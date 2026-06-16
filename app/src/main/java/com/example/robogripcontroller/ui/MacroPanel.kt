package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, RoundedCornerShape(22.dp))
            .background(AppColors.Background, RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                        isRecording -> "Recording..."
                        isReplaying -> "Replaying..."
                        recordedCommands.isEmpty() -> "No macro recorded"
                        else -> "${recordedCommands.size} commands saved"
                    },
                    color = if (isRecording || isReplaying) {
                        AppColors.Primary
                    } else {
                        AppColors.TextMuted
                    },
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isRecording) {
                        onStopRecord()
                    } else {
                        onStartRecord()
                    }
                },
                enabled = !isReplaying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) {
                        AppColors.Danger
                    } else {
                        AppColors.Primary
                    },
                    contentColor = if (isRecording) {
                        AppColors.TextMain
                    } else {
                        AppColors.Background
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isRecording) "STOP REC" else "RECORD",
                    fontWeight = FontWeight.Black
                )
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = onReplay,
                enabled = !isRecording && !isReplaying && recordedCommands.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.SurfaceSoft,
                    contentColor = AppColors.TextMain,
                    disabledContainerColor = AppColors.SurfaceStrong,
                    disabledContentColor = AppColors.TextDim
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "REPLAY",
                    fontWeight = FontWeight.Black
                )
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = onClear,
                enabled = !isRecording && !isReplaying && recordedCommands.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.SurfaceSoft,
                    contentColor = AppColors.TextMain,
                    disabledContainerColor = AppColors.SurfaceStrong,
                    disabledContentColor = AppColors.TextDim
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "CLEAR",
                    fontWeight = FontWeight.Black
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 42.dp, max = 86.dp)
                .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
                .background(AppColors.SurfaceStrong, RoundedCornerShape(16.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (recordedCommands.isEmpty()) {
                Text(
                    text = "Bấm RECORD rồi điều khiển xe để lưu thao tác.",
                    color = AppColors.TextDim,
                    fontSize = 12.sp
                )
            } else {
                recordedCommands.takeLast(3).forEach { item ->
                    Text(
                        text = "+${item.delayMs}ms  ${item.command}",
                        color = AppColors.Primary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}