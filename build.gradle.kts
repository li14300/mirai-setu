plugins {
    val kotlinVersion = "1.5.10"
    val miraiVersion = "2.10.0"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version miraiVersion // mirai-console version
}

group = "moe.ruabbit"
version = "1.2.2.1"

dependencies {
     // implementation("com.alibaba:fastjson:1.2.73")
}

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    maven("https://status.bintray.com/kotlin/kotlin-eap")
}
