package com.sirktv.app.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape of Xtream Codes `player_api.php?username=&password=`.
 * Fields are nullable/String-typed defensively: panel implementations vary
 * (XtreamCodes, XUI ONE, Xtream UI, ...) and are inconsistent about which
 * fields are present or how numbers are encoded.
 */
@Serializable
data class XtreamAuthResponseDto(
    @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerialName("server_info") val serverInfo: ServerInfoDto? = null
)

@Serializable
data class UserInfoDto(
    val username: String? = null,
    val message: String? = null,
    val auth: Int? = null,
    val status: String? = null,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    @SerialName("active_cons") val activeCons: String? = null,
    @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("allowed_output_formats") val allowedOutputFormats: List<String>? = null
)

@Serializable
data class ServerInfoDto(
    val url: String? = null,
    val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    val timezone: String? = null
)
