package io.silv.movie.api.service.piped

import androidx.media3.extractor.Extractor
import io.silv.movie.api.model.ChapterSegment
import io.silv.movie.api.model.MetaInfo
import io.silv.movie.api.model.PipedStream
import io.silv.movie.api.model.PreviewFrames
import io.silv.movie.api.model.Streams
import io.silv.movie.api.model.Subtitle
import io.silv.movie.core.StreamItem
import kotlinx.datetime.toKotlinInstant
import kotlinx.io.IOException
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import retrofit2.HttpException


fun VideoStream.toPipedStream(): PipedStream = PipedStream(
    url = content,
    codec = codec,
    format = format.toString(),
    height = height,
    width = width,
    quality = getResolution(),
    mimeType = format?.mimeType,
    bitrate = bitrate,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    fps = fps,
    contentLength = itagItem?.contentLength ?: 0L
)

const val TYPE_STREAM = "stream"
const val TYPE_CHANNEL = "channel"
const val TYPE_PLAYLIST = "playlist"

fun StreamInfoItem.toStreamItem(
    uploaderAvatarUrl: String? = null
): StreamItem = StreamItem(
    type = TYPE_STREAM,
    url = url.toID(),
    title = name,
    uploaded = uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000) ?: -1,
    uploadedDate = textualUploadDate ?: uploadDate?.offsetDateTime()?.toLocalDateTime()?.toLocalDate()
        ?.toString(),
    uploaderName = uploaderName,
    uploaderUrl = uploaderUrl.toID(),
    uploaderAvatar = uploaderAvatarUrl ?: uploaderAvatars.maxByOrNull { it.height }?.url,
    thumbnail = thumbnails.maxByOrNull { it.height }?.url,
    duration = duration,
    views = viewCount,
    uploaderVerified = isUploaderVerified,
    shortDescription = shortDescription,
    isShort = isShortFormContent
)

fun String.toID(): String {
    return this
        .replace(YOUTUBE_FRONTEND_URL, "")
        .replace("/watch?v=", "") // videos
        .replace("/channel/", "") // channels
        .replace("/playlist?list=", "") // playlists
        // channel urls for different categories than the main one
        .removeSuffix("/shorts")
        .removeSuffix("/streams")
        .removeSuffix("/videos")
}

object StreamsExtractor {

    suspend fun extractStreams(videoId: String): Streams {
        val resp = StreamInfo.getInfo("${YOUTUBE_FRONTEND_URL}/watch?v=$videoId")
        return Streams(
            title = resp.name,
            description = resp.description.content,
            uploader = resp.uploaderName,
            uploaderAvatar = resp.uploaderAvatars.maxBy { it.height }.url,
            uploaderUrl = resp.uploaderUrl.toID(),
            uploaderVerified = resp.isUploaderVerified,
            uploaderSubscriberCount = resp.uploaderSubscriberCount,
            category = resp.category,
            views = resp.viewCount,
            likes = resp.likeCount,
            dislikes = 0,
            license = resp.licence,
            hls = resp.hlsUrl,
            dash = resp.dashMpdUrl,
            tags = resp.tags,
            metaInfo = resp.metaInfo.map {
                MetaInfo(
                    it.title,
                    it.content.content,
                    it.urls.map { url -> url.toString() },
                    it.urlTexts
                )
            },
            visibility = resp.privacy.name.lowercase(),
            duration = resp.duration,
            thumbnailUrl = resp.thumbnails.maxBy { it.height }.url,
            relatedStreams = resp.relatedItems.filterIsInstance<StreamInfoItem>().map(StreamInfoItem::toStreamItem),
            chapters = resp.streamSegments.map {
                ChapterSegment(
                    title = it.title,
                    image = it.previewUrl.orEmpty(),
                    start = it.startTimeSeconds.toLong()
                )
            },
            audioStreams = resp.audioStreams.map {
                PipedStream(
                    url = it.content,
                    format = it.format?.toString(),
                    quality = "${it.averageBitrate} bits",
                    bitrate = it.bitrate,
                    mimeType = it.format?.mimeType,
                    initStart = it.initStart,
                    initEnd = it.initEnd,
                    indexStart = it.indexStart,
                    indexEnd = it.indexEnd,
                    contentLength = it.itagItem?.contentLength ?: 0L,
                    codec = it.codec,
                    audioTrackId = it.audioTrackId,
                    audioTrackName = it.audioTrackName,
                    audioTrackLocale = it.audioLocale?.toLanguageTag(),
                    audioTrackType = it.audioTrackType?.name,
                    videoOnly = false
                )
            },
            videoStreams = resp.videoOnlyStreams.map {
                it.toPipedStream().copy(videoOnly = true)
            } + resp.videoStreams.map {
                it.toPipedStream().copy(videoOnly = false)
            },
            previewFrames = resp.previewFrames.map {
                PreviewFrames(
                    it.urls,
                    it.frameWidth,
                    it.frameHeight,
                    it.totalCount,
                    it.durationPerFrame.toLong(),
                    it.framesPerPageX,
                    it.framesPerPageY
                )
            },
            subtitles = resp.subtitles.map {
                Subtitle(
                    it.content,
                    it.format?.mimeType,
                    it.displayLanguageName,
                    it.languageTag,
                    it.isAutoGenerated
                )
            }
        )
    }
}

const val YOUTUBE_FRONTEND_URL = "https://www.youtube.com"
const val YOUTUBE_SHORT_URL = "https://youtu.be"