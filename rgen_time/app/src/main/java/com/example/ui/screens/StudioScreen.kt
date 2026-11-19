package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.CollageImageSlicer
import com.example.data.CustomMediaHelper
import com.example.data.MediaFormatKind
import com.example.model.CanvasOverlayStyle
import com.example.model.ClockPreferences
import com.example.model.DialStyle
import com.example.model.EyeColorPreset
import com.example.model.FxCategory
import com.example.model.HudColorTheme
import com.example.model.IrisPupilStyle
import com.example.model.MediaFitMode
import com.example.model.MediaTransform
import com.example.ui.components.ChamferedHudShape
import com.example.ui.components.CodexHexColorSelector
import com.example.ui.components.CurvedSciFiHudBanner
import com.example.ui.components.EyeModuleOverlay
import com.example.ui.components.FxStudioSection
import com.example.ui.components.UniversalMediaLayer
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalNekoPalette
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.ui.theme.colorToHexString
import com.example.ui.theme.resolveEyeColor
import com.example.ui.theme.toAccentPalette
import com.example.ui.theme.toColor
import java.io.File

private val SUPPORTED_UNIVERSAL_MIMES = arrayOf(
    "image/*",
    "application/json",
    "application/zip",
    "application/octet-stream"
)

@Composable
fun StudioScreen(
    preferences: ClockPreferences,
    isSlicingCollage: Boolean,
    onImportCustomBackground: (Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    onSetCustomBackgroundAlpha: (Float) -> Unit,
    onSetBackgroundTransform: (Float, Float, Float, MediaFitMode) -> Unit,
    onImportCustomPanelArt: (Uri) -> Unit,
    onClearCustomPanelArt: () -> Unit,
    onSetPanelAlpha: (Float) -> Unit,
    onSetPanelTransform: (Float, Float, Float, MediaFitMode) -> Unit,
    onImportCustomEyeMedia: (Uri) -> Unit,
    onClearCustomEyeMedia: () -> Unit,
    onSetCustomEyeMediaAlpha: (Float) -> Unit,
    onSetCustomEyeMediaFitMode: (MediaFitMode) -> Unit,
    onSelectColorTheme: (HudColorTheme) -> Unit,
    onSetCustomHudPrimaryHex: (String?) -> Unit,
    onSetCustomHudSecondaryHex: (String?) -> Unit,
    onSelectDialStyle: (DialStyle) -> Unit,
    onToggleBiolumEyes: (Boolean) -> Unit,
    onSelectCanvasOverlayStyle: (CanvasOverlayStyle) -> Unit,
    onSelectIrisPupilStyle: (IrisPupilStyle) -> Unit,
    onSelectEyeColor: (EyeColorPreset) -> Unit,
    onSetCustomEyeColorHex: (String?) -> Unit,
    onSetEyeGlowIntensity: (Float) -> Unit,
    onSetEyeCanvasScale: (Float) -> Unit,
    onSetEyeVerticalOffset: (Float) -> Unit,
    onSetEyeHorizontalOffset: (Float) -> Unit,
    onToggleEyeWhiskers: (Boolean) -> Unit,
    onToggleEyeBrows: (Boolean) -> Unit,
    onToggleEyeBlink: (Boolean) -> Unit,
    onUpdateHudTextBoxes: (String, String, String, String, String, String, String, String) -> Unit,
    onToggleParticleStream: (Boolean) -> Unit,
    onToggleStarfield: (Boolean) -> Unit,
    onToggleAuroraNebula: (Boolean) -> Unit,
    onToggleAnamorphicFlare: (Boolean) -> Unit,
    onToggleGravityHorizon: (Boolean) -> Unit,
    onToggleCinematicScanlines: (Boolean) -> Unit,
    onTogglePerspectiveGrid3d: (Boolean) -> Unit,
    onToggleHoloPrism3d: (Boolean) -> Unit,
    onToggleNeonPulseRings: (Boolean) -> Unit,
    onToggleCyberMatrixRain: (Boolean) -> Unit,
    onToggleAtmosphericFog: (Boolean) -> Unit,
    onSetFxIntensity: (Float) -> Unit,
    onApplyFxPreset: (String) -> Unit,
    onAddCustomFxLayer: (String, FxCategory, String, Float, Float) -> Unit,
    onToggleCustomFxLayer: (String, Boolean) -> Unit,
    onDeleteCustomFxLayer: (String) -> Unit,
    onToggleBatteryOptimization: (Boolean) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onToggleSeconds: (Boolean) -> Unit,
    onToggleBeatPulse: (Boolean) -> Unit,
    onToggleSeoulClock: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNekoPalette.current
    val context = LocalContext.current
    val imageLoader = remember(context) { CustomMediaHelper.createMediaImageLoader(context) }
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val chipShape = remember { ChamferedHudShape(8.dp) }
    val eyeColor = remember(preferences.eyeColorPreset, preferences.customEyeColorHex) {
        resolveEyeColor(preferences.eyeColorPreset, preferences.customEyeColorHex)
    }
    val activeEyeHex = remember(preferences.eyeColorPreset, preferences.customEyeColorHex) {
        preferences.customEyeColorHex ?: preferences.eyeColorPreset.hex
    }

    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportCustomBackground(uri) }

    val panelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportCustomPanelArt(uri) }

    val eyePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportCustomEyeMedia(uri) }

    var brandTagInput by remember(preferences.hudBrandTag) { mutableStateOf(preferences.hudBrandTag) }
    var bannerPrimaryInput by remember(preferences.hudBannerPrimary) { mutableStateOf(preferences.hudBannerPrimary) }
    var bannerSecondaryInput by remember(preferences.hudBannerSecondary) { mutableStateOf(preferences.hudBannerSecondary) }
    var labelDayInput by remember(preferences.panelLabelDay) { mutableStateOf(preferences.panelLabelDay) }
    var labelHoursInput by remember(preferences.panelLabelHours) { mutableStateOf(preferences.panelLabelHours) }
    var labelMinutesInput by remember(preferences.panelLabelMinutes) { mutableStateOf(preferences.panelLabelMinutes) }
    var labelSecondsInput by remember(preferences.panelLabelSeconds) { mutableStateOf(preferences.panelLabelSeconds) }
    var labelDateInput by remember(preferences.panelLabelDate) { mutableStateOf(preferences.panelLabelDate) }

    val saveTexts = {
        onUpdateHudTextBoxes(
            brandTagInput,
            bannerPrimaryInput,
            bannerSecondaryInput,
            labelDayInput,
            labelHoursInput,
            labelMinutesInput,
            labelSecondsInput,
            labelDateInput
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.studio_title),
                style = MaterialTheme.typography.headlineSmall,
                color = InkCream
            )

            Spacer(modifier = Modifier.height(6.dp))

            CurvedSciFiHudBanner(
                primaryText = "ARQUITECTURA, MULTIMEDIA & TEXTOS HUD",
                secondaryText = "${stringResource(preferences.dialStyle.labelRes)} · ${preferences.colorTheme.hudCode}"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Dial Architecture: COLLAGER4 | PLASMA | HUD-4
            Text(
                text = stringResource(R.string.studio_dial_header),
                style = MaterialTheme.typography.titleMedium,
                color = palette.secondaryGlow
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DialStyle.entries.forEachIndexed { index, style ->
                    val selected = preferences.dialStyle == style
                    SegmentedButton(
                        selected = selected,
                        onClick = { onSelectDialStyle(style) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DialStyle.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = palette.primaryGlow,
                            activeContentColor = StageVoid,
                            inactiveContainerColor = StageCard.copy(alpha = 0.85f),
                            inactiveContentColor = InkCream,
                            activeBorderColor = palette.primaryGlow,
                            inactiveBorderColor = StageBorder
                        ),
                        modifier = Modifier.testTag("studio_dial_${style.id}")
                    ) {
                        Text(
                            text = stringResource(style.labelRes),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Eye Module (Vectorial Optical Eye OR Custom GIF / Lottie .json / .lottie / .riv / Image)
            Text(
                text = stringResource(R.string.studio_eyes_header),
                style = MaterialTheme.typography.titleMedium,
                color = eyeColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
                border = BorderStroke(1.dp, eyeColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Live COLLAGER4 proportional preview box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(166.dp)
                            .clip(chipShape)
                            .background(StageVoid.copy(alpha = 0.92f))
                            .border(1.dp, StageBorder, chipShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CollageImageSlicer.PANEL_HEIGHT_FRACTIONS.forEachIndexed { idx, hFrac ->
                                Box(
                                    modifier = Modifier
                                        .weight(CollageImageSlicer.PANEL_WEIGHTS[idx])
                                        .fillMaxHeight(hFrac)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(StageCard.copy(alpha = 0.5f))
                                        .border(0.6.dp, StageBorder.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        if (preferences.biolumEyesOverlayEnabled) {
                            EyeModuleOverlay(
                                preferences = preferences,
                                eyeColor = eyeColor,
                                secondaryGlow = palette.secondaryGlow,
                                beatPulse = 0.45f,
                                eyeScale = preferences.eyeCanvasScale,
                                collageProportionalMode = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "NÚCLEO CENTRAL DESACTIVADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PreferenceToggleRow(
                        title = stringResource(R.string.pref_eyes_title),
                        subtitle = stringResource(R.string.pref_eyes_desc),
                        checked = preferences.biolumEyesOverlayEnabled,
                        onCheckedChange = onToggleBiolumEyes,
                        testTag = "pref_switch_eyes"
                    )

                    // Custom Animation / Media replacement for the Eye module
                    GalleryMediaSlotCard(
                        title = stringResource(R.string.studio_gallery_eye_title),
                        mediaPath = preferences.customEyeMediaPath,
                        slicePaths = emptyList(),
                        transform = preferences.eyeMediaTransform,
                        alpha = preferences.customEyeMediaAlpha,
                        isProcessing = false,
                        imageLoader = imageLoader,
                        accentColor = eyeColor,
                        onPickClick = { eyePickerLauncher.launch(SUPPORTED_UNIVERSAL_MIMES) },
                        onClearClick = onClearCustomEyeMedia,
                        onAlphaChange = onSetCustomEyeMediaAlpha,
                        onTransformChange = { scale, offX, offY, fitMode ->
                            onSetEyeCanvasScale(scale)
                            onSetEyeHorizontalOffset(offX)
                            onSetEyeVerticalOffset(offY)
                            onSetCustomEyeMediaFitMode(fitMode)
                        },
                        pickTestTag = "pick_custom_eye_button",
                        clearTestTag = "clear_custom_eye_button"
                    )

                    if (preferences.customEyeMediaPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.studio_eyes_style),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(CanvasOverlayStyle.entries) { style ->
                                val selected = preferences.canvasOverlayStyle == style
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectCanvasOverlayStyle(style) },
                                    shape = chipShape,
                                    label = { Text(stringResource(style.labelRes)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = eyeColor.copy(alpha = 0.24f),
                                        selectedLabelColor = eyeColor,
                                        containerColor = StageVoid.copy(alpha = 0.7f),
                                        labelColor = InkCream
                                    ),
                                    modifier = Modifier.testTag("canvas_style_${style.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.studio_eyes_pupil),
                            style = MaterialTheme.typography.labelLarge,
                            color = InkMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(IrisPupilStyle.entries) { pupil ->
                                val selected = preferences.irisPupilStyle == pupil
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectIrisPupilStyle(pupil) },
                                    shape = chipShape,
                                    label = { Text(stringResource(pupil.labelRes)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.secondaryGlow.copy(alpha = 0.24f),
                                        selectedLabelColor = palette.secondaryGlow,
                                        containerColor = StageVoid.copy(alpha = 0.7f),
                                        labelColor = InkCream
                                    ),
                                    modifier = Modifier.testTag("pupil_style_${pupil.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(EyeColorPreset.entries) { preset ->
                                val presetColor = preset.toColor()
                                val selected = preferences.customEyeColorHex == null && preferences.eyeColorPreset == preset
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectEyeColor(preset) },
                                    shape = chipShape,
                                    label = { Text(stringResource(preset.labelRes)) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(presetColor)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = presetColor.copy(alpha = 0.24f),
                                        selectedLabelColor = presetColor,
                                        containerColor = StageVoid.copy(alpha = 0.7f),
                                        labelColor = InkCream
                                    ),
                                    modifier = Modifier.testTag("eye_color_${preset.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        CodexHexColorSelector(
                            title = stringResource(R.string.studio_eyes_color),
                            activeHex = activeEyeHex,
                            onColorHexSelected = { onSetCustomEyeColorHex(it) },
                            onResetToPreset = { onSetCustomEyeColorHex(null) },
                            testTagPrefix = "eye"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${stringResource(R.string.studio_eyes_intensity)} · ${(preferences.eyeGlowIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkMuted
                        )
                        Slider(
                            value = preferences.eyeGlowIntensity,
                            onValueChange = onSetEyeGlowIntensity,
                            valueRange = 0.35f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = eyeColor,
                                activeTrackColor = eyeColor
                            )
                        )

                        MediaResizeSliders(
                            accentColor = eyeColor,
                            transform = preferences.eyeMediaTransform,
                            showFitSelector = false,
                            onTransformChange = { scale, offX, offY, _ ->
                                onSetEyeCanvasScale(scale)
                                onSetEyeHorizontalOffset(offX)
                                onSetEyeVerticalOffset(offY)
                            }
                        )

                        PreferenceToggleRow(
                            title = stringResource(R.string.pref_eyes_whiskers_title),
                            subtitle = stringResource(R.string.pref_eyes_whiskers_desc),
                            checked = preferences.eyeWhiskersEnabled,
                            onCheckedChange = onToggleEyeWhiskers,
                            testTag = "pref_switch_whiskers"
                        )
                        PreferenceToggleRow(
                            title = stringResource(R.string.pref_eyes_brows_title),
                            subtitle = stringResource(R.string.pref_eyes_brows_desc),
                            checked = preferences.eyeBrowsEnabled,
                            onCheckedChange = onToggleEyeBrows,
                            testTag = "pref_switch_brows"
                        )
                        PreferenceToggleRow(
                            title = stringResource(R.string.pref_eyes_blink_title),
                            subtitle = stringResource(R.string.pref_eyes_blink_desc),
                            checked = preferences.eyeBlinkEnabled,
                            onCheckedChange = onToggleEyeBlink,
                            testTag = "pref_switch_blink"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Universal Multimedia + Resizing in ALL Fields
            Text(
                text = stringResource(R.string.studio_gallery_header),
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryGlow
            )
            Spacer(modifier = Modifier.height(8.dp))

            GalleryMediaSlotCard(
                title = stringResource(R.string.studio_gallery_bg_title),
                mediaPath = preferences.customBackgroundPath,
                slicePaths = emptyList(),
                transform = preferences.bgTransform,
                alpha = preferences.customBackgroundAlpha,
                isProcessing = false,
                imageLoader = imageLoader,
                accentColor = palette.secondaryGlow,
                onPickClick = { bgPickerLauncher.launch(SUPPORTED_UNIVERSAL_MIMES) },
                onClearClick = onClearCustomBackground,
                onAlphaChange = onSetCustomBackgroundAlpha,
                onTransformChange = onSetBackgroundTransform,
                pickTestTag = "pick_custom_bg_button",
                clearTestTag = "clear_custom_bg_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            GalleryMediaSlotCard(
                title = stringResource(R.string.studio_gallery_panel_title),
                mediaPath = preferences.customPanelArtPath,
                slicePaths = preferences.customPanelSlices,
                transform = preferences.panelTransform,
                alpha = preferences.panelAlpha,
                isProcessing = isSlicingCollage,
                imageLoader = imageLoader,
                accentColor = palette.primaryGlow,
                onPickClick = { panelPickerLauncher.launch(SUPPORTED_UNIVERSAL_MIMES) },
                onClearClick = onClearCustomPanelArt,
                onAlphaChange = onSetPanelAlpha,
                onTransformChange = onSetPanelTransform,
                pickTestTag = "pick_custom_panel_button",
                clearTestTag = "clear_custom_panel_button"
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Editable HUD Text Boxes Card
            Text(
                text = stringResource(R.string.studio_text_header),
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryGlow
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.84f)),
                border = BorderStroke(1.dp, StageBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brandTagInput,
                        onValueChange = { brandTagInput = it.take(18); saveTexts() },
                        label = { Text("Etiqueta corporativa superior") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_text_brand")
                    )
                    OutlinedTextField(
                        value = bannerPrimaryInput,
                        onValueChange = { bannerPrimaryInput = it.take(48); saveTexts() },
                        label = { Text("Título del banner HUD") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_text_banner_primary")
                    )
                    OutlinedTextField(
                        value = bannerSecondaryInput,
                        onValueChange = { bannerSecondaryInput = it.take(48); saveTexts() },
                        label = { Text("Subtítulo del banner HUD") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primaryGlow,
                            unfocusedBorderColor = StageBorder,
                            focusedTextColor = InkCream,
                            unfocusedTextColor = InkCream
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_text_banner_secondary")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = labelDayInput,
                            onValueChange = { labelDayInput = it.take(16); saveTexts() },
                            label = { Text("Etiqueta 1") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = labelHoursInput,
                            onValueChange = { labelHoursInput = it.take(16); saveTexts() },
                            label = { Text("Etiqueta 2") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = labelMinutesInput,
                            onValueChange = { labelMinutesInput = it.take(16); saveTexts() },
                            label = { Text("Etiqueta 3") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = labelSecondsInput,
                            onValueChange = { labelSecondsInput = it.take(16); saveTexts() },
                            label = { Text("Etiqueta 4") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = labelDateInput,
                            onValueChange = { labelDateInput = it.take(16); saveTexts() },
                            label = { Text("Etiqueta 5") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primaryGlow,
                                unfocusedBorderColor = StageBorder,
                                focusedTextColor = InkCream,
                                unfocusedTextColor = InkCream
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Atmospheric Color Theme + Codex & HEX HUD Override
            Text(
                text = stringResource(R.string.studio_theme_header),
                style = MaterialTheme.typography.titleMedium,
                color = palette.secondaryGlow
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(HudColorTheme.entries) { theme ->
                    val themePalette = theme.toAccentPalette()
                    val selected = preferences.colorTheme == theme &&
                        preferences.customHudPrimaryHex == null &&
                        preferences.customHudSecondaryHex == null
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectColorTheme(theme) },
                        shape = chipShape,
                        label = { Text(stringResource(theme.nameRes)) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(themePalette.primaryGlow)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themePalette.primaryGlow,
                            selectedLabelColor = StageVoid,
                            containerColor = StageCard.copy(alpha = 0.84f),
                            labelColor = InkCream
                        ),
                        modifier = Modifier.testTag("theme_chip_${theme.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.82f)),
                border = BorderStroke(1.dp, StageBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CodexHexColorSelector(
                        title = "Color Primario HUD",
                        activeHex = preferences.customHudPrimaryHex ?: colorToHexString(palette.primaryGlow),
                        onColorHexSelected = { onSetCustomHudPrimaryHex(it) },
                        onResetToPreset = { onSetCustomHudPrimaryHex(null) },
                        testTagPrefix = "hud_primary"
                    )

                    CodexHexColorSelector(
                        title = "Color Secundario HUD",
                        activeHex = preferences.customHudSecondaryHex ?: colorToHexString(palette.secondaryGlow),
                        onColorHexSelected = { onSetCustomHudSecondaryHex(it) },
                        onResetToPreset = { onSetCustomHudSecondaryHex(null) },
                        testTagPrefix = "hud_secondary"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Dedicated Apartado FX (Particles, Atmosphere, 3D, Neon EFX & User Custom FX)
            FxStudioSection(
                preferences = preferences,
                onSetFxIntensity = onSetFxIntensity,
                onApplyFxPreset = onApplyFxPreset,
                onToggleParticleStream = onToggleParticleStream,
                onTogglePerspectiveGrid3d = onTogglePerspectiveGrid3d,
                onToggleHoloPrism3d = onToggleHoloPrism3d,
                onToggleNeonPulseRings = onToggleNeonPulseRings,
                onToggleCyberMatrixRain = onToggleCyberMatrixRain,
                onToggleAtmosphericFog = onToggleAtmosphericFog,
                onToggleStarfield = onToggleStarfield,
                onToggleAuroraNebula = onToggleAuroraNebula,
                onToggleAnamorphicFlare = onToggleAnamorphicFlare,
                onToggleGravityHorizon = onToggleGravityHorizon,
                onToggleCinematicScanlines = onToggleCinematicScanlines,
                onAddCustomFxLayer = onAddCustomFxLayer,
                onToggleCustomFxLayer = onToggleCustomFxLayer,
                onDeleteCustomFxLayer = onDeleteCustomFxLayer
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 7. Battery Optimization & Clock Parameters
            Text(
                text = stringResource(R.string.studio_prefs_header),
                style = MaterialTheme.typography.titleMedium,
                color = palette.secondaryGlow
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = hudShape,
                colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.82f)),
                border = BorderStroke(1.dp, StageBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PreferenceToggleRow(
                        title = "Optimización de batería y respuesta",
                        subtitle = "Pausa ciclos en segundo plano y adapta la tasa de refresco según pestaña",
                        checked = preferences.batteryOptimizationEnabled,
                        onCheckedChange = onToggleBatteryOptimization,
                        testTag = "pref_switch_battery_opt"
                    )
                    PreferenceToggleRow(
                        title = stringResource(R.string.pref_24h_title),
                        subtitle = stringResource(R.string.pref_24h_desc),
                        checked = preferences.use24Hour,
                        onCheckedChange = onToggle24Hour,
                        testTag = "pref_switch_24h"
                    )
                    PreferenceToggleRow(
                        title = stringResource(R.string.pref_seconds_title),
                        subtitle = stringResource(R.string.pref_seconds_desc),
                        checked = preferences.showSeconds,
                        onCheckedChange = onToggleSeconds,
                        testTag = "pref_switch_seconds"
                    )
                    PreferenceToggleRow(
                        title = stringResource(R.string.pref_beat_title),
                        subtitle = stringResource(R.string.pref_beat_desc),
                        checked = preferences.beatPulseEnabled,
                        onCheckedChange = onToggleBeatPulse,
                        testTag = "pref_switch_beat"
                    )
                    PreferenceToggleRow(
                        title = stringResource(R.string.pref_seoul_title),
                        subtitle = stringResource(R.string.pref_seoul_desc),
                        checked = preferences.showSeoulClock,
                        onCheckedChange = onToggleSeoulClock,
                        testTag = "pref_switch_seoul"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun GalleryMediaSlotCard(
    title: String,
    mediaPath: String?,
    slicePaths: List<String>,
    transform: MediaTransform,
    alpha: Float,
    isProcessing: Boolean,
    imageLoader: coil.ImageLoader,
    accentColor: Color,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
    onAlphaChange: (Float) -> Unit,
    onTransformChange: (scale: Float, offsetX: Float, offsetY: Float, fitMode: MediaFitMode) -> Unit,
    pickTestTag: String,
    clearTestTag: String
) {
    val context = LocalContext.current
    val hudShape = remember { ChamferedHudShape(14.dp) }
    val thumbShape = remember { ChamferedHudShape(8.dp) }
    val formatKind = remember(mediaPath) { MediaFormatKind.fromPath(mediaPath) }

    Card(
        shape = hudShape,
        colors = CardDefaults.cardColors(containerColor = StageCard.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, if (mediaPath != null) accentColor else StageBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(thumbShape)
                        .background(StageVoid)
                        .border(1.dp, accentColor.copy(alpha = 0.6f), thumbShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    } else if (!mediaPath.isNullOrBlank()) {
                        if (formatKind == MediaFormatKind.LOTTIE_JSON ||
                            formatKind == MediaFormatKind.DOT_LOTTIE ||
                            formatKind == MediaFormatKind.RIVE_ANIM
                        ) {
                            UniversalMediaLayer(
                                mediaPath = mediaPath,
                                transform = MediaTransform(fitMode = MediaFitMode.FIT),
                                alpha = 1f,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(mediaPath))
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (pickTestTag.contains("eye")) Icons.Filled.Animation else Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkCream
                    )
                    Text(
                        text = when {
                            isProcessing -> "Segmentando en 5 marcos…"
                            slicePaths.size == 5 -> stringResource(R.string.studio_gallery_sliced_ok)
                            !mediaPath.isNullOrBlank() -> "${stringResource(R.string.studio_gallery_active)} · ${formatKind.name}"
                            else -> stringResource(R.string.studio_gallery_none)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!mediaPath.isNullOrBlank()) accentColor else InkMuted
                    )
                }
            }

            if (slicePaths.size == 5) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slicePaths.forEachIndexed { idx, sliceFile ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(sliceFile))
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = "Marco ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(CollageImageSlicer.PANEL_WEIGHTS[idx])
                                .fillMaxHeight(CollageImageSlicer.PANEL_HEIGHT_FRACTIONS[idx])
                                .clip(RoundedCornerShape(4.dp))
                                .border(0.8.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPickClick,
                    shape = thumbShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = StageVoid
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(pickTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.studio_gallery_pick))
                }

                if (!mediaPath.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onClearClick,
                        shape = thumbShape,
                        border = BorderStroke(1.dp, StageBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkCream),
                        modifier = Modifier.testTag(clearTestTag)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.studio_gallery_clear))
                    }
                }
            }

            // Resize, Fit Mode, Offset X/Y & Opacity controls in ALL active fields!
            if (!mediaPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${stringResource(R.string.studio_gallery_opacity)} · ${(alpha * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted
                )
                Slider(
                    value = alpha,
                    onValueChange = onAlphaChange,
                    valueRange = 0.15f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    )
                )
                MediaResizeSliders(
                    accentColor = accentColor,
                    transform = transform,
                    showFitSelector = true,
                    onTransformChange = onTransformChange
                )
            }
        }
    }
}

@Composable
private fun MediaResizeSliders(
    accentColor: Color,
    transform: MediaTransform,
    showFitSelector: Boolean,
    onTransformChange: (scale: Float, offsetX: Float, offsetY: Float, fitMode: MediaFitMode) -> Unit
) {
    val chipShape = remember { ChamferedHudShape(6.dp) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showFitSelector) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Encuadre", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                TextButton(
                    onClick = { onTransformChange(1.0f, 0.0f, 0.0f, MediaFitMode.CROP) }
                ) {
                    Text("Centrar 100%", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = accentColor)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(MediaFitMode.entries) { mode ->
                    FilterChip(
                        selected = transform.fitMode == mode,
                        onClick = { onTransformChange(transform.scale, transform.offsetX, transform.offsetY, mode) },
                        shape = chipShape,
                        label = { Text(mode.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.24f),
                            selectedLabelColor = accentColor,
                            containerColor = StageVoid,
                            labelColor = InkCream
                        )
                    )
                }
            }
        }

        Text(
            text = "Redimensión (Escala) · ${(transform.scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted
        )
        Slider(
            value = transform.scale,
            onValueChange = { onTransformChange(it, transform.offsetX, transform.offsetY, transform.fitMode) },
            valueRange = 0.4f..2.2f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Posición X · ${(transform.offsetX * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
                Slider(
                    value = transform.offsetX,
                    onValueChange = { onTransformChange(transform.scale, it, transform.offsetY, transform.fitMode) },
                    valueRange = -0.45f..0.45f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Posición Y · ${(transform.offsetY * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
                Slider(
                    value = transform.offsetY,
                    onValueChange = { onTransformChange(transform.scale, transform.offsetX, it, transform.fitMode) },
                    valueRange = -0.45f..0.45f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkCream
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted
            )
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
