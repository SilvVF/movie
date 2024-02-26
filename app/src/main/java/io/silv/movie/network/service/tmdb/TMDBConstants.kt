package io.silv.movie.network.service.tmdb


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object TMDBConstants {

    val genreIdToName by lazy {
        buildMap {
            genres.forEach {
                put(it.id, it.name)
            }
        }
    }

    val genreNameToId by lazy {
        buildMap {
            genres.forEach { put(it.name, it.id) }
        }
    }

    val genres: List<io.silv.movie.core.SGenre> by lazy {
        val json = """
            {
              "genres": [
                {
                  "id": 28,
                  "name": "Action"
                },
                {
                  "id": 12,
                  "name": "Adventure"
                },
                {
                  "id": 16,
                  "name": "Animation"
                },
                {
                  "id": 35,
                  "name": "Comedy"
                },
                {
                  "id": 80,
                  "name": "Crime"
                },
                {
                  "id": 99,
                  "name": "Documentary"
                },
                {
                  "id": 18,
                  "name": "Drama"
                },
                {
                  "id": 14,
                  "name": "Fantasy"
                },
                {
                  "id": 36,
                  "name": "History"
                },
                {
                  "id": 27,
                  "name": "Horror"
                },
                {
                  "id": 10402,
                  "name": "Music"
                },
                {
                  "id": 9648,
                  "name": "Mystery"
                },
                {
                  "id": 10749,
                  "name": "Romance"
                },
                {
                  "id": 878,
                  "name": "Science Fiction"
                },
                {
                  "id": 10770,
                  "name": "TV Movie"
                },
                {
                  "id": 53,
                  "name": "Thriller"
                },
                {
                  "id": 10752,
                  "name": "War"
                },
                {
                     "id": 10759,
                     "name": "Action & Adventure"
                  },
                    {
                      "id": 10751,
                      "name": "Family"
                    },
                    {
                      "id": 10762,
                      "name": "Kids"
                    },
                    {
                      "id": 10763,
                      "name": "News"
                    },
                    {
                      "id": 10764,
                      "name": "Reality"
                    },
                    {
                      "id": 10765,
                      "name": "Sci-Fi & Fantasy"
                    },
                    {
                      "id": 10766,
                      "name": "Soap"
                    },
                    {
                      "id": 10767,
                      "name": "Talk"
                    },
                    {
                      "id": 10768,
                      "name": "War & Politics"
                    },
                    {
                      "id": 37,
                      "name": "Western"
                    }
              ]
            }
        """.trimIndent()

        Json.decodeFromString<TmdbGenres>(json).genres
    }

    private fun genresStringFromIds(genreIds: List<Long>, joinMode: Int): String {
        return when {
            joinMode and JOIN_MODE_MASK_OR == JOIN_MODE_MASK_OR -> {
                genreIds.joinToString(separator = "|")
            }
            joinMode and JOIN_MODE_MASK_AND == JOIN_MODE_MASK_AND -> {
                genreIds.joinToString(",")
            }
            else -> ""
        }
    }

    fun genresString(genres: List<String>, joinMode: Int): String {
        val genreIds = genres.mapNotNull { genreNameToId[it] }
        return genresStringFromIds(genreIds, joinMode)
    }

    const val JOIN_MODE_MASK_OR = 0x1
    const val JOIN_MODE_MASK_AND = 0x10

    @Serializable
    data class TmdbGenres(
        @SerialName("genres")
        val genres: List<TmdbGenre>
    ) {

        @Serializable
        data class TmdbGenre(
            @SerialName("id")
            override var id: Long?,
            @SerialName("name")
            override var name: String
        ): io.silv.movie.core.SGenre
    }
}