package io.silv.movie.data.content.movie.local


interface LocalContentDelegate: MovieRepository , ShowRepository

class LocalContentRepositoryImpl(
    private val movieRepository: MovieRepository,
    private val showRepository: ShowRepository,
) : MovieRepository by movieRepository, ShowRepository by showRepository, LocalContentDelegate