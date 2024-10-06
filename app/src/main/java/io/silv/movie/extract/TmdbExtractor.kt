package io.silv.movie.extract

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import timber.log.Timber
import java.net.InetAddress

fun OkHttpClient.Builder.dohCloudflare() =
    dns(
        DnsOverHttps.Builder().client(build())
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("162.159.36.1"),
                InetAddress.getByName("162.159.46.1"),
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
                InetAddress.getByName("162.159.132.53"),
                InetAddress.getByName("2606:4700:4700::1111"),
                InetAddress.getByName("2606:4700:4700::1001"),
                InetAddress.getByName("2606:4700:4700::0064"),
                InetAddress.getByName("2606:4700:4700::6400"),
            )
            .build(),
    )


object  TmdbExtractor {

    val extractor = VidsrcExtractor(
        OkHttpClient()
            .newBuilder()
            .dohCloudflare()
            .addInterceptor {

                val req = it.request()

                Timber.d(req.toString())

                it.proceed(req)
            }
            .build(),
        commonEmptyHeaders
    )

    fun extractVideo() {
        GlobalScope.launch {

        }
    }
}