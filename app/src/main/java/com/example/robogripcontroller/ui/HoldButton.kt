package com.example.robogripcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope

@Composable
fun HoldButton(
    text: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFFFC857),
    onHoldChanged: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .height(132.dp)
            .width(210.dp)
            .border(1.dp, primaryColor.copy(alpha = 0.55f), shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f),
                        Color(0xFF13161D)
                    )
                ),
                shape = shape
            )
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
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
    }
}
