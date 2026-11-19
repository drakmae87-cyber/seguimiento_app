package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AlarmItem
import com.example.model.ClockPreferences
import com.example.model.DialStyle
import com.example.model.TelemetryTimeFormatter
import com.example.model.WorldCities
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.components.EyeModuleOverlay
import com.example.ui.components.SciFiTopHudBar
import com.example.ui.components.SpaceBackgroundLayer
import com.example.ui.components.SplitPanelCatClock
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import com.example.ui.theme.resolveEyeColor
import com.example.viewmodel.ClockFormatters
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    nowMillis: Long,
    preferences: ClockPreferences,
    nextAlarm: AlarmItem?,
    onCycleDialStyle: () -> Unit,
    onSelectDialStyle: (DialStyle) -> Unit,
    onCycleColorTheme: () -> Unit,
    onToggleBiolumEyes: (Boolean) -> Unit,
    onToggleParticleStream: (Boolean) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onSelectWorldCity: (String) -> Unit,
    onUpdateHudTextBoxes: (
        brandTag: String,
        bannerPrimary: String,
        bannerSecondary: String,
        labelDay: String,
        labelHours: String,
        labelMinutes: String,
        labelSeconds: String,
        labelDate: String
    ) -> Unit,
    onOpenAlarmsTab: () -> Unit,
    onEnterDeskMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    var showCityPicker by remember { mutableStateOf(false) }
    var showEditTextsDialog by remember { mutableStateOf(false) }

    val seoulZone = remember { TimeZone.getTimeZone("Asia/Seoul") }
    val selectedCity = remember(preferences.selectedWorldCityId) {
        WorldCities.byId(preferences.selectedWorldCityId)
    }
    val selectedCityZone = remember(selectedCity.zoneId) {
        TimeZone.getTimeZone(selectedCity.zoneId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SciFiTopHudBar(
                brandTag = preferences.hudBrandTag.ifBlank { "RGEN" },
                envCode = preferences.colorTheme.hudCode,
                skinCode = stringResource(preferences.dialStyle.labelRes),
                onCycleEnvironment = onCycleColorTheme,
                onEnterDeskMode = onEnterDeskMode
            )

            Spacer(modifier = Modifier.height(6.dp))

            val bannerPrimary = preferences.hudBannerPrimary.ifBlank {
                "RGEN_TIME // ${ClockFormatters.formatDateEs(nowMillis)}"
            }
            val bannerSecondary = preferences.hudBannerSecondary.ifBlank {
                "${stringResource(preferences.dialStyle.labelRes)} · TELEMETRÍA"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditTextsDialog = true }
            ) {
                CurvedSciFiHudBanner(
                    primaryText = bannerPrimary,
                    secondaryText = bannerSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // COLLAGER4 / PLASMA / HUD-4 Clock Architecture
            SplitPanelCatClock(
                nowMillis = nowMillis,
                preferences = preferences,
                onClockClick = onCycleDialStyle
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.clock_tap_style_hint),
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted.copy(alpha = 0.85f)
            )

            if (nextAlarm != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = onOpenAlarmsTab,
                    shape = ChamferedHudShape(8.dp),
                    color = StageCard.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = palette.secondaryGlow,
                            modifier = Modifier.size(15.dp)
                        )
                        val remainingHhMmSs = if (nextAlarm.nextTriggerAtMillis > nowMillis) {
                            TelemetryTimeFormatter.formatRemainingHhMmSs(nextAlarm.nextTriggerAtMillis, nowMillis)
                        } else {
                            "00:00:00"
                        }
                        Text(
                            text = "Próxima: ${nextAlarm.formattedTime()} · Quedan $remainingHhMmSs · ${nextAlarm.label}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = InkCream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val chipShape = remember { ChamferedHudShape(7.dp) }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(DialStyle.entries) { style ->
                    val selected = preferences.dialStyle == style
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectDialStyle(style) },
                        shape = chipShape,
                        label = {
                            Text(
                                text = stringResource(style.labelRes),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primaryGlow,
                            selectedLabelColor = StageVoid,
                            containerColor = StageCard.copy(alpha = 0.82f),
                            labelColor = InkCream
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = StageBorder,
                            selectedBorderColor = palette.primaryGlow
                        ),
                        modifier = Modifier.testTag("dial_chip_${style.id}")
                    )
                }

                item {
                    val eyeColor = remember(preferences.eyeColorPreset, preferences.customEyeColorHex) {
                        resolveEyeColor(preferences.eyeColorPreset, preferences.customEyeColorHex)
                    }
                    FilterChip(
                        selected = preferences.biolumEyesOverlayEnabled,
                        onClick = { onToggleBiolumEyes(!preferences.biolumEyesOverlayEnabled) },
                        shape = chipShape,
                        label = { Text("NÚCLEO") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CenterFocusStrong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = eyeColor.copy(alpha = 0.24f),
                            selectedLabelColor = eyeColor,
                            selectedLeadingIconColor = eyeColor,
                            containerColor = StageCard.copy(alpha = 0.82f),
                            labelColor = InkMuted
                        ),
                        modifier = Modifier.testTag("toggle_eyes_chip")
                    )
                }

                item {
                    FilterChip(
                        selected = preferences.particleStreamEnabled,
                        onClick = { onToggleParticleStream(!preferences.particleStreamEnabled) },
                        shape = chipShape,
                        label = { Text("3D FX") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.BlurOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.secondaryGlow.copy(alpha = 0.22f),
                            selectedLabelColor = palette.secondaryGlow,
                            selectedLeadingIconColor = palette.secondaryGlow,
                            containerColor = StageCard.copy(alpha = 0.82f),
                            labelColor = InkMuted
                        ),
                        modifier = Modifier.testTag("toggle_particles_chip")
                    )
                }

                item {
                    FilterChip(
                        selected = preferences.use24Hour,
                        onClick = { onToggle24Hour(!preferences.use24Hour) },
                        shape = chipShape,
                        label = {
                            Text(
                                text = if (preferences.use24Hour) "24H" else "12H",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primaryGlow.copy(alpha = 0.22f),
                            selectedLabelColor = palette.primaryGlow,
                            selectedLeadingIconColor = palette.primaryGlow,
                            containerColor = StageCard.copy(alpha = 0.82f),
                            labelColor = InkMuted
                        ),
                        modifier = Modifier.testTag("toggle_24h_chip")
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = { showEditTextsDialog = true },
                        shape = chipShape,
                        label = { Text("TEXTOS") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StageCard.copy(alpha = 0.82f),
                            labelColor = InkCream,
                            iconColor = palette.secondaryGlow
                        ),
                        modifier = Modifier.testTag("edit_hud_texts_chip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (preferences.showSeoulClock) {
                    WorldClockHudCard(
                        cityName = "Seúl",
                        zoneTag = "KST · SYNC",
                        timeText = ClockFormatters.formatMainTime(nowMillis, preferences.use24Hour, seoulZone),
                        amPm = if (preferences.use24Hour) "" else ClockFormatters.formatAmPm(nowMillis, seoulZone),
                        offsetText = ClockFormatters.formatOffsetFromLocal(nowMillis, seoulZone),
                        isDaytime = ClockFormatters.isDaytime(nowMillis, seoulZone),
                        accentColor = palette.secondaryGlow,
                        onClick = { showCityPicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("seoul_clock_card")
                    )
                }

                WorldClockHudCard(
                    cityName = selectedCity.name,
                    zoneTag = "${selectedCity.tag} · HUD",
                    timeText = ClockFormatters.formatMainTime(nowMillis, preferences.use24Hour, selectedCityZone),
                    amPm = if (preferences.use24Hour) "" else ClockFormatters.formatAmPm(nowMillis, selectedCityZone),
                    offsetText = ClockFormatters.formatOffsetFromLocal(nowMillis, selectedCityZone),
                    isDaytime = ClockFormatters.isDaytime(nowMillis, selectedCityZone),
                    accentColor = palette.primaryGlow,
                    onClick = { showCityPicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("world_clock_card")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showEditTextsDialog) {
        EditHudTextsDialog(
            preferences = preferences,
            onDismiss = { showEditTextsDialog = false },
            onSave = { tag, b1, b2, d, h, m, s, f ->
                onUpdateHudTextBoxes(tag, b1, b2, d, h, m, s, f)
                showEditTextsDialog = false
            }
        )
    }

    if (showCityPicker) {
        val itemShape = ChamferedHudShape(10.dp)
        ModalBottomSheet(
            onDismissRequest = { showCityPicker = false },
            containerColor = StageCardElevated,
            contentColor = InkCream
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = palette.secondaryGlow
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.clock_world_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkCream
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                WorldCities.all.forEach { city ->
                    val tz = TimeZone.getTimeZone(city.zoneId)
                    val isSelected = city.id == selectedCity.id
                    Surface(
                        onClick = {
                            onSelectWorldCity(city.id)
                            showCityPicker = false
                        },
                        shape = itemShape,
                        color = if (isSelected) palette.primaryGlow.copy(alpha = 0.22f) else StageCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) palette.primaryGlow else StageBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("city_option_${city.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkCream
                                )
                                Text(
                                    text = "${city.tag} · ${ClockFormatters.formatOffsetFromLocal(nowMillis, tz)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted
                                )
                            }
                            Text(
                                text = ClockFormatters.formatMainTime(nowMillis, preferences.use24Hour, tz),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (isSelected) palette.secondaryGlow else InkCream
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EditHudTextsDialog(
    preferences: ClockPreferences,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Unit
) {
    val palette = LocalNekoPalette.current
    var brandTag by remember { mutableStateOf(preferences.hudBrandTag) }
    var bannerPrimary by remember { mutableStateOf(preferences.hudBannerPrimary) }
    var bannerSecondary by remember { mutableStateOf(preferences.hudBannerSecondary) }
    var labelDay by remember { mutableStateOf(preferences.panelLabelDay) }
    var labelHours by remember { mutableStateOf(preferences.panelLabelHours) }
    var labelMinutes by remember { mutableStateOf(preferences.panelLabelMinutes) }
    var labelSeconds by remember { mutableStateOf(preferences.panelLabelSeconds) }
    var labelDate by remember { mutableStateOf(preferences.panelLabelDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        title = { Text("EDITAR CUADROS DE TEXTO HUD", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = brandTag,
                    onValueChange = { brandTag = it.take(18) },
                    label = { Text("Etiqueta superior") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bannerPrimary,
                    onValueChange = { bannerPrimary = it.take(48) },
                    label = { Text("Encabezado principal") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bannerSecondary,
                    onValueChange = { bannerSecondary = it.take(48) },
                    label = { Text("Subtítulo técnico") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = labelDay,
                        onValueChange = { labelDay = it.take(16) },
                        label = { Text("Panel 1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = labelHours,
                        onValueChange = { labelHours = it.take(16) },
                        label = { Text("Panel 2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = labelMinutes,
                        onValueChange = { labelMinutes = it.take(16) },
                        label = { Text("Panel 3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = labelSeconds,
                        onValueChange = { labelSeconds = it.take(16) },
                        label = { Text("Panel 4") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = labelDate,
                        onValueChange = { labelDate = it.take(16) },
                        label = { Text("Panel 5") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        brandTag, bannerPrimary, bannerSecondary,
                        labelDay, labelHours, labelMinutes, labelSeconds, labelDate
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryGlow, contentColor = StageVoid)
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = InkMuted)
            }
        }
    )
}

@Composable
private fun WorldClockHudCard(
    cityName: String,
    zoneTag: String,
    timeText: String,
    amPm: String,
    offsetText: String,
    isDaytime: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hudShape = remember { ChamferedHudShape(12.dp) }
    Surface(
        onClick = onClick,
        shape = hudShape,
        color = StageCard.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = zoneTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.NightlightRound,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeText,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = InkCream
                )
                if (amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = amPm,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$cityName · $offsetText",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DeskModeOverlay(
    nowMillis: Long,
    preferences: ClockPreferences,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val palette = LocalNekoPalette.current
    val seoulZone = remember { TimeZone.getTimeZone("Asia/Seoul") }
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val eyeColor = remember(preferences.eyeColorPreset, preferences.customEyeColorHex) {
        resolveEyeColor(preferences.eyeColorPreset, preferences.customEyeColorHex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StageVoid)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExit
            )
            .testTag("desk_mode_overlay")
    ) {
        SpaceBackgroundLayer(
            customBackgroundPath = preferences.customBackgroundPath,
            customBackgroundAlpha = preferences.customBackgroundAlpha,
            bgTransform = preferences.bgTransform,
            starfieldEnabled = preferences.starfieldEnabled,
            particleStreamEnabled = preferences.particleStreamEnabled,
            auroraNebulaEnabled = preferences.auroraNebulaEnabled,
            anamorphicFlareEnabled = preferences.anamorphicFlareEnabled,
            gravityHorizonEnabled = preferences.gravityHorizonEnabled,
            cinematicScanlinesEnabled = preferences.cinematicScanlinesEnabled,
            perspectiveGrid3dEnabled = preferences.perspectiveGrid3dEnabled,
            holoPrism3dEnabled = preferences.holoPrism3dEnabled,
            neonPulseRingsEnabled = preferences.neonPulseRingsEnabled,
            cyberMatrixRainEnabled = preferences.cyberMatrixRainEnabled,
            atmosphericFogEnabled = preferences.atmosphericFogEnabled,
            fxIntensity = preferences.fxIntensity,
            customFxLayers = preferences.customFxLayers,
            batteryOptimized = preferences.batteryOptimizationEnabled
        )

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 20.dp)
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(StageCard.copy(alpha = 0.78f))
                .border(1.dp, palette.secondaryGlow.copy(alpha = 0.6f), CircleShape)
                .testTag("exit_desk_mode_button")
        ) {
            Icon(
                imageVector = Icons.Filled.FullscreenExit,
                contentDescription = stringResource(R.string.cd_exit_desk_mode),
                tint = InkCream
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (preferences.biolumEyesOverlayEnabled) {
                EyeModuleOverlay(
                    preferences = preferences,
                    eyeColor = eyeColor,
                    secondaryGlow = palette.secondaryGlow,
                    beatPulse = 0.5f,
                    eyeScale = preferences.eyeCanvasScale,
                    collageProportionalMode = false,
                    modifier = Modifier
                        .width(268.dp)
                        .height(148.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = ClockFormatters.formatMainTime(nowMillis, preferences.use24Hour),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 66.sp,
                    color = InkCream
                )
                if (preferences.showSeconds) {
                    Text(
                        text = ":${ClockFormatters.formatSeconds(nowMillis)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = eyeColor,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CurvedSciFiHudBanner(
                primaryText = preferences.hudBannerPrimary.ifBlank { ClockFormatters.formatDateEs(nowMillis) },
                secondaryText = if (preferences.showSeoulClock) {
                    "SEÚL ${ClockFormatters.formatMainTime(nowMillis, preferences.use24Hour, seoulZone)} KST"
                } else {
                    preferences.colorTheme.hudCode
                }
            )
        }

        Surface(
            shape = hudShape,
            color = StageCard.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, StageBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.clock_desk_exit_hint),
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
