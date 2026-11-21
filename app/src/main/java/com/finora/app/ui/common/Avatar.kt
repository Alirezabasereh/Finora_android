package com.finora.app.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finora.app.R
import com.finora.app.data.AvatarPresets
import com.finora.app.data.Profile
import com.finora.app.ui.theme.Finora
import com.finora.app.ui.theme.rgba

private val presetRes = intArrayOf(R.drawable.avatar_preset_0, R.drawable.avatar_preset_1, R.drawable.avatar_preset_2, R.drawable.avatar_preset_3)

/** img.home-avatar-preview — preset (SVG data URL) or uploaded picture, radius 18, 1px border. */
@Composable
fun ProfileAvatar(profile: Profile, size: Dp = 54.dp, radius: Dp = 18.dp, modifier: Modifier = Modifier) {
    val c = Finora.colors
    val shape = RoundedCornerShape(radius)
    val avatar = profile.avatar
    val presetIndex = AvatarPresets.dataUrls.indexOf(avatar)
    val bitmap = remember(avatar) {
        if (avatar.startsWith("data:image/") && !avatar.startsWith("data:image/svg")) {
            try {
                val b64 = avatar.substringAfter("base64,", "")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) { null }
        } else null
    }
    val base = modifier
        .size(size)
        .clip(shape)
        .background(if (c.isLight) rgba(15, 23, 42, .06f) else rgba(255, 255, 255, .08f))
        .border(1.dp, if (c.isLight) rgba(15, 23, 42, .10f) else rgba(255, 255, 255, .10f), shape)
    if (bitmap != null) {
        Image(BitmapPainter(bitmap), null, base, contentScale = ContentScale.Crop)
    } else {
        val idx = when {
            presetIndex >= 0 -> presetIndex
            profile.avatarIndex in 0..3 -> profile.avatarIndex
            else -> 0
        }
        Image(painterResource(presetRes[idx]), null, base, contentScale = ContentScale.Crop)
    }
}
