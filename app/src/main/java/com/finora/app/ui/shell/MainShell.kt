package com.finora.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finora.app.core.AppController
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.GlassCard
import com.finora.app.ui.common.appBackdrop
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba
import com.finora.app.util.Fa

/**
 * Signed-in shell: app background, page area and the floating bottom navigation.
 * Stage 1 only has a temporary "home" page (account, sync status, theme, logout);
 * the real pages replace the placeholders in the next stages.
 */
@Composable
fun MainShell(controller: AppController) {
    val c = Finora.colors
    var pageName by rememberSaveable { mutableStateOf(NavPage.HOME.name) }
    val page = NavPage.valueOf(pageName)

    Box(Modifier.fillMaxSize().appBackdrop(c.isLight)) {
        when (page) {
            NavPage.HOME -> Stage1Home(controller)
            NavPage.ANALYTICS -> ComingSoon("تحلیل", FaGlyph.CHART_PIE)
            NavPage.BUDGET -> ComingSoon("بودجه", FaGlyph.WALLET)
            NavPage.AI -> ComingSoon("هوش مصنوعی", FaGlyph.BRAIN)
            NavPage.ADD -> Stage1Home(controller)
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

@Composable
private fun Stage1Home(controller: AppController) {
    val c = Finora.colors
    val user by controller.user.collectAsState()
    val data by controller.data.collectAsState()
    val themeMode by controller.themeMode.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 110.dp)
    ) {
        // topbar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                FText("خوش اومدی 👋", 15.2f, color = c.muted, modifier = Modifier.padding(bottom = 4.dp))
                FText(data.profile.name, 27.2f, weight = FontWeight.ExtraBold, lineHeight = 27.2f * 1.2f)
            }
            val light = c.isLight
            IconButton54(if (light) FaGlyph.SUN else FaGlyph.MOON) {
                controller.setThemeMode(if (light) "dark" else "light")
            }
        }
        Spacer(Modifier.height(18.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FText("اتصال به حساب برقرار است", 16f, weight = FontWeight.ExtraBold)

                // .auth-user-info
                val infoShape = RoundedCornerShape(14.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(infoShape)
                        .background(if (c.isLight) c.surface else rgba(255, 255, 255, .05f))
                        .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .08f), infoShape)
                        .padding(horizontal = 17.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FaIcon(FaGlyph.USER, 16f, color = c.accent2)
                    FText(user?.email ?: "", 13.6f, color = c.text.copy(alpha = .75f), modifier = Modifier.weight(1f))
                }

                StatRow("تراکنش‌ها", Fa.digits(data.transactions.size))
                StatRow("حساب‌ها", Fa.digits(data.accounts.size))
                StatRow("اهداف", Fa.digits(data.goals.size))
                StatRow("دسته‌بندی‌ها", Fa.digits(data.categories.size))
                StatRow("موجودی اولیه", Fa.money(data.initialBalance))
                StatRow("حالت نمایش", when (themeMode) { "light" -> "روشن"; "dark" -> "تیره"; else -> "مطابق سیستم" })
            }
        }

        Spacer(Modifier.height(18.dp))
        FText(
            "این صفحه موقتی است و فقط برای بررسی ورود، همگام‌سازی و تم ساخته شده؛ صفحه‌ی خانه‌ی اصلی در مرحله‌ی ۲ می‌آید.",
            13.12f, Modifier.fillMaxWidth().padding(horizontal = 4.dp), color = c.muted, lineHeight = 13.12f * 1.8f
        )
        Spacer(Modifier.height(18.dp))

        // .logout-btn
        val bad = if (c.isLight) c.bad else Color_FF7A95
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(rgba(255, 93, 125, .12f))
                .border(1.dp, rgba(255, 93, 125, .22f), shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { controller.logout() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaIcon(FaGlyph.RIGHT_FROM_BRACKET, 15.2f, color = bad)
            FText("خروج از حساب", 15.2f, weight = FontWeight.SemiBold, color = bad)
        }
    }
}

private val Color_FF7A95 = androidx.compose.ui.graphics.Color(0xFFFF7A95)

@Composable
private fun StatRow(label: String, value: String) {
    val c = Finora.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FText(label, 14.08f, color = c.muted, modifier = Modifier.weight(1f))
        FText(value, 14.08f, weight = FontWeight.Bold)
    }
}

/** .icon-btn — 54×54, radius 18, card background. */
@Composable
private fun IconButton54(glyph: String, onClick: () -> Unit) {
    val c = Finora.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .size(54.dp)
            .clip(shape)
            .background(if (c.isLight) androidx.compose.ui.graphics.Color.White else c.card)
            .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else c.border, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FaIcon(glyph, 17.6f, color = c.text)
    }
}
