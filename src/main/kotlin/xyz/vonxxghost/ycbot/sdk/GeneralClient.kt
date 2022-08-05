package xyz.vonxxghost.ycbot.sdk

import okhttp3.Request
import okhttp3.Response
import org.springframework.stereotype.Component

/**
 * 通用接口客户端
 * Created by VonXXGhost on 2022/8/11 14:43
 */
@Component
class GeneralClient(val botContext: BotContext) {

    fun queryMyGuilds(): Response {
        val request = Request.Builder()
            .url("${botContext.baseUrl}/users/@me/guilds")
            .get()
            .build()
        return botContext.jsonClient.newCall(request).execute()
    }

    fun querySubChannels(guildId: String): Response {
        val request = Request.Builder()
            .url("${botContext.baseUrl}/guilds/${guildId}/channels")
            .get()
            .build()
        return botContext.jsonClient.newCall(request).execute()
    }
}
