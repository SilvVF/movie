package io.silv.movie.core

import kotlinx.serialization.Serializable

@Serializable
data class StreamItem(
    val url: String? = null,
    val type: String? = null,
    var title: String? = null,
    var thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val duration: Long? = null,
    val views: Long? = null,
    val uploaderVerified: Boolean? = null,
    val uploaded: Long = 0,
    val shortDescription: String? = null,
    val isShort: Boolean = false
)