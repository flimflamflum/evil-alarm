package com.evilalarm.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Dark gradient with soft colour glows; the glass surfaces sit on top of it. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Glass.Night, Glass.NightMid, Glass.Night))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun glow(color: Color, center: Offset, radius: Float) = drawCircle(
                brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
                radius = radius,
                center = center,
            )
            val r = size.minDimension
            glow(Glass.Violet.copy(alpha = 0.45f), Offset(size.width * 0.05f, size.height * 0.12f), r * 0.8f)
            glow(Glass.Pink.copy(alpha = 0.28f), Offset(size.width * 1.0f, size.height * 0.48f), r * 0.75f)
            glow(Glass.Cyan.copy(alpha = 0.18f), Offset(size.width * 0.15f, size.height * 0.95f), r * 0.8f)
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .background(Glass.Fill)
            .border(1.dp, Glass.Border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (accent) Glass.AccentBrush else SolidColor(Glass.FillStrong))
            .border(1.dp, Glass.Border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Glass.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    accent: Boolean = false,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (accent) Glass.AccentBrush else SolidColor(Glass.FillStrong))
            .border(1.dp, Glass.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Glass.TextPrimary, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Glass.Pink,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Glass.TextSecondary,
            uncheckedTrackColor = Glass.Fill,
            uncheckedBorderColor = Color.White.copy(alpha = 0.2f),
        ),
    )
}

/** Pill-shaped single-choice selector, e.g. Easy / Medium / Hard. */
@Composable
fun <T> GlassSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(50)
    Row(
        modifier
            .clip(outer)
            .background(Glass.Fill)
            .border(1.dp, Glass.Border, outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(outer)
                    .background(if (isSelected) Glass.AccentBrush else SolidColor(Color.Transparent))
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    color = if (isSelected) Glass.TextPrimary else Glass.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun glassTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Glass.Pink,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedContainerColor = Glass.Fill,
    unfocusedContainerColor = Glass.Fill,
    focusedTextColor = Glass.TextPrimary,
    unfocusedTextColor = Glass.TextPrimary,
    cursorColor = Glass.Pink,
    focusedLabelColor = Glass.Pink,
    unfocusedLabelColor = Glass.TextSecondary,
    focusedPlaceholderColor = Glass.TextFaint,
    unfocusedPlaceholderColor = Glass.TextFaint,
)

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(16.dp))
        Text(title, color = Glass.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        color = Glass.TextFaint,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Medium,
    )
}
