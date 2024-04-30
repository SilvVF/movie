package io.silv.movie.presentation.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.user.UserInfo
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.cache.ProfileImageCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.user.UserRepository
import io.silv.movie.presentation.EventProducer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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


class ProfileScreenModel(
    private val userRepository: UserRepository,
    private val auth: Auth,
    private val networkMonitor: NetworkMonitor,
    private val contentListRepository: ContentListRepository,
    private val profileImageCache: ProfileImageCache
):
    StateScreenModel<ProfileState>(ProfileState.Loading),
    EventProducer<ProfileEvent> by EventProducer.default() {

    private val isOnline = networkMonitor.isOnline
        .stateIn(screenModelScope, SharingStarted.Lazily, true)

    val currentUser = userRepository.currentUser
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            userRepository.currentUser.value
        )

    init {
        auth.sessionStatus
            .combine(isOnline) { a, b ->  a to b}
            .onEach { (status, online) ->

                if (!online && state.value.loggedIn == null) {
                    mutableState.value = ProfileState.Offline
                    return@onEach
                }

                mutableState.value = when(status) {
                    SessionStatus.LoadingFromStorage -> ProfileState.Loading
                    SessionStatus.NetworkError ->
                        ProfileState.LoggedOut("Network Error")
                    is SessionStatus.Authenticated -> {
                        if(state.value.loggedIn != null) {
                            return@onEach
                        }
                        val info =  status.session.user ?: return@onEach
                        ProfileState.LoggedIn(info = info)
                    }
                    is SessionStatus.NotAuthenticated -> ProfileState.LoggedOut()
                }
            }
            .launchIn(screenModelScope)
    }



    fun updateUsername(name: String) {
        screenModelScope.launch {
            val user = userRepository.currentUser.value ?: return@launch

            userRepository.updateUser(user.copy(username = name))
        }
    }

    val publicLists = state.map { it.loggedIn?.info?.id }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            contentListRepository.observeLibraryItems("")
                .map { contentListItems ->
                    contentListItems.applySorting()
                }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )

    val subscribedLists = state.map { it.loggedIn?.info?.id }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            contentListRepository.observeLibraryItems("")
                .map { contentListItems ->
                    contentListItems.applySorting()
                }

        }
            .stateIn(
                screenModelScope,
                SharingStarted.WhileSubscribed(5_000),
                persistentListOf()
            )

    private fun  List<Pair<ContentList, List<ContentItem>>>.applySorting(): ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>> {
        return sortedBy {it.first.lastModified }
            .map { it.first to it.second.toImmutableList() }
            .toImmutableList()
    }

    fun updateProfilePicture(path: String) {
        screenModelScope.launch {
            val user = userRepository.currentUser.value ?: return@launch

            val new = userRepository.updateUser(user.copy(profileImage = path))
            if(new != null) {
                profileImageCache.deleteCustomCover(new.userId)
            }
        }
    }

    fun deleteAccount() {
        ioCoroutineScope.launch {
            if(userRepository.deleteAccount()) {
                emitEvent(ProfileEvent.AccountDeleted)
                auth.clearSession()
            }
        }
    }

    fun signOut() {
        ioCoroutineScope.launch {
            userRepository.signOut()
        }
    }

    fun registerWithEmailAndPassword(email: String, password: String) {
        ioCoroutineScope.launch {
            updateJob(true)
            val result = userRepository.registerWithEmailAndPassword(email, password)
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
            val result = userRepository.signInWithEmailAndPassword(email, password)
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
            if (userRepository.resetPassword(email)) {
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
