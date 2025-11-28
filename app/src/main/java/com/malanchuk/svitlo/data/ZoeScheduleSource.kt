package com.malanchuk.svitlo.data

import android.util.Log
import com.malanchuk.svitlo.util.StartupTrace
import java.net.HttpURLConnection

class ZoeScheduleSource(
    private val url: String = SOURCE_URL,
) {
    fun fetchHtml(): String {
        StartupTrace.mark("ZOE request opening connection")
        val connection = (java.net.URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "uk-UA,uk;q=0.9,ru;q=0.8,en;q=0.5")
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            val responseCode = connection.responseCode
            StartupTrace.mark("ZOE response code $responseCode")
            if (responseCode !in 200..299) {
                val errorPreview = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText().take(LOG_PREVIEW_LENGTH) }
                    .orEmpty()
                Log.e(TAG, "ZOE HTTP $responseCode, url=${connection.url}, body=$errorPreview")
                error("ZOE returned HTTP $responseCode")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText().also { html ->
                    val normalizedPreview = html
                        .take(LOG_PREVIEW_LENGTH)
                        .replace(Regex("\\s+"), " ")
                    Log.i(
                        TAG,
                        "ZOE HTTP $responseCode, url=${connection.url}, " +
                            "type=${connection.contentType}, chars=${html.length}, " +
                            "articles=${Regex("<article", RegexOption.IGNORE_CASE).findAll(html).count()}, " +
                            "posts=${Regex("id=[\\\"']post-", RegexOption.IGNORE_CASE).findAll(html).count()}, " +
                            "preview=$normalizedPreview",
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val SOURCE_URL = "https://www.zoe.com.ua/outage/"
        const val TAG = "Svitlo"
        const val LOG_PREVIEW_LENGTH = 500
        const val USER_AGENT = "Svitlo/0.1 (Android; outage schedule reader)"
    }
}
