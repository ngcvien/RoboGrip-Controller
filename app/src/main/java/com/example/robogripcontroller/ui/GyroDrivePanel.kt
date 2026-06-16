package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    @Suppress("UNUSED_PARAMETER")
    onSensitivityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Steering Wheel Mode",
                    color = AppColors.TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (gyroState.isAvailable) {
                        "Rotate phone to steer"
                    } else {
                        "No motion sensor available"
                    },
                    color = AppColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            DashboardButton(
                text = "CALIBRATE",
                onClick = onCalibrate,
                modifier = Modifier.height(36.dp)
            )
        }

        GyroIndicator(turn = gyroState.turn)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HoldButton(
                text = "TỚI",
                modifier = Modifier.weight(1f),
                height = 64.dp,
                onHoldChanged = { pressed ->
                    onForwardChange(if (pressed) 100 else 0)
                }
            )

            HoldButton(
                text = "LÙI",
                modifier = Modifier.weight(1f),
                height = 64.dp,
                onHoldChanged = { pressed ->
                    onForwardChange(if (pressed) -100 else 0)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GyroMetric(
                label = "ANGLE",
                value = "${gyroState.filteredAngle.roundToInt()}°",
                modifier = Modifier.weight(1f)
            )
            GyroMetric(
                label = "TURN",
                value = "${gyroState.turn}",
                modifier = Modifier.weight(1f),
                highlight = abs(gyroState.turn) >= 5
            )
            GyroMetric(
                label = "DRIVE",
                value = "$forwardValue",
                modifier = Modifier.weight(1f)
            )
        }

        SteeringStateLabel(turn = gyroState.turn)
    }
}

@Composable
private fun GyroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = AppColors.TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = value,
            color = if (highlight) AppColors.Primary else AppColors.TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SteeringStateLabel(turn: Int) {
    Text(
        text = when {
            abs(turn) < 5 -> "CENTER"
            turn < 0 -> "STEERING LEFT"
            else -> "STEERING RIGHT"
        },
        color = if (abs(turn) < 5) AppColors.TextMuted else AppColors.Primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun GyroIndicator(turn: Int) {
    val normalized = (turn / 127f).coerceIn(-1f, 1f)
    val leftWeight = if (normalized < 0f) abs(normalized) else 0f
    val rightWeight = if (normalized > 0f) normalized else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .background(AppColors.Background, RoundedCornerShape(16.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f + leftWeight)
                .height(18.dp)
                .background(
                    if (turn < 0) AppColors.Primary else AppColors.SurfaceSoft,
                    RoundedCornerShape(12.dp)
                )
        )

        Box(
            modifier = Modifier
                .weight(0.26f)
                .height(18.dp)
                .background(AppColors.Border, RoundedCornerShape(12.dp))
        )

        Box(
            modifier = Modifier
                .weight(1f + rightWeight)
                .height(18.dp)
                .background(
                    if (turn > 0) AppColors.Primary else AppColors.SurfaceSoft,
                    RoundedCornerShape(12.dp)
                )
        )
    }
}
