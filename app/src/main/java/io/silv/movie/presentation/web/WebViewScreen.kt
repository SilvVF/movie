
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.silv.movie.presentation.web.AccompanistWebViewClient
import io.silv.movie.presentation.web.WebView
import io.silv.movie.presentation.web.rememberWebViewState

@Composable
fun WebViewScreenContent(
    url: String,
    modifier: Modifier = Modifier
) {

    val state = rememberWebViewState(url)

    WebView(
        state = state,
        captureBackPresses = false,
        modifier = modifier,
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