package com.audioeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformView(
    amplitudes: List<Float>,
    startRange: Float, // بین 0 تا 1
    endRange: Float,   // بین 0 تا 1
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.4f)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (amplitudes.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val middle = height / 2
        val count = amplitudes.size
        val space = width / count

        val startX = startRange * width
        val endX = endRange * width

        for (i in 0 until count) {
            val amplitude = amplitudes[i]
            val lineHeight = amplitude * (height * 0.8f)
            val x = i * space + space / 2

            val isSelected = x in startX..endX
            val color = if (isSelected) activeColor else inactiveColor

            drawLine(
                color = color,
                start = Offset(x, middle - lineHeight / 2),
                end = Offset(x, middle + lineHeight / 2),
                strokeWidth = 6f
            )
        }

        // رسم خطوط و مارکرهای انتخاب محدوده
        drawLine(
            color = Color.Red,
            start = Offset(startX, 0f),
            end = Offset(startX, height),
            strokeWidth = 8f
        )
        drawLine(
            color = Color.Red,
            start = Offset(endX, 0f),
            end = Offset(endX, height),
            strokeWidth = 8f
        )
    }
}
