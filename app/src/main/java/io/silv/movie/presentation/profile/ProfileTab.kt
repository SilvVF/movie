package io.silv.movie.presentation.profile

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.browse.components.RemoveEntryDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

object ProfileTab: Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = stringResource(id = R.string.profile_tab_title),
            icon = rememberVectorPainter(image = Icons.Filled.Person)
        )

    @Composable
    override fun Content() {
        Navigator(ProfileScreen) {
            FadeTransition(it)
        }
    }
}

object ProfileScreen: Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<ProfileScreenModel>()
        val state by screenModel.state.collectAsStateWithLifecycle()
        val authJobInProgress by screenModel.authJobInProgress.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        CollectEventsWithLifecycle(screenModel) { event ->
            when (event) {
                is ProfileEvent.AccountCreated -> snackbarHostState.showSnackbar(
                    message = "Account created for ${event.email} make sure to confirm your email",
                    duration = SnackbarDuration.Indefinite,
                    withDismissAction = false,
                    actionLabel = "Ok"
                )
                ProfileEvent.AccountDeleted -> snackbarHostState.showSnackbar(
                    message = "Account Deleted",
                    duration = SnackbarDuration.Indefinite,
                    withDismissAction = false,
                    actionLabel = "Ok"
                )
                ProfileEvent.PasswordResetSent ->  snackbarHostState.showSnackbar(
                    message = "Password reset sent",
                    duration = SnackbarDuration.Indefinite,
                    withDismissAction = false,
                    actionLabel = "Ok"
                )
            }
        }

        Crossfade(targetState = state, label = "") { targetState ->
            when (targetState) {
                ProfileState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }

                is ProfileState.LoggedIn -> {
                    SignedInScreen(
                        showOptionsClick = {
                            screenModel.changeLoggedInDialog(ProfileState.LoggedIn.Dialog.UserOptions)
                        },
                        snackbarHostState = snackbarHostState,
                        state = targetState
                    )
                    when (targetState.dialog) {
                        null -> Unit
                        ProfileState.LoggedIn.Dialog.UserOptions -> {
                            UserOptionsBottomSheet(
                                deleteAccountClick = {
                                    screenModel.changeLoggedInDialog(ProfileState.LoggedIn.Dialog.ConfirmDeleteAccount)
                                },
                                logOutClick = {
                                    screenModel.signOut()
                                },
                                onDismiss = { screenModel.changeLoggedInDialog(null) }
                            )
                        }

                        ProfileState.LoggedIn.Dialog.ConfirmDeleteAccount -> RemoveEntryDialog(
                            onDismissRequest = { screenModel.changeLoggedInDialog(null) },
                            onConfirm = { screenModel.deleteAccount() },
                            entryToRemove = targetState.user.email.orEmpty()
                        )
                    }
                }

                is ProfileState.LoggedOut -> {
                    val scope = rememberCoroutineScope()
                    val auth = koinInject<ComposeAuth>()
                    val context = LocalContext.current

                    fun showSnackBar(message: String) {
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }

                    val signInWithGoogle = auth.rememberSignInWithGoogle(
                        onResult = { result ->
                            when (result) {
                                is NativeSignInResult.Success -> {
                                    Toast.makeText(
                                        context,
                                        R.string.sign_in_success,
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }

                                is NativeSignInResult.ClosedByUser -> Unit
                                is NativeSignInResult.Error -> showSnackBar(result.message)
                                is NativeSignInResult.NetworkError -> showSnackBar(result.message)
                            }
                        }
                    )

                    var password by rememberSaveable { mutableStateOf("") }
                    var email by rememberSaveable { mutableStateOf("") }

                    val signIn = remember {
                        { email: String, password: String ->
                            screenModel.signInWithEmailAndPassword(
                                email,
                                password
                            )
                        }
                    }
                    val register = remember {
                        { email: String, password: String ->
                            screenModel.registerWithEmailAndPassword(
                                email,
                                password
                            )
                        }
                    }

                    val resetScreen = remember(email) { ResetPasswordScreen(email) }
                    val resetResultLauncher = rememberScreenWithResultLauncher(
                        screen = resetScreen
                    ) {
                        screenModel.resetPassword(it.email)
                    }

                    AuthScreenContent(
                        password = { password },
                        email = { email },
                        updateEmail = { email = it },
                        updatePassword = { password = it },
                        signInWithGoogle = {
                            signInWithGoogle.startFlow()
                        },
                        error = targetState.error,
                        inProgress = authJobInProgress,
                        signInWithEmailAndPassword = { signIn(email, password) },
                        registerWithEmailAndPassword = { register(email, password) },
                        optionsButtonClick = { screenModel.changeLoggedOutDialog(ProfileState.LoggedOut.Dialog.AccountOptions) },
                        snackbarHostState = snackbarHostState
                    )

                    when (targetState.dialog) {
                        null -> Unit
                        ProfileState.LoggedOut.Dialog.AccountOptions -> {
                            AccountOptionsBottomSheet(
                                resetPasswordClick = { resetResultLauncher.launch() },
                                onDismiss = { screenModel.changeLoggedOutDialog(null) }
                            )
                        }
                    }
                }
            }
        }
    }
}




