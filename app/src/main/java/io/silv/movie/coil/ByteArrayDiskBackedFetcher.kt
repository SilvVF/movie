package io.silv.movie.coil

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
import io.silv.movie.coil.CoilDiskUtils.toImageSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.File


class ByteArrayDiskBackedFetcher<T: Any>(
    override val keyer: Keyer<T>,
    override val diskStore: FetcherDiskStore<T>,
    override val context: Context,
    override val overrideCall: suspend (options: Options, data: T) -> FetchResult? = { _, _ -> null },
    val fetch: suspend (options: Options, data: T) -> ByteArray,
    override val memoryCacheInit: () -> MemoryCache,
    override val diskCacheInit: () -> DiskCache
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
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = CoilDiskUtils.writeResponseToCoverCache(
                    ByteArrayInputStream(response).source().buffer(),
                    imageCacheFile,
                    options
                )
                if (responseCoverCache != null) {
                    CoilDiskUtils.writeToMemCache(
                        options,
                        responseCoverCache.readBytes(),
                        memCacheKey,
                        memCache
                    )
                    return fileLoader(responseCoverCache, diskCacheKey)
                }

                // Read from disk cache
                snapshot = CoilDiskUtils.writeToDiskCache(response, diskCache, diskCacheKey)
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
                        source = ByteArrayInputStream(response).source().buffer(),
                        context = options.context
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.NETWORK
                )
            } catch (e: Exception) {
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }
}
