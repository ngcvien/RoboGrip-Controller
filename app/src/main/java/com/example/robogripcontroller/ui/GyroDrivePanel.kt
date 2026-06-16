package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.robogripcontroller.sensor.GyroSteeringState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GyroDrivePanel(
    gyroState: GyroSteeringState,
    forwardValue: Int,
    onForwardChange: (Int) -> Unit,
    onCalibrate: () -> Unit,
    onSensitivityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, RoundedCornerShape(24.dp))
            .background(AppColors.Surface, RoundedCornerShape(24.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Gyro Drive",
                    color = AppColors.TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (gyroState.isAvailable) {
                        "Tilt phone left/right to steer"
                    } else {
                        "No motion sensor available"
                    },
                    color = AppColors.TextMuted,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = onCalibrate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    contentColor = AppColors.Background
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "CALIBRATE",
                    fontWeight = FontWeight.Black
                )
            }
        }

        GyroIndicator(
            turn = gyroState.turn,
            angle = gyroState.filteredAngle
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HoldButton(
                text = "TỚI",
                modifier = Modifier.weight(1f),
                height = 96.dp,
                onHoldChanged = { pressed ->
                    onForwardChange(if (pressed) 100 else 0)
                }
            )

            HoldButton(
                text = "LÙI",
                modifier = Modifier.weight(1f),
                height = 96.dp,
                onHoldChanged = { pressed ->
                    onForwardChange(if (pressed) -100 else 0)
                }
            )
        }

        Text(
            text = "Sensitivity: ${String.format("%.1f", gyroState.sensitivity)}x",
            color = AppColors.TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Slider(
            value = gyroState.sensitivity,
            onValueChange = onSensitivityChange,
            valueRange = 0.4f..2.0f
        )


        Text(
            text = "Angle: ${gyroState.filteredAngle.roundToInt()}°  |  Turn: ${gyroState.turn}",
            color = AppColors.TextMuted,
            fontSize = 12.sp
        )

        if (abs(gyroState.turn) < 5) {
            Text(
                text = "CENTER",
                color = AppColors.TextDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (gyroState.turn < 0) {
            Text(
                text = "STEERING LEFT",
                color = AppColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "STEERING RIGHT",
                color = AppColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GyroIndicator(
    turn: Int,
    angle: Float
) {
    val normalized = (turn / 127f).coerceIn(-1f, 1f)
    val leftWeight = if (normalized < 0f) abs(normalized) else 0f
    val rightWeight = if (normalized > 0f) normalized else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, AppColors.Border, RoundedCornerShape(18.dp))
            .background(AppColors.Background, RoundedCornerShape(18.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f + leftWeight)
                .height(26.dp)
                .background(
                    if (turn < 0) AppColors.Primary else AppColors.SurfaceSoft,
                    RoundedCornerShape(14.dp)
                )
        )

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(0.28f)
                .height(26.dp)
                .background(AppColors.TextDim, RoundedCornerShape(14.dp))
        )

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f + rightWeight)
                .height(26.dp)
                .background(
                    if (turn > 0) AppColors.Primary else AppColors.SurfaceSoft,
                    RoundedCornerShape(14.dp)
                )
        )
    }
}
