package io.silv.movie.coil.fetchers

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
import io.silv.movie.coil.core.ByteArrayFetcherConfig
import io.silv.movie.coil.core.FetcherDiskStore
import io.silv.movie.coil.core.FetcherDiskStoreImageFile
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.presentation.covers.cache.ProfileImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath

@Serializable
private data class UserProfileImageResponse(
    @SerialName("profile_image")
    val profileImage: String? = null
)

class UserProfileImageFetcher(
    override val context: Context,
    private val storageLazy: Lazy<Storage>,
    private val profileImageCacheLazy: Lazy<ProfileImageCache>,
    private val postgrestLazy: Lazy<Postgrest>,
): ByteArrayFetcherConfig<UserProfileImageData> {

    private val storage
        get() = storageLazy.value

    private val profileImageCache
        get() = profileImageCacheLazy.value

    private val postgrest
        get() = postgrestLazy.value

    override val keyer: Keyer<UserProfileImageData> =
        Keyer { data, options ->
            val hasCustomCover = profileImageCache.getCustomCoverFile(data.userId).exists()
            if (hasCustomCover || data.isUserMe) {
                "${data.userId};${data.path}"
            } else {
                "profile_pictures;${data.path}"
            }
        }

    override val diskStore: FetcherDiskStore<UserProfileImageData> =
        FetcherDiskStoreImageFile { data, _ ->
            if (data.isUserMe) {
                profileImageCache.getCustomCoverFile(data.userId)
            } else {
                profileImageCache.getCoverFile(data.path)
            }
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
                        diskCacheKey = keyer.key(data, options)
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }
        }
        return null
    }

    private suspend fun getUserProfilePath(userId: String) = withContext(Dispatchers.IO) {
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

    override suspend fun fetch(options: Options, data: UserProfileImageData): ByteArray {
        val bucket = storage["profile_pictures"]

        if (data.path.isNullOrBlank() && !data.fetchPath)
            error("Path is blank and set to not fetch")

        return bucket.downloadPublic(
            data.path
                ?: getUserProfilePath(data.userId)
                ?: error("failed to fetch user profile image")
        )
    }
}