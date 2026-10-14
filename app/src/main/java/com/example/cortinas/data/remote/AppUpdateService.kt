package com.example.cortinas.data.remote

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val mandatory: Boolean,
    val releaseNotes: String,
    val sha256: String
)

class AppUpdateService(private val context: Context, private val config: SupabaseConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun checkForUpdate(): AppUpdate? {
        val url = "${config.url}/rest/v1/app_updates?select=version_code,version_name,apk_url,mandatory,release_notes,sha256&platform=eq.android&order=version_code.desc&limit=1"
        val request = Request.Builder().url(url)
            .header("apikey", config.anonKey)
            .header("Authorization", "Bearer ${config.anonKey}")
            .get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("No se pudo consultar la actualización (${response.code})")
            val rows = JsonParser.parseString(response.body?.string().orEmpty()).asJsonArray
            if (rows.size() == 0) return null
            val row = rows[0].asJsonObject
            val latest = AppUpdate(
                versionCode = row.get("version_code").asInt,
                versionName = row.get("version_name").asString,
                apkUrl = row.get("apk_url").asString,
                mandatory = row.get("mandatory")?.asBoolean ?: false,
                releaseNotes = row.get("release_notes")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                sha256 = row.get("sha256")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
            )
            require(latest.apkUrl.startsWith("https://")) { "El enlace del APK debe usar HTTPS" }
            return latest.takeIf { it.versionCode > installedVersionCode() }
        }
    }

    fun download(update: AppUpdate): File {
        val request = Request.Builder().url(update.apkUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("No se pudo descargar el APK (${response.code})")
            val body = response.body ?: error("El servidor devolvió un APK vacío")
            require(body.contentLength() in 1..200_000_000L) { "Tamaño de APK no válido" }
            val destination = File(context.cacheDir, "updates/cortinas-${update.versionCode}.apk")
            destination.parentFile?.mkdirs()
            val digest = MessageDigest.getInstance("SHA-256")
            body.byteStream().use { input -> FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            } }
            if (update.sha256.isNotBlank()) {
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual.equals(update.sha256.trim(), ignoreCase = true)) { "La verificación del APK falló" }
            }
            return destination
        }
    }

    fun installerUri(apk: File) = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", apk
    )

    private fun installedVersionCode(): Int {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else {
            @Suppress("DEPRECATION") info.versionCode
        }
    }
}
