/*
Copyright 2015 Javier Tomás

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package io.silv.data.prefrences

sealed interface PosterDisplayMode {

    data object CompactGrid : PosterDisplayMode
    data object ComfortableGrid : PosterDisplayMode
    data object List : PosterDisplayMode
    data object CoverOnlyGrid : PosterDisplayMode

    object Serializer {
        fun deserialize(serialized: String): PosterDisplayMode {
            return Companion.deserialize(serialized)
        }

        fun serialize(value: PosterDisplayMode): String {
            return value.serialize()
        }
    }

    companion object {
        val values by lazy { setOf(CompactGrid, ComfortableGrid, List, CoverOnlyGrid) }
        val default = CompactGrid

        fun deserialize(serialized: String): PosterDisplayMode {
            return when (serialized) {
                "COMFORTABLE_GRID" -> ComfortableGrid
                "COMPACT_GRID" -> CompactGrid
                "COVER_ONLY_GRID" -> CoverOnlyGrid
                "LIST" -> List
                else -> default
            }
        }
    }

    fun serialize(): String {
        return when (this) {
            ComfortableGrid -> "COMFORTABLE_GRID"
            CompactGrid -> "COMPACT_GRID"
            CoverOnlyGrid -> "COVER_ONLY_GRID"
            List -> "LIST"
        }
    }
}