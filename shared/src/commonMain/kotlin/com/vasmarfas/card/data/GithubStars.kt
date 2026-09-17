package com.vasmarfas.card.data

import com.vasmarfas.card.core.AppConfig
import com.vasmarfas.card.core.Net
import com.vasmarfas.card.core.Prefs
import com.vasmarfas.card.core.currentEpochMillis
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

object GithubStars {
    private const val CACHE_KEY = "github.stars"
    private const val CACHE_TIME_KEY = "github.stars.time"
    private const val MAX_AGE_MS = 6L * 60 * 60 * 1000
    private val serializer = MapSerializer(String.serializer(), Int.serializer())

    private val _stars = MutableStateFlow(readCache())
    val stars: StateFlow<Map<String, Int>> = _stars.asStateFlow()

    private fun readCache(): Map<String, Int> =
        runCatching { Prefs.store.get(CACHE_KEY)?.let { Net.json.decodeFromString(serializer, it) } }.getOrNull() ?: emptyMap()

    private fun cacheAge(): Long = currentEpochMillis() - (Prefs.store.get(CACHE_TIME_KEY)?.toLongOrNull() ?: 0L)

    fun repoOf(project: Project): String? {
        if (project.source != "github") return null
        val url = project.links.firstOrNull { it.type == "github" && it.url.contains("github.com/") }?.url ?: return null
        return url.substringAfter("github.com/").trim('/').split('/').take(2).takeIf { it.size == 2 }?.joinToString("/")
    }

    fun starsOf(project: Project): Int? = repoOf(project)?.let { _stars.value[it] } ?: project.stars

    suspend fun refresh(projects: List<Project>, force: Boolean = false) {
        if (!force && cacheAge() < MAX_AGE_MS && _stars.value.isNotEmpty()) return
        val repos = projects.mapNotNull(::repoOf).distinct()
        if (repos.isEmpty()) return
        val fetched = coroutineScope {
            repos.map { repo ->
                async {
                    runCatching {
                        val response = Net.client.get("${AppConfig.GITHUB_API}/repos/$repo") {
                            header("accept", "application/vnd.github+json")
                        }
                        if (!response.status.isSuccess()) return@async null
                        val count = (Net.json.parseToJsonElement(response.bodyAsText()).jsonObject["stargazers_count"] as? JsonPrimitive)?.intOrNull
                        count?.let { repo to it }
                    }.getOrNull()
                }
            }.mapNotNull { it.await() }
        }.toMap()
        if (fetched.isEmpty()) return
        val merged = _stars.value + fetched
        _stars.value = merged
        Prefs.store.put(CACHE_KEY, Net.json.encodeToString(serializer, merged))
        Prefs.store.put(CACHE_TIME_KEY, currentEpochMillis().toString())
    }
}
