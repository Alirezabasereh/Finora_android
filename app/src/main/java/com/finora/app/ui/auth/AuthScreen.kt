package com.finora.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.finora.app.R
import com.finora.app.core.AppController
import com.finora.app.core.AuthView
import com.finora.app.ui.common.FText
import com.finora.app.ui.common.boxShadow
import com.finora.app.ui.common.cssLinearGradient
import com.finora.app.ui.common.overlayBackdrop
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.Vazir
import com.finora.app.ui.theme.rgba

/** #authOverlay — sign-in / register / forgot / set-new-password, one card, dimensions taken from the web render. */
@Composable
fun AuthScreen(controller: AppController) {
    val ui by controller.authUi.collectAsState()
    val c = Finora.colors

    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPass by rememberSaveable { mutableStateOf("") }
    var regEmail by rememberSaveable { mutableStateOf("") }
    var regPass by rememberSaveable { mutableStateOf("") }
    var regConfirm by rememberSaveable { mutableStateOf("") }
    var forgotEmail by rememberSaveable { mutableStateOf("") }
    var recNew by rememberSaveable { mutableStateOf("") }
    var recConfirm by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = ui.view != AuthView.LOGIN) { controller.showAuthView(AuthView.LOGIN) }

    BoxWithConstraints(
        Modifier.fillMaxSize().overlayBackdrop(c.isLight).systemBarsPadding().imePadding()
    ) {
        val minH = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minH)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val cardShape = RoundedCornerShape(28.dp)
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .boxShadow(
                        color = if (c.isLight) rgba(15, 23, 42, .28f) else rgba(0, 0, 0, .40f),
                        offsetY = 24.dp, blur = 60.dp, spread = if (c.isLight) (-16).dp else 0.dp, radius = 28.dp
                    )
                    .background(if (c.isLight) Color.White else rgba(255, 255, 255, .07f), cardShape)
                    .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .11f), cardShape)
                    // CSS: 1px border + padding 36 32 32 32
                    .padding(start = 33.dp, top = 37.dp, end = 33.dp, bottom = 33.dp)
            ) {
                LogoBlock()

                if (ui.view == AuthView.LOGIN || ui.view == AuthView.REGISTER) {
                    AuthTabs(
                        registerActive = ui.view == AuthView.REGISTER,
                        onLogin = { controller.showAuthView(AuthView.LOGIN) },
                        onRegister = { controller.showAuthView(AuthView.REGISTER) }
                    )
                }

                when (ui.view) {
                    AuthView.LOGIN -> {
                        AuthField("ایمیل", loginEmail, { loginEmail = it }, "example@email.com",
                            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                        AuthField("رمز عبور", loginPass, { loginPass = it }, "رمز عبور", password = true,
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                            onAction = { controller.login(loginEmail, loginPass) })
                        SubmitButton("ورود به حساب", "در حال ورود...", ui.busy) { controller.login(loginEmail, loginPass) }
                        LinkButton("رمز عبور را فراموش کرده‌اید؟") {
                            forgotEmail = loginEmail.trim()
                            controller.showAuthView(AuthView.FORGOT)
                        }
                        Messages(ui.error, null)
                    }
                    AuthView.REGISTER -> {
                        AuthField("ایمیل", regEmail, { regEmail = it }, "example@email.com",
                            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                        AuthField("رمز عبور (حداقل ۶ کاراکتر)", regPass, { regPass = it }, "رمز عبور", password = true,
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                        AuthField("تکرار رمز عبور", regConfirm, { regConfirm = it }, "تکرار رمز عبور", password = true,
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                            onAction = { controller.register(regEmail, regPass, regConfirm) })
                        SubmitButton("ساخت حساب جدید", "در حال ثبت‌نام...", ui.busy) { controller.register(regEmail, regPass, regConfirm) }
                        Messages(ui.error, ui.success)
                    }
                    AuthView.FORGOT -> {
                        Hint("ایمیل حساب‌تان را وارد کنید تا لینک تعیین رمز جدید برایتان ارسال شود.")
                        AuthField("ایمیل", forgotEmail, { forgotEmail = it }, "example@email.com",
                            keyboardType = KeyboardType.Email, imeAction = ImeAction.Done,
                            onAction = { controller.sendRecovery(forgotEmail) })
                        SubmitButton("ارسال لینک بازیابی", "در حال ارسال...", ui.busy) { controller.sendRecovery(forgotEmail) }
                        LinkButton("بازگشت به ورود") { controller.showAuthView(AuthView.LOGIN) }
                        Messages(ui.error, ui.success)
                    }
                    AuthView.RECOVER -> {
                        Hint("رمز عبور جدید خود را وارد کنید.")
                        AuthField("رمز عبور جدید (حداقل ۶ کاراکتر)", recNew, { recNew = it }, "رمز عبور جدید", password = true,
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                        AuthField("تکرار رمز عبور جدید", recConfirm, { recConfirm = it }, "تکرار رمز عبور", password = true,
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                            onAction = { controller.saveRecoveredPassword(recNew, recConfirm) })
                        SubmitButton("ذخیره رمز جدید", "در حال ذخیره...", ui.busy) { controller.saveRecoveredPassword(recNew, recConfirm) }
                        Messages(ui.error, null)
                    }
                }
            }
        }
    }
}

// ───────────────────────── pieces ─────────────────────────

@Composable
private fun LogoBlock() {
    val c = Finora.colors
    Column(
        Modifier.fillMaxWidth().padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.finora_logo),
            contentDescription = "Finora",
            modifier = Modifier
                .size(72.dp)
                .boxShadow(rgba(106, 92, 255, .35f), offsetY = 8.dp, blur = 24.dp, radius = 22.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(12.dp))
        FText("Finora", 27.2f, Modifier.fillMaxWidth(), FontWeight.ExtraBold, c.text, TextAlign.Center, letterSpacing = -0.5f)
        FText(
            "مدیریت هزینه‌ها به‌صورت هوشمند", 14.08f,
            Modifier.fillMaxWidth().padding(top = 4.dp), color = c.text.copy(alpha = .6f), align = TextAlign.Center
        )
    }
}

