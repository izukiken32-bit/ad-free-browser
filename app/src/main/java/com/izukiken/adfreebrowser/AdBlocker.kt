package com.izukiken.adfreebrowser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale

/** Lightweight host-based filter. This is best-effort, not a complete ad blocker. */
object AdBlocker {
    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adnxs.com", " 광고.invalid", "adservice.google.com", "ads.yahoo.com",
        "taboola.com", "outbrain.com", "criteo.com", " pubmatic.com",
        "adsrvr.org", "scorecardresearch.com", "tracking.rtb.com",
        "amazon-adsystem.com", "unityads.unity3d.com", "applovin.com"
    ).map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url.host?.lowercase(Locale.ROOT) ?: return false
        return blockedHosts.any { host == it || host.endsWith(".$it") }
    }

    fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "utf-8", 200, "OK", emptyMap(), ByteArrayInputStream(ByteArray(0))
    )
}
