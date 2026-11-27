import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

val appVersionName: String = providers.gradleProperty("app.versionName").orElse("1.0.0").get()
val appVersionCode: Int = providers.gradleProperty("app.versionCode").orElse("1").get().toInt()

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val generatedStringsDir = layout.buildDirectory.dir("generated/source/strings/kotlin")

val generateStringIds by tasks.registering {
    val xmlFile = file("src/main/resources/i18n/values/strings.xml")
    inputs.file(xmlFile)
    outputs.dir(generatedStringsDir)

    doLast {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val nodes = doc.getElementsByTagName("string")
        val names = mutableListOf<String>()
        for (i in 0 until nodes.length) {
            val name = nodes.item(i).attributes?.getNamedItem("name")?.nodeValue
            if (!name.isNullOrBlank()) {
                names.add(name)
            }
        }
        val outFile = generatedStringsDir.get().file("com/mosquitobr/controlepoco/R.kt").asFile
        outFile.parentFile.mkdirs()
        val sb = StringBuilder()
        sb.appendLine("package com.mosquitobr.controlepoco")
        sb.appendLine()
        sb.appendLine("// Auto-generated from src/main/resources/i18n/values/strings.xml")
        sb.appendLine("object R {")
        sb.appendLine("    object string {")
        names.forEachIndexed { idx, name ->
            sb.appendLine("        const val $name: Int = $idx")
        }
        sb.appendLine("    }")
        sb.appendLine()
        sb.appendLine("    val keys: Array<String> = arrayOf(")
        names.forEachIndexed { idx, name ->
            val comma = if (idx + 1 < names.size) "," else ""
            sb.appendLine("        \"$name\"$comma")
        }
        sb.appendLine("    )")
        sb.appendLine("}")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
    }
}

val generateBuildInfo by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/source/buildinfo/kotlin")
    inputs.property("versionName", appVersionName)
    inputs.property("versionCode", appVersionCode)
    outputs.dir(outDir)

    doLast {
        val outFile = outDir.get().file("com/mosquitobr/controlepoco/BuildConfig.kt").asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(
            """
            package com.mosquitobr.controlepoco

            object BuildConfig {
                const val APPLICATION_ID: String = "com.mosquitobr.controlepoco.desktop"
                const val VERSION_NAME: String = "$appVersionName"
                const val VERSION_CODE: Int = $appVersionCode
            }
            """.trimIndent() + "\n",
            Charsets.UTF_8
        )
    }
}

sourceSets {
    main {
        kotlin.srcDir(generateStringIds.map { it.outputs.files.singleFile })
        kotlin.srcDir(generateBuildInfo.map { it.outputs.files.singleFile })
    }
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    systemProperty("captureScreenshots", System.getProperty("captureScreenshots") ?: "false")
    systemProperty("screenshotOutDir", rootProject.file("docs/screenshots").absolutePath)
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

compose.desktop {
    application {
        mainClass = "com.mosquitobr.controlepoco.MainKt"

        jvmArgs += listOf(
            "-Dapple.awt.application.appearance=system",
            "-Dsun.java2d.uiScale.enabled=true"
        )

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.AppImage
            )
            packageName = "ControleDePoco2"
            packageVersion = appVersionName
            description = "Educational Well Control Simulator (Windows 11, Linux/WSL & Android)"
            copyright = "Copyright (c) 2026 mosquitobr — Apache License 2.0"
            vendor = "mosquitobr"
            licenseFile.set(rootProject.file("LICENSE"))

            modules("java.desktop", "java.logging", "java.xml", "jdk.unsupported")

            windows {
                menuGroup = "Well Control Free Simulator"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8d3e4b2a-9a17-4f6b-9f25-3c1f2e77a112"
                iconFile.set(project.file("src/main/resources/icons/app_icon.ico"))
            }

            linux {
                packageName = "well-control-free-simulator"
                debMaintainer = "97371621+mosquitobr@users.noreply.github.com"
                menuGroup = "Education;Science;Engineering"
                appCategory = "Education"
                shortcut = true
                iconFile.set(project.file("src/main/resources/icons/app_icon.png"))
            }
        }
    }
}
