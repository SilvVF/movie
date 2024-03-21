package io.silv.movie

import android.content.Context
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import io.silv.movie.coil.ByteArrayFetcherConfig
import io.silv.movie.coil.FetcherDiskStore
import io.silv.movie.coil.FetcherDiskStoreImageFile
import io.silv.movie.data.cache.ProfileImageCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
private data class UserProfileImageResponse(
    @SerialName("profile_image")
    val profileImage: String
)

data class UserProfileImageData(
    val userId: String,
    val imageLastUpdated: Long = -1L
) {

    val path by lazy {
        runBlocking {

            val koin = object : KoinComponent {}
            val postgrest by koin.inject<Postgrest>()

            postgrest["users"]
                .select(
                    columns = Columns.list("profile_image")
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(1)
            }
                .decodeSingle<UserProfileImageResponse>()
                .profileImage
        }
    }
}

data class BucketFetchItem(
    val bucket: String,
    val path: String
)

class BucketItemFetcher(
    override val context: Context,
    private val storageLazy: Lazy<Storage>,
    private val profileImageCacheLazy: Lazy<ProfileImageCache>,
): ByteArrayFetcherConfig<BucketFetchItem> {

    private val storage
        get() = storageLazy.value

    private val profileImageCache
        get() = profileImageCacheLazy.value

    override val keyer: Keyer<BucketFetchItem> =
        Keyer { data, options -> "${data.bucket};${data.path}" }

    override val diskStore: FetcherDiskStore<BucketFetchItem>
        get() = FetcherDiskStoreImageFile { data, options ->
            if (data.bucket == "profile_pictures") {
                profileImageCache.getCoverFile(data.path)
            } else {
                null
            }
        }

    override suspend fun fetch(options: Options, data: BucketFetchItem): ByteArray {
        val bucket = storage[data.bucket]
        return bucket.downloadPublic(data.path)
    }
}

class UserProfileImageFetcher(
    override val context: Context,
    private val storageLazy: Lazy<Storage>,
    private val profileImageCacheLazy: Lazy<ProfileImageCache>,
): ByteArrayFetcherConfig<UserProfileImageData> {

    private val storage
        get() = storageLazy.value

    private val profileImageCache
        get() = profileImageCacheLazy.value

    override val keyer: Keyer<UserProfileImageData> =
        Keyer { data, options ->
            val hasCustomCover = profileImageCache.getCustomCoverFile(data.userId).exists()
            if (hasCustomCover) {
                "${data.userId};${data.imageLastUpdated}"
            } else {
                "profile_pictures;${data.path}"
            }
        }

    override val diskStore: FetcherDiskStore<UserProfileImageData> =
        FetcherDiskStoreImageFile { data, _ ->
            profileImageCache.getCoverFile(data.path)
        }

    override suspend fun overrideFetch(options: Options, data: UserProfileImageData): FetchResult? {
        // Use custom cover if exists
        val useCustomCover = options.parameters.value(ContentPosterFetcher.USE_CUSTOM_COVER) ?: true
        if (useCustomCover) {
            val customCoverFile =  profileImageCache.getCustomCoverFile(data.userId)
            if (customCoverFile.exists()) {
                return SourceResult(
                    source = ImageSource(
                        file = customCoverFile.toOkioPath(),
                        diskCacheKey = keyer.key(data, options).takeIf {
                            !(options.parameters.value(DISABLE_KEYS) ?: false)
                        }
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }
        }
        return null
    }

    override suspend fun fetch(options: Options, data: UserProfileImageData): ByteArray {
        val bucket = storage["profile_pictures"]
        return bucket.downloadPublic(data.path)
    }

    companion object {
        const val DISABLE_KEYS = "disable_keys"
    }
}