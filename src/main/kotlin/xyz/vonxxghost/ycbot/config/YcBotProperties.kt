package xyz.vonxxghost.ycbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Created by VonXXGhost on 2022/8/5 10:46
 */
@Configuration
@ConfigurationProperties(prefix = "ycbot")
data class YcBotProperties(
    var channel: Channel = Channel(),
    var task: Task = Task(),
)

/**
 * @property baseUrl 频道api基础域名
 * @property botId 频道bot id
 * @property botToken 频道bot token
 * @property threadId 帖子子频道id
 */
data class Channel(
    var baseUrl: String = "",
    var botId: String = "",
    var botToken: String = "",
    var threadId: String = "",
)

/**
 * @property febriTalkCron febri talk任务计划
 */
data class Task(
    var febriTalkCron: String = "",
)
