package io.silv.movie.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import io.silv.movie.AppState
import io.silv.movie.coil.fetchers.model.UserProfileImageData
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.prefrences.AppTheme
import io.silv.movie.data.prefrences.ThemeMode
import io.silv.movie.data.prefrences.UiPreferences
import io.silv.movie.data.user.User
import io.silv.movie.presentation.tabs.LibraryTab
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

private val defaultAppState by lazy {
    AppState(
        appTheme = AppTheme.DEFAULT,
        themeMode = ThemeMode.DARK,
        amoled = false,
        dateFormat = UiPreferences.dateFormat(""),
        relativeTimestamp = true,
        startScreen = LibraryTab,
        sharedElementTransitions = true
    )
}

val LocalAppState = compositionLocalOf<AppState> { defaultAppState }


@Composable
fun ProvideLocalsForPreviews(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalUser providesDefault null,
        LocalAppState providesDefault defaultAppState,
        LocalIsScrolling providesDefault remember { mutableStateOf(true) },
        LocalContentInteractor providesDefault remember {
            object : ContentInteractor, EventProducer<ContentInteractor.ContentEvent> by EventProducer.default() {
                override fun toggleFavorite(contentItem: ContentItem) = Unit
                override fun addToList(contentList: ContentList, contentItem: ContentItem) = Unit
                override fun addToList(listId: Long, contentItem: ContentItem) = Unit
                override fun addToAnotherList(listId: Long, contentItem: ContentItem)= Unit
                override fun removeFromList(contentList: ContentList, contentItem: ContentItem) = Unit
            }
        },
        LocalListInteractor providesDefault remember {
            object: ListInteractor, EventProducer<ListInteractor.ListEvent> by EventProducer.default() {
                override fun deleteList(contentList: ContentList)  = Unit
                override fun toggleListVisibility(contentList: ContentList)  = Unit
                override fun copyList(contentList: ContentList)  = Unit
                override fun editList(contentList: ContentList, update: (ContentList) -> ContentList) = Unit
                override fun subscribeToList(contentList: ContentList)  = Unit
                override fun unsubscribeFromList(contentList: ContentList)  = Unit
                override fun togglePinned(contentList: ContentList)  = Unit
            }
        }
    ) {
        content()
    }
}

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
