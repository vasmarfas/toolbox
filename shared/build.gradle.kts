import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    android {
        namespace = "com.vasmarfas.card.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        jvmMain.get().dependsOn(jvmCommonMain)
        androidMain.get().dependsOn(jvmCommonMain)

        // the dependsOn edges above switch off the default hierarchy template, so the Apple
        // source set has to be attached by hand or nothing in iosMain reaches a compilation
        val appleShared = maybeCreate("iosMain")
        appleShared.dependsOn(commonMain.get())
        getByName("iosArm64Main").dependsOn(appleShared)
        getByName("iosSimulatorArm64Main").dependsOn(appleShared)

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.perf)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.ktor.client.core)
            implementation(libs.materialKolor)
            implementation(libs.qrose)
            implementation(libs.qrose.oned)
        }
        jvmTest.dependencies {
            // Compose Resources on the JVM loads through Skiko, which needs the desktop runtime present.
            implementation(compose.desktop.currentOs)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
        }
    }
}


// Compose Resources only ever resolves the active locale, so a search that has to match both
// languages at once needs its own copy of the tables. Regenerated from the same strings.xml.
val generateStringIndex by tasks.registering {
    val english = layout.projectDirectory.file("src/commonMain/composeResources/values/strings.xml").asFile
    val russian = layout.projectDirectory.file("src/commonMain/composeResources/values-ru/strings.xml").asFile
    val outputDir = layout.buildDirectory.dir("generated/stringIndex/kotlin").get().asFile
    inputs.files(english, russian)
    outputs.dir(outputDir)
    doLast {
        val backslash = '\\'
        val quote = '"'
        val newline = 10.toChar()
        val tab = 9.toChar()

        fun parse(file: File): List<Pair<String, String>> {
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            return (0 until nodes.length).map { index ->
                val node = nodes.item(index) as org.w3c.dom.Element
                // mirror what the resource loader does: backslash sequences are unescaped,
                // quotes are not
                val raw = node.textContent
                val text = StringBuilder(raw.length).also { sb ->
                    var at = 0
                    while (at < raw.length) {
                        val c = raw[at]
                        if (c == backslash && at + 1 < raw.length) {
                            when (val next = raw[at + 1]) {
                                'n' -> sb.append(newline)
                                't' -> sb.append(tab)
                                backslash -> sb.append(backslash)
                                else -> sb.append(c).append(next)
                            }
                            at += 2
                        } else {
                            sb.append(c)
                            at++
                        }
                    }
                }.toString()
                node.getAttribute("name") to text
            }
        }

        fun literal(value: String): String = value
            .replace("" + backslash, "" + backslash + backslash)
            .replace("" + quote, "" + backslash + quote)
            .replace("$", "" + backslash + "$")
            .replace("\n", "" + backslash + "n")
            .replace("\t", "" + backslash + "t")
            .replace("\r", "")

        val target = File(outputDir, "com/vasmarfas/card/resources")
        target.mkdirs()
        val chunkSize = 300
        val out = StringBuilder()
        out.appendLine("// Generated by generateStringIndex from composeResources/values*/strings.xml.")
        out.appendLine("package com.vasmarfas.card.resources")
        out.appendLine()
        out.appendLine("import org.jetbrains.compose.resources.StringResource")
        out.appendLine()
        listOf("en" to parse(english), "ru" to parse(russian)).forEach { (name, rows) ->
            val chunks = rows.chunked(chunkSize)
            chunks.forEachIndexed { index, chunk ->
                out.appendLine("private fun " + name + index + "(m: MutableMap<String, String>) {")
                chunk.forEach { (key, text) ->
                    out.appendLine("    m[" + quote + key + quote + "] = " + quote + literal(text) + quote)
                }
                out.appendLine("}")
            }
            val calls = chunks.indices.joinToString("; ") { name + it + "(it)" }
            out.appendLine(
                "private val " + name + ": Map<String, String> by lazy { HashMap<String, String>(" +
                    rows.size + ").also { " + calls + " } }"
            )
            out.appendLine()
        }
        out.appendLine("/** The stored English text, or null when the key is missing from values/strings.xml. */")
        out.appendLine("fun StringResource.english(): String? = en[key]")
        out.appendLine()
        out.appendLine("/** The stored Russian text, or null when the key is missing from values-ru/strings.xml. */")
        out.appendLine("fun StringResource.russian(): String? = ru[key]")
        out.appendLine()
        out.appendLine("/** Matches the query against both translations, whichever language the UI is in. */")
        out.appendLine("fun StringResource.matches(query: String): Boolean =")
        out.appendLine("    english()?.contains(query, ignoreCase = true) == true ||")
        out.appendLine("        russian()?.contains(query, ignoreCase = true) == true")
        File(target, "StringIndex.kt").writeText(out.toString())
    }
}

kotlin.sourceSets.commonMain.get().kotlin.srcDir(generateStringIndex)

val generateAppVersion by tasks.registering {
    val version = rootProject.extra["appVersionName"] as String
    val outputDir = layout.buildDirectory.dir("generated/version/kotlin").get().asFile
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val quote = 0x22.toChar()
        val target = File(outputDir, "com/vasmarfas/card/core")
        target.mkdirs()
        val out = StringBuilder()
        out.appendLine("// Generated by generateAppVersion from the root build script.")
        out.appendLine("package com.vasmarfas.card.core")
        out.appendLine()
        out.appendLine("const val APP_VERSION: String = " + quote + version + quote)
        File(target, "AppVersion.kt").writeText(out.toString())
    }
}

kotlin.sourceSets.commonMain.get().kotlin.srcDir(generateAppVersion)

// Xcode picks the version up from the xcconfig, which the root task rewrites
tasks.matching { it.name.contains("Framework") }.configureEach {
    dependsOn(rootProject.tasks.named("syncIosVersion"))
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.vasmarfas.card.resources"
    generateResClass = always
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
