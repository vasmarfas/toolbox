import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.compose.ui)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.vasmarfas.card.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )
            packageName = "Mobitool"
            packageVersion = (rootProject.extra["appVersionName"] as String).substringBefore('-')
            vendor = "vasmarfas"
            description = "vasmarfas Mobitool: business card, portfolio and a multitool"
            modules("jdk.unsupported", "java.naming", "java.net.http")

            windows {
                menuGroup = "vasmarfas"
                shortcut = true
                perUserInstall = true
                iconFile.set(project.file("icons/logo.ico"))
                upgradeUuid = "8B2C1A7E-6F3D-4B2A-9C1E-5D7F0A3B9E21"
            }
            macOS {
                bundleID = "com.vasmarfas.mobitool"
                minimumSystemVersion = "12.0"
                iconFile.set(project.file("icons/logo.icns"))
                packageBuildVersion = rootProject.extra["appVersionCode"].toString()
                val identity = System.getenv("APPLE_DEVELOPER_ID_IDENTITY")
                if (!identity.isNullOrBlank()) {
                    signing {
                        sign.set(true)
                        this.identity.set(identity)
                    }
                }
                val appleId = System.getenv("APPLE_ID")
                if (!appleId.isNullOrBlank()) {
                    notarization {
                        appleID.set(appleId)
                        password.set(System.getenv("APPLE_ID_PASSWORD"))
                        teamID.set(System.getenv("APPLE_TEAM_ID"))
                    }
                }
            }
            linux {
                iconFile.set(project.file("icons/logo.png"))
                packageName = "vasmarfas"
                debMaintainer = "vasmarfas@mail.ru"
                menuGroup = "Utility"
            }
        }
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}

val ffmpegDeb = listOf(
    "libpulse0", "libva2", "libva-drm2", "libva-x11-2", "libvdpau1", "libdrm2",
    "libxcb1", "libxcb-shm0", "libxcb-shape0", "libxcb-xfixes0", "libasound2t64 | libasound2",
)
val ffmpegRpm = listOf(
    "libpulse.so.0", "libva.so.2", "libva-drm.so.2", "libva-x11.so.2", "libvdpau.so.1", "libdrm.so.2",
    "libxcb.so.1", "libxcb-shm.so.0", "libxcb-shape.so.0", "libxcb-xfixes.so.0", "libasound.so.2",
).map { "$it()(64bit)" }

tasks.withType<AbstractJPackageTask>().configureEach {
    when (targetFormat) {
        TargetFormat.Deb -> freeArgs.addAll("--linux-package-deps", ffmpegDeb.joinToString(", "))
        TargetFormat.Rpm -> freeArgs.addAll("--linux-package-deps", ffmpegRpm.joinToString(", "))
        else -> {}
    }
}
