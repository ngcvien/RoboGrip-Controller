package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope

@Composable
fun HoldButton(
    text: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = AppColors.Primary,
    height: Dp = 132.dp,
    onHoldChanged: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, primaryColor.copy(alpha = 0.55f), shape)
            .background(AppColors.SurfaceSoft, shape)
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        val event = awaitPointerEventScope { awaitPointerEvent() }
                        val pressed = event.changes.any { it.pressed }
                        onHoldChanged(pressed)
                    }
                }
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.TextMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
    }
}
