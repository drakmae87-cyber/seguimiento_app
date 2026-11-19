package com.example

import com.example.alarm.AlarmScheduler
import com.example.data.AiConnectorRepository
import com.example.data.CollageImageSlicer
import com.example.data.EventCategory
import com.example.data.MediaDiscoveryRepository
import com.example.data.MediaFormatKind
import com.example.model.AiBackendMode
import com.example.model.AlarmItem
import com.example.model.AlarmRepeatUnit
import com.example.model.AppTab
import com.example.model.CanvasOverlayStyle
import com.example.model.DialStyle
import com.example.model.EqualizerPreset
import com.example.model.ExternalApiFormat
import com.example.model.EyeColorPreset
import com.example.model.FxCategory
import com.example.model.HudColorCodex
import com.example.model.HudColorTheme
import com.example.model.IrisPupilStyle
import com.example.model.LocalEdgeFormat
import com.example.model.TelemetryPushCategory
import com.example.model.TelemetryTimeFormatter
import com.example.model.VideoOrientationFormat
import com.example.model.WorldCities
import com.example.ui.screens.parseQuickDateDdMmAa
import com.example.ui.theme.colorToHexString
import com.example.ui.theme.parseHexColorOrNull
import com.example.viewmodel.ClockFormatters
import com.example.viewmodel.TimerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {
    @Test
    fun formatTimerCountdown_formatsMinutesAndSeconds() {
        assertEquals("25:00", ClockFormatters.formatTimerCountdown(25 * 60_000L))
        assertEquals("00:05", ClockFormatters.formatTimerCountdown(4_200L))
        assertEquals("00:00", ClockFormatters.formatTimerCountdown(0L))
    }

    @Test
    fun formatStopwatch_formatsCentiseconds() {
        assertEquals("00:00.00", ClockFormatters.formatStopwatch(0L))
        assertEquals("01:05.42", ClockFormatters.formatStopwatch(65_420L))
    }

    @Test
    fun timerProgress_clampsWithinUnitInterval() {
        val full = TimerUiState(totalMillis = 60_000L, remainingMillis = 60_000L)
        val half = TimerUiState(totalMillis = 60_000L, remainingMillis = 30_000L)
        assertEquals(1f, full.progress, 0.001f)
        assertEquals(0.5f, half.progress, 0.001f)
    }

    @Test
    fun modelLookups_fallbackGracefullyAndExcludeRemovedEyeStyles() {
        assertEquals(HudColorTheme.INTERSTELLAR, HudColorTheme.fromId("unknown"))
        assertEquals(DialStyle.COLLAGER4, DialStyle.fromId("unknown"))
        assertEquals(EyeColorPreset.EMERALD, EyeColorPreset.fromId("unknown"))
        assertEquals(CanvasOverlayStyle.CELESTIAL_CORE, CanvasOverlayStyle.fromId("unknown"))
        assertEquals(CanvasOverlayStyle.HUD_RETICLE, CanvasOverlayStyle.fromId("feline_eyes"))
        assertEquals(CanvasOverlayStyle.HUD_RETICLE, CanvasOverlayStyle.fromId("cyber_visor"))
        assertEquals(CanvasOverlayStyle.TENSOR_PRISM, CanvasOverlayStyle.fromId("tensor_prism"))
        assertEquals(3, CanvasOverlayStyle.entries.size)
        assertEquals(IrisPupilStyle.CROSSHAIR, IrisPupilStyle.fromId("unknown"))
        assertEquals(AiBackendMode.LOCAL_EDGE, AiBackendMode.fromId("unknown"))
        assertEquals(LocalEdgeFormat.OPENAI_COMPAT, LocalEdgeFormat.fromId("unknown"))
        assertEquals(ExternalApiFormat.OPENAI_COMPAT, ExternalApiFormat.fromId("unknown"))
        assertEquals(EventCategory.OPERACIONES, EventCategory.fromId("unknown"))
        assertEquals(FxCategory.PARTICLES, FxCategory.fromId("unknown"))
        assertEquals("Seúl", WorldCities.byId("seoul").name)
        assertEquals(6, AppTab.entries.size)
        assertTrue(ClockFormatters.formatMainTime(0L, true, TimeZone.getTimeZone("UTC")).contains(":"))
    }

    @Test
    fun telemetryTimeFormatter_formatsRemainingHhMmSsExactly() {
        val midnight = 1_000_000_000_000L
        val twoAm = midnight + (2 * 3600_000L)
        assertEquals("02:00:00", TelemetryTimeFormatter.formatRemainingHhMmSs(twoAm, midnight))
        assertEquals("01:05:09", TelemetryTimeFormatter.formatRemainingHhMmSs(3909_000L, 0L))
        assertEquals("00:00:00", TelemetryTimeFormatter.formatRemainingHhMmSs(midnight, twoAm))

        val alarm = AlarmItem(id = "a_sleep", hour = 2, minute = 0, label = "Turno Nocturno")
        val log = TelemetryTimeFormatter.buildAlarmActivationLog(alarm, twoAm, midnight)
        assertEquals(TelemetryPushCategory.ALARM, log.category)
        assertEquals("02:00:00", log.countdownHhMmSs)
        assertTrue(log.message.contains("02:00:00"))
    }

    @Test
    fun mediaHelpers_extractYoutubeIdsAndParseLrcLyrics() {
        assertEquals(
            "dQw4w9WgXcQ",
            MediaDiscoveryRepository.extractYoutubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            MediaDiscoveryRepository.extractYoutubeVideoId("https://youtu.be/dQw4w9WgXcQ?t=12")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            MediaDiscoveryRepository.extractYoutubeVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ")
        )

        val lrc = """
            [00:12.50] Primera línea de telemetría
            [01:05.00] Segunda línea sincronizada
        """.trimIndent()
        val parsed = MediaDiscoveryRepository.parseLrcLyrics(lrc)
        assertEquals(2, parsed.size)
        assertEquals(12_500L, parsed[0].timeMs)
        assertEquals("Primera línea de telemetría", parsed[0].text)
        assertEquals(65_000L, parsed[1].timeMs)

        assertTrue(VideoOrientationFormat.HORIZONTAL_16_9.isLandscape)
        assertFalse(VideoOrientationFormat.VERTICAL_9_16.isLandscape)
        assertEquals(EqualizerPreset.SPATIAL_3D, EqualizerPreset.fromId("spatial"))
    }

    @Test
    fun splitPanelFormatters_produceTwoDigitAndShortLabels() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("00", ClockFormatters.formatHourTwoDigits(0L, true, utc))
        assertEquals("00", ClockFormatters.formatMinuteTwoDigits(0L, utc))
        assertEquals("00", ClockFormatters.formatSeconds(0L, utc))
        assertEquals("01", ClockFormatters.formatDayNumber(0L, utc))
        assertEquals("70", ClockFormatters.formatYearShort(0L, utc))
        assertTrue(ClockFormatters.formatDayShortEs(0L, utc).isNotEmpty())
        assertTrue(ClockFormatters.formatMonthShortEs(0L, utc).isNotEmpty())
    }

    @Test
    fun collageImageSlicer_computesFiveDistinctStaggeredRegions() {
        val regions = CollageImageSlicer.computePanelRegions()
        assertEquals(5, regions.size)

        for (i in 1 until regions.size) {
            assertTrue(regions[i].xStartFrac > regions[i - 1].xStartFrac + regions[i - 1].widthFrac)
        }
        assertEquals(1.0f, regions[2].heightFrac, 0.001f)
        assertEquals(0.0f, regions[2].yStartFrac, 0.001f)
        assertTrue(regions[2].widthFrac > regions[0].widthFrac)
        assertEquals(regions[0].heightFrac, regions[4].heightFrac, 0.001f)
        assertEquals(regions[1].heightFrac, regions[3].heightFrac, 0.001f)
    }

    @Test
    fun hexColorParsing_handlesCodexAndCustomHexValues() {
        val parsed = parseHexColorOrNull("#00FFA3")
        assertNotNull(parsed)
        assertEquals("#00FFA3", colorToHexString(parsed!!))

        assertNotNull(parseHexColorOrNull("FF2E93"))
        assertNull(parseHexColorOrNull("invalid"))
        assertNull(parseHexColorOrNull("#12"))

        assertTrue(HudColorCodex.tokens.size >= 10)
        HudColorCodex.tokens.forEach { token ->
            assertNotNull(parseHexColorOrNull(token.hex))
        }
    }

    @Test
    fun mediaFormatKind_detectsUniversalFormats() {
        assertEquals(MediaFormatKind.ANIMATED_GIF, MediaFormatKind.fromPath("/tmp/eye_1.gif"))
        assertEquals(MediaFormatKind.VECTOR_SVG, MediaFormatKind.fromPath("/tmp/logo.SVG"))
        assertEquals(MediaFormatKind.LOTTIE_JSON, MediaFormatKind.fromPath("/tmp/anim.json"))
        assertEquals(MediaFormatKind.DOT_LOTTIE, MediaFormatKind.fromPath("/tmp/anim.lottie"))
        assertEquals(MediaFormatKind.RIVE_ANIM, MediaFormatKind.fromPath("/tmp/hud.riv"))
        assertEquals(MediaFormatKind.RASTER_IMAGE, MediaFormatKind.fromPath("/tmp/bg.png"))
    }

    @Test
    fun parseQuickDateDdMmAa_validatesDatesProperly() {
        assertEquals(Triple(2026, 2, 25), parseQuickDateDdMmAa("25", "03", "26"))
        assertEquals(Triple(2028, 1, 29), parseQuickDateDdMmAa("29", "02", "2028"))
        assertNull(parseQuickDateDdMmAa("31", "02", "26"))
        assertNull(parseQuickDateDdMmAa("15", "13", "26"))
        assertNull(parseQuickDateDdMmAa("", "05", "26"))
    }

    @Test
    fun alarmScheduler_computesFutureTriggerAndIntervals() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val laterToday = AlarmItem(id = "a1", hour = 9, minute = 30, label = "Reunión")
        val triggerLater = AlarmScheduler.computeNextTriggerAtMillis(laterToday, now)
        assertTrue(triggerLater > now)
        assertEquals(90 * 60_000L, triggerLater - now)

        val intervalAlarm = AlarmItem(
            id = "a2",
            hour = 7,
            minute = 0,
            label = "Intervalo",
            repeatIntervalCount = 30,
            repeatIntervalUnit = AlarmRepeatUnit.MINUTES
        )
        val intervalNext = AlarmScheduler.computeRecurringNextAfterFire(intervalAlarm, now)
        assertEquals(now + 30 * 60_000L, intervalNext)
    }

    @Test
    fun aiRepository_buildsValidLottieTelemetryJson() {
        val repo = AiConnectorRepository()
        val json = repo.buildProceduralTelemetryLottieJson("#00E5FF", "#00FFA3", "#FF2E93", "scanner_reticle")
        assertTrue(json.contains("\"v\":\"5.9.0\""))
        assertTrue(json.contains("\"layers\""))
        assertTrue(json.contains("outer_reticle"))
    }
}
