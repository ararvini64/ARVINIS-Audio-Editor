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
import kotlin.random.Random

@Composable
fun WaveformView(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val middle = height / 2
        val count = 40
        val space = width / count

        for (i in 0 until count) {
            val lineHeight = Random.nextFloat() * (height * 0.7f)
            val x = i * space + space / 2

            drawLine(
                color = color,
                start = Offset(x, middle - lineHeight / 2),
                end = Offset(x, middle + lineHeight / 2),
                strokeWidth = 8f
            )
        }
    }
}
