package com.okta.android.samples.authenticator.ui.loggedin

import com.okta.idx.kotlin.dto.TokenResponse

/**
 * User details post authentication that is exposed to the UI
 */
data class LoggedInUserView(
    val tokens: TokenResponse,
)

internal object LoggedInUserModel {
    var _loggedInUserView: LoggedInUserView? = null
    val tokens: TokenResponse
        get() {
            return _loggedInUserView?.tokens!!
        }
}
