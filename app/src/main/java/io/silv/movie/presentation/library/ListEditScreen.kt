package io.silv.movie.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.core_ui.voyager.ScreenResult
import io.silv.core_ui.voyager.ScreenWithResult
import io.silv.core_ui.voyager.setScreenResult
import io.silv.movie.R
import kotlinx.parcelize.Parcelize

class ListEditScreen(
    private val name: String,
): ScreenWithResult<ListEditScreen.ListEditResult> {

    @Parcelize
    data class ListEditResult(
        val name: String
    ): ScreenResult

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
        val background = MaterialTheme.colorScheme.background

        var name by rememberSaveable {
            mutableStateOf(name)
        }

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
            Column(
                Modifier.imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.edit_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(22.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it }
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
                        onClick = {
                            setScreenResult(
                                ListEditResult(name)
                            )
                            navigator.pop()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            }
        }
    }
}