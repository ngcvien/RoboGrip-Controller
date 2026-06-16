package com.example.robogripcontroller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun JoystickPad(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    onMove: (forward: Int, turn: Int) -> Unit
) {
    val radiusPx = with(LocalDensity.current) { (size / 2).toPx() }
    val knobRadiusPx = radiusPx * 0.28f
    val maxKnobDistance = radiusPx - knobRadiusPx

    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(knobOffset) {
        val forward = (-knobOffset.y / maxKnobDistance * 127f).roundToInt().coerceIn(-127, 127)
        val turn = (knobOffset.x / maxKnobDistance * 127f).roundToInt().coerceIn(-127, 127)
        onMove(forward, turn)
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { knobOffset = Offset.Zero },
                    onDragCancel = { knobOffset = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = knobOffset + dragAmount
                        knobOffset = next.limitLength(maxKnobDistance)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            drawCircle(
                color = AppColors.SurfaceStrong,
                radius = radiusPx,
                center = center
            )
            drawCircle(
                color = AppColors.Border,
                radius = radiusPx * 0.82f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = AppColors.TextDim,
                radius = radiusPx * 0.34f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = AppColors.Primary,
                radius = knobRadiusPx,
                center = center + knobOffset
            )
            drawCircle(
                color = AppColors.PrimaryHighlight,
                radius = knobRadiusPx * 0.35f,
                center = center + knobOffset
            )
        }
    }
}

private fun Offset.limitLength(maxLength: Float): Offset {
    val length = sqrt(x * x + y * y)
    if (length <= maxLength || length == 0f) return this
    val scale = maxLength / length
    return Offset(x * scale, y * scale)
}
