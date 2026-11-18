package com.finora.app.ui.common

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

private val shadowCache = object : LruCache<String, ImageBitmap>(32) {}

/**
 * CSS `box-shadow: <x> <y> <blur> <spread> <color>` (outer shadow only).
 * Rendered with a real Gaussian blur into a cached software bitmap, and clipped so nothing paints
 * inside the element itself — exactly what browsers do (matters for translucent cards).
 */
fun Modifier.boxShadow(
    color: Color,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    blur: Dp = 0.dp,
    spread: Dp = 0.dp,
    radius: Dp = 0.dp
): Modifier = this.drawBehind {
    if (color.alpha <= 0f) return@drawBehind
    val sp = spread.toPx()
    val blurPx = blur.toPx()
    val r = radius.toPx()
    val shapeW = size.width + 2 * sp
    val shapeH = size.height + 2 * sp
    if (shapeW <= 0f || shapeH <= 0f) return@drawBehind
    val shapeR = max(0f, r + sp)
    val pad = ceil(blurPx * 1.6f).toInt() + 2
    val bw = ceil(shapeW).toInt() + 2 * pad
    val bh = ceil(shapeH).toInt() + 2 * pad
    val key = "$bw|$bh|${shapeR}|${blurPx}|${color.toArgb()}"
    val bmp = shadowCache.get(key) ?: run {
        val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
        paint.color = color.toArgb()
        if (blurPx > 0f) {
            val sigma = blurPx / 2f                       // CSS blur radius -> gaussian sigma
            val skRadius = max(0.01f, (sigma - 0.5f) / 0.57735f)
            paint.maskFilter = BlurMaskFilter(skRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(pad.toFloat(), pad.toFloat(), pad + shapeW, pad + shapeH), shapeR, shapeR, paint)
        val ib = bitmap.asImageBitmap()
        shadowCache.put(key, ib)
        ib
    }
    val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r))) }
    clipPath(outline, clipOp = ClipOp.Difference) {
        drawImage(bmp, topLeft = Offset(offsetX.toPx() - sp - pad, offsetY.toPx() - sp - pad))
    }
}

/** CSS `linear-gradient(<angle>deg, stops…)` for a box of the given size (angle 180 = top→bottom, 90 = left→right). */
fun cssLinearGradient(angleDeg: Float, width: Float, height: Float, vararg stops: Pair<Float, Color>): Brush {
    val a = Math.toRadians(angleDeg.toDouble())
    val dx = sin(a).toFloat()
    val dy = (-cos(a)).toFloat()
    val len = abs(width * dx) + abs(height * dy)
    val cx = width / 2f
    val cy = height / 2f
    return Brush.linearGradient(
        *stops,
        start = Offset(cx - dx * len / 2f, cy - dy * len / 2f),
        end = Offset(cx + dx * len / 2f, cy + dy * len / 2f)
    )
}

/** `radial-gradient(circle at <corner>, color, transparent <stopPct>%)` — default size = farthest-corner. */
fun cssCornerGlow(w: Float, h: Float, atRight: Boolean, atTop: Boolean, color: Color, stop: Float): Brush {
    val center = Offset(if (atRight) w else 0f, if (atTop) 0f else h)
    val radius = hypot(w, h)
    val clear = color.copy(alpha = 0f)      // same hue -> matches the browser's premultiplied fade
    return Brush.radialGradient(
        0f to color, stop to clear, 1f to clear,
        center = center,
        radius = radius
    )
}
