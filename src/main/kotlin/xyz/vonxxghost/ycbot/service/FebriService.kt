package xyz.vonxxghost.ycbot.service

import com.alibaba.fastjson2.JSON
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import xyz.vonxxghost.ycbot.model.WebPageItem
import xyz.vonxxghost.ycbot.sdk.ThreadClient
import xyz.vonxxghost.ycbot.task.TaskCenter
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.persistence.*

/**
 * @property id 主键
 * @property url 文章地址
 * @property title 标题
 * @property content 抓取内容
 * @property threadPostResponse 频道帖子发送响应记录（仅成功）
 * @property createdAt 抓取时间
 */
@Entity
@Table(name = "febri_record")
class FebriRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, nullable = false)
    var url: String? = null

    var title: String? = null

    var content: String? = null

    @Column(name = "thread_post_response")
    var threadPostResponse: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var createdAt: Date? = Date()
}


@Repository
interface FebriRecordRepository : JpaRepository<FebriRecord, Long> {

    fun findFirstByUrl(url: String): FebriRecord?
}


@Service
class FebriService {

    private val log = KotlinLogging.logger { }

    @Autowired
    lateinit var febriRecordRepository: FebriRecordRepository

    @Autowired
    lateinit var taskCenter: TaskCenter

    @Autowired
    lateinit var threadClient: ThreadClient

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    private fun OkHttpClient.get(url: String) = newCall(Request.Builder().url(url).build()).execute()

    /**
     * 获取最新的talk列表
     * @return 此时返回的content为空
     */
    fun queryTalkList(): List<WebPageItem> {
        val response = client.get("https://febri.jp/febritalk/").also { assert(it.isSuccessful) }
        return Jsoup.parse(response.body!!.string())
            .select("div.box-article")
            .map {
                val url = it.select("a")[0].attr("href")
                val title = it.select("h4.title")[0].ownText()
                val subTitle = it.select("span.sub_title").text()
                val limitText = it.select("p.limitText").text()
                WebPageItem(url, title, subTitle, limitText, null)
            }
    }

    /**
     * 获取talk访谈正文 html
     */
    fun queryTalkContent(url: String): String {
        val response = client.get(url).also { assert(it.isSuccessful) }
        return Jsoup.parse(response.body!!.string())
            .select("#blocks_general_field")
            .html()
    }

    /**
     * 保存数据
     * @return (是否保存了新数据, 数据记录）
     */
    fun saveRecord(pageItem: WebPageItem): Pair<Boolean, FebriRecord> {
        febriRecordRepository.findFirstByUrl(pageItem.url)?.let {
            log.warn { "url对应记录已存在: ${pageItem.url}" }
            return Pair(false, it)
        }
        val record = FebriRecord().apply {
            url = pageItem.url
            title = "${pageItem.title} - ${pageItem.subTitle}"
            content = pageItem.content
        }
        return Pair(true, febriRecordRepository.save(record))
    }

    private fun start(task: () -> Unit): Future<*> = taskCenter.startWithRetry30s(task)

    /**
     * 从获取talk列表开始执行任务
     */
    @Scheduled(cron = "\${ycbot.task.febri-talk-cron:0 10 11 ? * *}")
    fun startTaskFromTalkList() {
        start {
            log.info { "startTaskFromTalkList" }
            val item = queryTalkList()[0]
            startTaskFromFillTalkContent(item)
            log.info { "startTaskFromTalkList end" }
        }
    }

    private fun startTaskFromFillTalkContent(item: WebPageItem) {
        start {
            log.info { "startTaskFromFillTalkContent, url: ${item.url}" }
            item.content = queryTalkContent(item.url)
            val (saved, record) = saveRecord(item)
            if (saved) {
                startTaskFromPostTalk(record)
            }
            log.info { "startTaskFromFillTalkContent end" }
        }
    }

    fun startTaskFromPostTalk(record: FebriRecord) {
        start {
            log.info { "startTaskFromPostTalk, id: ${record.id}" }
            if (record.threadPostResponse?.isNotBlank() == true) {
                log.info { "已存在发帖记录，不再发送, id: ${record.id}" }
                return@start
            }
            checkNotNull(record.id)

            val title = "【Febri访谈】${record.title}"
            val content = "<p>${record.url}</p> ${record.content}"
            val response = threadClient.postThreadAsHtml(title, content).also { it.isSuccessful }
                .body!!.string()
            val data = JSON.parseObject(response)
            if (data.containsKey("task_id")) {
                log.info { "发帖成功，记录id: ${record.id}" }
                record.threadPostResponse = response
                febriRecordRepository.save(record)
            } else {
                log.warn { "发帖失败，记录id: ${record.id}" }
            }
            log.info { "startTaskFromPostTalk end" }
        }
    }
}
