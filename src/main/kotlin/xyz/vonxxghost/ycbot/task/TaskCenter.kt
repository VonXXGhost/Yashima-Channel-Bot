package xyz.vonxxghost.ycbot.task

import mu.KotlinLogging
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 任务处理中心
 * Created by VonXXGhost on 2022/8/18 10:09
 */
@Component
class TaskCenter {

    private val log = KotlinLogging.logger { }

    private val coreNumber = Runtime.getRuntime().availableProcessors()
    private val scheduler = Executors.newScheduledThreadPool(coreNumber)
    val executors = ThreadPoolTaskExecutor().apply {
        corePoolSize = coreNumber * 2
        maxPoolSize = coreNumber * 4
        queueCapacity = 5
        setThreadNamePrefix("TaskExec-")
        initialize()
    }

    /**
     * 异步执行
     */
    fun start(task: () -> Unit): Future<*> = executors.submit(task)

    /**
     * 延迟执行
     */
    fun schedule(delay: Long, unit: TimeUnit, task: () -> Unit): ScheduledFuture<*> =
        scheduler.schedule(task, delay, unit)

    /**
     * 异常自动延迟重试封装
     */
    fun withRetry(delay: Long, unit: TimeUnit, tryCount: Int = 0, task: () -> Unit): () -> Unit = {
        try {
            task()
        } catch (e: Exception) {
            if (tryCount >= MAX_RETRY_COUNT) {
                log.warn(e) { "任务重试次数过多，已放弃" }
                throw RuntimeException("任务重试次数过多，已放弃", e)
            }
            log.warn(e) { "任务异常，将会在 $delay $unit 后重试，当前重试次数 $tryCount" }
            schedule(delay, unit, withRetry(delay, unit, tryCount + 1, task))
        }
    }

    /**
     * 30秒延迟自动重试
     */
    fun withRetry30s(task: () -> Unit): () -> Unit = withRetry(30, TimeUnit.SECONDS, task = task)

    fun startWithRetry30s(task: () -> Unit): Future<*> = start(withRetry30s(task))

    companion object {
        // 最大重试次数
        const val MAX_RETRY_COUNT = 5
    }
}
