package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.core.await
import io.silv.movie.data.local.CreditRepository
import io.silv.movie.data.local.networkToLocalCredit
import io.silv.movie.data.model.Credit
import io.silv.movie.data.model.toDomain
import io.silv.movie.api.model.toSCredit
import io.silv.movie.api.service.tmdb.TMDBPersonService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class PersonInfo(
    val bio: String,
    val birthday: String?,
    val deathday: String?,
    val isMale: Boolean,
    val homepage: String?,
    val knownForDepartment: String,
    val placeOfBirth: String,
    val popularity: Double,
)

class PersonViewScreenModel(
    private val personService: TMDBPersonService,
    private val creditsRepo: CreditRepository,
    private val personId: Long,
    private val profilePath: String,
): StateScreenModel<PersonViewState>(PersonViewState.Loading) {

    private var refreshJob: Job? = null

    var refreshing by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true)
            return

        refreshing = true

        refreshJob = ioCoroutineScope.launch {
            runCatching {
                val details = personService.details(personId.toString())
                    .await()
                    .body()!!

                withContext(Dispatchers.Main) {
                    mutableState.update {
                        PersonViewState.Success(
                            PersonInfo(
                                bio = details.biography.orEmpty(),
                                birthday = details.birthday,
                                deathday = details.deathday,
                                isMale = details.gender == 2,
                                homepage = details.homepage,
                                knownForDepartment = details.knownForDepartment.orEmpty(),
                                placeOfBirth = details.placeOfBirth.orEmpty(),
                                popularity = details.popularity ?: -1.0
                            )
                        )
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    mutableState.update{ PersonViewState.Error }
                }
            }
            runCatching {
                val credits = personService.combinedCredits(personId.toString())
                    .await()
                    .body()!!

                credits.cast.forEach { cast ->
                    val credit = cast.toSCredit().toDomain().copy(
                        personId = personId,
                        title = cast.title ?: cast.originalTitle.orEmpty(),
                        profilePath = profilePath,
                        posterPath = "https://image.tmdb.org/t/p/original${cast.posterPath}".takeIf { cast.posterPath.orEmpty().isNotBlank() }
                    )
                    insertCreditWithContent(credit, cast.id.toLong(), cast.mediaType == "movie")
                }
                credits.crew.forEach { crew ->
                    val credit = crew.toSCredit().toDomain().copy(
                        personId = personId,
                        title = crew.title.ifBlank { crew.originalTitle },
                        profilePath = profilePath,
                        posterPath =  "https://image.tmdb.org/t/p/original${crew.posterPath}".takeIf { crew.posterPath.orEmpty().isNotBlank() }
                    )
                    insertCreditWithContent(credit, crew.id.toLong(), crew.mediaType == "movie")
                }
            }
                .onFailure { Timber.d(it) }

            withContext(Dispatchers.Main) { refreshing = false }
        }
    }

    private suspend fun insertCreditWithContent(
        credit: Credit,
        contentId: Long,
        isMovie: Boolean,
    ): Credit? {
        return runCatching {
            creditsRepo.networkToLocalCredit(
                credit,
                contentId,
                isMovie
            )
        }
            .getOrNull()
    }

    val credits = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { creditsRepo.personCreditsPagingSource(personId) },
    ).flow
        .cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )

}

sealed interface PersonViewState {
    data object Loading: PersonViewState
    data object Error: PersonViewState
    data class Success(
        val info: PersonInfo
    ): PersonViewState
}
