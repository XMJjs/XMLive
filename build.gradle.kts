// build.gradle.kts
// import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
}

group = "XMLIVE"      // 替换成你的包名
version = "1.0.0"              // 你的插件版本

repositories {
    mavenCentral()
    // PaperMC 仓库，用于获取 Paper API
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API 1.21+，编译时依赖（服务器已自带，所以用 compileOnly）
    // 确保版本与你的服务器版本匹配
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

java {
    // 使用 Java 21，Paper 1.21+ 要求
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    processResources {
        // 在构建时自动将 version 写入 plugin.yml
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }

    // 打包时的 JAR 命名
    jar {
        archiveFileName.set("XMLIVE-${project.version}.jar")
    }
}
