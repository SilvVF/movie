package io.silv.movie.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import io.silv.movie.presentation.covers.EditCoverAction
import io.silv.movie.presentation.covers.PosterCoverDialog
import io.silv.movie.presentation.covers.screenmodel.MovieCoverScreenModel
import io.silv.movie.presentation.covers.screenmodel.TVCoverScreenModel
import io.silv.movie.presentation.toPoster
import org.koin.core.parameter.parametersOf

@Composable
fun Screen.ChangeMovieCoverDialog(
    movieId: Long,
    screenModel: MovieCoverScreenModel = koinScreenModel { parametersOf(movieId) },
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val movie by screenModel.state.collectAsStateWithLifecycle()

    if (movie != null) {
        val getContent = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) {
            if (it == null) return@rememberLauncherForActivityResult
            screenModel.editCover(context, it)
        }
        val poster = remember(movie) { movie!!.toPoster() }
        PosterCoverDialog(
            coverDataProvider = { poster },
            isCustomCover = remember(movie) { screenModel.hasCustomCover(movie!!) },
            onShareClick = { screenModel.shareCover(context) },
            onSaveClick = { screenModel.saveCover(context) },
            snackbarHostState = screenModel.snackbarHostState,
            onEditClick = if (movie!!.favorite || movie!!.inList) {
                {
                    when (it) {
                        EditCoverAction.EDIT -> getContent.launch("image/*")
                        EditCoverAction.DELETE -> screenModel.deleteCustomCover(context)
                    }
                }
            } else null,
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
fun Screen.ChangeShowCoverDialog(
    showId: Long,
    screenModel: TVCoverScreenModel = koinScreenModel { parametersOf(showId) },
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val tvShow by screenModel.state.collectAsStateWithLifecycle()

    if (tvShow != null) {
        val getContent = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) {
            if (it == null) return@rememberLauncherForActivityResult
            screenModel.editCover(context, it)
        }
        val poster = remember(tvShow) { tvShow!!.toPoster() }
        PosterCoverDialog(
            coverDataProvider = { poster },
            isCustomCover = remember(tvShow) { screenModel.hasCustomCover(tvShow!!) },
            onShareClick = { screenModel.shareCover(context) },
            onSaveClick = { screenModel.saveCover(context) },
            snackbarHostState = screenModel.snackbarHostState,
            onEditClick = if (tvShow!!.favorite || tvShow!!.inList) {
                {
                    when (it) {
                        EditCoverAction.EDIT -> getContent.launch("image/*")
                        EditCoverAction.DELETE -> screenModel.deleteCustomCover(context)
                    }
                }
            } else null,
            onDismissRequest = onDismissRequest,
        )
    }
}