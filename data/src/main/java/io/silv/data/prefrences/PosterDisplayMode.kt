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

    sealed interface Grid: PosterDisplayMode {
        data object CompactGrid : Grid
        data object ComfortableGrid : Grid
        data object CoverOnlyGrid : Grid
    }

    data object List : PosterDisplayMode

    object Serializer {
        fun deserialize(serialized: String): PosterDisplayMode {
            return Companion.deserialize(serialized)
        }

        fun serialize(value: PosterDisplayMode): String {
            return value.serialize()
        }
    }

    companion object {
        val values by lazy { setOf(Grid.CompactGrid, Grid.ComfortableGrid, List, Grid.CoverOnlyGrid) }
        val default = Grid.CompactGrid

        fun deserialize(serialized: String): PosterDisplayMode {
            return when (serialized) {
                "COMFORTABLE_GRID" -> Grid.ComfortableGrid
                "COMPACT_GRID" -> Grid.CompactGrid
                "COVER_ONLY_GRID" -> Grid.CoverOnlyGrid
                "LIST" -> List
                else -> default
            }
        }
    }

    fun serialize(): String {
        return when (this) {
            Grid.ComfortableGrid -> "COMFORTABLE_GRID"
            Grid.CompactGrid -> "COMPACT_GRID"
            Grid.CoverOnlyGrid -> "COVER_ONLY_GRID"
            List -> "LIST"
        }
    }
}