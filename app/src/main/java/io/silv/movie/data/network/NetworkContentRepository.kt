package io.silv.movie.data.network




interface NetworkContentDelegate: SourceMovieRepository, SourceShowRepository

class NetworkContentDelegateImpl(
    private val sourceMovieRepository: SourceMovieRepository,
    private val sourceShowRepository: SourceShowRepository
) : SourceMovieRepository by sourceMovieRepository, SourceShowRepository by sourceShowRepository,
    NetworkContentDelegate