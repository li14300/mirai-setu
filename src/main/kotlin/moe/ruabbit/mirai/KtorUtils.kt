package moe.ruabbit.mirai

import moe.ruabbit.mirai.config.SettingsConfig
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.util.*

object KtorUtils {
    // 使用代理的ktor客户端
    @OptIn(KtorExperimentalAPI::class)
    val proxyClient = HttpClient(OkHttp) {
        engine {
            proxy = when (SettingsConfig.proxyConfig) {
                0 -> null
                1 -> ProxyBuilder.http(SettingsConfig.httpProxy.proxy)
                2 -> ProxyBuilder.socks(host = SettingsConfig.socksProxy.host, port = SettingsConfig.socksProxy.port)
                else -> null
            }
        }
    }

    // 未使用代理的Ktor客户端
    val normalClient = HttpClient(OkHttp)

    // 安全的关闭客户端, 防止堵塞主线程
    fun closeClient() {
        proxyClient.close()
        normalClient.close()
    }

}
