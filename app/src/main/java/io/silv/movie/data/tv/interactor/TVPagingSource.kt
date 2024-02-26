package io.silv.movie.data.tv.interactor

import io.silv.movie.core.STVShow
import io.silv.movie.core.await
import io.silv.movie.data.tv.SourceTVPagingSource
import io.silv.movie.network.model.toSTVShow
import io.silv.movie.network.service.tmdb.TMDBConstants
import io.silv.movie.network.service.tmdb.TMDBTVShowService


data class TVPage(val shows: List<STVShow>, val hasNextPage: Boolean)

class DiscoverTVPagingSource(
    private val genres: List<String>,
    private val movieService: TMDBTVShowService
): SourceTVPagingSource() {
    override suspend fun getNextPage(page: Int): TVPage {
        val response = movieService.discover(
            page = page,
            genres = TMDBConstants.genresString(genres, TMDBConstants.JOIN_MODE_MASK_OR)
        )
            .await()
            .body()!!

        return TVPage(
            shows = response.results.map { it.toSTVShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class SearchTVPagingSource(
    private val query: String,
    private val movieService: TMDBTVShowService
): SourceTVPagingSource() {

    override suspend fun getNextPage(page: Int): TVPage {
        val response = movieService.search(
            query = query,
            page = page
        )
            .await()
            .body()!!

        return TVPage(
            shows = response.results.map { it.toSTVShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}


class NowPlayingTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceTVPagingSource() {

    override suspend fun getNextPage(page: Int): TVPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.NowPlaying.toString(),
            page = page
        )
            .await()
            .body()!!

        return TVPage(
            shows = response.results.map { it.toSTVShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}


class TopRatedTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceTVPagingSource() {

    override suspend fun getNextPage(page: Int): TVPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.TopRated.toString(),
            page = page
        )
            .await()
            .body()!!

        return TVPage(
            shows = response.results.map { it.toSTVShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class UpcomingTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceTVPagingSource() {

    override suspend fun getNextPage(page: Int): TVPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.Upcoming.toString(),
            page = page
        )
            .await()
            .body()!!

        return TVPage(
            shows = response.results.map { it.toSTVShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class PopularTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceTVPagingSource() {

    override suspend fun getNextPage(page: Int): TVPage {

        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.Popular.toString(),
            page = page
        )
            .await()
            .body()!!

        return TVPage(
            shows = response.results.map { it.toSTVShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}