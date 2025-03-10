package io.silv.movie.presentation.components.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.covers.cache.ProfileImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import timber.log.Timber


class ProfileScreenModel(
    private val backendRepository: BackendRepository,
    private val auth: Auth,
    private val networkMonitor: NetworkMonitor,
    private val contentListRepository: ContentListRepository,
    private val profileImageCache: ProfileImageCache
):
    StateScreenModel<ProfileState>(ProfileState.Loading),
    EventProducer<ProfileEvent> by EventProducer.default() {

    private val isOnline = networkMonitor.isOnline
        .stateIn(screenModelScope, SharingStarted.Lazily, true)

    val currentUser = backendRepository.currentUser
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            backendRepository.currentUser.value
        )

    init {
        screenModelScope.launch {
            val expiration = auth.currentSessionOrNull()?.expiresAt?.epochSeconds ?: return@launch
            if (expiration < Clock.System.now().epochSeconds) {
                auth.refreshCurrentSession()
            }
        }

        auth.sessionStatus
            .combine(isOnline) { a, b ->  a to b}
            .onEach { (status, online) ->

                Timber.d(status.toString())
                if (!online && state.value.loggedIn == null) {
                    mutableState.value = ProfileState.Offline
                    return@onEach
                }

                mutableState.value = when(status) {
                    SessionStatus.Initializing -> ProfileState.Loading
                    is SessionStatus.RefreshFailure -> ProfileState.LoggedOut(
                        when (val cause = status.cause) {
                            is RefreshFailureCause.InternalServerError -> cause.exception.error
                            is RefreshFailureCause.NetworkError -> cause.exception.message ?: "unknown error"
                        }
                    )
                    is SessionStatus.Authenticated -> {
                        val info =  status.session.user ?: run {
                            Timber.d("clearing session no user")
                            auth.clearSession()
                            return@onEach
                        }
                        ProfileState.LoggedIn(info = info)
                    }
                    is SessionStatus.NotAuthenticated -> ProfileState.LoggedOut()
                }
            }
            .launchIn(screenModelScope)
    }



    fun updateUsername(name: String) {
        screenModelScope.launch {
            val user = backendRepository.currentUser.value ?: return@launch

            backendRepository.updateUser(user.copy(username = name))
        }
    }

    val publicLists = state.map { it.loggedIn?.info?.id }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            contentListRepository.observeLibraryItems("")
                .map { contentListItems ->
                    contentListItems
                        .filter { (list, _) ->
                            list.createdBy == userId && list.public
                        }
                        .sortedBy { it.first.lastModified }
                }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val subscribedLists = state.map { it.loggedIn?.info?.id }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            contentListRepository.observeLibraryItems("")
                .map { contentListItems ->
                    contentListItems
                        .filter { (list, _) ->
                            list.createdBy != userId && list.createdBy != null
                        }
                        .sortedBy { it.first.lastModified }
                }

        }
            .stateIn(
                screenModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun updateProfilePicture(path: String) {
        screenModelScope.launch {
            val user = backendRepository.currentUser.value ?: return@launch
            backendRepository.updateUser(user.copy(profileImage = path))
                .onSuccess {
                    profileImageCache.deleteCustomCover(it.userId)
                }
        }
    }

    fun deleteAccount() {
        ioCoroutineScope.launch {
            if(backendRepository.deleteAccount()) {
                emitEvent(ProfileEvent.AccountDeleted)
                auth.clearSession()
            }
        }
    }

    fun signOut() {
        ioCoroutineScope.launch {
            backendRepository.signOut()
        }
    }

    fun registerWithEmailAndPassword(email: String, password: String) {
        ioCoroutineScope.launch {
            updateJob(true)
            val result = backendRepository.registerWithEmailAndPassword(email, password)
            if (result) {
                mutableState.updateLoggedOut { state ->
                    state.copy(error =  "Failed to create an account")
                }
            } else {
                mutableState.updateLoggedOut { state ->
                    state.copy(error = null)
                }
                emitEvent(ProfileEvent.AccountCreated(email))
            }
            updateJob(false)
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        ioCoroutineScope.launch {
            updateJob(true)
            val result = backendRepository.signInWithEmailAndPassword(email, password)
            if (!result) {
                mutableState.updateLoggedOut {state ->
                    state.copy(error = "Failed to login")
                }
            }
            updateJob(false)
        }
    }

    fun changeLoggedInDialog(dialog: ProfileState.LoggedIn.Dialog?) {
        screenModelScope.launch {
            mutableState.updateLoggedIn { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun changeLoggedOutDialog(dialog: ProfileState.LoggedOut.Dialog?) {
        screenModelScope.launch {
            mutableState.updateLoggedOut { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun resetPassword(email: String) {
        ioCoroutineScope.launch {
            updateJob(true)
            if (backendRepository.resetPassword(email)) {
                emitEvent(ProfileEvent.PasswordResetSent)
            }
            updateJob(false)
        }
    }

    private suspend fun updateJob(inProgress: Boolean) = withContext(Dispatchers.Main){
        mutableState.updateLoggedOut { state ->
            state.copy(jobInProgress = inProgress)
        }
    }

    private fun MutableStateFlow<ProfileState>.updateLoggedIn(
        function: (ProfileState.LoggedIn) -> ProfileState.LoggedIn
    ) {
        update {
            when (it) {
                is ProfileState.LoggedIn -> function(it)
                else -> it
            }
        }
    }

    private fun MutableStateFlow<ProfileState>.updateLoggedOut(
        function: (ProfileState.LoggedOut) -> ProfileState.LoggedOut
    ) {
        update {
            when (it) {
                is ProfileState.LoggedOut -> function(it)
                else -> it
            }
        }
    }
}


sealed interface ProfileEvent {
    data class AccountCreated(val email: String): ProfileEvent
    data object AccountDeleted: ProfileEvent
    data object PasswordResetSent: ProfileEvent
}

@Stable
@Immutable
sealed interface ProfileState {

    @Immutable
    data object Loading: ProfileState

    @Immutable
    data object Offline: ProfileState

    @Immutable
    data class LoggedIn(
        val info: UserInfo,
        val dialog: Dialog? = null
    ): ProfileState {

        @Stable
        sealed interface Dialog {

            data object UserOptions: Dialog

            data object ConfirmDeleteAccount: Dialog
        }
    }

    @Immutable
    data class LoggedOut(
        val error: String? = null,
        val dialog: Dialog? = null,
        val jobInProgress: Boolean = false
    ): ProfileState {

        @Stable
        sealed interface Dialog {

            data object AccountOptions: Dialog
        }
    }

    val loggedIn get() = this as? LoggedIn
}
