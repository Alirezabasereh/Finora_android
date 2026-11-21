package com.finora.app.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/** Parses "#rrggbb" (falls back to grey). */
fun parseHex(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF94A3B8) }

/** CSS `radial-gradient(<rx> <ry> at <cx> <cy>, color, transparent <stop>)` with an explicit ellipse size. */
fun DrawScope.ellipseGlow(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, stop: Float) {
    val clear = color.copy(alpha = 0f)
    withTransform({
        translate(cx, cy)
        scale(rx, ry, pivot = Offset.Zero)
    }) {
        drawRect(
            brush = Brush.radialGradient(0f to color, stop to clear, 1f to clear, center = Offset.Zero, radius = 1f),
            topLeft = Offset(-2f, -2f),
            size = Size(4f, 4f)
        )
    }
}

/** `radial-gradient(circle, color, transparent <stop>)` inside the box (cx,cy,radius = farthest corner). */
fun DrawScope.circleGlow(cx: Float, cy: Float, boxHalf: Float, color: Color, stop: Float) {
    val r = boxHalf * 1.41421356f
    val clear = color.copy(alpha = 0f)
    drawRect(
        brush = Brush.radialGradient(0f to color, stop to clear, 1f to clear, center = Offset(cx, cy), radius = r),
        topLeft = Offset(cx - boxHalf, cy - boxHalf),
        size = Size(boxHalf * 2, boxHalf * 2)
    )
}
