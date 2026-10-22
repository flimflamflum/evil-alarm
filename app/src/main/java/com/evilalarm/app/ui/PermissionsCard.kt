package com.evilalarm.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassCard

/** Status of everything the alarm needs to ring reliably, each with a shortcut to the right settings page. */
@Composable
fun PermissionsCard(onlyMissing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val requirements = rememberRequirements()
    val shown = if (onlyMissing) requirements.filterNot { it.granted } else requirements
    if (shown.isEmpty()) return

    GlassCard(modifier.fillMaxWidth()) {
        if (onlyMissing) {
            Text("Finish setup", color = Glass.TextPrimary, fontSize = 18.sp)
            Text("Without these, Evil Alarm might not ring.", color = Glass.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        shown.forEach { requirement ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !requirement.granted) { openSettings(context, requirement.fix) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (requirement.granted) Glass.Success else Glass.Pink),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(requirement.title, color = Glass.TextPrimary, fontSize = 15.sp)
                    Text(requirement.summary, color = Glass.TextSecondary, fontSize = 12.sp)
                }
                Text(
                    if (requirement.granted) "On" else "Fix",
                    color = if (requirement.granted) Glass.TextFaint else Glass.Pink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
