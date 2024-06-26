package io.silv.movie.coil.utils

import coil.annotation.ExperimentalCoilApi
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.Options
import okio.Source
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File

internal object CoilDiskUtils {

    fun writeToMemCache(
        options: Options,
        bytes: ByteArray?,
        memCacheKey: MemoryCache.Key,
        memCache: MemoryCache
    ) {
        try {
//            if (options.memoryCachePolicy.writeEnabled && bytes != null) {
//                val bmp = with(
//                    BitmapFactory.Options().apply { inMutable = true }
//                ) {
//                    BitmapFactory.decodeByteArray(
//                        bytes, 0,
//                        bytes.size,
//                        this
//                    )
//                }!!
//                memCache[memCacheKey] = MemoryCache.Value(bitmap = bmp)
//            }
        } catch (e: Exception) { Timber.e(e) }
    }

    fun writeSourceToCoverCache(
        input: Source,
        cacheFile: File,
    ) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun readFromDiskCache(
        options: Options,
        diskCache: DiskCache,
        diskCacheKey: String
    ): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCache.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun moveSnapshotToCoverCache(
        diskCache: DiskCache,
        diskCacheKey: String,
        snapshot: DiskCache.Snapshot,
        cacheFile: File?,
    ): File? {
        if (cacheFile == null) return null
        return try {
            diskCache.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Timber.e(
                "moveSnapshotToCoverCache",
                "Failed to write snapshot data to cover cache ${cacheFile.name}"
            )
            null
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun DiskCache.Snapshot.toImageSource(key: String): ImageSource {
        return ImageSource(
            file = data,
            diskCacheKey = key,
            closeable = this
        )
    }
}
