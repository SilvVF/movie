package io.silv.movie.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.compose.auth.ui.AuthForm
import io.github.jan.supabase.compose.auth.ui.LocalAuthState
import io.github.jan.supabase.compose.auth.ui.annotations.AuthUiExperimental
import io.github.jan.supabase.compose.auth.ui.email.OutlinedEmailField
import io.silv.core_ui.voyager.ScreenResult
import io.silv.core_ui.voyager.ScreenWithResult
import io.silv.core_ui.voyager.setScreenResult
import io.silv.movie.R
import kotlinx.parcelize.Parcelize

@AuthUiExperimental
class ResetPasswordScreen(
    private val email: String,
): ScreenWithResult<ResetPasswordScreen.Result> {

    override val key: ScreenKey = "password_reset"

    @Parcelize
    data class Result(
        val email: String
    ): ScreenResult

    @OptIn(SupabaseExperimental::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
        val background = MaterialTheme.colorScheme.background

        var email by rememberSaveable { mutableStateOf(email) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(background, surfaceContainer, primaryContainer)
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AuthForm {
                Column(
                    Modifier.imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.reset_password_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    OutlinedEmailField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-Mail") },
                        mandatory = email.isNotBlank() //once an email is entered, it is mandatory. (which enable validation)
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Row {
                        TextButton(
                            onClick = { navigator.pop() }
                        ) {
                            Text(stringResource(id = R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(22.dp))
                        Button(
                            enabled = LocalAuthState.current.validForm,
                            onClick = {
                                setScreenResult(
                                    Result(email)
                                )
                                navigator.pop()
                            }
                        ) {
                            Text(text = stringResource(id = R.string.reset))
                        }
                    }
                }
            }
        }
    }
}