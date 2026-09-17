import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
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
            packageName = "Toolbox"
            packageVersion = (rootProject.extra["appVersionName"] as String).substringBefore('-')
            vendor = "vasmarfas"
            description = "vasmarfas Toolbox: business card, portfolio and a multitool"
            modules("jdk.unsupported", "java.naming", "java.net.http")

            windows {
                menuGroup = "vasmarfas"
                shortcut = true
                perUserInstall = true
                iconFile.set(project.file("icons/logo.ico"))
                upgradeUuid = "8B2C1A7E-6F3D-4B2A-9C1E-5D7F0A3B9E21"
            }
            macOS {
                bundleID = "com.vasmarfas.toolbox"
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
