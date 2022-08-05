package xyz.vonxxghost.ycbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class YashimaChannelBotApplication

fun main(args: Array<String>) {
    runApplication<YashimaChannelBotApplication>(*args)
}
