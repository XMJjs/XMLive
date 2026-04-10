plugins {
    java
}

group = "com.xmjjs"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }
    jar {
        archiveFileName.set("XMLIVE-${project.version}.jar")
    }
}

// 独立任务：打印版本号，供 GitHub Actions 使用
tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
