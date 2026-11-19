package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ClockPreferences
import com.example.model.FxCategory
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageCardElevated
import com.example.ui.theme.StageVoid
import com.example.ui.theme.parseHexColorOrNull

@Composable
fun FxStudioSection(
    preferences: ClockPreferences,
    onSetFxIntensity: (Float) -> Unit,
    onApplyFxPreset: (String) -> Unit,
    onToggleParticleStream: (Boolean) -> Unit,
    onTogglePerspectiveGrid3d: (Boolean) -> Unit,
    onToggleHoloPrism3d: (Boolean) -> Unit,
    onToggleNeonPulseRings: (Boolean) -> Unit,
    onToggleCyberMatrixRain: (Boolean) -> Unit,
    onToggleAtmosphericFog: (Boolean) -> Unit,
    onToggleStarfield: (Boolean) -> Unit,
    onToggleAuroraNebula: (Boolean) -> Unit,
    onToggleAnamorphicFlare: (Boolean) -> Unit,
    onToggleGravityHorizon: (Boolean) -> Unit,
    onToggleCinematicScanlines: (Boolean) -> Unit,
    onAddCustomFxLayer: (name: String, category: FxCategory, colorHex: String, speed: Float, density: Float) -> Unit,
    onToggleCustomFxLayer: (layerId: String, enabled: Boolean) -> Unit,
    onDeleteCustomFxLayer: (layerId: String) -> Unit
) {
    val palette = LocalNekoPalette.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }
    var showAddFxDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = null,
                tint = palette.primaryGlow,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.studio_fx_header),
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryGlow
            )
        }
        Button(
            onClick = { showAddFxDialog = true },
            shape = chipShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.secondaryGlow,
                contentColor = StageVoid
            ),
            modifier = Modifier.testTag("fx_add_custom_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Agregar FX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Card(
        shape = hudShape,
        colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
        border = BorderStroke(1.dp, palette.primaryGlow.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Perfiles FX Instantáneos",
                style = MaterialTheme.typography.labelLarge,
                color = InkMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "telemetry_3d" to "Telemetría 3D",
                    "neon_storm" to "Tormenta Neón EFX",
                    "deep_atmos" to "Atmósfera Profunda",
                    "eco_battery" to "Eco Batería"
                )
                items(presets) { (id, label) ->
                    OutlinedButton(
                        onClick = { onApplyFxPreset(id) },
                        shape = chipShape,
                        border = BorderStroke(1.dp, palette.secondaryGlow.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("fx_preset_$id")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = palette.secondaryGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(label, fontSize = 11.sp, color = InkCream)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Intensidad global FX · ${(preferences.fxIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
            Slider(
                value = preferences.fxIntensity,
                onValueChange = onSetFxIntensity,
                valueRange = 0.2f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.primaryGlow,
                    activeTrackColor = palette.primaryGlow
                ),
                modifier = Modifier.testTag("fx_intensity_slider")
            )

            // User-created FX layers section
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "CAPAS FX AGREGADAS POR EL USUARIO (${preferences.customFxLayers.size})",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.secondaryGlow
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (preferences.customFxLayers.isEmpty()) {
                Surface(
                    shape = chipShape,
                    color = StageVoid.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, StageBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sin capas personalizadas. Pulse \"+ Agregar FX\" para crear capas de partículas, 3D, atmósfera o neón EFX.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    preferences.customFxLayers.forEach { layer ->
                        val layerColor = parseHexColorOrNull(layer.colorHex) ?: palette.primaryGlow
                        Surface(
                            shape = chipShape,
                            color = StageVoid.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, layerColor.copy(alpha = if (layer.enabled) 0.7f else 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(layerColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkCream
                                    )
                                    Text(
                                        text = "${layer.category.label} · Vel ${(layer.speed * 100).toInt()}% · Dens ${(layer.density * 100).toInt()}%",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = InkMuted
                                    )
                                }
                                Switch(
                                    checked = layer.enabled,
                                    onCheckedChange = { onToggleCustomFxLayer(layer.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = StageVoid,
                                        checkedTrackColor = layerColor
                                    )
                                )
                                IconButton(
                                    onClick = { onDeleteCustomFxLayer(layer.id) },
                                    modifier = Modifier.testTag("delete_fx_${layer.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Eliminar capa FX",
                                        tint = InkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "CAPAS NATIVAS 3D, PARTÍCULAS, ATMÓSFERA & NEÓN EFX",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = palette.primaryGlow
            )

            FxToggleRow(
                title = "Malla 3D de Perspectiva",
                subtitle = "Plano reticular tridimensional dinámico estilo espacial",
                checked = preferences.perspectiveGrid3dEnabled,
                onCheckedChange = onTogglePerspectiveGrid3d,
                testTag = "pref_switch_grid_3d"
            )
            FxToggleRow(
                title = "Poliedro Holográfico 3D",
                subtitle = "Cubo wireframe 3D rotatorio proyectado en escena",
                checked = preferences.holoPrism3dEnabled,
                onCheckedChange = onToggleHoloPrism3d,
                testTag = "pref_switch_prism_3d"
            )
            FxToggleRow(
                title = "Ondas Neón EFX",
                subtitle = "Expansión radial de anillos de alta luminancia",
                checked = preferences.neonPulseRingsEnabled,
                onCheckedChange = onToggleNeonPulseRings,
                testTag = "pref_switch_neon_rings"
            )
            FxToggleRow(
                title = "Flujo Fotónico Vertical",
                subtitle = "Estelas descendentes de alta velocidad tipo telemetría",
                checked = preferences.cyberMatrixRainEnabled,
                onCheckedChange = onToggleCyberMatrixRain,
                testTag = "pref_switch_photon_rain"
            )
            FxToggleRow(
                title = "Bruma Atmosférica",
                subtitle = "Niebla volumétrica profunda en capas horizontales",
                checked = preferences.atmosphericFogEnabled,
                onCheckedChange = onToggleAtmosphericFog,
                testTag = "pref_switch_atmos_fog"
            )
            FxToggleRow(
                title = stringResource(R.string.pref_particles_title),
                subtitle = stringResource(R.string.pref_particles_desc),
                checked = preferences.particleStreamEnabled,
                onCheckedChange = onToggleParticleStream,
                testTag = "pref_switch_particles"
            )
            FxToggleRow(
                title = stringResource(R.string.pref_starfield_title),
                subtitle = stringResource(R.string.pref_starfield_desc),
                checked = preferences.starfieldEnabled,
                onCheckedChange = onToggleStarfield,
                testTag = "pref_switch_starfield"
            )
            FxToggleRow(
                title = stringResource(R.string.pref_aurora_title),
                subtitle = stringResource(R.string.pref_aurora_desc),
                checked = preferences.auroraNebulaEnabled,
                onCheckedChange = onToggleAuroraNebula,
                testTag = "pref_switch_aurora"
            )
            FxToggleRow(
                title = stringResource(R.string.pref_flare_title),
                subtitle = stringResource(R.string.pref_flare_desc),
                checked = preferences.anamorphicFlareEnabled,
                onCheckedChange = onToggleAnamorphicFlare,
                testTag = "pref_switch_flare"
            )
            FxToggleRow(
                title = stringResource(R.string.pref_gravity_title),
                subtitle = stringResource(R.string.pref_gravity_desc),
                checked = preferences.gravityHorizonEnabled,
                onCheckedChange = onToggleGravityHorizon,
                testTag = "pref_switch_gravity"
            )
            FxToggleRow(
                title = stringResource(R.string.pref_scanlines_title),
                subtitle = stringResource(R.string.pref_scanlines_desc),
                checked = preferences.cinematicScanlinesEnabled,
                onCheckedChange = onToggleCinematicScanlines,
                testTag = "pref_switch_scanlines"
            )
        }
    }

    if (showAddFxDialog) {
        AddCustomFxDialog(
            onDismiss = { showAddFxDialog = false },
            onConfirm = { name, category, hex, speed, density ->
                onAddCustomFxLayer(name, category, hex, speed, density)
                showAddFxDialog = false
            }
        )
    }
}

@Composable
private fun FxToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalNekoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = InkCream)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StageVoid,
                checkedTrackColor = palette.primaryGlow,
                uncheckedThumbColor = InkMuted,
                uncheckedTrackColor = StageVoid
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AddCustomFxDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: FxCategory, colorHex: String, speed: Float, density: Float) -> Unit
) {
    val palette = LocalNekoPalette.current
    var fxName by remember { mutableStateOf("Pulso Neón 3D") }
    var selectedCategory by remember { mutableStateOf(FxCategory.NEON_EFX) }
    var selectedHex by remember { mutableStateOf("#00E5FF") }
    var speed by remember { mutableFloatStateOf(1.1f) }
    var density by remember { mutableFloatStateOf(0.9f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StageCardElevated,
        titleContentColor = InkCream,
        textContentColor = InkCream,
        title = {
            Text("NUEVA CAPA FX PERSONALIZADA", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fxName,
                    onValueChange = { fxName = it.take(28) },
                    label = { Text("Nombre de la capa FX") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryGlow,
                        unfocusedBorderColor = StageBorder,
                        focusedTextColor = InkCream,
                        unfocusedTextColor = InkCream
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fx_dialog_name_input")
                )

                Text("Motor de renderizado", style = MaterialTheme.typography.labelLarge, color = InkMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FxCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primaryGlow,
                                selectedLabelColor = StageVoid,
                                containerColor = StageVoid,
                                labelColor = InkCream
                            )
                        )
                    }
                }

                CodexHexColorSelector(
                    title = "Color de emisión FX",
                    activeHex = selectedHex,
                    onColorHexSelected = { selectedHex = it },
                    onResetToPreset = { selectedHex = "#00E5FF" },
                    testTagPrefix = "fx_custom"
                )

                Text(
                    text = "Velocidad · ${(speed * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted
                )
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.3f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = palette.primaryGlow, activeTrackColor = palette.primaryGlow)
                )

                Text(
                    text = "Densidad / Intensidad · ${(density * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted
                )
                Slider(
                    value = density,
                    onValueChange = { density = it },
                    valueRange = 0.2f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = palette.secondaryGlow, activeTrackColor = palette.secondaryGlow)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(fxName, selectedCategory, selectedHex, speed, density) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryGlow, contentColor = StageVoid),
                modifier = Modifier.testTag("fx_dialog_confirm")
            ) {
                Text("Crear Capa FX")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = InkMuted)
            }
        }
    )
}
