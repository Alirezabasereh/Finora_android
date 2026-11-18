package com.finora.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.finora.app.core.AppController
import com.finora.app.core.Screen
import com.finora.app.ui.auth.AuthScreen
import com.finora.app.ui.shell.MainShell
import com.finora.app.ui.shell.SyncBar
import com.finora.app.ui.splash.SplashScreen
import com.finora.app.ui.theme.FinoraTheme

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FinoraRoot(controller: AppController) {
    val screen by controller.screen.collectAsState()
    val themeMode by controller.themeMode.collectAsState()
    val bar by controller.sync.bar.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val light = themeMode == "light" || (themeMode == "system" && !systemDark)

    // status / navigation bar icon colours follow the theme
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window
        if (window != null) {
            val ctrl = WindowCompat.getInsetsController(window, view)
            ctrl.isAppearanceLightStatusBars = light
            ctrl.isAppearanceLightNavigationBars = light
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        // the web page ignores the OS font-size setting for layout; pin it so every box keeps its measured size
        LocalDensity provides Density(density.density, fontScale = 1f)
    ) {
        FinoraTheme(light) {
            Box(Modifier.fillMaxSize()) {
                Crossfade(targetState = screen, animationSpec = tween(350), label = "screen") { s ->
                    when (s) {
                        Screen.SPLASH -> SplashScreen()
                        Screen.AUTH -> AuthScreen(controller)
                        Screen.MAIN -> MainShell(controller)
                    }
                }
                SyncBar(bar, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
