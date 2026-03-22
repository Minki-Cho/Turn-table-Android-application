package com.truth.vinylremote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class LyricsProvider {
    suspend fun fetchLyrics(
        title: String,
        artist: String,
        durationMs: Long?
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank()) return@withContext null

        val titleCandidates = buildTitleCandidates(cleanTitle)
        val artistCandidates = buildArtistCandidates(cleanArtist).ifEmpty { listOf("") }
        val bestTitle = titleCandidates.firstOrNull().orEmpty()
        val bestArtist = artistCandidates.firstOrNull().orEmpty()

        // Fast path: run likely matches in parallel and return first success.
        val fastResult = raceFirstNonBlank(
            requests = buildList {
                if (bestArtist.isNotBlank()) {
                    add { fetchByGet(bestTitle, bestArtist, durationMs) }
                    add { fetchBySearch(bestTitle, bestArtist) }
                    add { fetchByLyricsOvh(bestTitle, bestArtist) }
                }
                add { fetchByQueryOnly("$bestTitle $bestArtist".trim()) }
            },
            timeoutMs = 1800L
        )
        if (!fastResult.isNullOrBlank()) return@withContext fastResult

        // Fallback path: wider candidate permutations.
        for (candidateTitle in titleCandidates.take(4)) {
            for (candidateArtist in artistCandidates.take(4)) {
                val result = raceFirstNonBlank(
                    requests = buildList {
                        if (candidateArtist.isNotBlank()) {
                            add { fetchByGet(candidateTitle, candidateArtist, durationMs) }
                            add { fetchBySearch(candidateTitle, candidateArtist) }
                            add { fetchByLyricsOvh(candidateTitle, candidateArtist) }
                        } else {
                            add { fetchByQueryOnly(candidateTitle) }
                        }
                    },
                    timeoutMs = 2200L
                )
                if (!result.isNullOrBlank()) return@withContext result
            }
        }
        null
    }

    private fun fetchByGet(title: String, artist: String, durationMs: Long?): String? {
        val query = mutableListOf(
            "track_name=${urlEncode(title)}",
            "artist_name=${urlEncode(artist)}"
        )
        durationMs?.takeIf { it > 0L }?.let {
            query += "duration=${it / 1000L}"
        }
        val url = "https://lrclib.net/api/get?${query.joinToString("&")}"
        val body = request(url) ?: return null
        return parseLyricsFromObject(JSONObject(body))
    }

    private fun fetchBySearch(title: String, artist: String): String? {
        val strictQuery = listOf(
            "track_name=${urlEncode(title)}",
            "artist_name=${urlEncode(artist)}"
        ).joinToString("&")
        val strictUrl = "https://lrclib.net/api/search?$strictQuery"
        val strictBody = request(strictUrl)
        val strictResult = strictBody?.let(::parseLyricsFromSearchResponse)
        if (!strictResult.isNullOrBlank()) return strictResult

        val fallbackUrl = "https://lrclib.net/api/search?q=${urlEncode("$title $artist")}"
        val body = request(fallbackUrl) ?: return null
        return parseLyricsFromSearchResponse(body)
    }

    private fun fetchByQueryOnly(title: String): String? {
        val url = "https://lrclib.net/api/search?q=${urlEncode(title)}"
        val body = request(url) ?: return null
        return parseLyricsFromSearchResponse(body)
    }

    private fun fetchByLyricsOvh(title: String, artist: String): String? {
        val url = "https://api.lyrics.ovh/v1/${urlEncode(artist)}/${urlEncode(title)}"
        val body = request(url) ?: return null
        val json = JSONObject(body)
        val lyrics = json.optString("lyrics").orEmpty()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        return lyrics.takeIf { it.isNotBlank() }
    }

    private fun buildTitleCandidates(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        val candidates = linkedSetOf<String>()
        candidates += trimmed

        val strippedBrackets = trimmed
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\[[^]]*]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (strippedBrackets.isNotBlank()) candidates += strippedBrackets

        val baseDash = strippedBrackets.substringBefore(" - ").trim()
        if (baseDash.isNotBlank()) candidates += baseDash

        val noFeat = baseDash
            .replace(Regex("""(?i)\b(feat|ft)\.?\b.*$"""), "")
            .trim()
        if (noFeat.isNotBlank()) candidates += noFeat

        return candidates.toList()
    }

    private fun buildArtistCandidates(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()

        val normalized = trimmed
            .replace("•", ",")
            .replace("·", ",")
            .replace("|", ",")
            .replace("/", ",")
            .replace("&", ",")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val first = normalized
            .split(",")
            .firstOrNull()
            .orEmpty()
            .replace(Regex("""(?i)\b(feat|ft)\.?\b.*$"""), "")
            .trim()

        val set = linkedSetOf<String>()
        set += trimmed
        if (normalized.isNotBlank()) set += normalized
        if (first.isNotBlank()) set += first
        return set.toList()
    }

    private fun parseLyricsFromSearchResponse(body: String): String? {
        val resultArray = JSONArray(body)
        if (resultArray.length() == 0) return null
        for (i in 0 until resultArray.length()) {
            val candidate = resultArray.optJSONObject(i) ?: continue
            val parsed = parseLyricsFromObject(candidate)
            if (!parsed.isNullOrBlank()) return parsed
        }
        return null
    }

    private fun containsLrcTimestamp(text: String): Boolean {
        val timestampRegex = Regex("""\[\d{1,2}:\d{1,2}(?:[.:]\d{1,3})?]""")
        return text.lines().any { line -> timestampRegex.containsMatchIn(line) }
    }

    private fun parseLyricsFromObject(obj: JSONObject): String? {
        val synced = obj.optString("syncedLyrics").orEmpty()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        if (synced.isNotBlank() && containsLrcTimestamp(synced)) {
            return synced
        }

        val plain = obj.optString("plainLyrics").orEmpty().trim()
        if (plain.isNotBlank()) return plain

        if (synced.isNotBlank()) {
            val withoutTimestamp = synced
                .replace(Regex("""\[[0-9]{1,2}:[0-9]{1,2}(?:[.:][0-9]{1,3})?]"""), "")
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .trim()
            if (withoutTimestamp.isNotBlank()) return withoutTimestamp
        }
        return null
    }

    private fun request(url: String): String? {
        val conn = (URL(url).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 1200
            conn.readTimeout = 1800
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "VinylRemote/0.1")
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use(BufferedReader::readText)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    private suspend fun raceFirstNonBlank(
        requests: List<() -> String?>,
        timeoutMs: Long
    ): String? = coroutineScope {
        if (requests.isEmpty()) return@coroutineScope null

        val results = Channel<String?>(requests.size)
        val deferreds = requests.map { req ->
            async(Dispatchers.IO) {
                withTimeoutOrNull(timeoutMs) { req() }
            }
        }
        deferreds.forEach { deferred ->
            launch {
                results.trySend(deferred.await())
            }
        }

        repeat(deferreds.size) {
            val result = results.receive()
            if (!result.isNullOrBlank()) {
                deferreds.forEach { it.cancel() }
                results.close()
                return@coroutineScope result
            }
        }
        deferreds.forEach { it.cancel() }
        results.close()
        null
    }
}
