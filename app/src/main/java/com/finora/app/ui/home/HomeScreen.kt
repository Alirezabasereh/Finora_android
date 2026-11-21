package com.finora.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finora.app.core.AppController
import com.finora.app.data.Transaction
import com.finora.app.finance.Finance
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.FaGlyph
import com.finora.app.ui.common.FaIcon
import com.finora.app.ui.common.ProfileAvatar
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba

/** The home page: topbar, hero card, expense breakdown, insight chips and the transactions list. */
@Composable
fun HomeScreen(
    controller: AppController,
    onOpenFilter: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onAddAccount: () -> Unit,
    onOpenProfile: () -> Unit,
    onFullReport: () -> Unit
) {
    val c = Finora.colors
    val data by controller.data.collectAsState()
    val filters by controller.txFilters.collectAsState()
    val heroAcct by controller.heroAccountId.collectAsState()
    val exclude by controller.excludeLoans.collectAsState()
    val viewing by controller.viewingMonthKey.collectAsState()
    val tick by controller.tick.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = maxWidth.value
        val fin = remember(data, exclude, viewing, tick) {
            Finance(data, controller.zone, System.currentTimeMillis(), exclude, viewing)
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 106.dp)
        ) {
            // ── topbar ──
            Row(
                Modifier.fillMaxWidth().height(64.6.dp).padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        data.profile,
                        modifier = Modifier.boxShadow(
                            if (c.isLight) rgba(15, 23, 42, .16f) else rgba(0, 0, 0, .28f),
                            offsetY = if (c.isLight) 12.dp else 18.dp, blur = if (c.isLight) 30.dp else 50.dp,
                            spread = if (c.isLight) (-10).dp else 0.dp, radius = 18.dp
                        )
                    )
                    Column {
                        FText("خوش اومدی 👋", 15.2f, Modifier.padding(bottom = 4.dp), color = c.muted)
                        FText(data.profile.name, 27.2f, lineHeight = 32.64f, maxLines = 1)
                    }
                }
                val btnShape = RoundedCornerShape(18.dp)
                Box(
                    Modifier
                        .size(54.dp)
                        .then(
                            if (c.isLight) Modifier
                                .boxShadow(rgba(15, 23, 42, .06f), offsetY = 1.dp, blur = 2.dp, radius = 18.dp)
                                .boxShadow(rgba(15, 23, 42, .18f), offsetY = 8.dp, blur = 22.dp, spread = (-8).dp, radius = 18.dp)
                            else Modifier.boxShadow(rgba(0, 0, 0, .28f), offsetY = 18.dp, blur = 50.dp, radius = 18.dp)
                        )
                        .clip(btnShape)
                        .background(if (c.isLight) androidx.compose.ui.graphics.Color.White else rgba(255, 255, 255, .07f))
                        .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .10f), btnShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) { FaIcon(FaGlyph.USER, 16f, color = c.text) }
            }
            Column(Modifier.padding(top = 18.dp)) {
                HeroCard(
                    fin = fin, heroAccountId = heroAcct, screenWidth = screenWidth,
                    onSelectAccount = { id -> controller.setHeroAccount(if (id == heroAcct) null else id) },
                    onAddAccount = onAddAccount, onOpenToday = {}, onOpenHealth = {}
                )
            }
            Column(Modifier.padding(top = 16.dp)) {
                BreakdownCard(fin, exclude, { controller.setExcludeLoans(it) }, screenWidth, onFullReport)
            }
            Column(Modifier.padding(top = 16.dp)) { InsightsRow(fin) }
            Column(Modifier.padding(top = 16.dp)) {
                TransactionsCard(fin, filters, onOpenFilter, onEditTransaction, onDeleteTransaction)
            }
        }
    }
}
