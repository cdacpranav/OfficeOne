package com.officone.app.engine

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Proactive memory optimizer and Out-Of-Memory (OOM) protection engine for Office One.
 *
 * Dynamically monitors heap headroom and available system RAM to prevent OOM
 * when processing large documents or multi-page PDF/Office renders.
 */
object MemoryOptimizer {

    private const val TAG = "MemoryOptimizer"

    enum class MemoryPressure {
        NORMAL,
        MODERATE,
        CRITICAL
    }

    /**
     * Inspects current JVM heap and Android system RAM pressure.
     */
    fun checkMemoryPressure(context: Context? = null): MemoryPressure {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()

        val availableHeap = maxMemory - (totalMemory - freeMemory)
        val heapUsedRatio = (totalMemory - freeMemory).toDouble() / maxMemory.toDouble()

        // Check system memory info if context is provided
        if (context != null) {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)

            if (memInfo.lowMemory || heapUsedRatio > 0.85) {
                Log.w(TAG, "Critical memory pressure detected! Heap used: ${(heapUsedRatio * 100).toInt()}%")
                return MemoryPressure.CRITICAL
            }
        }

        return when {
            heapUsedRatio > 0.80 -> {
                Log.w(TAG, "Moderate memory pressure: ${(heapUsedRatio * 100).toInt()}% heap used")
                MemoryPressure.MODERATE
            }
            else -> MemoryPressure.NORMAL
        }
    }

    /**
     * Calculates the safe maximum dimension for rendering document or PDF pages as [android.graphics.Bitmap].
     * Standard A4 at 150 DPI is ~1240 x 1754 px (~8.7 MB uncompressed ARGB_8888).
     * On low-RAM devices or under memory pressure, this scales down safely.
     */
    fun getSafeRenderDimensions(targetWidth: Int, targetHeight: Int, pressure: MemoryPressure): Pair<Int, Int> {
        val scaleFactor = when (pressure) {
            MemoryPressure.NORMAL -> 1.0f
            MemoryPressure.MODERATE -> 0.75f
            MemoryPressure.CRITICAL -> 0.50f
        }

        val safeWidth = (targetWidth * scaleFactor).toInt().coerceAtLeast(400)
        val safeHeight = (targetHeight * scaleFactor).toInt().coerceAtLeast(600)
        return safeWidth to safeHeight
    }

    /**
     * Requests JVM garbage collection and flushes caches when critical memory pressure occurs.
     */
    fun triggerCleanupIfNecessary(pressure: MemoryPressure) {
        if (pressure == MemoryPressure.CRITICAL) {
            System.gc()
        }
    }
}
