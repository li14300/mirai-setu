package moe.ruabbit.mirai.setu

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import moe.ruabbit.mirai.KtorUtils
import moe.ruabbit.mirai.PluginMain
import moe.ruabbit.mirai.config.MessageConfig
import moe.ruabbit.mirai.config.SettingsConfig
import moe.ruabbit.mirai.data.SetuData
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.error
import java.io.InputStream

class LoliconRequester(private val subject: Group, private val source: MessageSource) {
    // 图片数据
    private lateinit var imageResponse: LoliconResponse.SetuImageInfo

    @Throws(Throwable::class)
    @KtorExperimentalAPI
    suspend fun requestSetu(): Boolean {
        try {
            val response: String =
                KtorUtils.proxyClient.get(
                    "http://api.lolicon.app/setu/v2?r18=${
                        SetuData.groupPolicy[subject.id]
                    }"
                )
            parseSetu(response)
        } catch (e: RemoteApiException) {
            subject.sendMessage(source.quote() + "出现错误: ${e.message}")
            return false
        } catch (e: Throwable) {
            subject.sendMessage(source.quote() + "出现错误, 请联系管理员检查后台或重试\n${e.message}")
            throw e
        }
        return true
    }

    @Throws(Throwable::class)
    @KtorExperimentalAPI
    suspend fun requestSetu(keyword: String): Boolean {
        try {
            val setuResponse: String =
                KtorUtils.proxyClient.get(
                    "http://api.lolicon.app/setu/v2?keyword=${keyword}&r18=${
                        SetuData.groupPolicy[subject.id]
                    }"
                )
            parseSetu(setuResponse)
        } catch (e: RemoteApiException) {
            subject.sendMessage(source.quote() + "出现错误: ${e.message}")
            return false
        } catch (e: Throwable) {
            subject.sendMessage(source.quote() + "出现未知错误, 请联系管理员检查后台或重试\n${e.message}")
            throw e
        }
        return true
    }

    @Throws(Throwable::class)
    private fun parseSetu(rawJson: String) {
        val loliconResponse: LoliconResponse = Json.decodeFromString(rawJson)
        fun parseErrCode(message: String): String {
            return message
                .replace("%code%", loliconResponse.code.toString())
                .replace("%msg%", loliconResponse.msg)
        }

        when (loliconResponse.code) {
            0 -> {
                loliconResponse.data?.get(0)?.let {
                    imageResponse = it
                }
            }
            // 色图搜索404
            404 -> {
                PluginMain.logger.error { "lolicon 错误代码：${loliconResponse.code} 错误信息：${loliconResponse.msg}" }
                throw RemoteApiException(parseErrCode(MessageConfig.setuFailureCode404))
            }
            // -1和403 错误等一系列未知错误
            else -> {
                PluginMain.logger.error { "发生此错误请到github反馈错误 lolicon错误代码：${loliconResponse.code} 错误信息：${loliconResponse.msg}" }
                throw RemoteApiException(parseErrCode(MessageConfig.setuFailureCodeElse))
            }
        }
    }

    // 解析字符串
    private fun parseMessage(message: String): String {
        return message
            .replace("%pid%", imageResponse.pid.toString())
            .replace("%p%", imageResponse.p.toString())
            .replace("%uid%", imageResponse.uid.toString())
            .replace("%title%", imageResponse.title)
            .replace("%author%", imageResponse.author)
            .replace("%url%", imageResponse.url)
            .replace("%r18%", imageResponse.r18.toString())
            .replace("%width%", imageResponse.width.toString())
            .replace("%height%", imageResponse.height.toString())
            .replace("%tags%", imageResponse.tags.toString())
    }

    @KtorExperimentalAPI
    suspend fun getImage(): InputStream =
        KtorUtils.proxyClient.get(imageResponse.url.replace("i.pixiv.cat", SettingsConfig.domainProxy)) {
            headers.append("referer", "https://www.pixiv.net/")
        }

    @Throws(Throwable::class)
    @KtorExperimentalAPI
    suspend fun sendSetu() {
        // 发送信息
        val setuInfoMsg = subject.sendMessage(source.quote() + parseMessage(MessageConfig.setuReply))
        var setuImageMsg: MessageReceipt<Group>? = null
        // 发送setu
        try {
            setuImageMsg = subject.sendImage(getImage())
            // todo 捕获群上传失败的错误信息返回发送失败的信息（涩图被腾讯拦截）
        } catch (e: IllegalStateException) {
            subject.sendMessage(source.quote() + "图片上传失败，可能被腾讯拦截")
        } catch (e: ClientRequestException) {
            subject.sendMessage(MessageConfig.setuImage404)
        } catch (e: Throwable) {
            subject.sendMessage(source.quote() + "出现错误, 请联系管理员检查后台或重试\n${e.message}")
            throw e
        } finally {
            // 撤回图片
            if (SettingsConfig.autoRecallTime > 0) {
                try {
                    setuImageMsg?.recallIn(millis = SettingsConfig.autoRecallTime)
                } catch (e: Exception) {
                }
                try {
                    setuInfoMsg.recallIn(millis = SettingsConfig.autoRecallTime)
                } catch (e: Exception) {
                }
            }
        }
    }

}
