package com.audioeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun WaveformView(
    amplitudes: List<Float>,
    progress: Float,
    selectionRange: ClosedFloatingPointRange<Float>,
    onSelectionChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(amplitudes) {
                detectHorizontalDragGestures { change, dragAmount ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val deltaFraction = dragAmount / width
                        val currentStart = selectionRange.start
                        val currentEnd = selectionRange.endInclusive

                        // تشخیص اینکه کاربر دستگیره چپ رو می‌کشه یا راست
                        val touchXFraction = change.position.x / width
                        if (Math.abs(touchXFraction - currentStart) < Math.abs(touchXFraction - currentEnd)) {
                            val newStart = (currentStart + deltaFraction).coerceIn(0f, currentEnd - 0.05f)
                            onSelectionChanged(newStart..currentEnd)
                        } else {
                            val newEnd = (currentEnd + deltaFraction).coerceIn(currentStart + 0.05f, 1f)
                            onSelectionChanged(currentStart..newEnd)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        if (amplitudes.isEmpty()) {
            // رسم خط پیش‌فرض وقتی فایلی بارگذاری نشده
            drawLine(
                color = Color.Gray,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val barWidth = width / amplitudes.size.toFloat()
        
        // ۱. رسم شکل‌موج اصلی
        amplitudes.forEachIndexed { index, amp ->
            val x = index * barWidth
            val barHeight = (amp * (height / 2)).coerceAtLeast(4f)

            val color = if (index.toFloat() / amplitudes.size in selectionRange) {
                Color(0xFF6200EE) // بنفش برای بخش انتخاب‌شده
            } else {
                Color.LightGray // خاکستری برای بخش‌های بیرون از انتخاب
            }

            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = (barWidth * 0.8f).coerceAtLeast(2f)
            )
        }

        // ۲. رسم خط پیشرفت پخش (Playhead)
        val progressX = progress * width
        drawLine(
            color = Color.Red,
            start = Offset(progressX, 0f),
            end = Offset(progressX, height),
            strokeWidth = 4f
        )

        // ۳. رسم دستگیره‌های برش (Trim Handles)
        val startX = selectionRange.start * width
        val endX = selectionRange.endInclusive * width

        // دستگیره شروع
        drawRect(
            color = Color(0xFF03DAC5),
            topLeft = Offset(startX - 6f, 0f),
            size = Size(12f, height)
        )

        // دستگیره پایان
        drawRect(
            color = Color(0xFF03DAC5),
            topLeft = Offset(endX - 6f, 0f),
            size = Size(12f, height)
        )
    }
}
