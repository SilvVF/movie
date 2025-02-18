package io.silv.movie.data.network

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.movie.core.SShow
import io.silv.movie.core.await
import io.silv.movie.data.model.Filters
import io.silv.movie.data.model.GenreMode
import io.silv.movie.api.model.toSShow
import io.silv.movie.api.service.tmdb.TMDBConstants
import io.silv.movie.api.service.tmdb.TMDBTVShowService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShowPage(val shows: List<SShow>, val hasNextPage: Boolean)

abstract class SourceShowPagingSource : PagingSource<Long, SShow>() {

    abstract suspend fun getNextPage(page: Int): ShowPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SShow> {
        val page = params.key ?: 1

        val showPage = try {
            withContext(Dispatchers.IO) {
                getNextPage(page.toInt())
                    .takeIf { it.shows.isNotEmpty() }
                    ?: error("Empty page")
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = showPage.shows,
            prevKey = null,
            nextKey = if (showPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, SShow>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}

class DiscoverTVPagingSource(
    private val filters: Filters,
    private val movieService: TMDBTVShowService
): SourceShowPagingSource() {

    override suspend fun getNextPage(page: Int): ShowPage {
        val response = movieService.discover(
            page = page,
            genres = TMDBConstants.genresString(
                filters.genres.map { it.name },
                if(filters.genreMode == GenreMode.Or) TMDBConstants.JOIN_MODE_MASK_OR else TMDBConstants.JOIN_MODE_MASK_AND
            ),
            sortBy = filters.sortingOption.sort,
            companies = filters.companies.value.ifBlank { null },
            people = filters.people.value.ifBlank { null },
            keywords = filters.keywords.value.ifBlank { null },
            year = filters.year.value.toIntOrNull(),
            voteAverage = filters.voteAverage.value.toFloatOrNull(),
            voteCount = filters.voteCount.value.toFloatOrNull()
        )
            .await()
            .body()!!

        return ShowPage(
            shows = response.results.map { it.toSShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class SearchTVPagingSource(
    private val query: String,
    private val movieService: TMDBTVShowService
): SourceShowPagingSource() {

    override suspend fun getNextPage(page: Int): ShowPage {
        val response = movieService.search(
            query = query,
            page = page
        )
            .await()
            .body()!!

        return ShowPage(
            shows = response.results.map { it.toSShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}


class NowPlayingTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceShowPagingSource() {

    override suspend fun getNextPage(page: Int): ShowPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.NowPlaying.toString(),
            page = page
        )
            .await()
            .body()!!

        return ShowPage(
            shows = response.results.map { it.toSShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}


class TopRatedTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceShowPagingSource() {

    override suspend fun getNextPage(page: Int): ShowPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.TopRated.toString(),
            page = page
        )
            .await()
            .body()!!

        return ShowPage(
            shows = response.results.map { it.toSShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class UpcomingTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceShowPagingSource() {

    override suspend fun getNextPage(page: Int): ShowPage {
        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.Upcoming.toString(),
            page = page
        )
            .await()
            .body()!!

        return ShowPage(
            shows = response.results.map { it.toSShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

class PopularTVPagingSource(
    private val movieService: TMDBTVShowService
): SourceShowPagingSource() {

    override suspend fun getNextPage(page: Int): ShowPage {

        val response = movieService.tvList(
            type = TMDBTVShowService.TVType.Popular.toString(),
            page = page
        )
            .await()
            .body()!!

        return ShowPage(
            shows = response.results.map { it.toSShow() },
            hasNextPage = response.page < response.totalPages
        )
    }
}

