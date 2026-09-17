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
        HttpStatus(100, "Continue", Res.string.request_headers_accepted_the_client_may_send),
        HttpStatus(101, "Switching Protocols", Res.string.server_switches_to_the_protocol_requested_in),
        HttpStatus(102, "Processing", Res.string.webdav_the_request_is_being_processed_no_res),
        HttpStatus(103, "Early Hints", Res.string.preliminary_headers_usually_link_for_preload),
        HttpStatus(200, "OK", Res.string.request_succeeded),
        HttpStatus(201, "Created", Res.string.a_new_resource_was_created_its_uri_is_usuall),
        HttpStatus(202, "Accepted", Res.string.accepted_for_processing_but_not_completed_ye),
        HttpStatus(203, "Non-Authoritative Information", Res.string.response_was_modified_by_a_proxy),
        HttpStatus(204, "No Content", Res.string.success_no_body_in_the_response),
        HttpStatus(205, "Reset Content", Res.string.success_the_client_should_reset_the_document),
        HttpStatus(206, "Partial Content", Res.string.part_of_the_resource_as_requested_by_range),
        HttpStatus(207, "Multi-Status", Res.string.webdav_multiple_status_codes_for_multiple_re),
        HttpStatus(208, "Already Reported", Res.string.webdav_members_of_a_binding_were_already_lis),
        HttpStatus(226, "IM Used", Res.string.delta_encoding_response_is_a_diff_against_a),
        HttpStatus(300, "Multiple Choices", Res.string.several_representations_available_the_client),
        HttpStatus(301, "Moved Permanently", Res.string.resource_moved_permanently_to_the_uri_in_loc),
        HttpStatus(302, "Found", Res.string.temporary_redirect_browsers_usually_switch_t),
        HttpStatus(303, "See Other", Res.string.fetch_the_result_at_another_uri_with_get_typ),
        HttpStatus(304, "Not Modified", Res.string.cached_version_is_still_valid_if_none_match),
        HttpStatus(305, "Use Proxy", Res.string.deprecated_the_resource_must_be_accessed_thr),
        HttpStatus(307, "Temporary Redirect", Res.string.temporary_redirect_keeping_the_method_and_bo),
        HttpStatus(308, "Permanent Redirect", Res.string.permanent_redirect_keeping_the_method_and_bo),
        HttpStatus(400, "Bad Request", Res.string.malformed_request_syntax_or_invalid_paramete),
        HttpStatus(401, "Unauthorized", Res.string.authentication_required_or_failed_www_authen),
        HttpStatus(402, "Payment Required", Res.string.reserved_used_by_some_apis_for_billing_limit),
        HttpStatus(403, "Forbidden", Res.string.authenticated_but_not_allowed_to_access_the),
        HttpStatus(404, "Not Found", Res.string.resource_not_found),
        HttpStatus(405, "Method Not Allowed", Res.string.http_method_not_supported_for_this_resource),
        HttpStatus(406, "Not Acceptable", Res.string.no_representation_matches_the_accept_headers),
        HttpStatus(407, "Proxy Authentication Required", Res.string.authenticate_with_the_proxy_first),
        HttpStatus(408, "Request Timeout", Res.string.server_timed_out_waiting_for_the_request),
        HttpStatus(409, "Conflict", Res.string.conflict_with_the_current_state_of_the_resou),
        HttpStatus(410, "Gone", Res.string.resource_permanently_removed_no_forwarding_a),
        HttpStatus(411, "Length Required", Res.string.content_length_header_is_required),
        HttpStatus(412, "Precondition Failed", Res.string.a_precondition_header_if_match_etc_evaluated),
        HttpStatus(413, "Content Too Large", Res.string.request_body_exceeds_the_server_limit),
        HttpStatus(414, "URI Too Long", Res.string.request_uri_is_longer_than_the_server_accept),
        HttpStatus(415, "Unsupported Media Type", Res.string.request_body_format_is_not_supported),
        HttpStatus(416, "Range Not Satisfiable", Res.string.requested_range_is_outside_the_resource_size),
        HttpStatus(417, "Expectation Failed", Res.string.expect_header_requirement_cannot_be_met),
        HttpStatus(418, "I'm a teapot", Res.string.april_fools_joke_from_rfc_2324),
        HttpStatus(421, "Misdirected Request", Res.string.request_was_sent_to_a_server_that_cannot_pro),
        HttpStatus(422, "Unprocessable Content", Res.string.syntax_is_fine_but_semantic_validation_faile),
        HttpStatus(423, "Locked", Res.string.webdav_the_resource_is_locked),
        HttpStatus(424, "Failed Dependency", Res.string.webdav_a_dependent_request_failed),
        HttpStatus(425, "Too Early", Res.string.server_refuses_to_process_a_request_that_mig),
        HttpStatus(426, "Upgrade Required", Res.string.client_must_switch_to_another_protocol_upgra),
        HttpStatus(428, "Precondition Required", Res.string.server_requires_a_conditional_request_if_mat),
        HttpStatus(429, "Too Many Requests", Res.string.rate_limit_exceeded_see_retry_after),
        HttpStatus(431, "Request Header Fields Too Large", Res.string.headers_are_too_large),
        HttpStatus(451, "Unavailable For Legal Reasons", Res.string.blocked_for_legal_reasons_censorship_court_o),
        HttpStatus(500, "Internal Server Error", Res.string.unexpected_server_side_error),
        HttpStatus(501, "Not Implemented", Res.string.server_does_not_support_the_request_method),
        HttpStatus(502, "Bad Gateway", Res.string.gateway_or_proxy_got_an_invalid_response_fro),
        HttpStatus(503, "Service Unavailable", Res.string.server_is_overloaded_or_down_for_maintenance),
        HttpStatus(504, "Gateway Timeout", Res.string.gateway_did_not_get_a_timely_response_from_t),
        HttpStatus(505, "HTTP Version Not Supported", Res.string.http_version_in_the_request_is_not_supported),
        HttpStatus(506, "Variant Also Negotiates", Res.string.content_negotiation_configuration_error_on_t),
        HttpStatus(507, "Insufficient Storage", Res.string.webdav_not_enough_storage_to_complete_the_re),
        HttpStatus(508, "Loop Detected", Res.string.webdav_infinite_loop_detected_while_processi),
        HttpStatus(510, "Not Extended", Res.string.further_extensions_to_the_request_are_requir),
        HttpStatus(511, "Network Authentication Required", Res.string.network_access_requires_authentication_capti),
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
