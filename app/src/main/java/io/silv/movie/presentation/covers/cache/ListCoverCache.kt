/**
 * Copyright 2015 Javier Tomás
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.silv.movie.presentation.covers.cache

import android.content.Context
import io.silv.movie.core.DiskUtil
import io.silv.movie.data.content.lists.ContentList
import okio.IOException
import java.io.File
import java.io.InputStream

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Names of files are created with the md5 of the thumbnail URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class ListCoverCache(private val context: Context) {

    companion object {
        private const val CUSTOM_COVERS_DIR = "listcovers/custom"
    }


    private val customCoverCacheDir = getCacheDir(CUSTOM_COVERS_DIR)

    /**
     * Returns the custom cover from cache.
     *
     * @param movieId the movie id.
     * @return cover image.
     */
    fun getCustomCoverFile(listId: Long?): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(listId.toString()))
    }

    /**
     * Saves the given stream as the movie's custom cover to cache.
     *
     * @param movie the movie.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(list: ContentList, inputStream: InputStream) {
        getCustomCoverFile(list.id).outputStream().use {
            inputStream.copyTo(it)
        }
    }

    /**
     * Delete custom cover of the movie from the cache
     *
     * @param movieId the movie id.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(listId: Long?): Boolean {
        return getCustomCoverFile(listId).let {
            it.exists() && it.delete()
        }
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}