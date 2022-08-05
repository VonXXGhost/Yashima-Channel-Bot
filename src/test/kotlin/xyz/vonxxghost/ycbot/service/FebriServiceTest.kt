package xyz.vonxxghost.ycbot.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Created by VonXXGhost on 2022/8/12 11:36
 */
@Disabled
@SpringBootTest
@ActiveProfiles("dev")
internal class FebriServiceTest {

    @Autowired
    lateinit var febriService: FebriService

    @Test
    fun queryTalkList() {
        val queryTalkList = febriService.queryTalkList()
        println(queryTalkList)
    }
}
