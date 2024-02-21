package io.silv.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.core_ui.theme.MovieTheme
import org.koin.androidx.compose.defaultExtras
import org.koin.androidx.viewmodel.resolveViewModel
import org.koin.compose.currentKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope

val LocalMainViewModelStoreOwner =
    staticCompositionLocalOf<ViewModelStoreOwner> { error("not provided") }

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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalMainViewModelStoreOwner provides this) {
                MovieTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Navigator(TabHost) { navigator ->
                            FadeTransition(navigator)
                        }
                    }
                }
            }
        }
    }
}


