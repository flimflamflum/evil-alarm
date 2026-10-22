package com.evilalarm.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.evilalarm.app.blocker.Blocker
import com.evilalarm.app.blocker.InstalledApp
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.GlassSwitch
import com.evilalarm.app.ui.theme.ScreenHeader
import com.evilalarm.app.ui.theme.glassTextFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlockedAppsScreen(
    blockedPackages: Set<String>,
    onToggle: (packageName: String, blocked: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val apps by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { Blocker.launchableApps(context) }
    }
    // Apps that were already blocked float to the top, but only as of opening the screen,
    // so rows don't jump around while you toggle.
    val pinnedToTop = remember { blockedPackages }
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        apps.orEmpty()
            .filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            .sortedByDescending { it.packageName in pinnedToTop }
    }
    val bars = WindowInsets.systemBars.asPaddingValues()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = bars.calculateTopPadding() + 12.dp,
            bottom = bars.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader("Blocked apps", onBack) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("Search apps") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = glassTextFieldColors(),
            )
        }
        if (apps == null) {
            item { Text("Loading apps…", color = Glass.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(8.dp)) }
        }
        items(shown, key = { it.packageName }) { app ->
            val blocked = app.packageName in blockedPackages
            GlassCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { onToggle(app.packageName, !blocked) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.packageName)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        app.label,
                        color = if (blocked) Glass.TextPrimary else Glass.TextSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    GlassSwitch(checked = blocked, onCheckedChange = { onToggle(app.packageName, it) })
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Glass.Fill),
    ) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
