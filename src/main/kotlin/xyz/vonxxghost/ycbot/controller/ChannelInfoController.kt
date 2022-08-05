package xyz.vonxxghost.ycbot.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.vonxxghost.ycbot.sdk.GeneralClient

/**
 * Created by VonXXGhost on 2022/8/11 15:14
 */
@RestController
@RequestMapping("/channel")
class ChannelInfoController(
    val generalClient: GeneralClient
) {

    @GetMapping("myGuilds")
    fun myGuilds(): String = generalClient.queryMyGuilds().body!!.string()

    @GetMapping("subChannels/{guildId}")
    fun subChannels(@PathVariable guildId: String): String = generalClient.querySubChannels(guildId).body!!.string()

}
