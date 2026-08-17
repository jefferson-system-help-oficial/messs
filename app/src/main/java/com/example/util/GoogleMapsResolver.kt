package com.example.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ExtractedLocation(
    val latitude: Double,
    val longitude: Double,
    val addressText: String? = null
)

object GoogleMapsResolver {

    suspend fun resolveAndExtractLocation(inputUrlOrAddress: String, context: Context): ExtractedLocation? {
        return withContext(Dispatchers.IO) {
            try {
                val trimmed = inputUrlOrAddress.trim()
                if (trimmed.isBlank()) return@withContext null

                // If user entered or pasted a URL (e.g. contains http/https or maps.app.goo.gl or goo.gl or maps)
                if (trimmed.contains("http://") || trimmed.contains("https://") || trimmed.contains("maps") || trimmed.contains("goo.gl")) {
                    var finalUrl = trimmed
                    if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                        finalUrl = "https://$finalUrl"
                    }

                    // 1. If it's a short URL or redirect URL, follow HTTP redirects to get final URL
                    val resolvedUrl = followRedirects(finalUrl)

                    // 2. Try extracting coordinates using regex patterns on resolvedUrl or finalUrl
                    val coords = extractCoordsFromUrl(resolvedUrl) ?: extractCoordsFromUrl(finalUrl)

                    if (coords != null) {
                        // Reverse geocode to get human address string if needed
                        val addr = tryReverseGeocode(context, coords.first, coords.second)
                        return@withContext ExtractedLocation(coords.first, coords.second, addr)
                    }

                    // 3. If no @lat,lng in URL, check query param e.g. q=
                    val queryText = extractQueryTextFromUrl(resolvedUrl)
                    if (!queryText.isNullOrBlank()) {
                        val geo = tryGeocodeText(context, queryText)
                        if (geo != null) return@withContext geo
                    }
                }

                // If input is coordinates directly e.g. "-16.6868, -49.2648"
                val directMatch = Regex("""(-?\d+\.\d+),\s*(-?\d+\.\d+)""").find(trimmed)
                if (directMatch != null) {
                    val (lat, lng) = directMatch.destructured
                    val latD = lat.toDoubleOrNull()
                    val lngD = lng.toDoubleOrNull()
                    if (latD != null && lngD != null) {
                        val addr = tryReverseGeocode(context, latD, lngD)
                        return@withContext ExtractedLocation(latD, lngD, addr)
                    }
                }

                // Fallback: Geocode text address
                tryGeocodeText(context, trimmed)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun followRedirects(urlStr: String): String {
        return try {
            var currUrl = urlStr
            var count = 0
            while (count < 6) {
                val conn = URL(currUrl).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.connect()

                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        currUrl = loc
                        count++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            currUrl
        } catch (e: Exception) {
            urlStr
        }
    }

    private fun extractCoordsFromUrl(url: String): Pair<Double, Double>? {
        // Pattern 1: @-16.6868,-49.2648
        val matchAt = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""").find(url)
        if (matchAt != null) {
            val (lat, lng) = matchAt.destructured
            val latD = lat.toDoubleOrNull()
            val lngD = lng.toDoubleOrNull()
            if (latD != null && lngD != null) return Pair(latD, lngD)
        }

        // Pattern 2: ?q=-16.6868,-49.2648 or &q=-16.6868,-49.2648 or ll=-16.6868,-49.2648
        val matchQ = Regex("""[?&](?:q|ll|query)=(-?\d+\.\d+),(-?\d+\.\d+)""").find(url)
        if (matchQ != null) {
            val (lat, lng) = matchQ.destructured
            val latD = lat.toDoubleOrNull()
            val lngD = lng.toDoubleOrNull()
            if (latD != null && lngD != null) return Pair(latD, lngD)
        }

        // Pattern 3: !3d-16.6868!4d-49.2648 (Google maps embed / place URLs)
        val match3d4d = Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)""").find(url)
        if (match3d4d != null) {
            val (lat, lng) = match3d4d.destructured
            val latD = lat.toDoubleOrNull()
            val lngD = lng.toDoubleOrNull()
            if (latD != null && lngD != null) return Pair(latD, lngD)
        }

        // Pattern 4: any lat,lng pair in range
        val matchAny = Regex("""(-?\d+\.\d+),\s*(-?\d+\.\d+)""").find(url)
        if (matchAny != null) {
            val (lat, lng) = matchAny.destructured
            val latD = lat.toDoubleOrNull()
            val lngD = lng.toDoubleOrNull()
            if (latD != null && lngD != null && latD in -90.0..90.0 && lngD in -180.0..180.0) {
                return Pair(latD, lngD)
            }
        }

        return null
    }

    private fun extractQueryTextFromUrl(url: String): String? {
        val match = Regex("""[?&](?:q|query)=([^&]+)""").find(url)
        return match?.groupValues?.get(1)?.let {
            try {
                java.net.URLDecoder.decode(it, "UTF-8")
            } catch (e: Exception) {
                it
            }
        }
    }

    private fun tryReverseGeocode(context: Context, lat: Double, lng: Double): String? {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("pt", "BR"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val line = addr.getAddressLine(0)
                    if (!line.isNullOrBlank()) return line
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGeocodeText(context: Context, text: String): ExtractedLocation? {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("pt", "BR"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(text, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fullAddr = addr.getAddressLine(0) ?: text
                    return ExtractedLocation(addr.latitude, addr.longitude, fullAddr)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
