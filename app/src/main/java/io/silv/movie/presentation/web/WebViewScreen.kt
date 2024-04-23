
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.silv.core_ui.util.AccompanistWebViewClient
import io.silv.core_ui.util.WebView
import io.silv.core_ui.util.WebViewNavigator
import io.silv.core_ui.util.rememberSaveableWebViewState

@Composable
fun WebViewScreenContent(
    url: String,
    modifier: Modifier = Modifier
) {

    val state = rememberSaveableWebViewState()
    val scope = rememberCoroutineScope()


    val navigator = remember {
        WebViewNavigator(scope)
    }

    LaunchedEffect(url) {
        if (state.lastLoadedUrl != url) {
            navigator.loadUrl(url)
        }
    }

    WebView(
        state = state,
        captureBackPresses = false,
        modifier = modifier,
        navigator = navigator,
        client = object : AccompanistWebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {

                val reqUrl = request?.url.toString()

                if (reqUrl != url)
                    return null

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val reqUrl = request?.url.toString()

                if (reqUrl != url)
                    return true

                return super.shouldOverrideUrlLoading(view, request)
            }
        },
        onCreated = {
            it.settings.javaScriptEnabled = true
        }
    )
}