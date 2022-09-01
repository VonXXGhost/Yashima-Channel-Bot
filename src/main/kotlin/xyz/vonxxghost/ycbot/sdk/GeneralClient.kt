package xyz.vonxxghost.ycbot.sdk

import com.alibaba.fastjson2.JSONObject
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.stereotype.Component

/**
 * 通用接口客户端
 * Created by VonXXGhost on 2022/8/11 14:43
 */
@Component
class GeneralClient(val botContext: BotContext) {

    private fun execute(request: Request) = botContext.jsonClient.newCall(request).execute()

    fun queryMyGuilds(): Response {
        val request = Request.Builder()
            .url("${botContext.baseUrl}/users/@me/guilds")
            .get()
            .build()
        return execute(request)
    }

    fun querySubChannels(guildId: String): Response {
        val request = Request.Builder()
            .url("${botContext.baseUrl}/guilds/${guildId}/channels")
            .get()
            .build()
        return execute(request)
    }

    /**
     * 发送消息
     * @param messageId: 要回复的消息id(Message.id), 在 AT_CREATE_MESSAGE 事件中获取。
     */
    fun postMessage(content: String, channelId: String, messageId: String? = null): Response {
        val body = JSONObject.of("content", content)
        messageId?.let {
            body.put("msg_id", it)
        }
        return execute(
            Request.Builder()
                .url("${botContext.baseUrl}/channels/${channelId}/messages")
                .post(body.toString().toRequestBody())
                .build()
        )
    }
}
