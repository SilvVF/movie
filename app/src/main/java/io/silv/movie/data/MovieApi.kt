package io.silv.movie.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.silv.movie.types.movie.popular.MovieListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieApi(
    private val client: HttpClient
) {
    private val baseUrl = "https://api.themoviedb.org/3/"

    suspend fun getTopRatedMovies(page: Int = 1): MovieListResponse = withContext(Dispatchers.IO){
        val trendingEndpoint = "/movie/top_rated?language=en-US&page=$page"

        client.get(baseUrl + trendingEndpoint).body()
    }

    suspend fun getPopularMovies(page: Int = 1): MovieListResponse = withContext(Dispatchers.IO){
        val popularEndpoint = "/movie/popular?language=en-US&page=$page"

        client.get(baseUrl + popularEndpoint).body()
    }

    suspend fun getUpcomingMovies(page: Int = 1): MovieListResponse = withContext(Dispatchers.IO){
        val trendingEndpoint = "/movie/upcoming?language=en-US&page=$page"

        client.get(baseUrl + trendingEndpoint).body()
    }

    suspend fun getSearchMovies(
        page: Int = 1,
        query: String
    ): MovieListResponse = withContext(Dispatchers.IO){
        val popularEndpoint = "/search/movie?query=$query&language=en-US&page=$page"

        client.get(baseUrl + popularEndpoint).body()
    }
}