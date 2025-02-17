package io.silv.movie.data.content.movie.network

import io.silv.movie.data.content.movie.local.MovieRepository


interface NetworkContentDelegate: SourceMovieRepository, SourceShowRepository

class NetworkContentDelegateImpl(
    private val sourceMovieRepository: SourceMovieRepository,
    private val sourceShowRepository: SourceShowRepository
) : SourceMovieRepository by sourceMovieRepository, SourceShowRepository by sourceShowRepository, NetworkContentDelegate