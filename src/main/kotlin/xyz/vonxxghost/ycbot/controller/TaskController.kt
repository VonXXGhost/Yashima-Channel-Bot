package xyz.vonxxghost.ycbot.controller

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.vonxxghost.ycbot.service.FebriService

/**
 * Created by VonXXGhost on 2022/8/18 14:59
 */
@RestController
@RequestMapping("/task")
class TaskController {

    val log = KotlinLogging.logger { }

    @Autowired
    lateinit var febriService: FebriService

    @PostMapping("startTaskFromTalkList")
    fun startTaskFromTalkList(): String {
        febriService.startTaskFromTalkList()
        return "Task submitted"
    }

    @PostMapping("startTaskFromPostTalk")
    fun startTaskFromPostTalk(@RequestParam id: Long): String {
        val record =
            febriService.febriRecordRepository.findById(id).orElseThrow { IllegalArgumentException("id not found") }
        febriService.startTaskFromPostTalk(record)
        return "Task submitted"
    }
}
