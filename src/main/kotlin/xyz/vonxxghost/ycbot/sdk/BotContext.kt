package xyz.vonxxghost.ycbot.sdk

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Created by VonXXGhost on 2022/8/5 11:08
 */
class BotContext (
    val baseUrl: String,
    val botId: String,
    val botToken: String
) {
    val authToken = "$botId.$botToken"

    var threadId = ""

    val jsonClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor {
            val newRequest: Request = it.request().newBuilder()
                .addHeader("Authorization", "Bot ${this.authToken}")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()
            return@addInterceptor it.proceed(newRequest)
        }
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
}
