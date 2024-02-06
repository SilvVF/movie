package io.silv.coil_disk_fetcher

import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlin.reflect.KClass

inline fun <reified T: Any> ImageLoader.Builder.addDiskFetcher(
    fetcher: DiskBackedFetcher<T>,
    noinline diskCache: () -> DiskCache? =  { null },
    noinline memoryCache: () -> MemoryCache? =  { null }
): ImageLoader.Builder {
    return this.apply {
        addDiskFetcher(fetcher, T::class, diskCache, memoryCache)
    }
}

fun <T: Any> ImageLoader.Builder.addDiskFetcher(
    fetcher: DiskBackedFetcher<T>,
    kClass: KClass<T>,
    diskCache: () -> DiskCache? =  { null },
    memoryCache: () -> MemoryCache? =  { null }
) {
    val realFetcher =  DiskBackedFetcherImpl(
        keyer = fetcher.keyer,
        diskStore = fetcher.diskStore,
        context = fetcher.context,
        overrideCall = { options, data ->  fetcher.overrideFetch(options, data) },
        fetch = { options, data ->  fetcher.fetch(options, data) },
        diskCacheInit = diskCache ,
        memoryCacheInit = memoryCache
    )

    components {
        add(realFetcher, kClass.java)
    }
    diskCache(realFetcher.diskCache)
    memoryCache(realFetcher.memCache)
}