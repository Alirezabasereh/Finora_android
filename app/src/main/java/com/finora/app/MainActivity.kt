package com.finora.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.finora.app.net.SupabaseApi
import com.finora.app.ui.FinoraRoot

class MainActivity : ComponentActivity() {

    private val controller get() = (application as FinoraApp).controller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        controller.start()
        handleIntent(intent)
        setContent { FinoraRoot(controller) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        controller.onForeground()
    }

    override fun onStop() {
        super.onStop()
        controller.onBackground()
    }

    /** finora://auth-callback#access_token=...&type=recovery  (password-reset e-mail link) */
    private fun handleIntent(i: Intent?) {
        val uri = i?.data ?: return
        if (uri.scheme == "finora" && uri.host == "auth-callback") {
            val raw = uri.encodedFragment ?: uri.encodedQuery
            controller.onAuthCallback(SupabaseApi.parseFragment(raw))
            i.data = null   // consume, so a re-created activity does not replay the link
        }
    }
}
