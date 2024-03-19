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
import io.silv.movie.coil.CoilDiskUtils.writeSourceToCoverCache
import okhttp3.Response
import timber.log.Timber
import java.io.File


class OkHttpDiskBackedFetcher<T: Any>(
    val fetch: suspend (options: Options, data: T) -> Response,
    override val overrideCall: suspend (options: Options, data: T) -> FetchResult? = { _, _ -> null },
    override val keyer: Keyer<T>,
    override val diskStore: FetcherDiskStore<T>,
    override val context: Context,
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
            val responseBody = checkNotNull(response.body) { "Null response source" }

            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(
                    response,
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
                    return fileLoader(responseCoverCache, diskCacheKey, options)
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
                    responseBody.bytes(),
                    memCacheKey,
                    memCache
                )
                // Read from response if cache is unused or unusable
                return SourceResult(
                    source = ImageSource(
                        source = responseBody.source(),
                        context = options.context
                    ),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                response.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?, options: Options): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Timber.e("DiskBackedFetcher", "Failed to write response data to cover cache ${cacheFile.name}")
            null
        }
    }

    private fun writeToDiskCache(
        response: Response,
        diskCacheKey: String
    ): DiskCache.Snapshot? {
        val editor = diskCache.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                response.body!!.source().readAll(this)
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