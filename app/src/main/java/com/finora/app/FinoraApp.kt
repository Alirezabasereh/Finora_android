package com.finora.app

import android.app.Application
import com.finora.app.core.AppController
import com.finora.app.data.PrefsStore
import com.finora.app.net.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FinoraApp : Application() {
    lateinit var controller: AppController
        private set

    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        controller = AppController(
            scope = scope,
            store = PrefsStore(this),
            api = SupabaseApi(Config.SUPABASE_URL, Config.SUPABASE_ANON_KEY)
        )
    }
}
