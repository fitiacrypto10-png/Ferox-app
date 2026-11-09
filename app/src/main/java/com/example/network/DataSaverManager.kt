package com.example.network

import android.content.Context
import android.util.Log
import android.webkit.WebView
import java.io.File

/**
 * Gestionnaire de stockage léger et d'optimisation de données.
 * Empêche l'enregistrement des images dans les données locales de l'application
 * (garde l'application sous 20 Mo) et nettoie automatiquement les anciens caches accumulés.
 */
object DataSaverManager {

    private const val TAG = "DataSaverManager"

    /**
     * Nettoie les données accumulées (anciens dossiers d'images de 120 Mo, caches HTTP temporaires, etc.)
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
     * Supprime récursivement un dossier et tous ses sous-fichiers.
     */
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
}
