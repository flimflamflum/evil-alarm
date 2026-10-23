package com.evilalarm.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassButton
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.ScreenHeader

/**
 * Registers a QR code or barcode you'll have to scan later. [rejectReason] says why a scanned code
 * can't be used (null if it can), and scanning carries on until one passes.
 */
@Composable
fun ScanCodeScreen(
    title: String,
    description: String,
    rejectReason: (String) -> String?,
    onScanned: (String) -> Unit,
    onBack: () -> Unit,
) {
    val (cameraAllowed, askForCamera) = rememberCameraPermission()
    var torchOn by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var rejected by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title, onBack)
        Text(
            description,
            color = Glass.TextSecondary,
            fontSize = 14.sp,
        )

        GlassCard(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(Glass.Fill)
                    .border(1.dp, Glass.Border, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (cameraAllowed) {
                    CodeScanner(
                        onCode = { code ->
                            if (!done) {
                                rejected = rejectReason(code)
                                if (rejected == null) {
                                    done = true
                                    onScanned(code)
                                }
                            }
                        },
                        torchOn = torchOn,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                } else {
                    Text(
                        "Evil Alarm needs the camera to scan your code. It's only used on this screen, the alarm screen and the block screen.",
                        color = Glass.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            rejected?.let {
                Text(
                    it,
                    color = Glass.Danger,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (cameraAllowed) {
                Text(
                    if (torchOn) "Flashlight off" else "Flashlight on",
                    color = Glass.Pink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { torchOn = !torchOn }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        if (!cameraAllowed) {
            GlassButton("Allow camera", onClick = askForCamera, accent = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
