package com.example.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AppUpdaterManager {

    /**
     * URL du fichier version.json distant sur GitHub.
     */
    const val GITHUB_VERSION_JSON_URL =
        "https://raw.githubusercontent.com/fitiacrypto10-png/Ferox-app/main/version.json"

    private var activeDownloadId: Long? = null
    private var downloadReceiver: BroadcastReceiver? = null

    /**
     * Récupère le versionCode de l'application actuellement installée sur l'appareil.
     */
    @Suppress("DEPRECATION")
    fun getInstalledVersionCode(context: Context): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Vérifie en arrière-plan (sans bloquer l'interface) si une nouvelle version est disponible.
     * En cas d'erreur réseau ou d'absence d'internet, retourne silencieusement null.
     */
    suspend fun checkForUpdate(
        context: Context,
        urlStr: String = GITHUB_VERSION_JSON_URL
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val separator = if (urlStr.contains("?")) "&" else "?"
            val cacheBusterUrl = "$urlStr${separator}t=${System.currentTimeMillis()}"

            val url = URL(cacheBusterUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
                defaultUseCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Ferox-Updater-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val serverVersionCode = json.optLong("versionCode", -1L)
                val apkUrl = json.optString("apkUrl", "")
                val notes = json.optString("notes", "")

                val currentVersion = getInstalledVersionCode(context)

                if (serverVersionCode > currentVersion && apkUrl.isNotBlank()) {
                    return@withContext UpdateInfo(
                        versionCode = serverVersionCode,
                        apkUrl = apkUrl,
                        notes = notes
                    )
                }
            }
            null
        } catch (e: Exception) {
            // Ignorer silencieusement les erreurs réseau (pas d'internet, hôte inaccessible, etc.)
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Vérifie si l'autorisation d'installer des applications inconnues est accordée (Android 8.0+).
     */
    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Ouvre les paramètres système pour autoriser l'installation d'applications inconnues.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Télécharge l'APK avec DownloadManager et déclenche automatiquement l'installation.
     */
    fun downloadAndInstallApk(
        context: Context,
        updateInfo: UpdateInfo,
        onStarted: () -> Unit = {}
    ) {
        try {
            // Vérifier la permission d'installation avant de télécharger
            if (!canInstallUnknownApps(context)) {
                Toast.makeText(
                    context,
                    "Veuillez autoriser Ferox à installer des applications inconnues pour appliquer la mise à jour.",
                    Toast.LENGTH_LONG
                ).show()
                openInstallPermissionSettings(context)
            }

            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir

            val fileName = "Ferox-Update-${updateInfo.versionCode}.apk"
            val destinationFile = File(downloadDir, fileName)

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                Toast.makeText(context, "Service de téléchargement indisponible", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(updateInfo.apkUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Mise à jour Ferox v${updateInfo.versionCode}")
                setDescription("Téléchargement de la nouvelle version...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/vnd.android.package-archive")
                setDestinationUri(Uri.fromFile(destinationFile))
            }

            // Désinscrire un ancien récepteur si nécessaire
            downloadReceiver?.let {
                try {
                    context.applicationContext.unregisterReceiver(it)
                } catch (ignored: Exception) {}
            }

            val enqueueId = downloadManager.enqueue(request)
            activeDownloadId = enqueueId
            onStarted()

            Toast.makeText(
                context,
                "Téléchargement de la mise à jour démarré...",
                Toast.LENGTH_SHORT
            ).show()

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == enqueueId) {
                        try {
                            context.applicationContext.unregisterReceiver(this)
                        } catch (ignored: Exception) {}
                        downloadReceiver = null

                        installApk(context, destinationFile)
                    }
                }
            }

            downloadReceiver = receiver
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.applicationContext.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Impossible de démarrer le téléchargement : ${e.localizedMessage ?: "erreur"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Lance l'installation de l'APK téléchargé via FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "Le fichier de mise à jour est introuvable.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!canInstallUnknownApps(context)) {
            Toast.makeText(
                context,
                "Veuillez autoriser Ferox à installer des applications inconnues pour terminer l'installation.",
                Toast.LENGTH_LONG
            ).show()
            openInstallPermissionSettings(context)
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Erreur lors du lancement de l'installation : ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
