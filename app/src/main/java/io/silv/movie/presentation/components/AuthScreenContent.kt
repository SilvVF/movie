package io.silv.movie.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.compose.auth.ui.AuthForm
import io.github.jan.supabase.compose.auth.ui.LocalAuthState
import io.github.jan.supabase.compose.auth.ui.ProviderButtonContent
import io.github.jan.supabase.compose.auth.ui.email.OutlinedEmailField
import io.github.jan.supabase.compose.auth.ui.password.OutlinedPasswordField
import io.github.jan.supabase.compose.auth.ui.password.PasswordRule
import io.github.jan.supabase.compose.auth.ui.password.rememberPasswordRuleList
import io.github.jan.supabase.gotrue.providers.Google

@OptIn(ExperimentalMaterial3Api::class, SupabaseExperimental::class)
@Composable
fun AuthScreenContent(
    password: () -> String,
    email: () -> String,
    inProgress: Boolean,
    error: String?,
    updateEmail: (String) -> Unit,
    updatePassword: (String) -> Unit,
    optionsButtonClick: () -> Unit,
    signInWithGoogle: () -> Unit,
    signInWithEmailAndPassword: () -> Unit,
    registerWithEmailAndPassword: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick =  optionsButtonClick) {
                        Icon(Icons.Filled.MoreVert, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        AuthForm {
            val authState = LocalAuthState.current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                OutlinedEmailField(
                    value = email(),
                    onValueChange = updateEmail,
                    label = { Text("E-Mail") },
                    mandatory = email().isNotBlank() //once an email is entered, it is mandatory. (which enable validation)
                )
                OutlinedPasswordField(
                    value = password(),
                    onValueChange = updatePassword,
                    label = { Text("Password") },
                    rules = rememberPasswordRuleList(
                        PasswordRule.minLength(6),
                        PasswordRule.containsSpecialCharacter(),
                        PasswordRule.containsDigit(),
                        PasswordRule.containsLowercase(),
                        PasswordRule.containsUppercase()
                    )
                )
                AnimatedVisibility(visible = error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    onClick = signInWithEmailAndPassword, //Login with email and password,
                    enabled = authState.validForm && !inProgress,
                ) {
                    Text("Login")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    onClick = registerWithEmailAndPassword, //Login with email and password,
                    enabled = authState.validForm && !inProgress,
                ) {
                    Text("Create an account")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    enabled = !inProgress,
                    onClick = signInWithGoogle, //Login with Google,
                    content = { ProviderButtonContent(Google) }
                )
            }
        }
    }
}