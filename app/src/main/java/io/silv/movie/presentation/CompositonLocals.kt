package io.silv.movie.presentation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

val LocalIsScrolling = compositionLocalOf<MutableState<Boolean>> { error("not provided yet")  }