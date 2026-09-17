package com.vasmarfas.card.data

import com.vasmarfas.card.core.Tr
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val version: Int = 1,
    val updated: String = "",
    val person: Person,
    val avatar: Avatar = Avatar(),
    val links: List<Link> = emptyList(),
    val milestones: Milestones = Milestones(),
    val projects: List<Project> = emptyList(),
    val articles: List<Article> = emptyList(),
    val downloads: List<Link> = emptyList(),
)

@Serializable
data class Person(
    val name: Tr,
    val nickname: String = "",
    val title: Tr,
    val location: Tr,
    val bio: Tr,
    val about: List<Tr> = emptyList(),
)

@Serializable
data class Milestones(
    val androidSince: Int = 2022,
    val sysadminSince: Int = 2018,
)

/** `url` empty falls back to the bundled picture; `active` false hides the avatar entirely. */
@Serializable
data class Avatar(
    val url: String = "",
    val active: Boolean = false,
)

@Serializable
data class Link(
    val type: String,
    val url: String,
    val label: Tr? = null,
    val active: Boolean = true,
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    /** Where the project's numbers come from; only "github" carries a star count today. */
    val source: String = "",
    val tagline: Tr,
    val description: Tr,
    val tech: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val links: List<Link> = emptyList(),
    val year: String = "",
    val featured: Boolean = false,
    val stars: Int? = null,
    val status: Tr? = null,
)

@Serializable
data class Article(
    val title: Tr,
    val url: String,
    val date: String,
    val source: String = "habr",
    val views: String? = null,
    val summary: Tr? = null,
)

@Serializable
data class Resume(
    val version: Int = 1,
    val updated: String = "",
    val summary: Tr? = null,
    val skills: List<SkillGroup> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val courses: List<Education> = emptyList(),
    val languages: List<LanguageSkill> = emptyList(),
) {
    val isEmpty: Boolean
        get() = skills.isEmpty() && experience.isEmpty() && education.isEmpty() && courses.isEmpty() && languages.isEmpty()
}

@Serializable
data class SkillGroup(
    val title: Tr,
    val items: List<String>,
)

@Serializable
data class Experience(
    val company: Tr,
    val role: Tr,
    val from: String,
    val to: String? = null,
    val url: String? = null,
    val location: Tr? = null,
    val summary: Tr? = null,
    val bullets: List<Tr> = emptyList(),
    val tags: List<String> = emptyList(),
)

@Serializable
data class Education(
    val institution: Tr,
    val degree: Tr,
    val year: String,
    val url: String? = null,
    val note: Tr? = null,
)

@Serializable
data class LanguageSkill(
    val name: Tr,
    val level: Tr,
)
