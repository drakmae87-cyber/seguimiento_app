package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HudColorCodex
import com.example.ui.theme.InkCream
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StageBorder
import com.example.ui.theme.StageCard
import com.example.ui.theme.StageVoid
import com.example.ui.theme.colorToHexString
import com.example.ui.theme.parseHexColorOrNull
import android.graphics.Color as AndroidColor

@Composable
fun CodexHexColorSelector(
    title: String,
    activeHex: String,
    onColorHexSelected: (String) -> Unit,
    onResetToPreset: (() -> Unit)? = null,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val activeColor = remember(activeHex) {
        parseHexColorOrNull(activeHex) ?: Color(0xFF00FFA3)
    }

    val hsvArray = remember { FloatArray(3) }
    var hue by remember { mutableFloatStateOf(160f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    var hexInput by remember { mutableStateOf(activeHex.uppercase()) }

    LaunchedEffect(activeHex) {
        hexInput = activeHex.uppercase()
        AndroidColor.colorToHSV(activeColor.toArgb(), hsvArray)
        hue = hsvArray[0]
        saturation = hsvArray[1]
        value = hsvArray[2]
    }

    val chipShape = remember { ChamferedHudShape(8.dp) }
    val parsedInput = parseHexColorOrNull(hexInput)
    val isHexValid = parsedInput != null

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkCream
                )
            }
            Text(
                text = activeHex.uppercase(),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = activeColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Codex Tokens Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(HudColorCodex.tokens) { token ->
                val tokenColor = parseHexColorOrNull(token.hex) ?: activeColor
                val isSelected = activeHex.equals(token.hex, ignoreCase = true)
                Surface(
                    onClick = { onColorHexSelected(token.hex) },
                    shape = chipShape,
                    color = if (isSelected) tokenColor.copy(alpha = 0.22f) else StageVoid.copy(alpha = 0.72f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) tokenColor else StageBorder
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_codex_${token.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(tokenColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = token.code,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isSelected) tokenColor else InkCream
                            )
                            Text(
                                text = token.hex,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = InkMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hue Spectrum Strip + Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                )
        )
        Slider(
            value = hue,
            onValueChange = { newHue ->
                hue = newHue
                val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation.coerceAtLeast(0.25f), value.coerceAtLeast(0.35f)))
                onColorHexSelected(colorToHexString(Color(colorInt)))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor.copy(alpha = 0.45f),
                inactiveTrackColor = StageBorder.copy(alpha = 0.35f)
            ),
            modifier = Modifier.testTag("${testTagPrefix}_hue_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saturación ${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
                Slider(
                    value = saturation,
                    onValueChange = { newSat ->
                        saturation = newSat
                        val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
                        onColorHexSelected(colorToHexString(Color(colorInt)))
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = activeColor,
                        activeTrackColor = activeColor
                    )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Brillo ${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
                Slider(
                    value = value,
                    onValueChange = { newVal ->
                        value = newVal
                        val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
                        onColorHexSelected(colorToHexString(Color(colorInt)))
                    },
                    valueRange = 0.25f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = activeColor,
                        activeTrackColor = activeColor
                    )
                )
            }
        }

        // Direct HEX Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    val cleaned = raw.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }.take(9)
                    hexInput = cleaned
                },
                label = { Text("Código HEX (#RRGGBB)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = parsedInput ?: InkMuted
                    )
                },
                singleLine = true,
                isError = hexInput.isNotBlank() && !isHexValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeColor,
                    unfocusedBorderColor = StageBorder,
                    focusedTextColor = InkCream,
                    unfocusedTextColor = InkCream,
                    focusedLabelColor = activeColor,
                    unfocusedLabelColor = InkMuted
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_hex_input")
            )

            Button(
                onClick = {
                    parsedInput?.let { color ->
                        onColorHexSelected(colorToHexString(color))
                    }
                },
                enabled = isHexValid,
                shape = chipShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = StageVoid
                ),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("${testTagPrefix}_hex_apply")
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Aplicar HEX",
                    modifier = Modifier.size(18.dp)
                )
            }

            if (onResetToPreset != null) {
                OutlinedButton(
                    onClick = onResetToPreset,
                    shape = chipShape,
                    border = BorderStroke(1.dp, StageBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMuted),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("${testTagPrefix}_reset")
                ) {
                    Text("Auto", fontSize = 11.sp)
                }
            }
        }
    }
}
