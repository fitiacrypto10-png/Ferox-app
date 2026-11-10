package com.example.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File

/**
 * Assistant infaillible pour l'importation de photos de profil et fichiers.
 * - Ne cancelle JAMAIS prématurément le callback d'upload.
 * - Copie en cache local sécurisé (FileProvider) pour éliminer les refus de permissions inter-applications.
 * - Propose le choix entre Galerie et Caméra directe pour les photos de profil.
 */
object FileChooserHelper {

    private const val TAG = "FileChooserHelper"

    var pendingUploadCallback: ValueCallback<Array<Uri>>? = null
    var pendingCameraUri: Uri? = null
    var pendingCameraFile: File? = null

    /**
     * Annule un callback précédent UNIQUEMENT s'il y en avait un encore actif
     * avant d'enregistrer un nouveau.
     */
    fun cancelPreviousIfAny() {
        try {
            pendingUploadCallback?.onReceiveValue(null)
        } catch (ignored: Exception) {}
        pendingUploadCallback = null
        pendingCameraUri = null
        pendingCameraFile = null
    }

    /**
     * Construit un Intent de sélection optimisé (Galerie + Appareil Photo) pour les photos de profil et fichiers.
     * ATTENTION : Ne JAMAIS appeler cancel() ici car cela détruirait le callback actif !
     */
    fun createFileChooserIntent(
        context: Context,
        params: WebChromeClient.FileChooserParams?
    ): Intent {
        val acceptTypes = params?.acceptTypes ?: emptyArray()
        val isImage = acceptTypes.isEmpty() || acceptTypes.any { type ->
            type.contains("image", ignoreCase = true) ||
            type.contains("jpg", ignoreCase = true) ||
            type.contains("png", ignoreCase = true) ||
            type == "*/*" || type.isBlank()
        }

        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            if (isImage) {
                type = "image/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("image/jpeg", "image/png", "image/webp", "image/gif")
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

        // Ajouter l'option de prise de photo directe
        if (isImage) {
            try {
                val tempCameraFile = File(
                    context.cacheDir,
                    "profile_camera_${System.currentTimeMillis()}.jpg"
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
                Log.e(TAG, "Impossible de préparer l'appareil photo: ${e.message}")
            }
        }

        return chooserIntent
    }

    /**
     * Traite le résultat retourné par le sélecteur (Galerie ou Caméra) et transmet les URIs au WebView.
     */
    fun processResult(context: Context, resultCode: Int, intentData: Intent?) {
        val callback = pendingUploadCallback
        if (callback == null) {
            Log.w(TAG, "processResult: aucun callback en attente.")
            return
        }

        val resultUris: Array<Uri>? = if (resultCode == Activity.RESULT_OK) {
            val extracted = mutableListOf<Uri>()

            // 1. Photo prise par la caméra ?
            val camFile = pendingCameraFile
            val camUri = pendingCameraUri
            if (camFile != null && camFile.exists() && camFile.length() > 0 && camUri != null) {
                extracted.add(camUri)
            }

            // 2. Si pas de photo caméra, extraire depuis intentData
            if (extracted.isEmpty()) {
                val parsed = WebChromeClient.FileChooserParams.parseResult(resultCode, intentData)
                if (parsed != null && parsed.isNotEmpty()) {
                    extracted.addAll(parsed)
                } else if (intentData?.data != null) {
                    extracted.add(intentData.data!!)
                } else if (intentData?.clipData != null) {
                    val clip = intentData.clipData!!
                    for (i in 0 until clip.itemCount) {
                        extracted.add(clip.getItemAt(i).uri)
                    }
                }
            }

            // 3. Convertir en URIs sûres pour éliminer les problèmes de permissions
            if (extracted.isNotEmpty()) {
                val safeUris = extracted.map { uri -> resolveSafeUri(context, uri) }.toTypedArray()
                safeUris
            } else {
                null
            }
        } else {
            null
        }

        try {
            callback.onReceiveValue(resultUris)
            Log.d(TAG, "Photo/Fichier transmis avec succès: ${resultUris?.size ?: 0} élément(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur transmission callback: ${e.message}")
        } finally {
            pendingUploadCallback = null
            pendingCameraUri = null
            pendingCameraFile = null
        }
    }

    /**
     * Résout un accès fichier 100% garanti en copiant le fichier dans le cache local
     * et en générant une URI FileProvider interne à l'application.
     */
    private fun resolveSafeUri(context: Context, sourceUri: Uri): Uri {
        if (sourceUri.authority == "${context.packageName}.fileprovider") {
            return sourceUri
        }

        return try {
            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = if (!mimeType.isNullOrBlank()) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            } else {
                "jpg"
            }

            val safeFile = File(context.cacheDir, "avatar_upload_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                safeFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (safeFile.exists() && safeFile.length() > 0) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    safeFile
                )
            } else {
                sourceUri
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur copie sécurisée de l'image (${sourceUri}): ${e.message}")
            sourceUri
        }
    }

    /**
     * Annule le callback en cours si l'utilisateur quitte sans sélectionner.
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
