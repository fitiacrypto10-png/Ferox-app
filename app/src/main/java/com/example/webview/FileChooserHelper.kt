package com.example.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File

/**
 * Assistant pour la sélection et l'importation de fichiers et photos de profil.
 * Supporte à la fois la Galerie (Photos) et l'Appareil Photo (Caméra)
 * tout en conservant les callbacks de manière stable sans perte lors des rotations ou changements d'état.
 */
object FileChooserHelper {

    private const val TAG = "FileChooserHelper"

    var pendingUploadCallback: ValueCallback<Array<Uri>>? = null
    var pendingCameraUri: Uri? = null
    var pendingCameraFile: File? = null

    /**
     * Construit un Intent de sélection optimisé (Galerie + Appareil Photo) pour les photos de profil et fichiers.
     */
    fun createFileChooserIntent(
        context: Context,
        params: WebChromeClient.FileChooserParams?
    ): Intent {
        // Annuler tout callback précédent resté en attente pour éviter de bloquer WebView
        cancel()

        val acceptTypes = params?.acceptTypes ?: emptyArray()
        val isImage = acceptTypes.isEmpty() || acceptTypes.any { type ->
            type.contains("image", ignoreCase = true) || type.contains("jpg", ignoreCase = true) ||
            type.contains("png", ignoreCase = true) || type == "*/*" || type.isBlank()
        }

        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if (isImage) {
                type = "image/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/jpg")
                )
            } else {
                type = "*/*"
            }
            if (params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        val chooserTitle = if (isImage) {
            "Choisir une photo de profil ou un fichier"
        } else {
            "Choisir un fichier"
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, galleryIntent)
            putExtra(Intent.EXTRA_TITLE, chooserTitle)
        }

        // Si l'utilisateur importe une image, ajouter l'option directe de prendre une photo
        if (isImage) {
            try {
                val tempCameraFile = File(
                    context.cacheDir,
                    "profile_photo_${System.currentTimeMillis()}.jpg"
                )
                pendingCameraFile = tempCameraFile

                val cameraUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempCameraFile
                )
                pendingCameraUri = cameraUri

                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                chooserIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    arrayOf(cameraIntent)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Impossible d'initialiser l'appareil photo dans le sélecteur: ${e.message}")
            }
        }

        return chooserIntent
    }

    /**
     * Traite le résultat retourné par le sélecteur (Galerie ou Caméra) et transmet les URIs au WebView.
     */
    fun processResult(resultCode: Int, intentData: Intent?) {
        val callback = pendingUploadCallback
        if (callback == null) {
            Log.w(TAG, "processResult appelé mais aucun callback actif.")
            return
        }

        val uris: Array<Uri>? = if (resultCode == Activity.RESULT_OK) {
            // 1. Essayer le parseur natif de WebChromeClient
            val parsed = WebChromeClient.FileChooserParams.parseResult(resultCode, intentData)
            if (parsed != null && parsed.isNotEmpty()) {
                parsed
            } else if (intentData?.data != null) {
                // 2. URI unique dans data
                arrayOf(intentData.data!!)
            } else if (intentData?.clipData != null) {
                // 3. Plusieurs fichiers dans clipData
                val clipData = intentData.clipData!!
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else {
                // 4. Photo capturée avec l'appareil photo
                val camFile = pendingCameraFile
                val camUri = pendingCameraUri
                if (camFile != null && camFile.exists() && camFile.length() > 0 && camUri != null) {
                    arrayOf(camUri)
                } else {
                    null
                }
            }
        } else {
            null
        }

        try {
            callback.onReceiveValue(uris)
            Log.d(TAG, "Photo/Fichier transmis au WebView avec succès (${uris?.size ?: 0} éléments).")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la transmission au callback WebView: ${e.message}")
        } finally {
            pendingUploadCallback = null
            pendingCameraUri = null
            pendingCameraFile = null
        }
    }

    /**
     * Annule proprement le callback en cours si l'utilisateur quitte sans sélectionner.
     */
    fun cancel() {
        try {
            pendingUploadCallback?.onReceiveValue(null)
        } catch (ignored: Exception) {}
        pendingUploadCallback = null
        pendingCameraUri = null
        pendingCameraFile = null
    }
}
