package com.vasmarfas.card.tools.developer

import com.vasmarfas.card.core.Tr
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class JwtParts(
    val header: JsonObject,
    val payload: JsonObject,
    val signature: String,
)

class JwtException(val text: Tr) : Exception(text.en)

object Jwt {
    val claimNames: Map<String, Tr> = mapOf(
        "iss" to Tr("Issuer", "Издатель"),
        "sub" to Tr("Subject", "Субъект"),
        "aud" to Tr("Audience", "Аудитория"),
        "exp" to Tr("Expiration time", "Срок действия до"),
        "nbf" to Tr("Not before", "Действителен не раньше"),
        "iat" to Tr("Issued at", "Выдан"),
        "jti" to Tr("JWT ID", "Идентификатор токена"),
        "alg" to Tr("Algorithm", "Алгоритм"),
        "typ" to Tr("Type", "Тип"),
        "kid" to Tr("Key ID", "Идентификатор ключа"),
        "cty" to Tr("Content type", "Тип содержимого"),
        "scope" to Tr("Scopes", "Права"),
        "scp" to Tr("Scopes", "Права"),
        "azp" to Tr("Authorized party", "Авторизованная сторона"),
        "nonce" to Tr("Nonce", "Nonce"),
        "auth_time" to Tr("Authentication time", "Время аутентификации"),
        "sid" to Tr("Session ID", "Идентификатор сессии"),
        "email" to Tr("Email", "Email"),
        "email_verified" to Tr("Email verified", "Email подтверждён"),
        "name" to Tr("Name", "Имя"),
        "given_name" to Tr("Given name", "Имя"),
        "family_name" to Tr("Family name", "Фамилия"),
        "preferred_username" to Tr("Preferred username", "Имя пользователя"),
        "username" to Tr("Username", "Имя пользователя"),
        "roles" to Tr("Roles", "Роли"),
        "role" to Tr("Role", "Роль"),
        "groups" to Tr("Groups", "Группы"),
        "client_id" to Tr("Client ID", "Идентификатор клиента"),
        "token_use" to Tr("Token use", "Назначение токена"),
        "amr" to Tr("Authentication methods", "Методы аутентификации"),
        "acr" to Tr("Authentication context class", "Класс контекста аутентификации"),
        "at_hash" to Tr("Access token hash", "Хеш access-токена"),
        "picture" to Tr("Picture URL", "URL аватара"),
        "locale" to Tr("Locale", "Локаль"),
    )

    val timeClaims = setOf("exp", "nbf", "iat", "auth_time", "updated_at")

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeSegment(segment: String): String? = try {
        Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(segment).decodeToString(throwOnInvalidSequence = true)
    } catch (e: Exception) {
        null
    }

    fun decode(token: String): Result<JwtParts> {
        val parts = token.trim().removePrefix("Bearer ").trim().split('.')
        if (parts.size != 3) {
            return Result.failure(JwtException(Tr("A JWT has three dot-separated parts: header.payload.signature.", "JWT состоит из трёх частей через точку: header.payload.signature.")))
        }
        val header = decodeObject(parts[0]) ?: return Result.failure(JwtException(Tr("Header is not Base64URL-encoded JSON object.", "Заголовок — не JSON-объект в Base64URL.")))
        val payload = decodeObject(parts[1]) ?: return Result.failure(JwtException(Tr("Payload is not Base64URL-encoded JSON object.", "Полезная нагрузка — не JSON-объект в Base64URL.")))
        return Result.success(JwtParts(header, payload, parts[2]))
    }

    private fun decodeObject(segment: String): JsonObject? {
        val json = decodeSegment(segment) ?: return null
        return JsonTools.parse(json).getOrNull() as? JsonObject
    }

    fun claimText(value: JsonElement): String = when (value) {
        is JsonPrimitive -> value.content
        is JsonArray -> value.joinToString(", ") { claimText(it) }
        is JsonObject -> JsonTools.minify(value)
    }

    fun epochSeconds(value: JsonElement): Long? = (value as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toDoubleOrNull()?.toLong()
}