@Composable
private fun AuthTabs(registerActive: Boolean, onLogin: () -> Unit, onRegister: () -> Unit) {
    val c = Finora.colors
    Box(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (c.isLight) c.surface else rgba(255, 255, 255, .06f))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // RTL layout: the first child (login) sits on the right, like the web page
            AuthTab("ورود", !registerActive, Modifier.weight(1f), onLogin)
            AuthTab("ثبت‌نام", registerActive, Modifier.weight(1f), onRegister)
        }
    }
}

@Composable
private fun AuthTab(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Finora.colors
    val shape = RoundedCornerShape(10.dp)
    val activeBg = if (c.isLight) rgba(90, 73, 242, .12f) else rgba(106, 92, 255, .25f)
    val activeText = if (c.isLight) Color(0xFF4A3FD6) else Color(0xFFA89FFF)
    val glow = if (c.isLight) rgba(90, 73, 242, .16f) else rgba(106, 92, 255, .20f)
    Box(
        modifier
            .height(43.dp)
            .then(if (active) Modifier.boxShadow(glow, offsetY = 2.dp, blur = 8.dp, radius = 10.dp) else Modifier)
            .clip(shape)
            .background(if (active) activeBg else Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FText(label, 14.4f, weight = FontWeight.SemiBold, color = if (active) activeText else c.text, align = TextAlign.Center)
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onAction: (() -> Unit)? = null
) {
    val c = Finora.colors
    val focusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)

    val bg = if (c.isLight) (if (focused) Color.White else c.surface) else rgba(255, 255, 255, .07f)
    val borderColor = when {
        focused && c.isLight -> c.accent1
        focused -> rgba(106, 92, 255, .50f)
        c.isLight -> c.lineStrong
        else -> rgba(255, 255, 255, .10f)
    }
    val ring = when {
        !focused -> Color.Transparent
        c.isLight -> rgba(90, 73, 242, .22f)
        else -> rgba(106, 92, 255, .15f)
    }
    val placeholderColor = if (c.isLight) rgba(15, 23, 42, .40f) else Color(0xFF757575)

    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        FText(label, 13.12f, Modifier.fillMaxWidth().padding(bottom = 6.dp), FontWeight.SemiBold, c.text.copy(alpha = .7f))
        // the inputs are LTR / left-aligned in the web page
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = c.text, fontSize = 15.2.sp, fontFamily = Vazir, fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp, textAlign = TextAlign.Left, textDirection = TextDirection.Ltr
                ),
                cursorBrush = SolidColor(c.text),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { onAction?.invoke() }
                ),
                interactionSource = interaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .drawBehind {
                        if (ring.alpha > 0f) {
                            val g = 3.dp.toPx()
                            drawRoundRect(
                                color = ring,
                                topLeft = Offset(-g, -g),
                                size = Size(size.width + 2 * g, size.height + 2 * g),
                                cornerRadius = CornerRadius(12.dp.toPx() + g)
                            )
                        }
                    }
                    .background(bg, shape)
                    .border(1.dp, borderColor, shape),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize().padding(horizontal = 15.dp), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            FText(placeholder, 15.2f, color = placeholderColor, align = TextAlign.Left, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun SubmitButton(label: String, busyLabel: String, busy: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !busy) 0.98f else 1f, tween(100), label = "press")
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (busy) 0.55f else 1f }
            .height(53.dp)
            .boxShadow(rgba(106, 92, 255, .30f), offsetY = 6.dp, blur = 20.dp, radius = 14.dp)
            .clip(shape)
            .drawBehind {
                drawRect(
                    cssLinearGradient(135f, size.width, size.height, 0f to Color(0xFF6A5CFF), 1f to Color(0xFF00D2FF))
                )
            }
            .clickable(interactionSource = interaction, indication = null, enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FText(busyLabel, 16f, weight = FontWeight.Bold, color = Color.White, align = TextAlign.Center)
                AuthSpinner()
            }
        } else {
            FText(label, 16f, weight = FontWeight.Bold, color = Color.White, align = TextAlign.Center)
        }
    }
}

