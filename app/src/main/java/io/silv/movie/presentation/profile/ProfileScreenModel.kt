package io.silv.movie.presentation.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.SignOutScope
import io.github.jan.supabase.gotrue.admin.AdminApi
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.BuildConfig
import io.silv.movie.UserProfileImageData
import io.silv.movie.presentation.EventProducer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber


class ProfileScreenModel(
    private val auth: Auth,
    private val postgrest: Postgrest,
):
    StateScreenModel<ProfileState>(ProfileState.Loading),
    EventProducer<ProfileEvent> by EventProducer.default() {

    var authJob: Job? = null
    private val adminClient: AdminApi by lazy {
        runBlocking {
            val supabase = createSupabaseClient(
                supabaseKey = BuildConfig.SUPABASE_SERVICE_ROLE,
                supabaseUrl = BuildConfig.SUPABASE_URL
            ) {
                install(Auth) {
                    autoLoadFromStorage = false
                    alwaysAutoRefresh = false
                }
                // install other plugins (these will use the service role key)
            }
            supabase.auth.importAuthToken(BuildConfig.SUPABASE_SERVICE_ROLE)

            // Access auth admin api
            supabase.auth.admin
        }
    }

    val authJobInProgress = flow {
        while (true) {
            emit(authJob?.isActive ?: false)
            delay(10)
        }
    }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            false
        )

    init {
        auth.sessionStatus
            .onEach { status ->
                mutableState.value = when(status) {
                    SessionStatus.LoadingFromStorage -> ProfileState.Loading
                    SessionStatus.NetworkError -> ProfileState.LoggedOut("Network Error")
                    is SessionStatus.Authenticated ->
                        ProfileState.LoggedIn(
                            status.session.user ?: return@onEach,
                            profileImageData = UserProfileImageData(
                                status.session.user?.id ?: return@onEach,
                                imageLastUpdated = -1L
                            ),
                        )
                    is SessionStatus.NotAuthenticated -> ProfileState.LoggedOut()
                }
            }
            .launchIn(screenModelScope)
    }

    fun deleteAccount() {
        ioCoroutineScope.launch {
            runCatching {
                adminClient.deleteUser(
                    auth.currentUserOrNull()?.id ?: return@launch,

                )
            }
                .onFailure { Timber.e(it) }
                .onSuccess {
                    emitEvent(ProfileEvent.AccountDeleted)
                    auth.clearSession()
                }
        }
    }

    fun signOut() {
        authJob?.cancel()
        authJob = ioCoroutineScope.launch {
            runCatching {
                auth.signOut(
                    scope = SignOutScope.LOCAL
                )
            }
                .onFailure { Timber.e(it) }
        }
    }

    fun registerWithEmailAndPassword(email: String, password: String) {
        authJob?.cancel()
        authJob = ioCoroutineScope.launch {
            runCatching {
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
                .onFailure { t ->
                    Timber.e(t)
                    mutableState.updateLoggedOut { state ->
                        state.copy(error =  "Failed to create an account")
                    }
                }
                .onSuccess { result ->
                    mutableState.updateLoggedOut { state ->
                        state.copy(error = null)
                    }
                    emitEvent(ProfileEvent.AccountCreated(result?.email.orEmpty()))
                }
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        authJob?.cancel()
        authJob = ioCoroutineScope.launch {

            runCatching {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
                .onFailure { t ->
                    Timber.e(t)
                    mutableState.updateLoggedOut {state ->
                        state.copy(error = "Failed to login")
                    }
                }
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
            runCatching {
                auth.resetPasswordForEmail(email)
            }
                .onSuccess {
                    emitEvent(ProfileEvent.PasswordResetSent)
                }
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
    data class LoggedIn(
        val user: UserInfo,
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
        val dialog: Dialog? = null
    ): ProfileState {

        @Stable
        sealed interface Dialog {

            data object AccountOptions: Dialog
        }
    }
}
