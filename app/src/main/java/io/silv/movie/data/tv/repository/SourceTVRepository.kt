package io.silv.movie.data.tv.repository

import androidx.paging.PagingSource
import io.silv.movie.core.SGenre
import io.silv.movie.core.STVShow
import io.silv.movie.core.await
import io.silv.movie.data.Filters
import io.silv.movie.data.tv.interactor.DiscoverTVPagingSource
import io.silv.movie.data.tv.interactor.NowPlayingTVPagingSource
import io.silv.movie.data.tv.interactor.PopularTVPagingSource
import io.silv.movie.data.tv.interactor.SearchTVPagingSource
import io.silv.movie.data.tv.interactor.TopRatedTVPagingSource
import io.silv.movie.data.tv.interactor.UpcomingTVPagingSource
import io.silv.movie.network.service.tmdb.TMDBConstants
import io.silv.movie.network.service.tmdb.TMDBTVShowService

typealias TVPagingSourceType = PagingSource<Long, STVShow>

interface SourceTVRepository {

    suspend fun getShow(id: Long): STVShow?

    suspend fun getSourceGenres(): List<SGenre>

    fun discover(filters: Filters): TVPagingSourceType

    fun search(query: String): TVPagingSourceType

    fun nowPlaying(): TVPagingSourceType

    fun popular(): TVPagingSourceType

    fun upcoming(): TVPagingSourceType

    fun topRated(): TVPagingSourceType
}

class SourceTVRepositoryImpl(
    private val tvService: TMDBTVShowService
): SourceTVRepository {
    override suspend fun getShow(id: Long): STVShow? {
        val details = tvService.details(id).await().body() ?: return null
        return STVShow.create().apply {
            this.id = id
            url = "https://api.themoviedb.org/3/tv/$id"
            posterPath = "https://image.tmdb.org/t/p/original${details.posterPath}".takeIf { details.posterPath.isNotBlank() }
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

    override suspend fun getSourceGenres(): List<SGenre> {
        return TMDBConstants.genres
    }

    override fun discover(filters: Filters): TVPagingSourceType {
        return DiscoverTVPagingSource(filters, tvService)
    }

    override fun search(query: String): TVPagingSourceType {
        return SearchTVPagingSource(query, tvService)
    }

    override fun nowPlaying(): TVPagingSourceType {
        return NowPlayingTVPagingSource(tvService)
    }
    override fun popular(): TVPagingSourceType {
        return PopularTVPagingSource(tvService)
    }
    override fun upcoming(): TVPagingSourceType {
        return UpcomingTVPagingSource(tvService)
    }

    override fun topRated(): TVPagingSourceType {
        return TopRatedTVPagingSource(tvService)
    }
}