package com.vasmarfas.card.tools.developer

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

data class HttpStatus(val code: Int, val name: String, val description: StringResource) {
    val group: Int get() = code / 100
}

object HttpStatuses {
    val groupTitles: Map<Int, StringResource> = mapOf(
        1 to Res.string.s_1xx_informational,
        2 to Res.string.s_2xx_success,
        3 to Res.string.s_3xx_redirection,
        4 to Res.string.s_4xx_client_error,
        5 to Res.string.s_5xx_server_error,
    )

    val all: List<HttpStatus> = listOf(
        HttpStatus(100, "Continue", Res.string.http_100_description),
        HttpStatus(101, "Switching Protocols", Res.string.http_101_description),
        HttpStatus(102, "Processing", Res.string.http_102_description),
        HttpStatus(103, "Early Hints", Res.string.http_103_description),
        HttpStatus(200, "OK", Res.string.http_200_description),
        HttpStatus(201, "Created", Res.string.http_201_description),
        HttpStatus(202, "Accepted", Res.string.http_202_description),
        HttpStatus(203, "Non-Authoritative Information", Res.string.http_203_description),
        HttpStatus(204, "No Content", Res.string.http_204_description),
        HttpStatus(205, "Reset Content", Res.string.http_205_description),
        HttpStatus(206, "Partial Content", Res.string.http_206_description),
        HttpStatus(207, "Multi-Status", Res.string.http_207_description),
        HttpStatus(208, "Already Reported", Res.string.http_208_description),
        HttpStatus(226, "IM Used", Res.string.http_226_description),
        HttpStatus(300, "Multiple Choices", Res.string.http_300_description),
        HttpStatus(301, "Moved Permanently", Res.string.http_301_description),
        HttpStatus(302, "Found", Res.string.http_302_description),
        HttpStatus(303, "See Other", Res.string.http_303_description),
        HttpStatus(304, "Not Modified", Res.string.http_304_description),
        HttpStatus(305, "Use Proxy", Res.string.http_305_description),
        HttpStatus(307, "Temporary Redirect", Res.string.http_307_description),
        HttpStatus(308, "Permanent Redirect", Res.string.http_308_description),
        HttpStatus(400, "Bad Request", Res.string.http_400_description),
        HttpStatus(401, "Unauthorized", Res.string.http_401_description),
        HttpStatus(402, "Payment Required", Res.string.http_402_description),
        HttpStatus(403, "Forbidden", Res.string.http_403_description),
        HttpStatus(404, "Not Found", Res.string.http_404_description),
        HttpStatus(405, "Method Not Allowed", Res.string.http_405_description),
        HttpStatus(406, "Not Acceptable", Res.string.http_406_description),
        HttpStatus(407, "Proxy Authentication Required", Res.string.http_407_description),
        HttpStatus(408, "Request Timeout", Res.string.http_408_description),
        HttpStatus(409, "Conflict", Res.string.http_409_description),
        HttpStatus(410, "Gone", Res.string.http_410_description),
        HttpStatus(411, "Length Required", Res.string.http_411_description),
        HttpStatus(412, "Precondition Failed", Res.string.http_412_description),
        HttpStatus(413, "Content Too Large", Res.string.http_413_description),
        HttpStatus(414, "URI Too Long", Res.string.http_414_description),
        HttpStatus(415, "Unsupported Media Type", Res.string.http_415_description),
        HttpStatus(416, "Range Not Satisfiable", Res.string.http_416_description),
        HttpStatus(417, "Expectation Failed", Res.string.http_417_description),
        HttpStatus(418, "I'm a teapot", Res.string.http_418_description),
        HttpStatus(421, "Misdirected Request", Res.string.http_421_description),
        HttpStatus(422, "Unprocessable Content", Res.string.http_422_description),
        HttpStatus(423, "Locked", Res.string.http_423_description),
        HttpStatus(424, "Failed Dependency", Res.string.http_424_description),
        HttpStatus(425, "Too Early", Res.string.http_425_description),
        HttpStatus(426, "Upgrade Required", Res.string.http_426_description),
        HttpStatus(428, "Precondition Required", Res.string.http_428_description),
        HttpStatus(429, "Too Many Requests", Res.string.http_429_description),
        HttpStatus(431, "Request Header Fields Too Large", Res.string.http_431_description),
        HttpStatus(451, "Unavailable For Legal Reasons", Res.string.http_451_description),
        HttpStatus(500, "Internal Server Error", Res.string.http_500_description),
        HttpStatus(501, "Not Implemented", Res.string.http_501_description),
        HttpStatus(502, "Bad Gateway", Res.string.http_502_description),
        HttpStatus(503, "Service Unavailable", Res.string.http_503_description),
        HttpStatus(504, "Gateway Timeout", Res.string.http_504_description),
        HttpStatus(505, "HTTP Version Not Supported", Res.string.http_505_description),
        HttpStatus(506, "Variant Also Negotiates", Res.string.http_506_description),
        HttpStatus(507, "Insufficient Storage", Res.string.http_507_description),
        HttpStatus(508, "Loop Detected", Res.string.http_508_description),
        HttpStatus(510, "Not Extended", Res.string.http_510_description),
        HttpStatus(511, "Network Authentication Required", Res.string.http_511_description),
    )

    fun search(query: String): List<HttpStatus> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { s ->
            s.code.toString().startsWith(q) ||
                (q.length == 3 && q.endsWith("xx") && s.code.toString().startsWith(q.substring(0, 1))) ||
                s.name.lowercase().contains(q) ||
                s.description.matches(q)
        }
    }
}
