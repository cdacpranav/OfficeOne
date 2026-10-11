package com.officone.app.engine

import android.content.Context

/**
 * Factory that selects the best available [DocumentEngine].
 *
 * Checks if LOKit native binaries are loaded and functional;
 * falls back to Apache POI [FallbackEngine] if not.
 */
object DocumentEngineFactory {
    fun getEngine(context: Context): DocumentEngine {
        return if (LOKitEngine.isAvailable()) {
            LOKitEngine(context)
        } else {
            FallbackEngine(context)
        }
    }
}
