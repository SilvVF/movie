package io.silv.movie.coil

import coil.ComponentRegistry
import coil.disk.DiskCache
import coil.memory.MemoryCache

inline fun <reified T: Any> ComponentRegistry.Builder.addDiskFetcher(
    fetcher: OkHttpFetcherConfig<T>,
    noinline diskCache: () -> DiskCache,
    noinline memoryCache: () -> MemoryCache
) {

    val realFetcher =  OkHttpDiskBackedFetcher(
        keyer = fetcher.keyer,
        diskStore = fetcher.diskStore,
        context = fetcher.context,
        overrideCall = { options, data ->  fetcher.overrideFetch(options, data) },
        fetch = { options, data ->  fetcher.fetch(options, data) },
        diskCacheInit = diskCache,
        memoryCacheInit = memoryCache
    )

    this.add(realFetcher)
    this.add(fetcher.keyer)
}

inline fun <reified T: Any> ComponentRegistry.Builder.addByteArrayDiskFetcher(
    fetcher: ByteArrayFetcherConfig<T>,
    noinline diskCache: () -> DiskCache,
    noinline memoryCache: () -> MemoryCache
) {

    val realFetcher =  ByteArrayDiskBackedFetcher(
        keyer = fetcher.keyer,
        diskStore = fetcher.diskStore,
        context = fetcher.context,
        overrideCall = { options, data ->  fetcher.overrideFetch(options, data) },
        fetch = { options, data ->  fetcher.fetch(options, data) },
        diskCacheInit = diskCache,
        memoryCacheInit = memoryCache
    )

    this.add(realFetcher)
    this.add(fetcher.keyer)
}