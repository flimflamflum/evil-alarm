package com.evilalarm.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evilalarm.app.data.Alarm
import com.evilalarm.app.ui.theme.Glass
import com.evilalarm.app.ui.theme.GlassButton
import com.evilalarm.app.ui.theme.GlassCard
import com.evilalarm.app.ui.theme.ScreenHeader
import com.evilalarm.app.ui.theme.SectionTitle
import com.evilalarm.app.ui.theme.glassTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(
    alarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onBack: () -> Unit,
) {
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val initial = alarm ?: Alarm(id = Alarm.NEW_ID, hour = 7, minute = 0)
    val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24h)
    var days by remember { mutableStateOf(initial.days) }
    var label by remember { mutableStateOf(initial.label) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(if (alarm == null) "New alarm" else "Edit alarm", onBack)

        GlassCard(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = timeState, colors = glassTimePickerColors())
        }

        GlassCard(Modifier.fillMaxWidth()) {
            SectionTitle("Repeat")
            DayPicker(days = days, onChange = { days = it })
            Spacer(Modifier.height(10.dp))
            Text(formatDays(days), color = Glass.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }

        OutlinedTextField(
            value = label,
            onValueChange = { label = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Label") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (alarm != null) {
                GlassButton("Delete", onClick = { onDelete(alarm) })
            }
            GlassButton(
                "Save",
                onClick = {
                    onSave(
                        initial.copy(
                            hour = timeState.hour,
                            minute = timeState.minute,
                            days = days,
                            label = label.trim(),
                            enabled = true,
                        ),
                    )
                },
                accent = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, letter ->
            val day = index + 1
            val on = day in days
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (on) Glass.AccentBrush else SolidColor(Glass.Fill))
                    .border(1.dp, Glass.Border, CircleShape)
                    .clickable { onChange(if (on) days - day else days + day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    color = if (on) Glass.TextPrimary else Glass.TextSecondary,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun glassTimePickerColors(): TimePickerColors = TimePickerDefaults.colors(
    clockDialColor = Glass.Fill,
    clockDialSelectedContentColor = Color.White,
    clockDialUnselectedContentColor = Glass.TextSecondary,
    selectorColor = Glass.Pink,
    containerColor = Color.Transparent,
    periodSelectorBorderColor = Color.White.copy(alpha = 0.2f),
    periodSelectorSelectedContainerColor = Glass.Pink.copy(alpha = 0.3f),
    periodSelectorUnselectedContainerColor = Color.Transparent,
    periodSelectorSelectedContentColor = Color.White,
    periodSelectorUnselectedContentColor = Glass.TextSecondary,
    timeSelectorSelectedContainerColor = Glass.Pink.copy(alpha = 0.3f),
    timeSelectorUnselectedContainerColor = Glass.Fill,
    timeSelectorSelectedContentColor = Color.White,
    timeSelectorUnselectedContentColor = Glass.TextPrimary,
)
