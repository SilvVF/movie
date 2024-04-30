package io.silv.movie.coil.core

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import io.silv.movie.coil.utils.CoilDiskUtils
import io.silv.movie.coil.utils.CoilDiskUtils.toImageSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.File


class ByteArrayDiskBackedFetcher<T: Any>(
    val fetch: suspend (options: Options, data: T) -> ByteArray,
    override val overrideCall: suspend (options: Options, data: T) -> FetchResult? = { _, _ -> null },
    override val keyer: Keyer<T>,
    override val diskStore: FetcherDiskStore<T>,
    override val context: Context,
    override val memoryCacheInit: Lazy<MemoryCache>,
    override val diskCacheInit: Lazy<DiskCache>
): DefaultDiskBackedFetcher<T>(keyer, diskStore, context, memoryCacheInit, diskCacheInit, overrideCall) {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun fetch(
        data: T,
        options: Options,
        memCacheKey: MemoryCache.Key,
        diskCacheKey: String,
        imageCacheFile: File?
    ): FetchResult {
        var snapshot: DiskCache.Snapshot? = null

        try {
            // Fetch from network
            val response = fetch(options, data)
            val bis = ByteArrayInputStream(response).source().buffer()

            // Read from cover cache after library manga cover updated
            if (imageCacheFile != null && options.diskCachePolicy.writeEnabled) {
                CoilDiskUtils.writeSourceToCoverCache(bis, imageCacheFile)
            }

            if (imageCacheFile != null) {
                CoilDiskUtils.writeToMemCache(
                    options,
                    imageCacheFile.readBytes(),
                    memCacheKey,
                    memCache
                )
                return fileLoader(imageCacheFile, diskCacheKey, options)
            }
            // Read from disk cache
            snapshot = writeToDiskCache(response, diskCacheKey)

            if (snapshot != null) {
                CoilDiskUtils.writeToMemCache(
                    options,
                    snapshot.data.toFile().readBytes(),
                    memCacheKey,
                    memCache
                )
                return SourceResult(
                    source = snapshot.toImageSource(diskCacheKey),
                    mimeType = "image/*",
                    dataSource = DataSource.NETWORK
                )
            }
            CoilDiskUtils.writeToMemCache(
                options,
                response,
                memCacheKey,
                memCache
            )
            // Read from response if cache is unused or unusable
            return SourceResult(
                source = ImageSource(
                    source = bis    ,
                    context = options.context
                ),
                mimeType = "image/*",
                dataSource = DataSource.NETWORK,
            )
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private fun writeToDiskCache(
        response: ByteArray,
        diskCacheKey: String
    ): DiskCache.Snapshot? {
        val editor = diskCache.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                ByteArrayInputStream(response).source().buffer().readAll(this)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }
}