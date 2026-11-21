package com.finora.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finora.app.core.AppController
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.appBackdrop
import com.finora.app.ui.home.HomeScreen
import com.finora.app.ui.theme.Finora

/**
 * Signed-in shell: app background, the active page and the floating bottom navigation.
 * Home is real since stage 2; the other pages are filled in by the next stages.
 */
@Composable
fun MainShell(controller: AppController) {
    val c = Finora.colors
    var pageName by rememberSaveable { mutableStateOf(NavPage.HOME.name) }
    val page = NavPage.valueOf(pageName)

    Box(Modifier.fillMaxSize().appBackdrop(c.isLight)) {
        when (page) {
            NavPage.HOME, NavPage.ADD -> HomeScreen(
                controller = controller,
                onOpenFilter = {}, onEditTransaction = {}, onDeleteTransaction = {},
                onAddAccount = {},
                onOpenProfile = { pageName = NavPage.AI.name },
                onFullReport = { pageName = NavPage.ANALYTICS.name }
            )
            NavPage.ANALYTICS -> ComingSoon("تحلیل", FaGlyph.CHART_PIE)
            NavPage.BUDGET -> ComingSoon("بودجه", FaGlyph.WALLET)
            NavPage.AI -> ComingSoon("هوش مصنوعی", FaGlyph.BRAIN)
        }

        BottomNav(
            current = page,
            onSelect = { selected -> if (selected != NavPage.ADD) pageName = selected.name },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, bottom = 14.dp)
        )
    }
}

@Composable
private fun ComingSoon(title: String, glyph: String) {
    val c = Finora.colors
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 90.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FaIcon(glyph, 40f, color = c.accent2)
        Spacer(Modifier.height(14.dp))
        FText(title, 20.8f, weight = FontWeight.ExtraBold, align = TextAlign.Center)
        FText("در مرحله‌های بعد ساخته می‌شود", 14.08f, Modifier.padding(top = 6.dp), color = c.muted, align = TextAlign.Center)
    }
}
