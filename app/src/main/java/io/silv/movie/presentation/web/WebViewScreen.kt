
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.silv.movie.presentation.web.AccompanistWebViewClient
import io.silv.movie.presentation.web.WebView
import io.silv.movie.presentation.web.WebViewNavigator
import io.silv.movie.presentation.web.rememberSaveableWebViewState
import timber.log.Timber

fun addMyClickCallBackJs(): String {
    var js = "javascript:"
    js += "function myClick(event){" +
            "if(event.target.className == null){my.myClick(event.target.id)}" + "else{my.myClick(event.target.className)}}"
    js += "document.addEventListener(\"click\",myClick,true);"
    return js
}

class MyJsToAndroid: Any() {

    @JavascriptInterface
    fun myClick(idOrClass: String   ) {
        Timber.d("myClick-> " + idOrClass);
    }
}

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