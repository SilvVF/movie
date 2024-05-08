package io.silv.movie.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import io.silv.movie.AppState
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.user.User
import org.koin.androidx.compose.defaultExtras
import org.koin.androidx.viewmodel.resolveViewModel
import org.koin.compose.currentKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope

val LocalIsScrolling = compositionLocalOf<MutableState<Boolean>> { error("not provided yet")  }

val LocalMainViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> { error("not provided") }

val LocalContentInteractor = staticCompositionLocalOf<ContentInteractor> { error("ContentInteractor not provided in current scope") }

val LocalListInteractor = staticCompositionLocalOf<ListInteractor> { error("ListInteractor not provided in current scope") }

val LocalUser = compositionLocalOf<User?> { null }

val LocalAppState = compositionLocalOf<AppState> { error("Not provided in scope") }

@OptIn(KoinInternalApi::class)
@Composable
inline fun <reified T : ViewModel> getActivityViewModel(
    qualifier: Qualifier? = null,
    key: String? = null,
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition? = null,
): T {

    val viewModelStoreOwner = checkNotNull(LocalMainViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalMainViewModelStoreOwner"
    }
    val extras = defaultExtras(viewModelStoreOwner)

    return resolveViewModel(
        T::class, viewModelStoreOwner.viewModelStore, key, extras, qualifier, scope, parameters
    )
}

@Composable
fun User?.rememberProfileImageData(): UserProfileImageData? {

    val currentUser = LocalUser.current

    return remember(this?.profileImage, currentUser) {
        this?.let {
            UserProfileImageData(
                userId = it.userId,
                isUserMe = it.userId == currentUser?.userId,
                path = it.profileImage
            )
        }
    }
}
