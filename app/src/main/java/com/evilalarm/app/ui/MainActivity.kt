package com.evilalarm.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evilalarm.app.alarm.AlarmScheduler
import com.evilalarm.app.data.Alarm
import com.evilalarm.app.ui.theme.EvilAlarmTheme
import com.evilalarm.app.ui.theme.GlassBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EvilAlarmTheme {
                EvilAlarmNav()
            }
        }
    }
}

private sealed interface Screen {
    data object Alarms : Screen
    data class Edit(val alarm: Alarm?) : Screen
    data object Settings : Screen
    data object Blocker : Screen
    data object BlockedApps : Screen
    data object Setup : Screen
    data object Stats : Screen
    data object ScanCode : Screen
    data object ScanBlockerCode : Screen
}

@Composable
private fun EvilAlarmNav(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val blockUntil by vm.blockUntil.collectAsStateWithLifecycle()
    val lastDailyBlock by vm.lastDailyBlock.collectAsStateWithLifecycle()
    val setupDone by vm.setupDone.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Alarms) }
    var ready by remember { mutableStateOf(false) }
    val goBack = {
        screen = when (screen) {
            Screen.BlockedApps, Screen.ScanBlockerCode -> Screen.Blocker
            Screen.ScanCode -> Screen.Settings
            else -> Screen.Alarms
        }
    }

    LaunchedEffect(Unit) {
        // Safety net: make AlarmManager match what's saved (e.g. after a permission was granted).
        AlarmScheduler.rescheduleAll(context)
    }
    // Pick the first screen once we know whether setup has been done, before drawing anything.
    LaunchedEffect(setupDone) {
        val done = setupDone ?: return@LaunchedEffect
        if (!ready) {
            if (!done) screen = Screen.Setup
            ready = true
        }
    }

    BackHandler(enabled = screen != Screen.Alarms, onBack = goBack)

    GlassBackground {
        if (ready) AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.Alarms -> AlarmListScreen(
                    alarms = alarms,
                    blockUntil = blockUntil,
                    blockedPackages = settings.blockedPackages,
                    stats = stats,
                    onAdd = { screen = Screen.Edit(null) },
                    onEdit = { screen = Screen.Edit(it) },
                    onToggle = vm::setEnabled,
                    onOpenBlocker = { screen = Screen.Blocker },
                    onOpenStats = { screen = Screen.Stats },
                    onOpenSettings = { screen = Screen.Settings },
                )
                Screen.Blocker -> BlockerScreen(
                    settings = settings,
                    blockUntil = blockUntil,
                    lastDailyBlock = lastDailyBlock,
                    onChange = vm::updateSettings,
                    onStartBlock = vm::startBlockNow,
                    onEndBlock = vm::endBlock,
                    onPickApps = { screen = Screen.BlockedApps },
                    onScanCode = { screen = Screen.ScanBlockerCode },
                    onBack = goBack,
                )
                Screen.BlockedApps -> BlockedAppsScreen(
                    blockedPackages = settings.blockedPackages,
                    onToggle = vm::setAppBlocked,
                    onBack = goBack,
                )
                is Screen.Edit -> EditAlarmScreen(
                    alarm = current.alarm,
                    onSave = {
                        vm.save(it)
                        screen = Screen.Alarms
                    },
                    onDelete = {
                        vm.delete(it)
                        screen = Screen.Alarms
                    },
                    onBack = goBack,
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    onChange = vm::updateSettings,
                    onTestAlarm = vm::testAlarm,
                    onRunSetup = { screen = Screen.Setup },
                    onScanCode = { screen = Screen.ScanCode },
                    onBack = goBack,
                )
                Screen.ScanCode -> ScanCodeScreen(
                    title = "Your wake-up code",
                    description = "Pick a QR code or barcode somewhere you have to walk to: a printed QR code on the bathroom mirror, " +
                        "or the barcode on your shampoo or a cereal box. To turn the alarm off, you'll have to go scan it.",
                    rejectReason = { code ->
                        if (code == settings.blockerCode) "That's your app blocker code. Pick a different one." else null
                    },
                    onScanned = {
                        vm.saveWakeUpCode(it)
                        screen = Screen.Settings
                    },
                    onBack = goBack,
                )
                Screen.ScanBlockerCode -> ScanCodeScreen(
                    title = "Your blocker code",
                    description = "Pick a second QR code or barcode, different from your wake-up code, and somewhere out of the way: " +
                        "the back of a cupboard, the garage, a friend's fridge. To end a block early, you'll have to go scan it.",
                    rejectReason = { code ->
                        if (code == settings.qrCode) "That's your wake-up code. Pick a different one." else null
                    },
                    onScanned = {
                        vm.saveBlockerCode(it)
                        screen = Screen.Blocker
                    },
                    onBack = goBack,
                )
                Screen.Stats -> StatsScreen(stats, onBack = goBack)
                Screen.Setup -> SetupScreen(
                    onFinish = {
                        vm.markSetupDone()
                        screen = Screen.Alarms
                    },
                )
            }
        }
    }
}
