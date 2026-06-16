package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.roundToInt

@Composable
fun ArmControlPanel(
    armSpeed: Int,
    onArmSpeedChange: (Int) -> Unit,
    onArmCommand: (axis1: Int, axis2: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, RoundedCornerShape(22.dp))
            .background(AppColors.Background, RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Arm Control",
                    color = AppColors.TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "2-axis gripper",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "$armSpeed",
                color = AppColors.Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        Slider(
            value = armSpeed.toFloat(),
            onValueChange = { onArmSpeedChange(it.roundToInt()) },
            valueRange = 80f..255f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArmHoldButton(
                text = "NÂNG",
                modifier = Modifier.weight(1f),
                onHoldChanged = { pressed ->
                    onArmCommand(if (pressed) armSpeed else 0, 0)
                }
            )

            ArmHoldButton(
                text = "HẠ",
                modifier = Modifier.weight(1f),
                onHoldChanged = { pressed ->
                    onArmCommand(if (pressed) -armSpeed else 0, 0)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArmHoldButton(
                text = "MỞ",
                modifier = Modifier.weight(1f),
                onHoldChanged = { pressed ->
                    onArmCommand(0, if (pressed) armSpeed else 0)
                }
            )

            ArmHoldButton(
                text = "ĐÓNG",
                modifier = Modifier.weight(1f),
                onHoldChanged = { pressed ->
                    onArmCommand(0, if (pressed) -armSpeed else 0)
                }
            )
        }
    }
}

@Composable
private fun ArmHoldButton(
    text: String,
    modifier: Modifier = Modifier,
    onHoldChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, AppColors.Primary.copy(alpha = 0.65f), shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.PrimarySoft,
                        AppColors.SurfaceStrong
                    )
                ),
                shape = shape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHoldChanged(true)
                        tryAwaitRelease()
                        onHoldChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
    }
}