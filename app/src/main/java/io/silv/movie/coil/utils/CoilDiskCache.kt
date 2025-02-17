package io.silv.movie.coil.utils

import android.content.Context
import coil.disk.DiskCache
import io.silv.movie.prefrences.StoragePreferences
import io.silv.movie.prefrences.core.getOrDefaultBlocking

/**
 * Direct copy of Coil's internal SingletonDiskCache so that [StorageItemFetcher] can access it.
 */
internal object CoilDiskCache {

    private const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context, storagePreferences: StoragePreferences): DiskCache {
            return instance ?: run {
                val safeCacheDir = context.cacheDir.apply { mkdirs() }
                // Create the singleton disk cache instance.
                DiskCache.Builder()
                    .directory(safeCacheDir.resolve(FOLDER_NAME))
                    .maxSizePercent(storagePreferences.cacheSizePct.getOrDefaultBlocking().toDouble().coerceIn(0.05..1.0))
                    .maximumMaxSizeBytes(storagePreferences.cacheMaxSizeMB.getOrDefaultBlocking().toLong() * 1024 * 1024)
                    .build()
                    .also { instance = it }
            }
        }
}