package com.example.data

import android.util.Log
import com.example.model.DownloadFormatKind
import com.example.model.DownloadFormatOption
import com.example.model.LyricLine
import com.example.model.MediaSourceType
import com.example.model.MediaTrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "MediaDiscoveryRepo"

class MediaDiscoveryRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun searchMedia(source: MediaSourceType, rawQuery: String): List<MediaTrackItem> =
        withContext(Dispatchers.IO) {
            val query = rawQuery.trim()
            if (query.isEmpty()) return@withContext emptyList()

            // Direct URL paste support (YouTube links or direct .mp4/.mp3/.m4a/.webm URLs)
            if (query.startsWith("http://", ignoreCase = true) || query.startsWith("https://", ignoreCase = true)) {
                val ytId = extractYoutubeVideoId(query)
                if (ytId != null) {
                    return@withContext listOf(
                        MediaTrackItem(
                            id = "yt_$ytId",
                            title = "Video / Audio YouTube ($ytId)",
                            artist = "YouTube",
                            sourceType = if (query.contains("music.youtube")) MediaSourceType.YOUTUBE_MUSIC else MediaSourceType.YOUTUBE,
                            pageUrl = "https://www.youtube.com/watch?v=$ytId",
                            videoId = ytId,
                            thumbnailUrl = "https://i.ytimg.com/vi/$ytId/hqdefault.jpg",
                            isVideo = !query.contains("music.youtube")
                        )
                    )
                }
                val isVid = query.contains(".mp4", ignoreCase = true) ||
                    query.contains(".webm", ignoreCase = true) ||
                    query.contains(".mkv", ignoreCase = true) ||
                    query.contains(".m3u8", ignoreCase = true)
                val name = query.substringAfterLast('/').substringBefore('?').ifBlank { "Transmisión Directa" }
                return@withContext listOf(
                    MediaTrackItem(
                        id = UUID.randomUUID().toString(),
                        title = name,
                        artist = "URL Externa",
                        sourceType = source,
                        streamUrl = query,
                        pageUrl = query,
                        isVideo = isVid,
                        mimeType = if (isVid) "video/mp4" else "audio/mpeg"
                    )
                )
            }

            when (source) {
                MediaSourceType.YOUTUBE -> searchYouTube(query, musicOnly = false)
                MediaSourceType.YOUTUBE_MUSIC -> {
                    val ytMusic = searchYouTubeMusic(query)
                    if (ytMusic.isNotEmpty()) ytMusic else searchITunesAndPiped(query, MediaSourceType.YOUTUBE_MUSIC)
                }
                MediaSourceType.GOOGLE -> searchGoogleAudioVideo(query)
                MediaSourceType.LOCAL -> emptyList()
            }
        }

    private fun searchYouTube(query: String, musicOnly: Boolean): List<MediaTrackItem> {
        val innertube = runCatching { queryYoutubeInnertubeSearch(query, musicOnly) }.getOrDefault(emptyList())
        if (innertube.isNotEmpty()) return innertube

        val pipedFilter = if (musicOnly) "music_songs" else "videos"
        val mirrors = listOf("https://pipedapi.kavin.rocks", "https://api.piped.private.coffee")
        for (base in mirrors) {
            val piped = runCatching { queryPipedSearch(base, query, pipedFilter, musicOnly) }.getOrDefault(emptyList())
            if (piped.isNotEmpty()) return piped
        }
        return emptyList()
    }

    private fun searchYouTubeMusic(query: String): List<MediaTrackItem> {
        val remix = runCatching {
            val payload = JSONObject().apply {
                put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject()
                            .put("clientName", "WEB_REMIX")
                            .put("clientVersion", "1.20241127.01.00")
                            .put("hl", "es")
                            .put("gl", "ES")
                    )
                )
                put("query", query)
                put("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV")
            }
            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://music.youtube.com")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching emptyList()
                val raw = resp.body?.string().orEmpty()
                parseYtMusicItems(raw)
            }
        }.getOrDefault(emptyList())

        if (remix.isNotEmpty()) return remix
        return searchYouTube(query, musicOnly = true)
    }

    private fun searchITunesAndPiped(query: String, source: MediaSourceType): List<MediaTrackItem> {
        val piped = searchYouTube(query, musicOnly = true)
        if (piped.isNotEmpty()) return piped
        return queryITunesSearch(query, source)
    }

    private fun searchGoogleAudioVideo(query: String): List<MediaTrackItem> {
        val results = mutableListOf<MediaTrackItem>()
        results.addAll(runCatching { queryITunesSearch(query, MediaSourceType.GOOGLE) }.getOrDefault(emptyList()))
        val ytExtra = runCatching { searchYouTube(query, musicOnly = false).take(6) }.getOrDefault(emptyList())
        ytExtra.forEach { item ->
            results.add(item.copy(sourceType = MediaSourceType.GOOGLE))
        }
        return results
    }

    private fun queryYoutubeInnertubeSearch(query: String, musicOnly: Boolean): List<MediaTrackItem> {
        val payload = JSONObject().apply {
            put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB")
                        .put("clientVersion", "2.20241126.01.00")
                        .put("hl", "es")
                )
            )
            put("query", if (musicOnly) "$query audio" else query)
        }
        val req = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string().orEmpty()
            val list = mutableListOf<MediaTrackItem>()
            collectVideoRenderers(JSONObject(body), list, musicOnly)
            return list.distinctBy { it.videoId }.take(18)
        }
    }

    private fun collectVideoRenderers(node: Any?, out: MutableList<MediaTrackItem>, musicOnly: Boolean) {
        if (out.size >= 18) return
        when (node) {
            is JSONObject -> {
                if (node.has("videoRenderer")) {
                    val vr = node.getJSONObject("videoRenderer")
                    val vid = vr.optString("videoId")
                    if (vid.isNotBlank()) {
                        val title = extractRunsText(vr.optJSONObject("title"))
                        val author = extractRunsText(vr.optJSONObject("ownerText"))
                            .ifBlank { extractRunsText(vr.optJSONObject("longBylineText")) }
                            .ifBlank { "YouTube" }
                        val durStr = vr.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
                        val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val thumb = thumbs?.optJSONObject((thumbs.length() - 1).coerceAtLeast(0))?.optString("url")
                            ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                        if (title.isNotBlank()) {
                            out.add(
                                MediaTrackItem(
                                    id = "yt_$vid",
                                    title = title,
                                    artist = author,
                                    durationMs = parseDurationTextToMs(durStr),
                                    sourceType = if (musicOnly) MediaSourceType.YOUTUBE_MUSIC else MediaSourceType.YOUTUBE,
                                    pageUrl = if (musicOnly) "https://music.youtube.com/watch?v=$vid" else "https://www.youtube.com/watch?v=$vid",
                                    videoId = vid,
                                    thumbnailUrl = thumb,
                                    isVideo = !musicOnly
                                )
                            )
                        }
                    }
                } else {
                    val keys = node.keys()
                    while (keys.hasNext()) {
                        collectVideoRenderers(node.opt(keys.next()), out, musicOnly)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectVideoRenderers(node.opt(i), out, musicOnly)
                }
            }
        }
    }

    private fun parseYtMusicItems(raw: String): List<MediaTrackItem> {
        val out = mutableListOf<MediaTrackItem>()
        fun walk(node: Any?) {
            if (out.size >= 18) return
            when (node) {
                is JSONObject -> {
                    if (node.has("musicResponsiveListItemRenderer")) {
                        val item = node.getJSONObject("musicResponsiveListItemRenderer")
                        val vid = item.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
                        if (vid.isNotBlank()) {
                            val cols = item.optJSONArray("flexColumns")
                            val title = cols?.optJSONObject(0)
                                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")
                                ?.let { extractRunsText(it) }
                                .orEmpty()
                            val artist = cols?.optJSONObject(1)
                                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")
                                ?.let { extractRunsText(it) }
                                .orEmpty()
                                .substringBefore(" •")
                                .ifBlank { "YouTube Music" }
                            val thumbs = item.optJSONObject("thumbnail")
                                ?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")
                                ?.optJSONArray("thumbnails")
                            val thumb = thumbs?.optJSONObject((thumbs.length() - 1).coerceAtLeast(0))?.optString("url")
                                ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                            if (title.isNotBlank()) {
                                out.add(
                                    MediaTrackItem(
                                        id = "ytm_$vid",
                                        title = title,
                                        artist = artist,
                                        sourceType = MediaSourceType.YOUTUBE_MUSIC,
                                        pageUrl = "https://music.youtube.com/watch?v=$vid",
                                        videoId = vid,
                                        thumbnailUrl = thumb,
                                        isVideo = false
                                    )
                                )
                            }
                        }
                    } else {
                        val keys = node.keys()
                        while (keys.hasNext()) walk(node.opt(keys.next()))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) walk(node.opt(i))
                }
            }
        }
        walk(JSONObject(raw))
        return out.distinctBy { it.videoId }
    }

    private fun queryPipedSearch(
        baseUrl: String,
        query: String,
        filter: String,
        musicOnly: Boolean
    ): List<MediaTrackItem> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val req = Request.Builder()
            .url("$baseUrl/search?q=$enc&filter=$filter")
            .header("User-Agent", "RGEN_time/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val obj = JSONObject(resp.body?.string().orEmpty())
            val items = obj.optJSONArray("items") ?: return emptyList()
            return buildList {
                for (i in 0 until items.length()) {
                    val it = items.optJSONObject(i) ?: continue
                    val urlPart = it.optString("url")
                    val vid = extractYoutubeVideoId("https://www.youtube.com$urlPart") ?: continue
                    val title = it.optString("title").ifBlank { continue }
                    add(
                        MediaTrackItem(
                            id = "piped_$vid",
                            title = title,
                            artist = it.optString("uploaderName", "YouTube"),
                            durationMs = it.optLong("duration", 0L).coerceAtLeast(0L) * 1000L,
                            sourceType = if (musicOnly) MediaSourceType.YOUTUBE_MUSIC else MediaSourceType.YOUTUBE,
                            pageUrl = "https://www.youtube.com/watch?v=$vid",
                            videoId = vid,
                            thumbnailUrl = it.optString("thumbnail").ifBlank { "https://i.ytimg.com/vi/$vid/hqdefault.jpg" },
                            isVideo = !musicOnly
                        )
                    )
                }
            }
        }
    }

    private fun queryITunesSearch(query: String, source: MediaSourceType): List<MediaTrackItem> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val req = Request.Builder()
            .url("https://itunes.apple.com/search?term=$enc&media=music&limit=12")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val root = JSONObject(resp.body?.string().orEmpty())
            val arr = root.optJSONArray("results") ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val trackName = obj.optString("trackName").ifBlank { continue }
                    val previewUrl = obj.optString("previewUrl").takeIf { it.isNotBlank() }
                    val art = obj.optString("artworkUrl100").replace("100x100bb", "600x600bb")
                    add(
                        MediaTrackItem(
                            id = "it_${obj.optLong("trackId", i.toLong())}",
                            title = trackName,
                            artist = obj.optString("artistName", "Artista"),
                            album = obj.optString("collectionName", ""),
                            durationMs = obj.optLong("trackTimeMillis", 30_000L),
                            sourceType = source,
                            streamUrl = previewUrl,
                            pageUrl = obj.optString("trackViewUrl").ifBlank {
                                "https://www.google.com/search?q=${URLEncoder.encode("$trackName ${obj.optString("artistName")}", "UTF-8")}"
                            },
                            thumbnailUrl = art,
                            isVideo = false,
                            mimeType = "audio/mp4"
                        )
                    )
                }
            }
        }
    }

    /**
     * Resolves actual stream URL + downloadable MP4/WEBM/M4A/MP3 formats for playback & offline download.
     */
    suspend fun resolveStreamAndDownloads(track: MediaTrackItem): Pair<String?, List<DownloadFormatOption>> =
        withContext(Dispatchers.IO) {
            if (!track.localFilePath.isNullOrBlank()) {
                return@withContext track.localFilePath to emptyList()
            }
            if (!track.streamUrl.isNullOrBlank() && track.videoId.isNullOrBlank()) {
                val url = track.streamUrl
                val kind = when {
                    url.contains(".mp4", ignoreCase = true) && track.isVideo -> DownloadFormatKind.VIDEO_MP4
                    url.contains(".webm", ignoreCase = true) -> DownloadFormatKind.VIDEO_WEBM
                    url.contains(".mp3", ignoreCase = true) -> DownloadFormatKind.AUDIO_MP3
                    else -> DownloadFormatKind.AUDIO_M4A
                }
                return@withContext url to listOf(
                    DownloadFormatOption(
                        id = "direct_0",
                        url = url,
                        formatKind = kind,
                        qualityLabel = kind.label
                    )
                )
            }

            val vid = track.videoId ?: return@withContext null to emptyList()
            val formats = mutableListOf<DownloadFormatOption>()
            var bestPlaybackUrl: String? = null

            // Strategy 1: YouTube Innertube player (ANDROID_VR / IOS context provides direct non-ciphered URLs)
            runCatching {
                val innertubeResult = resolveViaInnertubePlayer(vid)
                formats.addAll(innertubeResult)
            }.onFailure { Log.w(TAG, "Innertube player lookup failed: ${it.message}") }

            // Strategy 2: Piped /streams/{videoId} fallback if Innertube returned empty
            if (formats.isEmpty()) {
                val mirrors = listOf("https://pipedapi.kavin.rocks", "https://api.piped.private.coffee")
                for (base in mirrors) {
                    val piped = runCatching { resolveViaPipedStreams(base, vid) }.getOrDefault(emptyList())
                    if (piped.isNotEmpty()) {
                        formats.addAll(piped)
                        break
                    }
                }
            }

            bestPlaybackUrl = if (track.isVideo) {
                formats.firstOrNull { it.formatKind == DownloadFormatKind.VIDEO_MP4 }?.url
                    ?: formats.firstOrNull { it.formatKind.isVideo }?.url
                    ?: formats.firstOrNull()?.url
            } else {
                formats.firstOrNull { !it.formatKind.isVideo }?.url
                    ?: formats.firstOrNull()?.url
            }

            bestPlaybackUrl to formats.distinctBy { it.formatKind.id + "_" + it.qualityLabel }
        }

    private fun resolveViaInnertubePlayer(videoId: String): List<DownloadFormatOption> {
        val payload = JSONObject().apply {
            put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "ANDROID_VR")
                        .put("clientVersion", "1.60.19")
                        .put("androidSdkVersion", 34)
                        .put("hl", "es")
                )
            )
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        val req = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 14)")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val root = JSONObject(resp.body?.string().orEmpty())
            val sd = root.optJSONObject("streamingData") ?: return emptyList()
            val out = mutableListOf<DownloadFormatOption>()

            fun parseFormatArray(arr: JSONArray?) {
                if (arr == null) return
                for (i in 0 until arr.length()) {
                    val f = arr.optJSONObject(i) ?: continue
                    val directUrl = f.optString("url")
                    if (directUrl.isBlank()) continue
                    val mime = f.optString("mimeType").lowercase()
                    val quality = f.optString("qualityLabel")
                        .ifBlank { "${f.optInt("bitrate", 128_000) / 1000} kbps" }
                    val size = f.optString("contentLength").toLongOrNull() ?: 0L
                    val kind = when {
                        mime.startsWith("video/mp4") -> DownloadFormatKind.VIDEO_MP4
                        mime.startsWith("video/webm") -> DownloadFormatKind.VIDEO_WEBM
                        mime.startsWith("audio/mp4") -> DownloadFormatKind.AUDIO_M4A
                        mime.startsWith("audio/webm") -> DownloadFormatKind.AUDIO_OPUS
                        else -> if (mime.startsWith("video")) DownloadFormatKind.VIDEO_MP4 else DownloadFormatKind.AUDIO_M4A
                    }
                    out.add(
                        DownloadFormatOption(
                            id = "it_${f.optInt("itag", i)}",
                            url = directUrl,
                            formatKind = kind,
                            qualityLabel = "${kind.label} ($quality)",
                            contentLengthBytes = size
                        )
                    )
                }
            }

            parseFormatArray(sd.optJSONArray("formats"))
            parseFormatArray(sd.optJSONArray("adaptiveFormats"))
            return out
        }
    }

    private fun resolveViaPipedStreams(baseUrl: String, videoId: String): List<DownloadFormatOption> {
        val req = Request.Builder()
            .url("$baseUrl/streams/$videoId")
            .header("User-Agent", "RGEN_time/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val root = JSONObject(resp.body?.string().orEmpty())
            val out = mutableListOf<DownloadFormatOption>()

            val videoStreams = root.optJSONArray("videoStreams")
            if (videoStreams != null) {
                for (i in 0 until videoStreams.length()) {
                    val v = videoStreams.optJSONObject(i) ?: continue
                    if (v.optBoolean("videoOnly", false)) continue
                    val url = v.optString("url").takeIf { it.isNotBlank() } ?: continue
                    val format = v.optString("format", "MPEG_4")
                    val kind = if (format.contains("WEBM", true)) DownloadFormatKind.VIDEO_WEBM else DownloadFormatKind.VIDEO_MP4
                    out.add(
                        DownloadFormatOption(
                            id = "pv_$i",
                            url = url,
                            formatKind = kind,
                            qualityLabel = "${kind.label} (${v.optString("quality", "HD")})"
                        )
                    )
                }
            }

            val audioStreams = root.optJSONArray("audioStreams")
            if (audioStreams != null) {
                for (i in 0 until audioStreams.length()) {
                    val a = audioStreams.optJSONObject(i) ?: continue
                    val url = a.optString("url").takeIf { it.isNotBlank() } ?: continue
                    val mime = a.optString("mimeType", "audio/mp4")
                    val kind = if (mime.contains("webm", true)) DownloadFormatKind.AUDIO_OPUS else DownloadFormatKind.AUDIO_M4A
                    out.add(
                        DownloadFormatOption(
                            id = "pa_$i",
                            url = url,
                            formatKind = kind,
                            qualityLabel = "${kind.label} (${a.optString("quality", "128kbps")})"
                        )
                    )
                }
            }
            return out
        }
    }

    suspend fun fetchLyrics(title: String, artist: String): List<LyricLine> = withContext(Dispatchers.IO) {
        val cleanTitle = title.replace(Regex("\\([^)]*\\)|\\[[^]]*]"), "").trim()
        val query = "$cleanTitle ${artist.takeIf { it != "YouTube" && it != "Desconocido" }.orEmpty()}".trim()
        if (query.isBlank()) return@withContext emptyList()

        runCatching {
            val enc = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://lrclib.net/api/search?q=$enc")
                .header("User-Agent", "RGEN_time/1.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                if (arr.length() == 0) return@withContext emptyList()
                val first = arr.getJSONObject(0)
                val synced = first.optString("syncedLyrics")
                if (synced.isNotBlank()) {
                    return@withContext parseLrcLyrics(synced)
                }
                val plain = first.optString("plainLyrics")
                if (plain.isNotBlank()) {
                    return@withContext plain.lines()
                        .filter { it.isNotBlank() }
                        .mapIndexed { idx, line -> LyricLine(timeMs = idx * 4_000L, text = line.trim()) }
                }
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun extractRunsText(obj: JSONObject?): String {
        if (obj == null) return ""
        val simple = obj.optString("simpleText")
        if (simple.isNotBlank()) return simple
        val runs = obj.optJSONArray("runs") ?: return ""
        return buildString {
            for (i in 0 until runs.length()) {
                append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
    }

    companion object {
        fun extractYoutubeVideoId(url: String): String? {
            val patterns = listOf(
                Regex("v=([a-zA-Z0-9_-]{11})"),
                Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
                Regex("shorts/([a-zA-Z0-9_-]{11})"),
                Regex("embed/([a-zA-Z0-9_-]{11})")
            )
            for (p in patterns) {
                val match = p.find(url)
                if (match != null) return match.groupValues[1]
            }
            return null
        }

        fun parseDurationTextToMs(text: String): Long {
            val parts = text.trim().split(":").mapNotNull { it.toLongOrNull() }
            return when (parts.size) {
                2 -> (parts[0] * 60L + parts[1]) * 1000L
                3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
                else -> 0L
            }
        }

        fun parseLrcLyrics(lrc: String): List<LyricLine> {
            val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?](.*)")
            return lrc.lines().mapNotNull { line ->
                val m = regex.find(line.trim()) ?: return@mapNotNull null
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = m.groupValues[3]
                val ms = when (fracRaw.length) {
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val text = m.groupValues[4].trim()
                if (text.isBlank()) null else LyricLine(min * 60_000L + sec * 1000L + ms, text)
            }
        }
    }
}
