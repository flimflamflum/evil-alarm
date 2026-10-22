package com.evilalarm.app.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.evilalarm.app.R
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassButton
import com.evilalarm.app.ui.theme.GlassCard
import kotlinx.coroutines.delay

/** First-run walkthrough: one page per permission, each with the shortcut to switch it on. */
@Composable
fun SetupScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val requirements = rememberRequirements()
    // 0 = welcome, 1..n = one page per requirement, n + 1 = done.
    var page by rememberSaveable { mutableIntStateOf(0) }
    val donePage = requirements.size + 1
    // Steps that are already allowed get skipped in both directions.
    fun nextPage(from: Int): Int = (from + 1..requirements.size).firstOrNull { !requirements[it - 1].granted } ?: donePage
    fun previousPage(from: Int): Int = (from - 1 downTo 1).firstOrNull { !requirements[it - 1].granted } ?: 0
    BackHandler(enabled = page > 0) { page = previousPage(page) }

    // Android 13+ shows its own notification prompt the first time; after that only Settings can change it.
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationPrompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val fix: (Requirement) -> Unit = { requirement ->
        if (requirement.key == RequirementKey.NOTIFICATIONS && canAskForNotifications && !askedForNotifications) {
            askedForNotifications = true
            notificationPrompt.launch(NOTIFICATION_PERMISSION)
        } else {
            openSettings(context, requirement.fix)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page in 1..requirements.size) {
                StepDots(requirements, current = page - 1)
            }
            Spacer(Modifier.weight(1f))
            if (page < donePage) QuietButton("Skip setup", onFinish)
        }

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { direction * it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -direction * it / 4 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "setup page",
        ) { current ->
            when (current) {
                0 -> WelcomePage(onStart = { page = nextPage(0) })
                donePage -> DonePage(requirements, onFinish)
                else -> RequirementPage(
                    requirement = requirements[current - 1],
                    step = current,
                    total = requirements.size,
                    onFix = fix,
                    onNext = { page = nextPage(current) },
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    PageLayout(
        buttons = { GlassButton("Let's go", onClick = onStart, accent = true, modifier = Modifier.fillMaxWidth()) },
    ) {
        Box(
            Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(Glass.Fill)
                .border(1.dp, Glass.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(132.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("Evil Alarm", color = Glass.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Thin)
        Spacer(Modifier.height(12.dp))
        Text(
            "An alarm you can't sleep through, and a lock on your apps the moment it rings.",
            color = Glass.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "First, a few things your phone needs to allow. It takes about a minute.",
            color = Glass.TextFaint,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RequirementPage(
    requirement: Requirement,
    step: Int,
    total: Int,
    onFix: (Requirement) -> Unit,
    onNext: () -> Unit,
) {
    // Move on by itself once this gets switched on, but not when revisiting a step that was already done.
    val wasMissing = remember { !requirement.granted }
    LaunchedEffect(requirement.granted) {
        if (wasMissing && requirement.granted) {
            delay(700)
            onNext()
        }
    }

    PageLayout(
        buttons = {
            if (requirement.granted) {
                GlassButton("Next", onClick = onNext, accent = true, modifier = Modifier.fillMaxWidth())
            } else {
                GlassButton(requirement.fixLabel, onClick = { onFix(requirement) }, accent = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                QuietButton("Skip this step", onNext)
            }
        },
    ) {
        StepIcon(if (requirement.granted) Icons.Rounded.Check else requirement.key.icon, requirement.granted)
        Spacer(Modifier.height(24.dp))
        Text(
            "STEP $step OF $total",
            color = Glass.TextFaint,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(requirement.title, color = Glass.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(requirement.detail, color = Glass.TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        StatusPill(requirement.granted)
        if (!requirement.granted) {
            requirementTip(requirement.key)?.let { tip ->
                Spacer(Modifier.height(20.dp))
                TipCard(tip)
            }
        }
    }
}

@Composable
private fun DonePage(requirements: List<Requirement>, onFinish: () -> Unit) {
    val missing = requirements.filterNot { it.granted }
    PageLayout(
        buttons = {
            GlassButton(
                if (missing.isEmpty()) "Set my first alarm" else "Done for now",
                onClick = onFinish,
                accent = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        StepIcon(Icons.Rounded.Check, granted = missing.isEmpty())
        Spacer(Modifier.height(24.dp))
        Text(
            if (missing.isEmpty()) "You're all set." else "Almost there.",
            color = Glass.TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (missing.isEmpty()) {
                "Evil Alarm can ring through anything and lock your apps when it does."
            } else {
                "Still off: ${missing.joinToString { it.title }}. You can switch these on any time from the Finish setup card or Settings."
            },
            color = Glass.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Centred, scrollable page content with the buttons pinned to the bottom. */
@Composable
private fun PageLayout(buttons: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            buttons()
        }
    }
}

@Composable
private fun StepIcon(icon: ImageVector, granted: Boolean) {
    Box(
        Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(if (granted) SolidColor(Glass.Success.copy(alpha = 0.25f)) else Glass.AccentBrush)
            .border(1.dp, Glass.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Glass.TextPrimary, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun StatusPill(granted: Boolean) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(Glass.Fill)
            .border(1.dp, Glass.Border, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (granted) Glass.Success else Glass.Pink),
        )
        Spacer(Modifier.width(8.dp))
        Text(if (granted) "Allowed" else "Not allowed yet", color = Glass.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun StepDots(requirements: List<Requirement>, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        requirements.forEachIndexed { index, requirement ->
            Box(
                Modifier
                    .size(width = if (index == current) 24.dp else 12.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            index == current -> Glass.AccentBrush
                            requirement.granted -> SolidColor(Glass.Success.copy(alpha = 0.7f))
                            else -> SolidColor(Glass.FillStrong)
                        },
                    ),
            )
        }
    }
}

private data class Tip(val text: String, val actionLabel: String? = null, val action: Intent? = null)

@Composable
private fun TipCard(tip: Tip) {
    val context = LocalContext.current
    GlassCard(Modifier.fillMaxWidth()) {
        Text(tip.text, color = Glass.TextSecondary, fontSize = 13.sp)
        if (tip.actionLabel != null && tip.action != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                tip.actionLabel,
                color = Glass.Pink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { openSettings(context, tip.action) }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun requirementTip(key: RequirementKey): Tip? {
    val context = LocalContext.current
    val appInfo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    return when (key) {
        RequirementKey.APP_BLOCKER -> Tip(
            buildString {
                append("1. Tap Open Accessibility settings.\n")
                append("2. Find \"Evil Alarm app blocker\". It may be under Installed apps or Downloaded apps.\n")
                append("3. Switch it on and confirm.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    append("\n\nSwitch greyed out? Open Evil Alarm's App info, tap ⋮ → Allow restricted settings, then try again.")
                }
            },
            actionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Open App info" else null,
            action = appInfo,
        )
        RequirementKey.BATTERY -> manufacturerBatteryTip()?.let { Tip(it, "Open App info", appInfo) }
        else -> null
    }
}

private val RequirementKey.icon: ImageVector
    get() = when (this) {
        RequirementKey.NOTIFICATIONS -> Icons.Rounded.Notifications
        RequirementKey.FULL_SCREEN -> Icons.Rounded.Phone
        RequirementKey.EXACT_ALARMS -> Icons.Rounded.DateRange
        RequirementKey.APP_BLOCKER -> Icons.Rounded.Lock
        RequirementKey.BATTERY -> Icons.Rounded.Refresh
    }

@Composable
private fun QuietButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Glass.TextFaint,
        fontSize = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
