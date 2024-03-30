package io.silv.movie.presentation.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.user.UserInfo
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.UserProfileImageData
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.user.User
import io.silv.movie.data.user.UserRepository
import io.silv.movie.presentation.EventProducer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
    private val userRepository: UserRepository,
    private val auth: Auth,
    private val networkMonitor: NetworkMonitor,
    private val contentListRepository: ContentListRepository,
):
    StateScreenModel<ProfileState>(ProfileState.Loading),
    EventProducer<ProfileEvent> by EventProducer.default() {

    private val isOnline = networkMonitor.isOnline
        .stateIn(screenModelScope, SharingStarted.Lazily, true)

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
                        ProfileState.LoggedIn(
                            info = info,
                            user = userRepository.getUser(info.id) ?: run {
                                auth.clearSession()
                                return@onEach
                            },
                            profileImageData = UserProfileImageData(info.id, isUserMe = true)
                        )
                    }
                    is SessionStatus.NotAuthenticated -> ProfileState.LoggedOut()
                }
            }
            .launchIn(screenModelScope)
    }



    fun updateUsername(name: String) {
        screenModelScope.launch {
            val user = state.value.loggedIn?.user ?: return@launch

            val new = userRepository.updateUser(user.copy(username = name))
                ?: return@launch

            mutableState.updateLoggedIn {state ->
                state.copy(user = new)
            }
        }
    }

    val publicLists = contentListRepository.observeLibraryItems("")
        .map { contentListItems ->
            val userid =  state.value.loggedIn?.info?.id
            contentListItems.filter { item ->
                val createdBy = item.list.createdBy

                Timber.d("pub $createdBy  $userid $item")
                createdBy != null && createdBy == userid && item.list.public
            }
        }.map {
            it.groupBy { it.list }
                .mapValues { (k, v) ->
                    v.mapNotNull { (it as? ContentListItem.Item)?.contentItem  }.toImmutableList()
                }
                .toList()
                .toImmutableList()
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val subscribedLists = contentListRepository.observeLibraryItems("")
        .map { contentListItems ->
            val userid =  state.value.loggedIn?.info?.id
            contentListItems.filter { item ->
                val createdBy = item.list.createdBy

                Timber.d("sub $createdBy  $userid $item")
                createdBy != null && createdBy != userid && userid != null
            }
        }.map {
            it.groupBy { it.list }
                .mapValues { (k, v) ->
                    v.mapNotNull { (it as? ContentListItem.Item)?.contentItem  }.toImmutableList()
                }
                .toList()
                .toImmutableList()
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun updateProfilePicture(path: String) {
        screenModelScope.launch {

            val user = state.value.loggedIn?.user ?: return@launch

            val new = userRepository.updateUser(user.copy(profileImage = path))
                ?: return@launch

            mutableState.updateLoggedIn { state ->
                state.copy(
                    user = new,
                    profileImageData = UserProfileImageData(
                        userId = new.userId,
                        isUserMe = true,
                        imageLastUpdated = Clock.System.now().toEpochMilliseconds()
                    )
                )
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
        val user: User,
        val dialog: Dialog? = null,
        val profileImageData: UserProfileImageData
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
