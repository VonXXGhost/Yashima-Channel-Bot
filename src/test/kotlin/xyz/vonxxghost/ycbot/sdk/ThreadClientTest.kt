package xyz.vonxxghost.ycbot.sdk

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * Created by VonXXGhost on 2022/8/11 14:00
 */
@Disabled
@SpringBootTest
@ActiveProfiles("dev")
internal class ThreadClientTest {

    @Autowired
    lateinit var threadClient: ThreadClient

    @Test
    fun postThreadAsHtml() {
        val response = threadClient.postThreadAsHtml("单元测试${LocalDateTime.now()}", "<p>Test content</p>")
        assertTrue(response.isSuccessful)
    }
}
