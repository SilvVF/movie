package io.silv.coil_disk_fetcher

import coil.request.Options
import java.io.File

interface FetcherDiskStore<T: Any> {
    fun getImageFile(data: T, options: Options): File?
}
