package com.sirktv.app.data.mapper

import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.model.SubscriptionStatus
import com.sirktv.app.domain.model.UserProfile
import com.sirktv.app.network.dto.XtreamAuthResponseDto

internal object AuthResponseMapper {

    fun toLoginResult(dto: XtreamAuthResponseDto, serverUrl: String): LoginResult {
        val userInfo = dto.userInfo
            ?: return LoginResult.ServerError("Unexpected response - is this a valid Xtream Codes server?")

        if (userInfo.auth != 1) return LoginResult.InvalidCredentials

        val status = mapStatus(userInfo.status)
        val expiresAt = userInfo.expDate?.toLongOrNull()
        val nowSeconds = System.currentTimeMillis() / 1000

        if (status != SubscriptionStatus.ACTIVE) {
            return LoginResult.SubscriptionInactive(status, statusMessage(status))
        }
        if (expiresAt != null && expiresAt < nowSeconds) {
            return LoginResult.SubscriptionInactive(
                SubscriptionStatus.EXPIRED,
                statusMessage(SubscriptionStatus.EXPIRED)
            )
        }

        val profile = UserProfile(
            username = userInfo.username.orEmpty(),
            serverUrl = serverUrl,
            status = status,
            expiresAtEpochSeconds = expiresAt,
            isTrial = userInfo.isTrial == "1",
            activeConnections = userInfo.activeCons?.toIntOrNull() ?: 0,
            maxConnections = userInfo.maxConnections?.toIntOrNull() ?: 1,
            allowedOutputFormats = userInfo.allowedOutputFormats.orEmpty()
        )
        return LoginResult.Success(profile)
    }

    // Unrecognized/missing status defaults to ACTIVE: auth == 1 already confirms
    // valid credentials, and many panel variants omit or vary this field.
    private fun mapStatus(raw: String?): SubscriptionStatus = when (raw?.trim()?.lowercase()) {
        "expired" -> SubscriptionStatus.EXPIRED
        "disabled" -> SubscriptionStatus.DISABLED
        "banned" -> SubscriptionStatus.BANNED
        else -> SubscriptionStatus.ACTIVE
    }

    private fun statusMessage(status: SubscriptionStatus): String = when (status) {
        SubscriptionStatus.EXPIRED -> "Your subscription has expired."
        SubscriptionStatus.DISABLED -> "This account has been disabled."
        SubscriptionStatus.BANNED -> "This account has been banned."
        SubscriptionStatus.ACTIVE -> ""
    }
}
