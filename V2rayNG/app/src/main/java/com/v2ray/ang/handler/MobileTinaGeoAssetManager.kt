package com.v2ray.ang.handler

import android.content.Context
import android.content.res.AssetManager
import android.os.Process
import android.system.Os
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.io.File
import java.io.FileOutputStream

/**
 * Makes the bundled Xray geo assets available before any native core/test process starts.
 *
 * The old bootstrap copied directly into the final file from MainActivity. If Android killed the
 * process during that copy, a truncated geosite.dat/geoip.dat was left behind and subsequent
 * launches skipped it merely because the file existed. Samsung devices with aggressive process
 * management made this race easier to hit.
 *
 * This bootstrap is synchronous and writes through a temporary file followed by an atomic rename.
 * Existing non-trivial user-managed geo files are intentionally preserved during normal startup.
 * If Xray explicitly reports one of the geo databases as invalid, only that named bundled file is
 * restored once and the core is allowed to retry.
 */
object MobileTinaGeoAssetManager {
    private const val MIN_PLAUSIBLE_GEO_BYTES = 64L * 1024L
    private val geoNames = arrayOf(
        AppConfig.GEOSITE_DAT,
        AppConfig.GEOIP_DAT,
        AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT
    )

    @Synchronized
    fun ensureReady(context: Context, assets: AssetManager = context.assets) {
        val assetDir = getAssetDir(context) ?: return
        val packaged = packagedAssets(assets)

        geoNames.filter { it in packaged }.forEach { name ->
            val target = File(assetDir, name)
            if (isPlausible(target)) return@forEach
            installAtomically(assets, name, target)
        }
    }

    /**
     * Repairs a geo database only when the native/core error explicitly identifies geo data.
     * This covers a full-size but corrupted file without replacing legitimate user-updated files
     * on every launch.
     */
    @Synchronized
    fun repairFromCoreError(context: Context, error: Throwable): Boolean {
        val messages = generateSequence<Throwable?>(error) { it.cause }
            .filterNotNull()
            .joinToString("\n") { it.message.orEmpty() }
            .lowercase()

        val requestedName = when {
            messages.contains(AppConfig.GEOSITE_DAT.lowercase()) || messages.contains("geosite") -> AppConfig.GEOSITE_DAT
            messages.contains(AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT.lowercase()) -> AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT
            messages.contains(AppConfig.GEOIP_DAT.lowercase()) || messages.contains("geoip") -> AppConfig.GEOIP_DAT
            else -> null
        } ?: return false

        val assets = context.assets
        if (requestedName !in packagedAssets(assets)) return false
        val assetDir = getAssetDir(context) ?: return false

        LogUtil.w(AppConfig.TAG, "Core reported invalid geo asset; restoring bundled $requestedName once")
        return installAtomically(assets, requestedName, File(assetDir, requestedName))
    }

    private fun getAssetDir(context: Context): File? {
        val assetDirPath = Utils.userAssetPath(context)
        if (assetDirPath.isBlank()) return null

        val assetDir = File(assetDirPath)
        if (!assetDir.exists() && !assetDir.mkdirs()) {
            LogUtil.e(AppConfig.TAG, "Unable to create geo asset directory: ${assetDir.absolutePath}")
            return null
        }
        return assetDir
    }

    private fun packagedAssets(assets: AssetManager): Set<String> =
        runCatching { assets.list("")?.toSet().orEmpty() }
            .getOrElse {
                LogUtil.e(AppConfig.TAG, "Unable to enumerate bundled geo assets", it)
                emptySet()
            }

    private fun isPlausible(file: File): Boolean =
        file.isFile && file.length() >= MIN_PLAUSIBLE_GEO_BYTES

    private fun installAtomically(assets: AssetManager, name: String, target: File): Boolean {
        val temp = File(target.parentFile, ".${target.name}.${Process.myPid()}.tmp")
        return runCatching {
            if (temp.exists()) temp.delete()
            assets.open(name, AssetManager.ACCESS_STREAMING).use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                    output.flush()
                    output.fd.sync()
                }
            }

            if (!isPlausible(temp)) {
                error("Bundled $name is unexpectedly small (${temp.length()} bytes)")
            }

            // POSIX rename on Android is atomic on the same filesystem and replaces a stale,
            // truncated target without exposing a half-written final file to the daemon process.
            Os.rename(temp.absolutePath, target.absolutePath)
            LogUtil.i(AppConfig.TAG, "Geo asset prepared atomically: ${target.absolutePath}")
            true
        }.getOrElse {
            temp.delete()
            LogUtil.e(AppConfig.TAG, "Failed to prepare bundled geo asset $name", it)
            false
        }
    }
}
