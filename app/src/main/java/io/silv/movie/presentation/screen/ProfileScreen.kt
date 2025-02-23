package io.silv.movie.presentation.screen

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExploreOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.ui.annotations.AuthUiExperimental
import io.silv.core_ui.components.Action
import io.silv.core_ui.components.EmptyScreen
import io.silv.core_ui.voyager.rememberScreenWithResultLauncher
import io.silv.movie.R
import cafe.adriel.voyager.koin.koinScreenModel
import io.silv.movie.presentation.CollectEventsWithLifecycle
import io.silv.movie.presentation.components.AuthScreenContent
import io.silv.movie.presentation.components.SignedInScreen
import io.silv.movie.presentation.components.dialog.RemoveEntryDialog
import io.silv.movie.presentation.components.profile.AccountOptionsBottomSheet
import io.silv.movie.presentation.components.profile.ProfileEvent
import io.silv.movie.presentation.components.profile.ProfileScreenModel
import io.silv.movie.presentation.components.profile.ProfileState
import io.silv.movie.presentation.components.profile.UserOptionsBottomSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data object ProfileScreen: Screen {

    @OptIn(AuthUiExperimental::class)
    @Composable
    override fun Content() {

        val screenModel = koinScreenModel<ProfileScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        val state by screenModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val user by screenModel.currentUser.collectAsStateWithLifecycle()

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

        when (val s = state) {
            ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }

            is ProfileState.LoggedIn -> {
                val imageSelectScreen = remember { SelectProfileImageScreen }
                val imageSelectScreenLauncher = rememberScreenWithResultLauncher(
                    screen = imageSelectScreen
                ) {
                    screenModel.updateProfilePicture(it.path)
                }
                val editUsernameScreen = remember(user?.username) {
                    UsernameEditScreen(user?.username.orEmpty())
                }
                val editUsernameScreenLauncher = rememberScreenWithResultLauncher(
                    screen = editUsernameScreen,
                ) {
                    screenModel.updateUsername(it.name)
                }
                val subscribed by screenModel.subscribedLists.collectAsStateWithLifecycle()
                val public by screenModel.publicLists.collectAsStateWithLifecycle()

                SignedInScreen(
                    showOptionsClick = {
                        screenModel.changeLoggedInDialog(ProfileState.LoggedIn.Dialog.UserOptions)
                    },
                    snackbarHostState = snackbarHostState,
                    onProfileImageClicked = { imageSelectScreenLauncher.launch() },
                    subscribed = subscribed,
                    public = public,
                    onListClick = {
                        navigator.push(ListViewScreen(it.id))
                    },
                    state = s
                )
                when (s.dialog) {
                    null -> Unit
                    ProfileState.LoggedIn.Dialog.UserOptions -> {
                        UserOptionsBottomSheet(
                            deleteAccountClick = {
                                screenModel.changeLoggedInDialog(ProfileState.LoggedIn.Dialog.ConfirmDeleteAccount)
                            },
                            logOutClick = {
                                screenModel.signOut()
                            },
                            changeUsername = { editUsernameScreenLauncher.launch() },
                            onDismiss = { screenModel.changeLoggedInDialog(null) }
                        )
                    }

                    ProfileState.LoggedIn.Dialog.ConfirmDeleteAccount -> RemoveEntryDialog(
                        onDismissRequest = { screenModel.changeLoggedInDialog(null) },
                        onConfirm = { screenModel.deleteAccount() },
                        entryToRemove = s.info.email.orEmpty()
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
                    error = s.error,
                    inProgress = s.jobInProgress,
                    signInWithEmailAndPassword = { signIn(email, password) },
                    registerWithEmailAndPassword = { register(email, password) },
                    optionsButtonClick = { screenModel.changeLoggedOutDialog(ProfileState.LoggedOut.Dialog.AccountOptions) },
                    snackbarHostState = snackbarHostState
                )

                when (s.dialog) {
                    null -> Unit
                    ProfileState.LoggedOut.Dialog.AccountOptions -> {
                        AccountOptionsBottomSheet(
                            resetPasswordClick = { resetResultLauncher.launch() },
                            onDismiss = { screenModel.changeLoggedOutDialog(null) }
                        )
                    }
                }
            }
            ProfileState.Offline -> {
                val context = LocalContext.current
                EmptyScreen(
                    icon = Icons.Filled.ExploreOff,
                    iconSize = 182.dp,
                    message = stringResource(id = R.string.no_internet_error),
                    actions = listOf(
                        Action(
                            R.string.take_to_settings,
                            onClick = {
                                try {
                                    val settingsIntent: Intent =
                                        Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                    context.startActivity(settingsIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    )
                )
            }
        }
    }
}
