package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ArmControlPanel(
    armSpeed: Int,
    isGripHolding: Boolean,
    onArmSpeedChange: (Int) -> Unit,
    onLiftChange: (Int) -> Unit,
    onGripHold: () -> Unit,
    onGripOpenChanged: (Boolean) -> Unit,
    onStopArm: () -> Unit
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Arm Control",
                    color = AppColors.TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (isGripHolding) {
                        "Gripper holding object"
                    } else {
                        "Lift + latch gripper"
                    },
                    color = if (isGripHolding) AppColors.Primary else AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onStopArm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Danger,
                    contentColor = AppColors.TextMain
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "STOP ARM",
                    fontWeight = FontWeight.Black
                )
            }
        }

        Text(
            text = "Arm speed: $armSpeed",
            color = AppColors.TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Slider(
            value = armSpeed.toFloat(),
            onValueChange = { onArmSpeedChange(it.roundToInt()) },
            valueRange = 80f..255f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HoldButton(
                text = "NÂNG",
                modifier = Modifier.weight(1f),
                height = 56.dp,
                onHoldChanged = { pressed ->
                    onLiftChange(if (pressed) armSpeed else 0)
                }
            )

            HoldButton(
                text = "HẠ",
                modifier = Modifier.weight(1f),
                height = 56.dp,
                onHoldChanged = { pressed ->
                    onLiftChange(if (pressed) -armSpeed else 0)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onGripHold,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGripHolding) {
                        AppColors.Primary
                    } else {
                        AppColors.SurfaceSoft
                    },
                    contentColor = if (isGripHolding) {
                        AppColors.Background
                    } else {
                        AppColors.TextMain
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isGripHolding) "ĐANG GẮP" else "GẮP / GIỮ",
                    fontWeight = FontWeight.Black
                )
            }

            HoldButton(
                text = "MỞ",
                modifier = Modifier.weight(1f),
                height = 56.dp,
                onHoldChanged = { pressed ->
                    onGripOpenChanged(pressed)
                }
            )
        }
    }
}
