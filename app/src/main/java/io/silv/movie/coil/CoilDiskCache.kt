package io.silv.movie.coil

import android.content.Context
import coil.disk.DiskCache

/**
 * Direct copy of Coil's internal SingletonDiskCache so that [StorageItemFetcher] can access it.
 */
internal object CoilDiskCache {

    private const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context): DiskCache {
            return instance ?: run {
                val safeCacheDir = context.cacheDir.apply { mkdirs() }
                // Create the singleton disk cache instance.
                DiskCache.Builder()
                    .directory(safeCacheDir.resolve(FOLDER_NAME))
                    .maxSizePercent(0.1)
                    .maximumMaxSizeBytes(150L * 1024 * 1024)
                    .build()
                    .also { instance = it }
            }
        }
}