package io.silv.movie.data.content.movie.network

import androidx.paging.PagingSource
import io.silv.movie.core.SGenre
import io.silv.movie.core.SShow
import io.silv.movie.core.await
import io.silv.movie.data.content.movie.model.ContentPagedType
import io.silv.movie.data.content.movie.model.Filters
import io.silv.movie.network.service.tmdb.TMDBConstants
import io.silv.movie.network.service.tmdb.TMDBTVShowService

typealias TVPagingSourceType = PagingSource<Long, SShow>

interface SourceShowRepository {

    suspend fun getShow(id: Long): SShow?

    suspend fun getShowGenres(): List<SGenre>

    fun discoverShows(filters: Filters): TVPagingSourceType

    fun searchShows(query: String): TVPagingSourceType

    fun nowPlayingShows(): TVPagingSourceType

    fun popularShows(): TVPagingSourceType

    fun upcomingShows(): TVPagingSourceType

    fun topRatedShows(): TVPagingSourceType
}

fun SourceShowRepository.getShowPager(type: ContentPagedType) = when (type) {
    is ContentPagedType.Search -> searchShows(type.query)
    is ContentPagedType.Default -> {
        when(type) {
            ContentPagedType.Default.Popular -> popularShows()
            ContentPagedType.Default.TopRated -> topRatedShows()
            ContentPagedType.Default.Upcoming -> upcomingShows()
        }
    }
    is ContentPagedType.Discover -> discoverShows(type.filters)
}

class SourceShowRepositoryImpl(
    private val tvService: TMDBTVShowService
): SourceShowRepository {
    override suspend fun getShow(id: Long): SShow? {
        val details = tvService.details(id).await().body() ?: return null
        return SShow.create().apply {
            this.id = id
            url = "https://api.themoviedb.org/3/tv/$id"
            posterPath = "https://image.tmdb.org/t/p/original${details.posterPath}".takeIf { details.posterPath.orEmpty().isNotBlank() }
            adult = details.adult
            releaseDate = details.firstAirDate
            overview = details.overview
            genres = details.genres.map { Pair(it.id, it.name) }
            genreIds = details.genres.map { it.id }
            originalLanguage = details.originalLanguage
            originalTitle = details.originalName
            title = details.name
            backdropPath = details.backdropPath
            popularity = details.popularity
            voteCount = details.voteCount
            voteAverage = details.voteAverage
            productionCompanies = details.productionCompanies.map { it.name }
            status = io.silv.movie.core.Status.fromString(details.status)
        }
    }

    override suspend fun getShowGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discoverShows(filters: Filters): TVPagingSourceType {
        return DiscoverTVPagingSource(filters, tvService)
    }

    override fun searchShows(query: String): TVPagingSourceType {
        return SearchTVPagingSource(query, tvService)
    }

    override fun nowPlayingShows(): TVPagingSourceType {
        return NowPlayingTVPagingSource(tvService)
    }
    override fun popularShows(): TVPagingSourceType {
        return PopularTVPagingSource(tvService)
    }
    override fun upcomingShows(): TVPagingSourceType {
        return UpcomingTVPagingSource(tvService)
    }

    override fun topRatedShows(): TVPagingSourceType {
        return TopRatedTVPagingSource(tvService)
    }
}