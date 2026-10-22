package com.x2softinc.masthan.data

import java.io.IOException
import java.net.SocketTimeoutException

/** Raised for any non-successful HTTP response. */
class HttpStatusException(val code: Int, message: String) : IOException(message)

enum class ErrorIcon { WIFI_OFF, LOCK, LOCATION_OFF, NOTIFICATIONS_OFF, WARNING, SERVER, TIMEOUT }

sealed interface RecoveryAction {
    val buttonTitle: String

    class Retry(val action: suspend () -> Unit) : RecoveryAction {
        override val buttonTitle = "Try Again"
    }

    data object OpenSettings : RecoveryAction {
        override val buttonTitle = "Open Settings"
    }

    data object Contact : RecoveryAction {
        override val buttonTitle = "Contact Support"
    }

    data object None : RecoveryAction {
        override val buttonTitle = "OK"
    }
}

/**
 * User-friendly error wrapper that converts technical errors into readable messages
 * (port of helper/UserFriendlyError.swift).
 */
class UserFriendlyError(
    val title: String,
    override val message: String,
    val icon: ErrorIcon,
    val recoveryAction: RecoveryAction?,
) : Exception(message) {

    companion object {
        fun network(retryAction: (suspend () -> Unit)? = null) = UserFriendlyError(
            title = "Connection Problem",
            message = "We're having trouble connecting to the server. Please check your internet connection and try again.",
            icon = ErrorIcon.WIFI_OFF,
            recoveryAction = retryAction?.let { RecoveryAction.Retry(it) },
        )

        fun authentication(retryAction: (suspend () -> Unit)? = null) = UserFriendlyError(
            title = "Authentication Required",
            message = "Your device needs to reconnect to access this feature. Please reconnect in Settings.",
            icon = ErrorIcon.LOCK,
            recoveryAction = RecoveryAction.OpenSettings,
        )

        fun locationPermission() = UserFriendlyError(
            title = "Location Access Needed",
            message = "Masthan Gani Masjid needs access to your location to calculate accurate prayer times. Please enable location access in Settings.",
            icon = ErrorIcon.LOCATION_OFF,
            recoveryAction = RecoveryAction.OpenSettings,
        )

        fun notificationPermission() = UserFriendlyError(
            title = "Notification Access Needed",
            message = "To receive prayer time reminders, please enable notifications in Settings.",
            icon = ErrorIcon.NOTIFICATIONS_OFF,
            recoveryAction = RecoveryAction.OpenSettings,
        )

        fun dataLoading(feature: String, retryAction: (suspend () -> Unit)? = null) = UserFriendlyError(
            title = "Unable to Load $feature",
            message = "We couldn't load $feature right now. This might be a temporary issue. Please try again.",
            icon = ErrorIcon.WARNING,
            recoveryAction = retryAction?.let { RecoveryAction.Retry(it) },
        )

        fun serverError() = UserFriendlyError(
            title = "Server Issue",
            message = "Our server is having trouble right now. Please try again in a few moments.",
            icon = ErrorIcon.SERVER,
            recoveryAction = RecoveryAction.Contact,
        )

        fun timeout(retryAction: (suspend () -> Unit)? = null) = UserFriendlyError(
            title = "Request Timed Out",
            message = "The request took too long to complete. Please check your internet connection and try again.",
            icon = ErrorIcon.TIMEOUT,
            recoveryAction = retryAction?.let { RecoveryAction.Retry(it) },
        )

        fun generic(error: Throwable, retryAction: (suspend () -> Unit)? = null): UserFriendlyError {
            if (error is UserFriendlyError) return error
            if (error is HttpStatusException) {
                if (error.code == 401) return authentication(retryAction)
                if (error.code >= 500) return serverError()
            }
            if (error is SocketTimeoutException) return timeout(retryAction)
            if (error is IOException) return network(retryAction)
            return UserFriendlyError(
                title = "Something Went Wrong",
                message = "We encountered an unexpected problem. Please try again.",
                icon = ErrorIcon.WARNING,
                recoveryAction = retryAction?.let { RecoveryAction.Retry(it) },
            )
        }
    }
}
