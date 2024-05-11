package io.silv.movie.presentation.media.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// Based off of https://github.com/TeamPiped/Piped/blob/master/src/utils/DashUtils.js

object DashHelper {

    /**
     * Convert a proxied Piped url to a YouTube url that's not proxied
     */
    fun unwrapUrl(url: String, unwrap: Boolean = true) = url.toHttpUrlOrNull()
        ?.takeIf { unwrap }
        ?.let {
            val host = it.queryParameter("host")
            // if there's no host parameter specified, there's no way to unwrap the URL
            // and the proxied one must be used. That's the case if using LBRY.
            if (host.isNullOrEmpty()) return@let url

            it.newBuilder()
                .host(host)
                .removeAllQueryParameters("host")
                // .removeAllQueryParameters("ump")
                .removeAllQueryParameters("qhash")
                .build()
                .toString()
        } ?: url

}
