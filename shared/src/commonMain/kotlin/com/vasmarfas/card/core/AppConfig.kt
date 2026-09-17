package com.vasmarfas.card.core

object AppConfig {
    const val APP_NAME = "vasmarfas"
    const val VERSION = "0.1.0"
    const val SITE_COM = "https://vasmarfas.com"
    const val SITE_RU = "https://vasmarfas.ru"
    const val GITHUB_USER = "vasmarfas"
    const val REPO_URL = "https://github.com/vasmarfas/toolbox"
    const val GITHUB_API = "https://api.github.com"
    private const val RAW_BASE = "https://raw.githubusercontent.com/vasmarfas/toolbox/main/shared/src/commonMain/composeResources/files"
    const val PROFILE_BUNDLED_PATH = "files/profile.json"
    const val RESUME_BUNDLED_PATH = "files/resume.json"
    const val AVATAR_BUNDLED_PATH = "files/images/avatar.png"

    fun contentBase(): String =
        if (siteHost()?.endsWith("vasmarfas.ru") == true) "$SITE_RU/content" else RAW_BASE

    fun contentUrl(name: String): String = "${contentBase()}/$name"
}
