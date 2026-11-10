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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
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
                instanceFollowRedirects = true
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
                android.util.Log.d(
                    "FeroxUpdater",
                    "Version check: installed=$currentVersion, server=$serverVersionCode, url=$apkUrl"
                )

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
     * Ouvre la page des versions GitHub dans le navigateur en cas de besoin de téléchargement direct.
     */
    fun openGitHubReleasesWeb(context: Context) {
        try {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/fitiacrypto10-png/Ferox-app/releases")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (ignored: Exception) {}
    }

    /**
     * Télécharge directement l'APK en arrière-plan avec gestion des redirections GitHub / AWS S3
     * et lance automatiquement l'installation dès la fin du téléchargement.
     */
    fun downloadAndInstallApk(
        context: Context,
        updateInfo: UpdateInfo,
        onStarted: () -> Unit = {}
    ) {
        if (!canInstallUnknownApps(context)) {
            Toast.makeText(
                context,
                "Veuillez autoriser Ferox à installer des applications inconnues pour appliquer la mise à jour.",
                Toast.LENGTH_LONG
            ).show()
            openInstallPermissionSettings(context)
        }

        onStarted()
        Toast.makeText(
            context,
            "Téléchargement de la mise à jour Ferox v${updateInfo.versionCode} en cours...",
            Toast.LENGTH_SHORT
        ).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var currentUrl = updateInfo.apkUrl
                var connection: HttpURLConnection? = null
                var redirectCount = 0

                // Suivre les redirections (GitHub vers AWS S3)
                while (redirectCount < 6) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 12000
                        readTimeout = 30000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "Ferox-Updater-Android")
                        setRequestProperty("Accept", "*/*")
                    }

                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == 307 || code == 308
                    ) {
                        val location = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!location.isNullOrBlank()) {
                            currentUrl = location
                            redirectCount++
                            continue
                        }
                    }
                    break
                }

                val conn = connection
                val finalCode = conn?.responseCode ?: -1

                if (finalCode !in 200..299 || conn == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Le fichier n'est pas encore disponible sur le serveur GitHub (Erreur $finalCode). Ouverture de la page GitHub...",
                            Toast.LENGTH_LONG
                        ).show()
                        openGitHubReleasesWeb(context)
                    }
                    return@launch
                }

                val destinationFile = File(context.cacheDir, "Ferox-Update-${updateInfo.versionCode}.apk")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val inputStream = conn.inputStream
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                conn.disconnect()

                // Vérifier que le fichier est un APK valide (> 500 Ko) et non une page d'erreur HTML
                if (destinationFile.exists() && destinationFile.length() > 500_000) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Téléchargement terminé ! Lancement de l'installation...", Toast.LENGTH_SHORT).show()
                        installApk(context, destinationFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Fichier de mise à jour incomplet. Ouverture de la page GitHub pour téléchargement manuel...",
                            Toast.LENGTH_LONG
                        ).show()
                        openGitHubReleasesWeb(context)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Erreur réseau: ${e.localizedMessage ?: "impossible de télécharger"}. Ouverture de GitHub...",
                        Toast.LENGTH_LONG
                    ).show()
                    openGitHubReleasesWeb(context)
                }
            }
        }
    }

    /**
     * Lance l'installation de l'APK téléchargé via FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() < 500_000) {
            Toast.makeText(context, "Le fichier de mise à jour est introuvable ou incomplet.", Toast.LENGTH_SHORT).show()
            openGitHubReleasesWeb(context)
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
            openGitHubReleasesWeb(context)
        }
    }
}
