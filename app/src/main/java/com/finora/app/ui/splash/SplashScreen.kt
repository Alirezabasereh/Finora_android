package com.finora.app.ui.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finora.app.R
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.common.overlayBackdrop
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba
import androidx.compose.animation.core.animateFloat

private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)        // CSS ease-out
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)   // CSS ease-in-out

/** #appSplash — logo with two ripple rings, breathing, spinner and the "Finora" title. */
@Composable
fun SplashScreen() {
    val c = Finora.colors
    val inf = rememberInfiniteTransition(label = "splash")
    val ripple by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "ripple"
    )
    val breathe by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "breathe"
    )
    val spin by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )

    val breatheScale = 1f + 0.05f * EaseInOut.transform(1f - kotlin.math.abs(2f * breathe - 1f))
    val ring2Phase = (ripple + 0.75f) % 1f          // second ring starts 0.6s (= 25%) later

    Column(
        modifier = Modifier.fillMaxSize().overlayBackdrop(c.isLight).systemBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            for (phase in listOf(ripple, ring2Phase)) {
                val e = EaseOut.transform(phase)
                val s = 0.7f + 0.65f * e
                Box(
                    Modifier
                        .size(104.dp)
                        .graphicsLayer { scaleX = s; scaleY = s; alpha = 0.5f * (1f - e) }
                        .border(1.5.dp, c.accent2, RoundedCornerShape(28.dp))
                )
            }
            Image(
                painter = painterResource(R.drawable.finora_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { scaleX = breatheScale; scaleY = breatheScale }
                    .boxShadow(rgba(106, 92, 255, .35f), offsetY = 16.dp, blur = 40.dp, radius = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
        }

        val track = if (c.isLight) rgba(15, 23, 42, .14f) else rgba(255, 255, 255, .15f)
        Canvas(Modifier.size(30.dp)) {
            val stroke = 3.dp.toPx()
            val d = size.width - stroke
            rotate(spin) {
                drawArc(
                    color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(d, d), style = Stroke(stroke)
                )
                // CSS `border-top-color` on a circle = the top quarter (-135° … -45°)
                drawArc(
                    color = c.accent2, startAngle = -135f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(d, d), style = Stroke(stroke)
                )
            }
        }

        FText("Finora", 16.8f, weight = FontWeight.ExtraBold, color = c.text, letterSpacing = 0.336f)
    }
}