/** .auth-loading — 16px ring, 2px border, white top arc, 0.7s linear spin. */
@Composable
private fun AuthSpinner() {
    val inf = rememberInfiniteTransition(label = "authSpin")
    val angle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Canvas(Modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        val d = size.width - stroke
        rotate(angle) {
            drawArc(
                color = Color.White.copy(alpha = .3f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(d, d), style = Stroke(stroke)
            )
            drawArc(
                color = Color.White, startAngle = -135f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(d, d), style = Stroke(stroke)
            )
        }
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit) {
    val c = Finora.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        FText(label, 13.76f, weight = FontWeight.SemiBold, color = c.accent2, align = TextAlign.Center)
    }
}

@Composable
private fun Hint(text: String) {
    val c = Finora.colors
    // .auth-hint: .88rem, line-height 1.8, opacity .75, margin-bottom 16
    FText(text, 14.08f, Modifier.fillMaxWidth().padding(bottom = 16.dp), color = c.text.copy(alpha = .75f), lineHeight = 14.08f * 1.8f)
}

@Composable
private fun Messages(error: String?, success: String?) {
    val c = Finora.colors
    if (error != null) {
        MessageBox(
            error,
            color = if (c.isLight) Color(0xFFC02045) else Color(0xFFFF7A95),
            bg = if (c.isLight) rgba(217, 45, 82, .09f) else rgba(255, 93, 125, .12f),
            border = if (c.isLight) rgba(217, 45, 82, .26f) else rgba(255, 93, 125, .25f)
        )
    }
    if (success != null) {
        MessageBox(
            success,
            color = if (c.isLight) Color(0xFF0A7A55) else Color(0xFF18E6A6),
            bg = if (c.isLight) rgba(14, 159, 110, .09f) else rgba(24, 230, 166, .10f),
            border = if (c.isLight) rgba(14, 159, 110, .26f) else rgba(24, 230, 166, .22f)
        )
    }
}

@Composable
private fun MessageBox(text: String, color: Color, bg: Color, border: Color) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 15.dp, vertical = 11.dp)
    ) {
        FText(text, 13.6f, Modifier.fillMaxWidth(), color = color)
    }
}
