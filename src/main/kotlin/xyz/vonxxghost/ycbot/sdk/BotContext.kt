package xyz.vonxxghost.ycbot.sdk

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

typealias WsEventHandler = (WsPayload) -> Unit

private val log = KotlinLogging.logger { }

/**
 * Created by VonXXGhost on 2022/8/5 11:08
 */
class BotContext(
    val baseUrl: String,
    val botId: String,
    val botToken: String
) {
    val authToken = "$botId.$botToken"

    var threadId = ""

    var isLogIn = false
        private set

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

    val wsClient: WebSocket by lazy {
        val request = Request.Builder()
            .url("$baseUrl/gateway")
            .header("Authorization", "Bot ${this.authToken}")
            .get()
            .build()
        val response = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build().newCall(request).execute()
            .also { assert(it.isSuccessful) }
            .body!!.string()
        val wsUrl = JSON.parseObject(response).getString("url").also { assert(it.isNullOrBlank()) }
        OkHttpClient.Builder().build().newWebSocket(
            Request.Builder().url(wsUrl).build(),
            webSocketHandler
        )
    }

    private val webSocketHandler = WebSocketHandler()

    init {
        wsCallbacks.getOrNew(OpCode.Hello).add(this::helloHandler)

        dispatchCallbacks.getOrNew(DispatchEvent.READY).add(this::readyHandler)
        dispatchCallbacks.getOrNew(DispatchEvent.RESUMED).add(this::resumedHandler)
    }

    fun send(payload: WsPayload) {
        val msg = JSON.toJSONString(payload)
        log.info { "WS send: $msg" }
        wsClient.send(msg)
    }

    /**
     * 登录
     */
    fun logIn() {
        val payload = WsPayload(
            OpCode.Identify.value,
            JSONObject.of(
                "token", authToken,
                "intents", INTENTS,
                "shard", intArrayOf(0, 1),
            ),
            null,
            null
        )
        send(payload)
    }

    /**
     * 连接建立成功处理
     */
    private fun helloHandler(payload: WsPayload) {
        webSocketHandler.heartbeatInterval =
            payload.dJson?.getLong("heartbeat_interval") ?: webSocketHandler.heartbeatInterval
    }

    /**
     * 连接恢复成功处理
     */
    private fun resumedHandler(payload: WsPayload) {
        isLogIn = true
    }

    /**
     * 登录成功处理
     */
    private fun readyHandler(payload: WsPayload) {
        isLogIn = true
        updateState(payload)
        webSocketHandler.sessionId = payload.dJson?.getString("session_id") ?: webSocketHandler.sessionId
        webSocketHandler.startNewHeartbeat()
    }

    private fun updateState(payload: WsPayload) {
        if (payload.s != null) {
            webSocketHandler.lastSeq.updateAndGet {
                maxOf(payload.s, it)
            }
        }
    }

    companion object {

        const val INTENTS = 1 shl 30 or 1 shl 27

        /**
         * 除Dispatch事件外的消息回调
         */
        val wsCallbacks: MutableMap<OpCode, MutableList<WsEventHandler>> = mutableMapOf()

        /**
         * Dispatch事件的回调
         */
        val dispatchCallbacks: MutableMap<DispatchEvent, MutableList<WsEventHandler>> = mutableMapOf()

    }

    inner class WebSocketHandler : WebSocketListener() {

        var lastSeq = AtomicLong(-1)
        var heartbeatInterval = 30_000L
        var heartbeatTimer: Timer? = null
        var sessionId = ""

        // 最后一次尝试重连的时间
        private var lastResumeTryTime = LocalDateTime.now()

        override fun onOpen(webSocket: WebSocket, response: Response) {
            log.info { "onOpen: $response" }
            logIn()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            log.info { "onClosed: $code, $reason" }
            isLogIn = false
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            log.info { "onClosing: $code, $reason" }
            isLogIn = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            log.warn(t) { "onFailure: $response, body:${response?.body?.string()}" }
            isLogIn = false
            tryResume()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            log.info { "onMessage: $text" }
            onMessage(JSONObject.parseObject(text, WsPayload::class.java))
        }

        private fun onMessage(payload: WsPayload) {
            // 根据事件类型回调
            val opCode = OpCode.by(payload.op!!)
            if (opCode == OpCode.Dispatch) {
                DispatchEvent.by(payload.t!!)?.let {
                    dispatchCallbacks[it]?.forEach { it(payload) }
                }
            } else {
                wsCallbacks[opCode]?.forEach { it(payload) }
            }
            updateState(payload)
        }

        /**
         * 开始新的心跳任务
         */
        fun startNewHeartbeat() {
            heartbeatTimer?.cancel()

            heartbeatTimer = kotlin.concurrent.timer("heartbeat", false, 0L, heartbeatInterval) {
                sendHeartbeat()
            }
        }

        /**
         * 发送心跳信息
         */
        private fun sendHeartbeat() {
            val d = if (lastSeq.get() > 0) lastSeq.get() else null
            send(WsPayload(OpCode.Heartbeat.value, d, null, null))
        }

        /**
         * 尝试发送重连信息
         */
        private fun tryResume() {
            if (sessionId.isBlank()) {
                return
            }
            if (ChronoUnit.SECONDS.between(LocalDateTime.now(), lastResumeTryTime) < 30) {
                // 避免短时间重复重试
                return
            }
            val payload = WsPayload(
                OpCode.Resume.value,
                JSONObject.of(
                    "token", authToken,
                    "session_id", sessionId,
                    "seq", lastSeq.get()
                ),
                null,
                null
            )
            lastResumeTryTime = LocalDateTime.now()
            send(payload)
        }

    }
}

fun <K, V> MutableMap<K, MutableList<V>>.getOrNew(key: K): MutableList<V> =
    computeIfAbsent(key) { mutableListOf() }


/**
 * https://bot.q.qq.com/wiki/develop/api/gateway/reference.html
 */
data class WsPayload(
    val op: Int?,
    val d: Any?,
    val s: Long?,
    val t: String?,
) {
    val dJson: JSONObject?
        @JsonIgnore get() {
            if (d == null) {
                return null
            }
            if (d is JSONObject) {
                return d
            }
            if (d is String) {
                return JSONObject.parseObject(d)
            }
            throw IllegalArgumentException("d [${d.javaClass}] is not json: $d")
        }
}


/**
 * https://bot.q.qq.com/wiki/develop/api/gateway/opcode.html
 */
enum class OpCode(val value: Int) {
    Dispatch(0),
    Heartbeat(1),
    Identify(2),
    Resume(6),
    Reconnect(7),
    InvalidSession(9),
    Hello(10),
    HeartbeatACK(11),
    HTTPCallbackACK(12),
    UNKNOWN(-1);

    companion object {
        private val map = values().associateBy(OpCode::value)

        fun by(value: Int) = map.getOrDefault(value, UNKNOWN)
    }
}


enum class DispatchEvent {
    READY,
    RESUMED;

    companion object {
        private val map = values().associateBy(DispatchEvent::name)

        fun by(name: String) = map[name]
    }
}
