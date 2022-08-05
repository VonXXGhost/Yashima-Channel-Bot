package xyz.vonxxghost.ycbot.sdk

import com.alibaba.fastjson2.JSONObject
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.stereotype.Component


/**
 * 帖子客户端
 * Created by VonXXGhost on 2022/8/5 11:06
 */
@Component
class ThreadClient(val botContext: BotContext) {

    fun postThreadAsHtml(title: String, content: String): Response {
        val body = JSONObject.of(
            "format", ThreadFormat.FORMAT_HTML.value,
            "title", title,
            "content", content
        )
        val request = Request.Builder()
            .url("${botContext.baseUrl}/channels/${botContext.threadId}/threads")
            .put(body.toString().toRequestBody())
            .build()
        return botContext.jsonClient.newCall(request)
            .execute()
    }
}

enum class ThreadFormat(val value: Int) {
    FORMAT_TEXT(1),
    FORMAT_HTML(2),
}
