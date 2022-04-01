package com.okta.android.samples.authenticator.ui.loggedin

import com.okta.idx.kotlin.dto.TokenResponse

/**
 * User details post authentication that is exposed to the UI.
 */
data class LoggedInUserView(
    val tokens: TokenResponse,
)

internal object LoggedInUserModel {
    private var loggedInUserViewData: LoggedInUserView? = null
    var loggedInUserView: LoggedInUserView
        get() {
            return loggedInUserViewData!!
        }
        set(value) {
            loggedInUserViewData = value
        }
    val tokens: TokenResponse
        get() {
            return loggedInUserViewData?.tokens!!
        }
}
