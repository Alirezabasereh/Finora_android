package com.finora.app.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.finora.app.ui.theme.rgba

/** Background of the splash + sign-in overlay (#appSplash / #authOverlay), dark and light variants. */
fun Modifier.overlayBackdrop(light: Boolean): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    if (light) {
        drawRect(cssLinearGradient(180f, w, h, 0f to Color(0xFFEEF1FB), 1f to Color(0xFFF6F8FD)))
        drawRect(cssCornerGlow(w, h, atRight = false, atTop = false, color = rgba(10, 136, 173, .12f), stop = .28f))
        drawRect(cssCornerGlow(w, h, atRight = true, atTop = true, color = rgba(90, 73, 242, .16f), stop = .34f))
    } else {
        drawRect(cssLinearGradient(180f, w, h, 0f to Color(0xFF070B17), .4f to Color(0xFF0B1022), 1f to Color(0xFF04060C)))
        drawRect(cssCornerGlow(w, h, atRight = false, atTop = false, color = rgba(0, 210, 255, .14f), stop = .24f))
        drawRect(cssCornerGlow(w, h, atRight = true, atTop = true, color = rgba(106, 92, 255, .22f), stop = .32f))
    }
}

/** Background of the signed-in app (body / body.light-mode). */
fun Modifier.appBackdrop(light: Boolean): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    if (light) {
        drawRect(cssLinearGradient(180f, w, h, 0f to Color(0xFFE8ECF7), 1f to Color(0xFFF3F5FB)))
        drawRect(cssCornerGlow(w, h, atRight = false, atTop = false, color = rgba(10, 136, 173, .08f), stop = .28f))
        drawRect(cssCornerGlow(w, h, atRight = true, atTop = true, color = rgba(90, 73, 242, .10f), stop = .30f))
    } else {
        drawRect(Color(0xFF101027))
    }
}
