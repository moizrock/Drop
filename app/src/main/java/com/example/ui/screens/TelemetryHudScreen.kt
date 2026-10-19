package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.service.TransferForegroundService
import com.example.domain.model.LiveTelemetrySnapshot
import com.example.domain.model.TelemetrySamplePoint
import com.example.domain.model.ThermalStateLevel
import com.example.ui.theme.AmberThermalWarn
import com.example.ui.theme.CrimsonCritical
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.CyberSlateCard
import com.example.ui.theme.ElectricSky
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldNominal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimaryIce
import com.example.ui.theme.TextSecondarySlate

@Composable
fun TelemetryHudScreen(
    telemetry: LiveTelemetrySnapshot,
    oemSurvivalStatus: TransferForegroundService.OemSurvivalStatus,
    onToggleOemSurvivalShield: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Technical telemetry is tucked inside the collapsible "Advanced Settings" menu by default
    var isAdvancedMenuOpen by remember { mutableStateOf(false) }

    val thermalColor = when (telemetry.thermalLevel) {
        ThermalStateLevel.NONE, ThermalStateLevel.LIGHT -> EmeraldNominal
        ThermalStateLevel.MODERATE -> AmberThermalWarn
        else -> CrimsonCritical
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("telemetry_hud_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Consumer Device Health & Connection Overview Card
        item {
            Surface(
                color = CyberSlateCard,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Connection Health",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Device & Transfer Status",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimaryIce,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = telemetry.bottleneckKind.diagnosticAdvice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ConsumerHealthTile(
                            title = "Battery Level",
                            value = "${telemetry.batterySoCPercent}%",
                            subtitle = if (telemetry.isCharging) "Charging" else "Ready",
                            accent = EmeraldNominal,
                            modifier = Modifier.weight(1f)
                        )
                        ConsumerHealthTile(
                            title = "Device Temp",
                            value = telemetry.thermalLevel.label,
                            subtitle = "${"%.1f".format(telemetry.batteryTempCelsius)} °C",
                            accent = thermalColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Background Transfer Protection Card (Consumer Friendly)
        item {
            Surface(
                color = CyberSlateCard,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (oemSurvivalStatus.isServiceRunning) EmeraldNominal else CyberBorderSubtle,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("oem_survival_service_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (oemSurvivalStatus.isServiceRunning) EmeraldNominal.copy(alpha = 0.16f)
                                        else NeonCyan.copy(alpha = 0.14f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Background Transfer Protection",
                                    tint = if (oemSurvivalStatus.isServiceRunning) EmeraldNominal else NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Keep Transfers Alive in Background",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryIce,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prevents large transfers from pausing when screen turns off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySlate
                                )
                            }
                        }

                        Switch(
                            checked = oemSurvivalStatus.isServiceRunning,
                            onCheckedChange = { onToggleOemSurvivalShield(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberObsidian,
                                checkedTrackColor = EmeraldNominal
                            ),
                            modifier = Modifier.testTag("toggle_oem_survival_service_button")
                        )
                    }
                }
            }
        }

        // 3. Hidden "Advanced Settings" Menu Toggle Card (Moves technical telemetry inside Advanced Settings)
        item {
            Surface(
                color = CyberSlateCard,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, CyberBorderSubtle, RoundedCornerShape(24.dp))
                    .clickable { isAdvancedMenuOpen = !isAdvancedMenuOpen }
                    .testTag("toggle_advanced_settings_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyberObsidian),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Advanced Settings",
                                tint = ElectricSky,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Advanced Settings",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryIce,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAdvancedMenuOpen) {
                                    "Tap to hide live network graph & technical telemetry"
                                } else {
                                    "View live speed graph, signal strength & thermal metrics"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isAdvancedMenuOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                }
            }
        }

        // 4. Collapsible Technical Telemetry Section (Only visible when Advanced Settings is opened)
        item {
            AnimatedVisibility(visible = isAdvancedMenuOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        color = CyberSlateCard,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(22.dp))
                            .testTag("telemetry_realtime_line_chart_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Live Performance Graph",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryIce,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Real-time transfer speed, signal & thermal balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySlate
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Live Chart",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ChartLegendBadge(
                                    color = NeonCyan,
                                    label = "Speed: ${"%.1f".format(telemetry.transferSpeedMBps)} MB/s"
                                )
                                ChartLegendBadge(
                                    color = ElectricViolet,
                                    label = "Signal: ${telemetry.wifiRssiDbm} dBm"
                                )
                                ChartLegendBadge(
                                    color = EmeraldNominal,
                                    label = "Thermal: ${"%.2f".format(telemetry.thermalHeadroomRatio)}"
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            MultiMetricTelemetryLineChart(
                                samples = telemetry.historySamples,
                                currentSpeedMBps = telemetry.transferSpeedMBps,
                                currentRssiDbm = telemetry.wifiRssiDbm,
                                currentHeadroom = telemetry.thermalHeadroomRatio,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(185.dp)
                                    .testTag("telemetry_canvas_chart")
                            )
                        }
                    }

                    Surface(
                        color = CyberSlateCard,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(22.dp))
                            .testTag("thermal_headroom_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Thermal Headroom Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySlate
                                )
                                Text(
                                    text = "${(telemetry.thermalHeadroomRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = thermalColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { telemetry.thermalHeadroomRatio.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = thermalColor,
                                trackColor = CyberObsidian
                            )
                        }
                    }
                }
            }
        }

        // 5. Settings & About Card
        item {
            Surface(
                color = CyberSlateCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .testTag("telemetry_about_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Settings & About",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "WarpDrop v1.0.0 — Built on 5 GHz P2P, X25519 Encryption & BLAKE3 Verified Streaming",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryIce,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("telemetry_about_version_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsumerHealthTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CyberObsidian)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondarySlate
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimaryIce
        )
    }
}

@Composable
private fun ChartLegendBadge(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimaryIce,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MultiMetricTelemetryLineChart(
    samples: List<TelemetrySamplePoint>,
    currentSpeedMBps: Float,
    currentRssiDbm: Int,
    currentHeadroom: Float,
    modifier: Modifier = Modifier
) {
    val plotPoints = if (samples.size >= 6) {
        samples
    } else {
        val base = samples.lastOrNull() ?: TelemetrySamplePoint(
            timestampMs = System.currentTimeMillis(),
            transferSpeedMBps = currentSpeedMBps,
            wifiRssiDbm = currentRssiDbm,
            thermalHeadroom = currentHeadroom,
            batteryTempCelsius = 31.5f
        )
        List(8) { idx ->
            base.copy(
                transferSpeedMBps = (base.transferSpeedMBps + (idx % 3) * 1.5f).coerceAtLeast(2.4f),
                wifiRssiDbm = (base.wifiRssiDbm + (idx % 2) - 1).coerceIn(-90, -30),
                thermalHeadroom = (base.thermalHeadroom + (idx % 3) * 0.01f).coerceIn(0.1f, 1.0f)
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberObsidian)
            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            listOf(0.1f, 0.35f, 0.6f, 0.85f).forEach { yFrac ->
                drawLine(
                    color = CyberBorderSubtle.copy(alpha = 0.7f),
                    start = Offset(0f, h * yFrac),
                    end = Offset(w, h * yFrac),
                    strokeWidth = 1.2f
                )
            }

            val speedPath = Path()
            val speedFillPath = Path()
            val rssiPath = Path()
            val headroomPath = Path()

            val count = plotPoints.size
            plotPoints.forEachIndexed { idx, pt ->
                val x = (idx.toFloat() / (count - 1).coerceAtLeast(1)) * w
                val speedNorm = (pt.transferSpeedMBps / 120f).coerceIn(0.08f, 0.92f)
                val rssiNorm = ((pt.wifiRssiDbm + 95f) / 65f).coerceIn(0.12f, 0.92f)
                val headroomNorm = pt.thermalHeadroom.coerceIn(0.08f, 0.92f)

                val speedY = h * (1f - speedNorm)
                val rssiY = h * (1f - rssiNorm)
                val headroomY = h * (1f - headroomNorm)

                if (idx == 0) {
                    speedPath.moveTo(x, speedY)
                    speedFillPath.moveTo(x, h)
                    speedFillPath.lineTo(x, speedY)
                    rssiPath.moveTo(x, rssiY)
                    headroomPath.moveTo(x, headroomY)
                } else {
                    speedPath.lineTo(x, speedY)
                    speedFillPath.lineTo(x, speedY)
                    rssiPath.lineTo(x, rssiY)
                    headroomPath.lineTo(x, headroomY)
                }

                if (idx == count - 1) {
                    speedFillPath.lineTo(x, h)
                    speedFillPath.close()
                }

                drawCircle(color = NeonCyan, radius = 4.5f, center = Offset(x, speedY))
                drawCircle(color = ElectricViolet, radius = 4f, center = Offset(x, rssiY))
                drawCircle(color = EmeraldNominal, radius = 4f, center = Offset(x, headroomY))
            }

            drawPath(
                path = speedFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            drawPath(
                path = speedPath,
                color = NeonCyan,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
            drawPath(
                path = rssiPath,
                color = ElectricViolet,
                style = Stroke(width = 3.2f, cap = StrokeCap.Round)
            )
            drawPath(
                path = headroomPath,
                color = EmeraldNominal,
                style = Stroke(width = 3.2f, cap = StrokeCap.Round)
            )
        }
    }
}
