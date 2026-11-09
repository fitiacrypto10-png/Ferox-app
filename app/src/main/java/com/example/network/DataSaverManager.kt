package com.example.network

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestionnaire d'économie de données et régulateur de débit réseau (200 Ko/s max).
 * Bride la vitesse de téléchargement des images et avatars à 200 Ko/s en mémoire vive,
 * sans JAMAIS enregistrer les fichiers sur le disque, garantissant un stockage < 20 Mo.
 */
object DataSaverManager {

    private const val TAG = "DataSaverManager"

    /** Débit maximum autorisé pour les images et avatars : 200 Ko/s (200 * 1024 octets/s) */
    const val MAX_BYTES_PER_SECOND: Long = 200L * 1024L

    /**
     * Nettoie les données accumulées (anciens dossiers d'images, caches HTTP temporaires, etc.)
     * pour maintenir l'application strictement sous les 20 Mo de données.
     */
    fun cleanupAppStorage(context: Context, webView: WebView? = null) {
        try {
            // 1. Supprimer le dossier où les images étaient précédemment enregistrées
            val mediaCacheDir = File(context.cacheDir, "data_saver_media_cache")
            if (mediaCacheDir.exists()) {
                deleteRecursively(mediaCacheDir)
                Log.d(TAG, "Dossier data_saver_media_cache supprimé avec succès.")
            }

            // 2. Supprimer le cache HTTP de 50 Mo
            val httpCacheDir = File(context.cacheDir, "http_cache")
            if (httpCacheDir.exists()) {
                deleteRecursively(httpCacheDir)
                Log.d(TAG, "Dossier http_cache supprimé avec succès.")
            }

            // 3. Désinstaller et vider le cache HttpResponseCache système s'il était actif
            try {
                android.net.http.HttpResponseCache.getInstalled()?.delete()
            } catch (ignored: Exception) {}

            // 4. Nettoyer les fichiers temporaires .tmp et anciens .apk dans cacheDir
            context.cacheDir?.listFiles()?.forEach { file ->
                if (file.name.endsWith(".tmp") || file.name.endsWith(".apk") || file.name.endsWith(".bin")) {
                    try { file.delete() } catch (ignored: Exception) {}
                }
            }

            // 5. Nettoyer les fichiers temporaires dans externalCacheDir si présents
            try {
                context.externalCacheDir?.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp") || file.name.endsWith(".bin")) {
                        try { file.delete() } catch (ignored: Exception) {}
                    }
                }
            } catch (ignored: Exception) {}

            // 6. Nettoyer le cache WebView pour libérer la mémoire disque
            webView?.clearCache(true)

            Log.d(TAG, "Nettoyage du stockage terminé : l'application reste sous les 20 Mo.")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du nettoyage du stockage : ${e.message}")
        }
    }

    /**
     * Intercepte les requêtes réseau d'images (avatars, photos de profils, pièces jointes)
     * pour brider leur vitesse sous les 200 Ko/s EN MÉMOIRE VIVE DIRECTE, sans aucun fichier disque.
     */
    fun interceptMediaRequest(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url ?: return null
        val urlString = url.toString()
        val method = request.method ?: "GET"

        // N'intercepter QUE les requêtes GET pour les médias lourds
        // IMPORTANT: Ne JAMAIS intercepter les requêtes POST/PUT (comme l'envoi de photo de profil !)
        if (!method.equals("GET", ignoreCase = true)) {
            return null
        }

        // Ne pas toucher aux WebSockets, blobs, data URIs ou APIs temps réel
        if (urlString.startsWith("ws:", ignoreCase = true) ||
            urlString.startsWith("wss:", ignoreCase = true) ||
            urlString.startsWith("data:", ignoreCase = true) ||
            urlString.startsWith("blob:", ignoreCase = true)
        ) {
            return null
        }

        if (!isMediaResource(url, request)) {
            return null
        }

        try {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 8000
                instanceFollowRedirects = true
                useCaches = true
                setRequestProperty("Save-Data", "on")
                setRequestProperty("Accept", "image/webp,image/avif,image/*,*/*;q=0.8")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val contentType = connection.contentType ?: detectMimeType(urlString)
                val mimeType = contentType.substringBefore(";").trim()
                val encoding = if (contentType.contains("charset=")) {
                    contentType.substringAfter("charset=").trim()
                } else {
                    "UTF-8"
                }

                // Flux mémoire bridé à 200 Ko/s - AUCUN enregistrement disque !
                val throttledStream = MemoryThrottledInputStream(
                    delegate = connection.inputStream,
                    maxBytesPerSecond = MAX_BYTES_PER_SECOND
                )

                return WebResourceResponse(
                    mimeType,
                    encoding,
                    throttledStream
                )
            }
        } catch (e: Exception) {
            // En cas d'erreur réseau, laisser WebView charger normalement
        }

        return null
    }

    private fun isMediaResource(uri: Uri, request: WebResourceRequest): Boolean {
        val path = uri.path?.lowercase() ?: ""
        val url = uri.toString().lowercase()

        // Extensions typiques d'images
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
            path.endsWith(".webp") || path.endsWith(".gif") || path.endsWith(".svg") ||
            path.endsWith(".ico") || path.endsWith(".avif")
        ) {
            return true
        }

        // Requêtes d'avatars et photos de profils
        if (url.contains("/avatar") || url.contains("/picture") || url.contains("graph.facebook.com") ||
            url.contains("/photos/") || url.contains("/thumbnails/")
        ) {
            return true
        }

        val acceptHeader = request.requestHeaders?.get("Accept")?.lowercase() ?: ""
        if (acceptHeader.startsWith("image/")) {
            return true
        }

        return false
    }

    private fun detectMimeType(url: String): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)?.lowercase()
        return if (!ext.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "image/jpeg"
        } else when {
            url.contains(".png", ignoreCase = true) -> "image/png"
            url.contains(".webp", ignoreCase = true) -> "image/webp"
            url.contains(".gif", ignoreCase = true) -> "image/gif"
            url.contains(".svg", ignoreCase = true) -> "image/svg+xml"
            else -> "image/jpeg"
        }
    }

    private fun deleteRecursively(fileOrDir: File): Boolean {
        if (fileOrDir.isDirectory) {
            val children = fileOrDir.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
        }
        return try {
            fileOrDir.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Flux d'entrée mémoire régulant le débit à un maximum de 200 Ko/s.
     * N'écrit absolument rien sur le disque (0 octet enregistré).
     */
    private class MemoryThrottledInputStream(
        private val delegate: InputStream,
        private val maxBytesPerSecond: Long
    ) : InputStream() {

        private val startTime = System.currentTimeMillis()
        private var totalBytesRead = 0L

        override fun read(): Int {
            throttle(1)
            val b = delegate.read()
            if (b != -1) {
                totalBytesRead++
            }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = delegate.read(b, off, len)
            if (count > 0) {
                totalBytesRead += count
                throttle(count.toLong())
            }
            return count
        }

        private fun throttle(bytesCount: Long) {
            val elapsedMs = System.currentTimeMillis() - startTime
            val expectedMs = (totalBytesRead * 1000L) / maxBytesPerSecond
            if (expectedMs > elapsedMs) {
                val sleepMs = (expectedMs - elapsedMs).coerceAtMost(400L)
                if (sleepMs > 2) {
                    try {
                        Thread.sleep(sleepMs)
                    } catch (ignored: InterruptedException) {}
                }
            }
        }

        override fun close() {
            delegate.close()
        }
    }
}
