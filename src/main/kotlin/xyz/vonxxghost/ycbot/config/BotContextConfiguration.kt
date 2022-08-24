package xyz.vonxxghost.ycbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.vonxxghost.ycbot.sdk.BotContext

/**
 * Created by VonXXGhost on 2022/8/5 11:12
 */
@Configuration
class BotContextConfiguration {

    @Bean
    fun botContext(ycBotProperties: YcBotProperties) =
        BotContext(
            ycBotProperties.channel.baseUrl,
            ycBotProperties.channel.botId,
            ycBotProperties.channel.botToken
        ).also {
            it.threadId = ycBotProperties.channel.threadId
            // init wsClient
            it.wsClient.request()
        }
}
